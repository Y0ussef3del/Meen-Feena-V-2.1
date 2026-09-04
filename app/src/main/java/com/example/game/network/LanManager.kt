package com.example.game.network

import android.util.Log
import com.example.game.model.RoomState
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

object OnlineManager {
    private const val TAG = "OnlineManager"
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val broadcastDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val db: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance(
                "https://meen-feena-default-rtdb.europe-west1.firebasedatabase.app"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseDatabase", e)
            null
        }

    private var roomRef: DatabaseReference? = null
    private var stateListener: ValueEventListener? = null
    private var commandsListener: ChildEventListener? = null

    private var rtcSignalsRef: DatabaseReference? = null
    private var rtcSignalsListener: ChildEventListener? = null

    val incomingCommands = MutableSharedFlow<Pair<String, String>>(
        extraBufferCapacity = 1024
    )
    val isClientConnected = MutableStateFlow(false)
    val clientConnectionError = MutableStateFlow<String?>(null)

    val localDeviceId: String by lazy {
        UUID.randomUUID().toString().substring(0, 8)
    }

    private var currentRoomCode: String? = null

    fun startHost(hostName: String, roomCode: String) {
        try {
            stopAll()

            val database = db ?: run {
                clientConnectionError.value = "لم يتم تهيئة FirebaseDatabase"
                return
            }

            currentRoomCode = roomCode
            val ref = database.getReference("rooms").child(roomCode)
            roomRef = ref

            ref.onDisconnect().removeValue()

            val cmdRef = ref.child("commands")
            commandsListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    processCommandSnapshot(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
                override fun onChildRemoved(snapshot: DataSnapshot) = Unit
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

                override fun onCancelled(error: DatabaseError) {
                    clientConnectionError.value = "خطأ المضيف: ${error.message}"
                    Log.e(TAG, "Host command listener cancelled", error.toException())
                }
            }

            cmdRef.addChildEventListener(commandsListener!!)

            prepareRtcInbox(
                database = database,
                roomCode = roomCode,
                deviceId = localDeviceId
            ) {
                isClientConnected.value = true
                Log.d(TAG, "Host started successfully room=$roomCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting host", e)
            clientConnectionError.value = e.localizedMessage
            isClientConnected.value = false
        }
    }

    fun broadcastStateToClients(state: RoomState) {
        val ref = roomRef ?: return

        managerScope.launch(broadcastDispatcher) {
            try {
                val stateStr = state.toSharedJsonString()
                ref.child("state").setValue(stateStr)
                    .addOnFailureListener {
                        clientConnectionError.value = "فشل نشر حالة الغرفة: ${it.message}"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting state", e)
            }
        }
    }

    fun connectToHost(roomCode: String, playerName: String, deviceId: String = localDeviceId) {
        try {
            isClientConnected.value = false
            clientConnectionError.value = null
            stopAll()

            val database = db ?: run {
                clientConnectionError.value = "فشل تهيئة قاعدة البيانات"
                return
            }

            currentRoomCode = roomCode
            val ref = database.getReference("rooms").child(roomCode)
            roomRef = ref

            var hasJoined = false

            stateListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (!snapshot.exists()) {
                            if (!hasJoined) {
                                clientConnectionError.value = "رمز الغرفة غير صحيح أو أن الغرفة أُغلقت"
                                isClientConnected.value = false
                            } else {
                                incomingCommands.tryEmit(
                                    Pair(
                                        "HOST",
                                        JSONObject().apply {
                                            put("type", "HOST_DISCONNECTED")
                                        }.toString()
                                    )
                                )
                            }
                            return
                        }

                        if (!hasJoined) {
                            hasJoined = true

                            prepareRtcInbox(
                                database = database,
                                roomCode = roomCode,
                                deviceId = deviceId
                            ) {
                                isClientConnected.value = true
                                Log.d(TAG, "Connected to room successfully room=$roomCode")

                                ref.child("commands")
                                    .child("leave_$deviceId")
                                    .onDisconnect()
                                    .setValue(
                                        JSONObject().apply {
                                            put("type", "CLIENT_LEAVE")
                                            put("deviceId", deviceId)
                                        }.toString()
                                    )

                                val joinCommand = JSONObject().apply {
                                    put("type", "JOIN")
                                    put("playerName", playerName)
                                    put("deviceId", deviceId)
                                }.toString()

                                sendCommandToHost(joinCommand)
                            }
                        } else {
                            val stateJsonStr = snapshot.getValue(String::class.java)
                            if (!stateJsonStr.isNullOrEmpty()) {
                                incomingCommands.tryEmit(
                                    Pair(
                                        "HOST",
                                        JSONObject().apply {
                                            put("type", "STATE_UPDATE")
                                            put("data", stateJsonStr)
                                        }.toString()
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in state listener", e)
                        clientConnectionError.value = "حدث خطأ غير متوقع: ${e.localizedMessage}"
                        isClientConnected.value = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    clientConnectionError.value = "فشل الاتصال: ${error.message}"
                    isClientConnected.value = false
                    Log.e(TAG, "State listener cancelled", error.toException())
                }
            }

            ref.child("state").addValueEventListener(stateListener!!)
        } catch (e: Exception) {
            clientConnectionError.value = "خطأ في الاتصال: ${e.localizedMessage}"
            isClientConnected.value = false
            Log.e(TAG, "connectToHost failed", e)
        }
    }

    private fun prepareRtcInbox(
        database: FirebaseDatabase,
        roomCode: String,
        deviceId: String,
        afterReady: () -> Unit
    ) {
        val deviceRef = database
            .getReference("rooms")
            .child(roomCode)
            .child("rtc_signals")
            .child(deviceId)

        currentRoomCode = roomCode
        deviceRef.onDisconnect().removeValue()

        deviceRef.removeValue()
            .addOnSuccessListener {
                listenToRtcSignals(roomCode, deviceId)
                afterReady()
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to clear RTC inbox device=$deviceId", error)
                listenToRtcSignals(roomCode, deviceId)
                afterReady()
            }
    }

    private fun listenToRtcSignals(roomCode: String, deviceId: String) {
        val database = db ?: return
        rtcSignalsListener?.let { rtcSignalsRef?.removeEventListener(it) }

        val ref = database
            .getReference("rooms")
            .child(roomCode)
            .child("rtc_signals")
            .child(deviceId)

        rtcSignalsRef = ref

        rtcSignalsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                processRtcSignalSnapshot(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "RTC signal listener cancelled", error.toException())
            }
        }

        ref.addChildEventListener(rtcSignalsListener!!)
        Log.d(TAG, "RTC signaling listener ready room=$roomCode device=$deviceId")
    }

    fun sendRtcSignal(targetId: String, jsonString: String) {
        if (targetId.isBlank() || jsonString.isBlank()) return

        val database = db ?: return
        val code = currentRoomCode ?: return

        managerScope.launch(Dispatchers.IO) {
            try {
                val signalRef = database
                    .getReference("rooms")
                    .child(code)
                    .child("rtc_signals")
                    .child(targetId)

                val key = signalRef.push().key ?: UUID.randomUUID().toString()
                signalRef.child(key).setValue(jsonString)
                    .addOnSuccessListener {
                        Log.d(TAG, "RTC signal sent target=$targetId")
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Failed to send RTC signal target=$targetId", error)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending RTC signal target=$targetId", e)
            }
        }
    }

    fun sendCommandToHost(jsonString: String) {
        val ref = roomRef ?: return
        if (jsonString.isBlank()) return

        managerScope.launch(Dispatchers.IO) {
            try {
                val cmdKey = ref.child("commands").push().key ?: UUID.randomUUID().toString()
                ref.child("commands").child(cmdKey).setValue(jsonString)
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Failed to send command to host", error)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command", e)
            }
        }
    }

    fun disconnectFromHost() {
        roomRef?.let {
            sendCommandToHost(
                JSONObject().apply {
                    put("type", "CLIENT_LEAVE")
                    put("deviceId", localDeviceId)
                }.toString()
            )
        }
        stopAll()
    }

    fun stopHost() {
        try {
            val database = db
            val code = currentRoomCode
            if (database != null && code != null) {
                val ref = database.getReference("rooms").child(code)
                ref.onDisconnect().cancel()
                ref.removeValue()
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Failed to remove room", error)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping host", e)
        }
        stopAll()
    }

    private fun processCommandSnapshot(snapshot: DataSnapshot) {
        try {
            val command = snapshot.getValue(String::class.java) ?: return
            if (command.isBlank()) return

            val jsonObj = JSONObject(command)
            val senderId = jsonObj.optString("deviceId", snapshot.key ?: "")

            if (incomingCommands.tryEmit(Pair(senderId, command))) {
                snapshot.ref.removeValue().addOnFailureListener { error ->
                    Log.w(TAG, "Failed to remove processed command", error)
                }
            } else {
                Log.w(TAG, "Command buffer full; keeping command key=${snapshot.key}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command snapshot", e)
        }
    }

    private fun processRtcSignalSnapshot(snapshot: DataSnapshot) {
        try {
            val signal = snapshot.getValue(String::class.java) ?: return
            if (signal.isBlank()) return

            val jsonObj = JSONObject(signal)
            val senderId = jsonObj.optString("senderId", "")
            val targetId = jsonObj.optString("targetId", "")

            if (targetId.isNotBlank() && targetId != localDeviceId) {
                snapshot.ref.removeValue()
                return
            }

            if (incomingCommands.tryEmit(Pair(senderId, signal))) {
                snapshot.ref.removeValue().addOnFailureListener { error ->
                    Log.w(TAG, "Failed to remove processed RTC signal", error)
                }
            } else {
                Log.w(TAG, "RTC signal buffer full; keeping signal key=${snapshot.key}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing RTC signal", e)
        }
    }

    private fun stopAll() {
        try {
            val ref = roomRef
            if (ref != null) {
                stateListener?.let { ref.child("state").removeEventListener(it) }
                commandsListener?.let { ref.child("commands").removeEventListener(it) }
            }
            rtcSignalsListener?.let { rtcSignalsRef?.removeEventListener(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing Firebase listeners", e)
        } finally {
            stateListener = null
            commandsListener = null
            rtcSignalsListener = null
            rtcSignalsRef = null
            roomRef = null
            currentRoomCode = null
            isClientConnected.value = false
        }
    }
}