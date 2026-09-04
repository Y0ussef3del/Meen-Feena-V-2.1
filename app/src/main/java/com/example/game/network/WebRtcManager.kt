package com.example.game.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class WebRtcManager(
    private val context: Context,
    var onIceCandidateGenerated: ((targetId: String, candidate: IceCandidate) -> Unit)? = null,
    var onSdpGenerated: ((targetId: String, sdp: SessionDescription) -> Unit)? = null
) {

    private val tag = "WebRtcManager"
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val peerConnections = ConcurrentHashMap<String, PeerConnection>()
    private val pendingCandidates = ConcurrentHashMap<String, CopyOnWriteArrayList<IceCandidate>>()
    private val remoteAudioTracks = ConcurrentHashMap<String, AudioTrack>()
    private val mutedPlayers = CopyOnWriteArraySet<String>()

    private val reconnectJobs = ConcurrentHashMap<String, Job>()
    private val iceTimeoutJobs = ConcurrentHashMap<String, Job>()
    private val offerJobs = ConcurrentHashMap<String, Job>()

    private val makingOffer = ConcurrentHashMap<String, Boolean>()
    private val forceRelay = ConcurrentHashMap<String, Boolean>()

    private val _voiceStatus = MutableStateFlow("جاري تهيئة المحادثة الصوتية...")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    @Volatile
    private var pendingVoiceRemoteIds: List<String> = emptyList()

    private val pendingOffers = ConcurrentHashMap<String, String>()
    private val pendingAnswers = ConcurrentHashMap<String, String>()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var myPlayerId: String = ""
    private var isSelfMuted = false
    private var isBackgrounded = false

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var headsetReceiver: BroadcastReceiver? = null

    @Volatile
    private var cloudflareIceServers: List<PeerConnection.IceServer> = emptyList()

    @Volatile
    private var turnCredentialsExpiresAtMs: Long = 0L

    @Volatile
    private var turnCredentialsFetchInProgress = false

    private var turnRefreshJob: Job? = null
    private val turnLock = Any()

    private val turnCredentialsUrl = "https://meenfaena.youssefad888.workers.dev/turn-credentials"

    private val fallbackIceServers: List<PeerConnection.IceServer> by lazy {
        listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
        )
    }

    private fun currentIceServers(): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()
        servers.addAll(fallbackIceServers)
        if (cloudflareIceServers.isNotEmpty()) {
            servers.addAll(cloudflareIceServers)
        }
        return servers
    }

    private fun hasUsableTurnCredentials(): Boolean {
        return cloudflareIceServers.isNotEmpty() &&
                System.currentTimeMillis() + TURN_REFRESH_SAFETY_MS < turnCredentialsExpiresAtMs
    }

    private fun ensureTurnCredentials() {
        if (hasUsableTurnCredentials()) return

        synchronized(turnLock) {
            if (hasUsableTurnCredentials() || turnCredentialsFetchInProgress) return
            turnCredentialsFetchInProgress = true
        }

        _voiceStatus.value = "جاري جلب سيرفرات الصوت (TURN)..."

        managerScope.launch {
            val success = fetchCloudflareTurnCredentials()
            synchronized(turnLock) {
                turnCredentialsFetchInProgress = false
            }

            if (success) {
                _voiceStatus.value = "تم جلب سيرفرات الصوت بنجاح 🟢"
                withContext(Dispatchers.IO) {
                    updateExistingPeerConnectionsWithTurn()
                    syncVoiceConnectionsInternal(pendingVoiceRemoteIds)

                    val waitingOffers = pendingOffers.entries.toList()
                    waitingOffers.forEach { (remoteId, sdp) ->
                        if (pendingOffers.remove(remoteId, sdp)) {
                            handleOfferInternal(remoteId, sdp)
                        }
                    }

                    val waitingAnswers = pendingAnswers.entries.toList()
                    waitingAnswers.forEach { (remoteId, sdp) ->
                        if (pendingAnswers.remove(remoteId, sdp)) {
                            handleAnswer(remoteId, sdp)
                        }
                    }
                }
                scheduleTurnCredentialRefresh()
            } else {
                _voiceStatus.value = "تعذر الاتصال بـ TURN، جاري استخدام الاتصال المباشر (STUN)"
                withContext(Dispatchers.IO) {
                    syncVoiceConnectionsInternal(pendingVoiceRemoteIds)
                }
            }
        }
    }

    private fun fetchCloudflareTurnCredentials(): Boolean {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val url = java.net.URL(turnCredentialsUrl)
            connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TURN_HTTP_TIMEOUT_MS
                readTimeout = TURN_HTTP_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json")
            }

            val status = connection.responseCode
            val bodyStream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = bodyStream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) return false

            val json = JSONObject(body)
            val serversElement = json.opt("iceServers") ?: return false
            val parsedServers = parseIceServers(serversElement)

            if (parsedServers.isEmpty()) return false

            val ttlSeconds = json.optLong("ttl", DEFAULT_TURN_TTL_SECONDS.toLong())
                .coerceIn(MIN_TURN_TTL_SECONDS.toLong(), MAX_TURN_TTL_SECONDS.toLong())

            cloudflareIceServers = parsedServers
            turnCredentialsExpiresAtMs = System.currentTimeMillis() + ttlSeconds * 1000L
            true
        } catch (e: Exception) {
            Log.e(tag, "Error fetching TURN credentials", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseIceServers(jsonInput: Any): List<PeerConnection.IceServer> {
        val result = ArrayList<PeerConnection.IceServer>()
        val array = when (jsonInput) {
            is JSONArray -> jsonInput
            is JSONObject -> JSONArray().apply { put(jsonInput) }
            else -> return result
        }

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val urlsValue = item.opt("urls") ?: item.opt("url")

            val urlList = ArrayList<String>()
            when (urlsValue) {
                is JSONArray -> {
                    for (j in 0 until urlsValue.length()) {
                        val urlStr = urlsValue.optString(j).trim()
                        if (urlStr.isNotEmpty()) urlList.add(urlStr)
                    }
                }
                is String -> {
                    if (urlsValue.trim().isNotEmpty()) urlList.add(urlsValue.trim())
                }
            }

            if (urlList.isEmpty()) continue

            val username = item.optString("username", "")
            val credential = item.optString("credential", "")

            val builder = PeerConnection.IceServer.builder(urlList)
            if (username.isNotEmpty()) builder.setUsername(username)
            if (credential.isNotEmpty()) builder.setPassword(credential)

            // تطبيق شهادة TLS فقط إذا كان الرابط يبدأ بـ turns:
            if (urlList.any { it.startsWith("turns:", ignoreCase = true) }) {
                builder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE)
            }

            result.add(builder.createIceServer())
        }
        return result
    }

    private fun scheduleTurnCredentialRefresh() {
        turnRefreshJob?.cancel()
        val expiresAt = turnCredentialsExpiresAtMs
        turnRefreshJob = managerScope.launch {
            val delayMs = (expiresAt - System.currentTimeMillis() - TURN_REFRESH_SAFETY_MS)
                .coerceAtLeast(30_000L)
            delay(delayMs)
            synchronized(turnLock) { turnCredentialsFetchInProgress = false }
            ensureTurnCredentials()
        }
    }

    private fun updateExistingPeerConnectionsWithTurn() {
        if (!hasUsableTurnCredentials()) return
        peerConnections.forEach { (remoteId, pc) ->
            try {
                val config = buildRtcConfiguration(forceRelay = forceRelay[remoteId] == true)
                if (pc.setConfiguration(config)) {
                    pc.restartIce()
                    if (shouldInitiate(remoteId)) {
                        createOffer(remoteId, isIceRestart = true)
                    }
                } else {
                    rebuildWithRelay(remoteId)
                }
            } catch (e: Exception) {
                rebuildWithRelay(remoteId)
            }
        }
    }

    private fun buildRtcConfiguration(forceRelay: Boolean): PeerConnection.RTCConfiguration {
        return PeerConnection.RTCConfiguration(currentIceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            iceTransportsType = if (forceRelay) PeerConnection.IceTransportsType.RELAY else PeerConnection.IceTransportsType.ALL
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            iceCandidatePoolSize = 1
            keyType = PeerConnection.KeyType.ECDSA
            enableDscp = true
        }
    }

    companion object {
        private const val TURN_HTTP_TIMEOUT_MS = 15_000
        private const val DEFAULT_TURN_TTL_SECONDS = 3_600
        private const val MIN_TURN_TTL_SECONDS = 300
        private const val MAX_TURN_TTL_SECONDS = 86_400
        private const val TURN_REFRESH_SAFETY_MS = 60_000L
        private const val ICE_CONNECT_TIMEOUT_MS = 10_000L
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val RELAY_REBUILD_DELAY_MS = 500L
    }

    fun setMyPlayerId(id: String) {
        if (id.isNotBlank()) myPlayerId = id
    }

    @Synchronized
    fun initialize() {
        if (peerConnectionFactory != null && localAudioTrack != null) {
            localAudioTrack?.setEnabled(!isSelfMuted && !isBackgrounded)
            return
        }

        try {
            val initOptions = PeerConnectionFactory.InitializationOptions
                .builder(context.applicationContext)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)

            val options = PeerConnectionFactory.Options().apply { networkIgnoreMask = 0 }

            val isAecSupported = JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported()
            val isNsSupported = JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported()

            val adm = JavaAudioDeviceModule.builder(context.applicationContext)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setUseHardwareAcousticEchoCanceler(isAecSupported)
                .setUseHardwareNoiseSuppressor(isNsSupported)
                .setUseStereoInput(false)
                .setUseStereoOutput(false)
                .createAudioDeviceModule()

            audioDeviceModule = adm

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .createPeerConnectionFactory()

            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }

            localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
            localAudioTrack = peerConnectionFactory?.createAudioTrack(
                "LOCAL_AUDIO_${System.currentTimeMillis()}",
                localAudioSource
            )
            localAudioTrack?.setEnabled(!isSelfMuted && !isBackgrounded)

        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize WebRTC", e)
            _voiceStatus.value = "خطأ في تهيئة الصوت: ${e.localizedMessage}"
        }
    }

    fun startVoiceChat(roomId: String, myPlayerId: String, remotePlayerIds: List<String>) {
        this.myPlayerId = myPlayerId
        initialize()
        registerAudioRoutingListener()
        updateAudioRouting()
        syncVoiceConnections(remotePlayerIds)
        ensureTurnCredentials()
    }

    @Synchronized
    fun syncVoiceConnections(remotePlayerIds: List<String>) {
        if (myPlayerId.isBlank()) return

        val validRemoteIds = remotePlayerIds
            .asSequence()
            .filter { it.isNotBlank() && it != myPlayerId }
            .distinct()
            .toList()

        pendingVoiceRemoteIds = validRemoteIds
        syncVoiceConnectionsInternal(validRemoteIds)
        ensureTurnCredentials()
    }

    @Synchronized
    private fun syncVoiceConnectionsInternal(validRemoteIds: List<String>) {
        if (myPlayerId.isBlank()) return

        peerConnections.keys.toList().forEach { remoteId ->
            if (remoteId !in validRemoteIds) {
                closePeerConnection(remoteId)
            }
        }

        validRemoteIds.forEach { remoteId ->
            if (!peerConnections.containsKey(remoteId)) {
                createPeerConnection(remoteId, relayOnly = false)?.let {
                    forceRelay[remoteId] = false
                    if (shouldInitiate(remoteId)) {
                        createOffer(remoteId, isIceRestart = false)
                    }
                }
            }
        }
    }

    private fun shouldInitiate(remoteId: String): Boolean {
        return myPlayerId.isNotBlank() && remoteId.isNotBlank() && myPlayerId > remoteId
    }

    fun toggleSelfMic(): Boolean {
        isSelfMuted = !isSelfMuted
        localAudioTrack?.setEnabled(!isSelfMuted && !isBackgrounded)
        return isSelfMuted
    }

    fun setBackgrounded(background: Boolean) {
        isBackgrounded = background
        localAudioTrack?.setEnabled(!isSelfMuted && !isBackgrounded)
    }

    fun toggleMutePlayer(playerId: String) {
        if (mutedPlayers.contains(playerId)) {
            mutedPlayers.remove(playerId)
            remoteAudioTracks[playerId]?.setEnabled(true)
        } else {
            mutedPlayers.add(playerId)
            remoteAudioTracks[playerId]?.setEnabled(false)
        }
    }

    fun isPlayerMuted(playerId: String): Boolean = mutedPlayers.contains(playerId)

    private fun registerAudioRoutingListener() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (audioDeviceCallback == null) {
                audioDeviceCallback = object : AudioDeviceCallback() {
                    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                        updateAudioRouting()
                    }
                    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                        updateAudioRouting()
                    }
                }
                audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
            }
        } else {
            if (headsetReceiver == null) {
                headsetReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        updateAudioRouting()
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_HEADSET_PLUG)
                    addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                }
                ContextCompat.registerReceiver(
                    context, headsetReceiver, filter, ContextCompat.RECEIVER_EXPORTED
                )
            }
        }
    }

    // إصلاح توجيه الصوت: تشغيل السبيكر الخارجي كخيار افتراضي ممتاز بدلاً من سماعة الأذن الداخلية
    fun updateAudioRouting() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isMicrophoneMute = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val preferredExternal = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }

                val speaker = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }

                when {
                    preferredExternal != null -> audioManager.setCommunicationDevice(preferredExternal)
                    speaker != null -> audioManager.setCommunicationDevice(speaker)
                    else -> audioManager.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                val externalConnected = audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = !externalConnected
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to update audio routing", e)
        }
    }

    private fun resetAudioMode() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
                audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
                audioDeviceCallback = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false
                headsetReceiver?.let {
                    try { context.unregisterReceiver(it) } catch (_: Exception) {}
                }
                headsetReceiver = null
            }
        } catch (e: Exception) {}
    }

    @Synchronized
    private fun createPeerConnection(remoteId: String, relayOnly: Boolean): PeerConnection? {
        if (remoteId.isBlank()) return null
        if (peerConnections[remoteId] != null) return peerConnections[remoteId]

        if (peerConnectionFactory == null || localAudioTrack == null) {
            initialize()
        }

        val factory = peerConnectionFactory ?: return null
        val localTrack = localAudioTrack ?: return null
        val rtcConfig = buildRtcConfiguration(relayOnly)

        val pc = factory.createPeerConnection(
            rtcConfig,
            object : PeerConnectionAdapter() {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    if (candidate == null) return
                    onIceCandidateGenerated?.invoke(remoteId, candidate)
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    if (track is AudioTrack) {
                        track.setEnabled(true)
                        track.setVolume(1.0)
                        remoteAudioTracks[remoteId] = track

                        if (mutedPlayers.contains(remoteId)) {
                            track.setEnabled(false)
                        }

                        _voiceStatus.value = "تم استلام صوت اللاعب $remoteId بنجاح 🔊"

                        managerScope.launch(Dispatchers.Main) {
                            updateAudioRouting()
                        }
                    }
                }

                override fun onRenegotiationNeeded() {
                    val currentPc = peerConnections[remoteId]
                    if (shouldInitiate(remoteId) &&
                        currentPc?.signalingState() == PeerConnection.SignalingState.STABLE
                    ) {
                        createOffer(remoteId, isIceRestart = false)
                    }
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    when (state) {
                        PeerConnection.IceConnectionState.CHECKING,
                        PeerConnection.IceConnectionState.NEW -> {
                            _voiceStatus.value = "جاري الاتصال باللاعب $remoteId..."
                            scheduleIceTimeout(remoteId, pcRef = { peerConnections[remoteId] })
                        }
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            _voiceStatus.value = "متصل صوتياً مع الجميع 🟢"
                            iceTimeoutJobs.remove(remoteId)?.cancel()
                            reconnectJobs.remove(remoteId)?.cancel()
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            _voiceStatus.value = "انقطع الاتصال باللاعب $remoteId، جاري إعادة الاتصال..."
                            scheduleReconnect(remoteId)
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            _voiceStatus.value = "فشل الاتصال باللاعب $remoteId، جاري التحويل لـ Relay..."
                            iceTimeoutJobs.remove(remoteId)?.cancel()
                            rebuildWithRelay(remoteId)
                        }
                        else -> Unit
                    }
                }
            }
        ) ?: return null

        peerConnections[remoteId] = pc
        forceRelay[remoteId] = relayOnly
        ensureLocalTrackAttached(pc, localTrack)

        return pc
    }

    private fun ensureLocalTrackAttached(pc: PeerConnection, track: AudioTrack?) {
        if (track == null) return
        try {
            if (pc.senders.any { it.track()?.id() == track.id() }) return
            pc.addTrack(track, listOf("media_stream_${myPlayerId}"))
        } catch (e: Exception) {
            Log.e(tag, "Failed to attach local audio track", e)
        }
    }

    private fun scheduleIceTimeout(remoteId: String, pcRef: () -> PeerConnection?) {
        iceTimeoutJobs[remoteId]?.cancel()
        iceTimeoutJobs[remoteId] = managerScope.launch {
            delay(ICE_CONNECT_TIMEOUT_MS)
            val pc = pcRef()
            val state = pc?.iceConnectionState()
            if (state == PeerConnection.IceConnectionState.NEW || state == PeerConnection.IceConnectionState.CHECKING) {
                if (forceRelay[remoteId] != true) {
                    rebuildWithRelay(remoteId)
                } else {
                    pc?.restartIce()
                    delay(300)
                    createOffer(remoteId, isIceRestart = true)
                }
            }
        }
    }

    private fun scheduleReconnect(remoteId: String) {
        reconnectJobs[remoteId]?.cancel()
        reconnectJobs[remoteId] = managerScope.launch {
            delay(RECONNECT_DELAY_MS)
            val pc = peerConnections[remoteId]
            if (pc?.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED) {
                if (forceRelay[remoteId] != true) {
                    rebuildWithRelay(remoteId)
                } else {
                    pc.restartIce()
                    delay(300)
                    createOffer(remoteId, isIceRestart = true)
                }
            }
        }
    }

    private fun rebuildWithRelay(remoteId: String) {
        if (remoteId.isBlank()) return
        reconnectJobs.remove(remoteId)?.cancel()
        iceTimeoutJobs.remove(remoteId)?.cancel()
        offerJobs.remove(remoteId)?.cancel()

        managerScope.launch {
            delay(RELAY_REBUILD_DELAY_MS)
            forceRelay[remoteId] = true

            val pc = peerConnections[remoteId]
            if (pc != null) {
                try {
                    val config = buildRtcConfiguration(forceRelay = true)
                    pc.setConfiguration(config)
                    pc.restartIce()
                } catch (e: Exception) {
                    Log.e(tag, "Failed to apply relay configuration", e)
                }
                delay(200)
                createOffer(remoteId, isIceRestart = true)
            } else {
                createPeerConnection(remoteId, relayOnly = true)?.let {
                    delay(200)
                    createOffer(remoteId, isIceRestart = true)
                }
            }
        }
    }

    fun createOffer(remoteId: String, isIceRestart: Boolean = false) {
        if (remoteId.isBlank()) return
        if (!isIceRestart && !shouldInitiate(remoteId)) return

        val pc = peerConnections[remoteId] ?: createPeerConnection(
            remoteId, relayOnly = forceRelay[remoteId] == true
        ) ?: return

        ensureLocalTrackAttached(pc, localAudioTrack)

        if (makingOffer[remoteId] == true) return
        makingOffer[remoteId] = true

        offerJobs[remoteId]?.cancel()
        offerJobs[remoteId] = managerScope.launch {
            delay(50)

            val currentPc = peerConnections[remoteId]
            if (currentPc == null) {
                makingOffer[remoteId] = false
                return@launch
            }

            val constraints = MediaConstraints().apply {
                val state = currentPc.iceConnectionState()
                if (isIceRestart || state == PeerConnection.IceConnectionState.FAILED || state == PeerConnection.IceConnectionState.DISCONNECTED) {
                    mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
                }
            }

            currentPc.createOffer(object : SdpObserverAdapter() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) {
                        makingOffer[remoteId] = false
                        return
                    }

                    currentPc.setLocalDescription(object : SdpObserverAdapter() {
                        override fun onSetSuccess() {
                            makingOffer[remoteId] = false
                            onSdpGenerated?.invoke(remoteId, sdp)
                        }

                        override fun onSetFailure(error: String?) {
                            makingOffer[remoteId] = false
                        }
                    }, sdp)
                }

                override fun onCreateFailure(error: String?) {
                    makingOffer[remoteId] = false
                }
            }, constraints)
        }
    }

    fun handleOffer(remoteId: String, sdpDescription: String) {
        if (remoteId.isBlank() || sdpDescription.isBlank()) return
        handleOfferInternal(remoteId, sdpDescription)
    }

    private fun handleOfferInternal(remoteId: String, sdpDescription: String) {
        if (remoteId.isBlank() || sdpDescription.isBlank()) return

        if (peerConnectionFactory == null || localAudioTrack == null) {
            initialize()
        }
        updateAudioRouting()

        var pc = peerConnections[remoteId]
        if (pc != null && pc.iceConnectionState() == PeerConnection.IceConnectionState.CLOSED) {
            closePeerConnection(remoteId, keepRelayPreference = forceRelay[remoteId] == true)
            pc = null
        }

        if (pc == null) {
            pc = createPeerConnection(remoteId, relayOnly = forceRelay[remoteId] == true)
        }
        if (pc == null) return

        val offer = SessionDescription(SessionDescription.Type.OFFER, sdpDescription)

        pc.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                drainPendingCandidates(remoteId, pc)
                ensureLocalTrackAttached(pc, localAudioTrack)

                val constraints = MediaConstraints()
                pc.createAnswer(object : SdpObserverAdapter() {
                    override fun onCreateSuccess(answer: SessionDescription?) {
                        if (answer == null) return
                        pc.setLocalDescription(object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                onSdpGenerated?.invoke(remoteId, answer)
                            }
                        }, answer)
                    }
                }, constraints)
            }
        }, offer)
    }

    fun handleAnswer(remoteId: String, sdpDescription: String) {
        if (remoteId.isBlank() || sdpDescription.isBlank()) return

        val pc = peerConnections[remoteId]
        if (pc == null) {
            pendingAnswers[remoteId] = sdpDescription
            return
        }

        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdpDescription)
        pc.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                drainPendingCandidates(remoteId, pc)
                ensureLocalTrackAttached(pc, localAudioTrack)
            }
        }, answer)
    }

    fun handleCandidate(senderId: String, sdpMid: String, sdpMLineIndex: Int, candidateStr: String) {
        if (senderId.isBlank() || candidateStr.isBlank()) return

        val candidate = IceCandidate(if (sdpMid.isBlank()) null else sdpMid, sdpMLineIndex, candidateStr)
        val pc = peerConnections[senderId]

        if (pc == null || pc.remoteDescription == null) {
            pendingCandidates.getOrPut(senderId) { CopyOnWriteArrayList() }.add(candidate)
            return
        }

        try {
            pc.addIceCandidate(candidate)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add ICE candidate", e)
        }
    }

    private fun drainPendingCandidates(remoteId: String, pc: PeerConnection) {
        val candidates = pendingCandidates.remove(remoteId) ?: return
        candidates.forEach { candidate ->
            try {
                pc.addIceCandidate(candidate)
            } catch (e: Exception) {
                Log.e(tag, "Failed to drain ICE candidate", e)
            }
        }
    }

    fun closePeerConnection(remoteId: String) {
        closePeerConnection(remoteId, keepRelayPreference = false)
    }

    private fun closePeerConnection(remoteId: String, keepRelayPreference: Boolean) {
        reconnectJobs.remove(remoteId)?.cancel()
        iceTimeoutJobs.remove(remoteId)?.cancel()
        offerJobs.remove(remoteId)?.cancel()
        makingOffer.remove(remoteId)

        val pc = peerConnections.remove(remoteId)
        remoteAudioTracks.remove(remoteId)
        pendingCandidates.remove(remoteId)
        pendingOffers.remove(remoteId)

        if (!keepRelayPreference) {
            forceRelay.remove(remoteId)
        }

        try { pc?.close() } catch (e: Exception) {}
    }

    fun stopVoiceChat() {
        try {
            reconnectJobs.values.forEach { it.cancel() }
            reconnectJobs.clear()
            iceTimeoutJobs.values.forEach { it.cancel() }
            iceTimeoutJobs.clear()
            offerJobs.values.forEach { it.cancel() }
            offerJobs.clear()

            val pcs = peerConnections.values.toList()
            peerConnections.clear()
            pendingCandidates.clear()
            remoteAudioTracks.clear()
            mutedPlayers.clear()
            makingOffer.clear()
            forceRelay.clear()
            pendingOffers.clear()
            pendingAnswers.clear()
            pendingVoiceRemoteIds = emptyList()
            turnRefreshJob?.cancel()
            turnRefreshJob = null

            pcs.forEach { pc ->
                try { pc.close() } catch (e: Exception) {}
            }

            resetAudioMode()
            _voiceStatus.value = "تم إيقاف المحادثة الصوتية"
        } catch (e: Exception) {}
    }

    fun release() {
        stopVoiceChat()
        managerScope.cancel()

        try {
            localAudioTrack?.dispose()
            localAudioSource?.dispose()
            audioDeviceModule?.release()
        } catch (e: Exception) {}

        localAudioTrack = null
        localAudioSource = null
        audioDeviceModule = null
        peerConnectionFactory = null
    }
}

open class PeerConnectionAdapter : PeerConnection.Observer {
    override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
    override fun onIceCandidate(candidate: IceCandidate?) {}
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onAddStream(stream: MediaStream?) {}
    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onDataChannel(channel: DataChannel?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
    override fun onTrack(transceiver: RtpTransceiver?) {}
}

open class SdpObserverAdapter : SdpObserver {
    private val tag = "SdpObserverAdapter"
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {
        Log.e(tag, "SDP Creation Failure: $error")
    }
    override fun onSetFailure(error: String?) {
        Log.e(tag, "SDP Setting Failure: $error")
    }
}