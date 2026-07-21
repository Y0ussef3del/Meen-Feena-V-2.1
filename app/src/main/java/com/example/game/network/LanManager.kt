package com.example.game.network

import android.util.Log
import com.example.game.model.RoomState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.*

object LanManager {
    private const val TAG = "LanManager"
    private const val TCP_PORT = 8888
    private const val UDP_PORT = 8889

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var udpBroadcastSocket: DatagramSocket? = null
    @Volatile
    private var udpDiscoverySocket: DatagramSocket? = null

    private var tcpServerJob: Job? = null
    private var udpBroadcastJob: Job? = null
    private var udpDiscoveryJob: Job? = null

    @Volatile
    private var clientSocket: Socket? = null
    private var clientReadJob: Job? = null

    private val clientConnections = Collections.synchronizedList(mutableListOf<ClientConnection>())

    val discoveredHosts = MutableStateFlow<Map<String, String>>(emptyMap())
    val incomingCommands = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)

    val isClientConnected = MutableStateFlow(false)
    val clientConnectionError = MutableStateFlow<String?>(null)

    val localDeviceId: String = UUID.randomUUID().toString().substring(0, 8)

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val element = interfaces.nextElement()
                val addresses = element.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error getting IP", ex)
        }
        return "127.0.0.1"
    }

    fun startHost(hostName: String, roomCode: String) {
        stopAll()
        Log.d(TAG, "Starting Host at Port $TCP_PORT...")

        tcpServerJob = managerScope.launch {
            try {
                serverSocket = ServerSocket(TCP_PORT).apply {
                    reuseAddress = true
                }
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
                    val connection = ClientConnection(socket)
                    clientConnections.add(connection)
                    connection.startReading { clientIp, command ->
                        incomingCommands.tryEmit(Pair(clientIp, command))
                    }
                }
            } catch (e: Throwable) {
                if (e !is SocketException) {
                    Log.e(TAG, "TCP Server failed", e)
                }
            } finally {
                try { serverSocket?.close() } catch (_: Throwable) {}
                serverSocket = null
            }
        }

        udpBroadcastJob = managerScope.launch {
            try {
                val broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                    reuseAddress = true
                }
                udpBroadcastSocket = broadcastSocket
                val broadcastMsg = "WHO_AMONG_US_HOST|$hostName|${getLocalIpAddress()}|$roomCode"
                val packetData = broadcastMsg.toByteArray()

                while (isActive) {
                    try {
                        val address = InetAddress.getByName("255.255.255.255")
                        val packet = DatagramPacket(packetData, packetData.size, address, UDP_PORT)
                        broadcastSocket.send(packet)
                    } catch (e: Throwable) {
                        try {
                            val subnetAddr = getBroadcastAddress()
                            if (subnetAddr != null) {
                                val packet = DatagramPacket(packetData, packetData.size, subnetAddr, UDP_PORT)
                                broadcastSocket.send(packet)
                            }
                        } catch (inner: Throwable) {
                            Log.e(TAG, "UDP Broadcast fallback failed", inner)
                        }
                    }
                    delay(2000)
                }
            } catch (outer: Throwable) {
                Log.e(TAG, "UDP Socket creation failed", outer)
            } finally {
                try { udpBroadcastSocket?.close() } catch (_: Throwable) {}
                udpBroadcastSocket = null
            }
        }
    }

    private fun getBroadcastAddress(): InetAddress? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            for (interfaceAddress in networkInterface.interfaceAddresses) {
                val broadcast = interfaceAddress.broadcast
                if (broadcast != null) return broadcast
            }
        }
        return null
    }

    fun broadcastStateToClients(state: RoomState) {
        val stateStr = state.toSharedJsonString()
        val envelope = JSONObject().apply {
            put("type", "STATE_UPDATE")
            put("data", stateStr)
        }.toString()

        managerScope.launch {
            val snapshot = synchronized(clientConnections) { clientConnections.toList() }
            val deadConnections = mutableListOf<ClientConnection>()

            for (conn in snapshot) {
                if (!conn.write(envelope)) {
                    Log.d(TAG, "Removing dead connection: ${conn.ip}")
                    conn.close()
                    deadConnections.add(conn)
                }
            }

            if (deadConnections.isNotEmpty()) {
                clientConnections.removeAll(deadConnections)
            }
        }
    }

    fun stopHost() {
        Log.d(TAG, "Stopping host server...")
        stopAll()
    }

    fun startDiscovery() {
        discoveredHosts.value = emptyMap()
        stopDiscovery()

        udpDiscoveryJob = managerScope.launch {
            var ds: DatagramSocket? = null
            try {
                ds = DatagramSocket(UDP_PORT).apply {
                    reuseAddress = true
                }
                udpDiscoverySocket = ds
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    ds.receive(packet)
                    val rxStr = String(packet.data, 0, packet.length).trim()
                    if (rxStr.startsWith("WHO_AMONG_US_HOST")) {
                        val parts = rxStr.split("|")
                        val myIp = getLocalIpAddress()
                        if (parts.size >= 4) {
                            val name = parts[1]
                            val ip = parts[2]
                            val rCode = parts[3]
                            if (ip != myIp) {
                                val current = discoveredHosts.value.toMutableMap()
                                current[ip] = "$name|$rCode"
                                discoveredHosts.value = current
                            }
                        } else if (parts.size == 3) {
                            val name = parts[1]
                            val ip = parts[2]
                            if (ip != myIp) {
                                val current = discoveredHosts.value.toMutableMap()
                                current[ip] = "$name|99999"
                                discoveredHosts.value = current
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is SocketException) {
                    Log.e(TAG, "UDP Discovery error", e)
                }
            } finally {
                try { ds?.close() } catch (_: Exception) {}
                udpDiscoverySocket = null
            }
        }
    }

    fun stopDiscovery() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
        try { udpDiscoverySocket?.close() } catch (_: Exception) {}
        udpDiscoverySocket = null
    }

    fun connectToHost(hostIp: String, playerName: String, deviceId: String) {
        isClientConnected.value = false
        clientConnectionError.value = null

        disconnectFromHost()

        clientReadJob = managerScope.launch {
            var socket: Socket? = null
            try {
                Log.d(TAG, "Connecting to Host: $hostIp...")
                socket = Socket().apply {
                    connect(InetSocketAddress(hostIp, TCP_PORT), 4000)
                }
                clientSocket = socket
                isClientConnected.value = true

                val joinCommand = JSONObject().apply {
                    put("type", "JOIN")
                    put("playerName", playerName)
                    put("deviceId", deviceId)
                }.toString()

                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println(joinCommand)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    incomingCommands.emit(Pair("HOST", line))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client Connection error", e)
                clientConnectionError.value = e.localizedMessage ?: "فشل الاتصال بالغرفة المحلية"
                isClientConnected.value = false
            } finally {
                try { socket?.close() } catch (_: Exception) {}
                clientSocket = null
            }
        }
    }

    fun sendCommandToHost(jsonString: String) {
        managerScope.launch {
            val socket = clientSocket
            if (socket != null && isClientConnected.value) {
                try {
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(jsonString)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send command to host", e)
                }
            }
        }
    }

    fun disconnectFromHost() {
        Log.d(TAG, "Disconnecting from host...")
        clientReadJob?.cancel()
        clientReadJob = null
        try { clientSocket?.close() } catch (_: Exception) {}
        clientSocket = null
        isClientConnected.value = false
    }

    private fun stopAll() {
        tcpServerJob?.cancel()
        udpBroadcastJob?.cancel()
        udpDiscoveryJob?.cancel()
        clientReadJob?.cancel()

        tcpServerJob = null
        udpBroadcastJob = null
        udpDiscoveryJob = null
        clientReadJob = null

        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null

        try { udpBroadcastSocket?.close() } catch (_: Exception) {}
        udpBroadcastSocket = null

        try { udpDiscoverySocket?.close() } catch (_: Exception) {}
        udpDiscoverySocket = null

        synchronized(clientConnections) {
            clientConnections.forEach { it.close() }
            clientConnections.clear()
        }

        try { clientSocket?.close() } catch (_: Exception) {}
        clientSocket = null
        isClientConnected.value = false
    }

    private class ClientConnection(val socket: Socket) {
        val ip: String = socket.inetAddress?.hostAddress ?: ""
        private var writer: PrintWriter? = null
        private var reader: BufferedReader? = null
        private var bgJob: Job? = null

        fun startReading(onCommandReceived: (String, String) -> Unit) {
            bgJob = managerScope.launch {
                try {
                    writer = PrintWriter(socket.getOutputStream(), true)
                    reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    while (isActive) {
                        val line = reader?.readLine() ?: break
                        onCommandReceived(ip, line)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Connection lost for $ip")
                } finally {
                    close()
                }
            }
        }

        fun write(msg: String): Boolean {
            return try {
                val pr = writer ?: PrintWriter(socket.getOutputStream(), true).also { writer = it }
                pr.println(msg)
                !pr.checkError()
            } catch (e: Exception) {
                false
            }
        }

        fun close() {
            bgJob?.cancel()
            try { socket.close() } catch (_: Exception) {}
        }
    }
}