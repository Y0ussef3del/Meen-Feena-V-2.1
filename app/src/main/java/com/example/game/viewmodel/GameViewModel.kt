package com.example.game.viewmodel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.data.CaseRepository
import com.example.game.model.*
import com.example.game.network.OnlineManager
import com.example.game.network.WebRtcManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "GameViewModel"

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    val myPlayerId = MutableStateFlow(OnlineManager.localDeviceId)
    val myPlayerName = MutableStateFlow("مكافح الجريمة")

    private val _completedCaseTitles = MutableStateFlow<Set<String>>(emptySet())
    val completedCaseTitles: StateFlow<Set<String>> = _completedCaseTitles.asStateFlow()

    val newLobbyPlayerName = MutableStateFlow("")
    private var timerJob: Job? = null

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

    private val webRtcManager = WebRtcManager(application)
    val voiceChatStatus: StateFlow<String> = webRtcManager.voiceStatus
    val isMicMuted = MutableStateFlow(false)
    val mutedPlayersState = MutableStateFlow<Set<String>>(emptySet())
    private var isVoiceChatActive = false

    companion object {
        const val MAX_HEARTS = 3
    }

    init {
        try {
            CaseRepository.init(application)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing CaseRepository", e)
        }
        val savedCompleted = sharedPreferences.getStringSet("completed_case_titles", emptySet()) ?: emptySet()
        _completedCaseTitles.value = savedCompleted

        webRtcManager.setMyPlayerId(myPlayerId.value)
        setupWebRtcCallbacks()
        setupOnlineListeners()
        checkAndResetDailyHearts()
        loadRewardedAdInternal()
        loadInterstitialAdInternal()

        viewModelScope.launch {
            OnlineManager.clientConnectionError.collect { error ->
                if (!error.isNullOrEmpty()) {
                    _roomState.update { current ->
                        if (current.mode == "ONLINE" && current.hostId != myPlayerId.value) {
                            RoomState(settings = current.settings)
                        } else current
                    }
                }
            }
        }

        viewModelScope.launch {
            var lastVolume: Float? = null
            roomState.collect { state ->
                if (lastVolume != state.settings.volume) {
                    lastVolume = state.settings.volume
                    try {
                        MysteryAudioPlayer.setVolume(state.settings.volume)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting volume", e)
                    }
                }
            }
        }

        viewModelScope.launch {
            roomState.collect { state ->
                if (state.mode == "ONLINE" && state.phase == GamePhase.DISCUSSION) {
                    startVoiceChatIfPermitted()
                } else {
                    if (isVoiceChatActive) {
                        isVoiceChatActive = false
                        try {
                            webRtcManager.stopVoiceChat()
                        } catch (e: Exception) {
                            Log.e(TAG, "Prevented crash during phase change", e)
                        }
                    }
                }
            }
        }
    }

    private fun setupWebRtcCallbacks() {
        webRtcManager.onIceCandidateGenerated = { targetId, candidate ->
            val json = JSONObject().apply {
                put("type", "WEBRTC_ICE")
                put("senderId", myPlayerId.value)
                put("targetId", targetId)
                put("sdpMid", candidate.sdpMid ?: "")
                put("sdpMLineIndex", candidate.sdpMLineIndex)
                put("candidate", candidate.sdp)
            }.toString()
            OnlineManager.sendRtcSignal(targetId, json)
        }

        webRtcManager.onSdpGenerated = { targetId, sdp ->
            val typeStr = if (sdp.type == org.webrtc.SessionDescription.Type.OFFER) "WEBRTC_OFFER" else "WEBRTC_ANSWER"
            val json = JSONObject().apply {
                put("type", typeStr)
                put("senderId", myPlayerId.value)
                put("targetId", targetId)
                put("sdp", sdp.description)
            }.toString()
            OnlineManager.sendRtcSignal(targetId, json)
        }
    }

    fun startVoiceChatIfPermitted() {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.phase == GamePhase.DISCUSSION) {
            val hasPermission = ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                webRtcManager.setMyPlayerId(myPlayerId.value)
                val remoteIds = state.players.map { it.id }

                if (!isVoiceChatActive) {
                    isVoiceChatActive = true
                    webRtcManager.startVoiceChat(state.roomId, myPlayerId.value, remoteIds)
                } else {
                    webRtcManager.syncVoiceConnections(remoteIds)
                }
            }
        }
    }

    fun toggleSelfMic() {
        val muted = webRtcManager.toggleSelfMic()
        isMicMuted.value = muted
    }

    fun toggleMutePlayer(playerId: String) {
        webRtcManager.toggleMutePlayer(playerId)
        val mutedSet = mutedPlayersState.value.toMutableSet()
        if (webRtcManager.isPlayerMuted(playerId)) {
            mutedSet.add(playerId)
        } else {
            mutedSet.remove(playerId)
        }
        mutedPlayersState.value = mutedSet
    }

    fun isPlayerMuted(playerId: String): Boolean {
        return webRtcManager.isPlayerMuted(playerId)
    }

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalHeartsCount(): Int {
        return sharedPreferences.getInt("hearts_count", MAX_HEARTS)
    }

    fun saveLocalHeartsCount(count: Int) {
        val safeCount = count.coerceIn(0, MAX_HEARTS)
        sharedPreferences.edit().putInt("hearts_count", safeCount).apply()
        _roomState.update { it.copy(heartsCount = safeCount) }
    }

    private fun checkAndResetDailyHearts() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastPlayDate = sharedPreferences.getString("last_play_date", "")

        if (lastPlayDate.isNullOrEmpty() || todayStr != lastPlayDate) {
            sharedPreferences.edit()
                .putString("last_play_date", todayStr)
                .putInt("hearts_count", MAX_HEARTS)
                .apply()
        }
        _roomState.update { it.copy(heartsCount = getLocalHeartsCount()) }
    }

    fun hasHeartsToPlay(): Boolean {
        checkAndResetDailyHearts()
        return getLocalHeartsCount() > 0
    }

    fun consumeHeart(): Boolean {
        val currentHearts = getLocalHeartsCount()
        return if (currentHearts > 0) {
            saveLocalHeartsCount(currentHearts - 1)
            true
        } else {
            false
        }
    }

    fun loadRewardedAdInternal() {
        if (rewardedAd != null || isAdLoading || !isNetworkAvailable(getApplication())) {
            return
        }
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(getApplication(), "ca-app-pub-6722529223110069/2125092694", adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isAdLoading = false
                }
            })
    }

    fun showAdToEarnHeart(activity: Activity, onFinished: (Boolean) -> Unit) {
        if (activity.isFinishing || activity.isDestroyed || !isNetworkAvailable(activity)) {
            onFinished(false)
            return
        }

        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAdInternal()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewardedAdInternal()
                    onFinished(false)
                }
            }
            ad.show(activity) { _ ->
                val currentHearts = getLocalHeartsCount()
                saveLocalHeartsCount(currentHearts + 1)
                onFinished(true)
            }
        } ?: run {
            val currentHearts = getLocalHeartsCount()
            saveLocalHeartsCount(currentHearts + 1)
            loadRewardedAdInternal()
            onFinished(true)
        }
    }

    fun loadInterstitialAdInternal() {
        if (interstitialAd != null || isInterstitialLoading || !isNetworkAvailable(getApplication())) {
            return
        }
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            getApplication(),
            "ca-app-pub-6722529223110069/3139787314",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed || !isNetworkAvailable(activity)) {
            onAdDismissed()
            return
        }

        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAdInternal()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAdInternal()
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } ?: run {
            loadInterstitialAdInternal()
            onAdDismissed()
        }
    }

    fun checkHeartsAndProceed(activity: Activity, onNoInternet: () -> Unit, onAccessGranted: () -> Unit) {
        if (hasHeartsToPlay()) {
            onAccessGranted()
        } else {
            showAdToEarnHeart(activity) { success ->
                if (success) {
                    onAccessGranted()
                } else {
                    onNoInternet()
                }
            }
        }
    }

    fun playButtonClick() { safePlaySound { MysteryAudioPlayer.playSelection() } }
    fun playSelection() { safePlaySound { MysteryAudioPlayer.playSelection() } }
    fun playSuccess() { safePlaySound { MysteryAudioPlayer.playSuccess() } }
    fun playError() { safePlaySound { MysteryAudioPlayer.playError() } }
    fun playWarning() { safePlaySound { MysteryAudioPlayer.playWarning() } }
    fun playVoteSound() { safePlaySound { MysteryAudioPlayer.playVote() } }
    fun playTransitionSound() { safePlaySound { MysteryAudioPlayer.playTransition() } }
    fun playRevealSound() { safePlaySound { MysteryAudioPlayer.playReveal() } }

    private inline fun safePlaySound(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {}
    }

    private fun setupOnlineListeners() {
        viewModelScope.launch {
            OnlineManager.incomingCommands.collect { (sourceId, msg) ->
                handleIncomingOnlineMessage(sourceId, msg)
            }
        }
    }

    private fun handleIncomingOnlineMessage(source: String, msg: String) {
        try {
            val json = JSONObject(msg)
            val type = json.optString("type")
            when (type) {
                "JOIN" -> {
                    val pName = json.optString("playerName", "لاعب")
                    val deviceId = json.optString("deviceId", UUID.randomUUID().toString())
                    addOnlineClientPlayer(deviceId, pName)
                }
                "REVEAL_SECRET" -> {
                    val playerId = json.optString("playerId", "")
                    handlePlayerRevealedRole(playerId)
                }
                "VOTE" -> {
                    val voterId = json.optString("voterId", "")
                    val targetId = json.optString("targetId", "")
                    if (voterId.isNotEmpty() && targetId.isNotEmpty()) castVote(voterId, targetId)
                }
                "JURY_VOTE" -> {
                    val voterId = json.optString("voterId", "")
                    val targetId = json.optString("targetId", "")
                    if (voterId.isNotEmpty() && targetId.isNotEmpty()) castJuryVote(voterId, targetId)
                }
                "CLIENT_LEAVE" -> {
                    val deviceId = json.optString("deviceId", "")
                    if (deviceId.isNotEmpty()) {
                        webRtcManager.closePeerConnection(deviceId)
                        removePlayerFromLobby(deviceId)
                    }
                }
                "STATE_UPDATE" -> {
                    val stateJsonStr = json.optString("data", "")
                    if (stateJsonStr.isNotEmpty()) {
                        val updatedState = RoomState.fromSharedJsonString(stateJsonStr)
                        _roomState.update { current -> updatedState.copy(roomId = current.roomId) }
                    }
                }
                "HOST_DISCONNECTED" -> {
                    OnlineManager.disconnectFromHost()
                    resetToMainMenu()
                }
                "WEBRTC_OFFER" -> {
                    val senderId = json.optString("senderId", source)
                    val sdp = json.optString("sdp", "")
                    if (sdp.isNotEmpty()) {
                        webRtcManager.setMyPlayerId(myPlayerId.value)
                        webRtcManager.handleOffer(senderId, sdp)
                    }
                }
                "WEBRTC_ANSWER" -> {
                    val senderId = json.optString("senderId", source)
                    val sdp = json.optString("sdp", "")
                    if (sdp.isNotEmpty()) {
                        webRtcManager.setMyPlayerId(myPlayerId.value)
                        webRtcManager.handleAnswer(senderId, sdp)
                    }
                }
                "WEBRTC_ICE" -> {
                    val senderId = json.optString("senderId", source)
                    val sdpMid = json.optString("sdpMid", "")
                    val sdpMLineIndex = json.optInt("sdpMLineIndex", 0)
                    val candidate = json.optString("candidate", "")
                    if (candidate.isNotEmpty()) {
                        webRtcManager.handleCandidate(senderId, sdpMid, sdpMLineIndex, candidate)
                    }
                }
            }
        } catch (e: Exception) {}
    }

    fun setupPassAndPlayGame() {
        stopTimer()
        val currentSettings = _roomState.value.settings
        val caseToKeep = _roomState.value.currentCase

        _roomState.value = RoomState(
            roomId = "PASS_AND_PLAY_ROOM",
            mode = "PASS_AND_PLAY",
            hostId = "LOCAL_HOST",
            phase = GamePhase.LOBBY,
            players = listOf(),
            settings = currentSettings,
            heartsCount = getLocalHeartsCount(),
            currentCase = caseToKeep
        )
        myPlayerId.value = "p1"
    }

    fun addLocalLobbyPlayer(name: String) {
        if (name.isBlank()) return
        playSelection()
        _roomState.update { current ->
            if (current.players.size >= 6) return@update current
            val currentPlayers = current.players.toMutableList()
            val newId = "p${currentPlayers.size + 1}"
            currentPlayers.add(Player(newId, name, avatarId = (currentPlayers.size % 6) + 1))
            current.copy(players = currentPlayers)
        }
    }

    fun removePlayerFromLobby(id: String) {
        playWarning()
        _roomState.update { current ->
            val updatedPlayers = current.players.filter { it.id != id }
            val updatedState = current.copy(players = updatedPlayers)
            if (updatedState.mode == "ONLINE") {
                OnlineManager.broadcastStateToClients(updatedState)
            }
            updatedState
        }
    }

    fun startLanHost(hostPlayerName: String) {
        startOnlineHost(hostPlayerName)
    }

    fun startOnlineHost(hostPlayerName: String) {
        stopTimer()
        playSuccess()
        val deviceId = OnlineManager.localDeviceId
        myPlayerId.value = deviceId
        myPlayerName.value = hostPlayerName
        webRtcManager.setMyPlayerId(deviceId)
        val currentSettings = _roomState.value.settings
        val roomCode = (kotlin.random.Random.nextInt(90000) + 10000).toString()
        val newState = RoomState(
            roomId = roomCode,
            mode = "ONLINE",
            hostId = deviceId,
            phase = GamePhase.LOBBY,
            players = listOf(Player(deviceId, hostPlayerName, avatarId = 1)),
            settings = currentSettings,
            heartsCount = getLocalHeartsCount()
        )
        _roomState.value = newState
        OnlineManager.startHost(hostPlayerName, roomCode)
        OnlineManager.broadcastStateToClients(newState)
    }

    fun joinLanHostByCode(roomCode: String, playerName: String): Boolean {
        if (roomCode.length != 5) return false
        joinOnlineRoom(roomCode, playerName)
        return true
    }

    fun joinLanHost(ip: String, playerName: String) {
        joinOnlineRoom(ip, playerName)
    }

    fun joinOnlineRoom(roomCode: String, playerName: String) {
        stopTimer()
        playSelection()
        val deviceId = OnlineManager.localDeviceId
        myPlayerId.value = deviceId
        myPlayerName.value = playerName
        webRtcManager.setMyPlayerId(deviceId)
        val currentSettings = _roomState.value.settings
        _roomState.value = RoomState(roomId = roomCode, mode = "ONLINE", phase = GamePhase.LOBBY, settings = currentSettings)
        OnlineManager.connectToHost(roomCode, playerName, deviceId)
    }

    private fun addOnlineClientPlayer(deviceId: String, name: String) {
        _roomState.update { current ->
            val currentPlayers = current.players.toMutableList()
            val existingIndex = currentPlayers.indexOfFirst { it.id == deviceId }
            if (existingIndex >= 0) {
                currentPlayers[existingIndex] = currentPlayers[existingIndex].copy(isConnected = true)
            } else {
                if (currentPlayers.size >= 6) return@update current
                currentPlayers.add(Player(deviceId, name, avatarId = (currentPlayers.size % 6) + 1))
            }
            val updatedState = current.copy(players = currentPlayers)
            OnlineManager.broadcastStateToClients(updatedState)
            updatedState
        }
    }

    fun startInvestigationGame() {
        executeStartInvestigationGame()
    }

    fun startInvestigationGameWithActivity(activity: Activity, onNoInternet: () -> Unit) {
        checkHeartsAndProceed(activity, onNoInternet) {
            executeStartInvestigationGame()
        }
    }

    private fun executeStartInvestigationGame() {
        if (!consumeHeart()) {
            playError()
            return
        }

        val state = _roomState.value
        val playersCount = state.players.size
        if (playersCount < 4 || playersCount > 6) {
            playError()
            return
        }
        playTransitionSound()

        val selectedCase = state.currentCase
            ?: CaseRepository.getUniqueCase(_completedCaseTitles.value, playersCount)

        var updatedPlayers = state.players
        selectedCase?.let { case ->
            val newCompletedSet = _completedCaseTitles.value + case.title
            _completedCaseTitles.value = newCompletedSet
            sharedPreferences.edit().putStringSet("completed_case_titles", newCompletedSet).apply()

            val shuffledCharacters = case.characters.shuffled()
            updatedPlayers = state.players.mapIndexed { index, player ->
                val assignedCharacter = shuffledCharacters.getOrNull(index)
                val isPlayerMafia = assignedCharacter?.isMafia == true
                player.copy(
                    isMafia = isPlayerMafia,
                    character = assignedCharacter,
                    isAlive = true,
                    isConnected = true
                )
            }
        } ?: run {
            return
        }

        _roomState.update { current ->
            val newState = current.copy(
                phase = GamePhase.ROLE_REVEAL,
                players = updatedPlayers,
                currentCase = selectedCase,
                currentEvidenceIndex = 0,
                activePassPlayerIndex = 0,
                rulesRevealed = false,
                votes = emptyMap(),
                juryVotes = emptyMap(),
                winnerSide = ""
            )
            if (newState.mode == "ONLINE") {
                OnlineManager.broadcastStateToClients(newState)
            }
            newState
        }
    }

    fun revealNextPassPlayerSecrets() {
        playRevealSound()
        _roomState.update { it.copy(rulesRevealed = true) }
    }

    fun confirmSecretsRevealed() {
        playSelection()
        val state = _roomState.value
        if (state.mode == "PASS_AND_PLAY") {
            val nextIndex = state.activePassPlayerIndex + 1
            if (nextIndex < state.players.size) {
                _roomState.update { it.copy(activePassPlayerIndex = nextIndex, rulesRevealed = false) }
            } else {
                transitionToPhase(GamePhase.CASE_INTRO)
            }
        } else {
            if (state.hostId == myPlayerId.value) {
                transitionToPhase(GamePhase.CASE_INTRO)
            }
        }
    }

    private fun handlePlayerRevealedRole(playerId: String) {}

    fun skipRoleRevealToCaseIntro() {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
        playTransitionSound()
        transitionToPhase(GamePhase.CASE_INTRO)
    }

    fun startCaseInvestigationIntro() {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
        playTransitionSound()
        _roomState.update { it.copy(currentEvidenceIndex = 0) }
        transitionToPhase(GamePhase.EVIDENCE_ROUND)
    }

    fun advanceFromEvidenceToDiscussion() {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
        playTransitionSound()
        transitionToPhase(GamePhase.DISCUSSION)
        startTimer(_roomState.value.settings.discussionTimeMinutes * 60) {
            advanceFromDiscussionToVoting()
        }
    }

    fun advanceFromDiscussionToVoting() {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
        stopTimer()
        playTransitionSound()
        val aliveCount = state.players.count { it.isAlive }

        if (aliveCount == 2) {
            _roomState.update { current ->
                val newState = current.copy(
                    juryVotes = emptyMap(),
                    phase = GamePhase.JURY_ROUND
                )
                if (newState.mode == "ONLINE") {
                    OnlineManager.broadcastStateToClients(newState)
                }
                newState
            }
            startTimer(state.settings.votingTimeMinutes * 60) {
                resolveJuryVotingTally()
            }
        } else {
            val firstAliveIndex = state.players.indexOfFirst { it.isAlive }
            _roomState.update { current ->
                val newState = current.copy(
                    votes = emptyMap(),
                    activePassPlayerIndex = if (firstAliveIndex != -1) firstAliveIndex else 0,
                    phase = GamePhase.VOTING
                )
                if (newState.mode == "ONLINE") {
                    OnlineManager.broadcastStateToClients(newState)
                }
                newState
            }
            startTimer(state.settings.votingTimeMinutes * 60) {
                resolveVotingTally()
            }
        }
    }

    fun submitVote(targetId: String) {
        playVoteSound()
        val state = _roomState.value
        val voterId = myPlayerId.value
        if (state.mode == "PASS_AND_PLAY") {
            val currentVoter = state.players.getOrNull(state.activePassPlayerIndex) ?: return
            val newVotes = state.votes.toMutableMap()
            newVotes[currentVoter.id] = targetId
            var nextIndex = state.activePassPlayerIndex + 1
            while (nextIndex < state.players.size && !state.players[nextIndex].isAlive) {
                nextIndex++
            }
            if (nextIndex < state.players.size) {
                _roomState.update { it.copy(votes = newVotes, activePassPlayerIndex = nextIndex) }
            } else {
                _roomState.update { it.copy(votes = newVotes) }
                resolveVotingTally()
            }
        } else {
            if (state.hostId == voterId) {
                castVote(voterId, targetId)
            } else {
                val cmd = JSONObject().apply {
                    put("type", "VOTE")
                    put("voterId", voterId)
                    put("targetId", targetId)
                }.toString()
                OnlineManager.sendCommandToHost(cmd)
            }
        }
    }

    private fun castVote(voterId: String, targetId: String) {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return

        var shouldResolveTally = false
        _roomState.update { current ->
            val voter = current.players.find { it.id == voterId }
            if (voter == null || !voter.isAlive) return@update current

            val newVotes = current.votes.toMutableMap()
            newVotes[voterId] = targetId
            val updatedState = current.copy(votes = newVotes)
            val alivePlayersCount = updatedState.players.count { it.isAlive }

            if (newVotes.size >= alivePlayersCount) {
                shouldResolveTally = true
            } else {
                OnlineManager.broadcastStateToClients(updatedState)
            }
            updatedState
        }

        if (shouldResolveTally) {
            resolveVotingTally()
        }
    }

    fun submitJuryVote(targetId: String) {
        playVoteSound()
        val state = _roomState.value
        val voterId = myPlayerId.value
        if (state.mode == "PASS_AND_PLAY") {
            val eliminatedPlayers = state.players.filter { !it.isAlive }
            val juryVoter = eliminatedPlayers.firstOrNull { it.id !in state.juryVotes.keys } ?: return
            val newJVotes = state.juryVotes.toMutableMap()
            newJVotes[juryVoter.id] = targetId
            _roomState.update { it.copy(juryVotes = newJVotes) }
            val nextJuryVoter = eliminatedPlayers.firstOrNull { it.id !in newJVotes.keys }
            if (nextJuryVoter == null) {
                resolveJuryVotingTally()
            }
        } else {
            if (state.hostId == voterId) {
                castJuryVote(voterId, targetId)
            } else {
                val cmd = JSONObject().apply {
                    put("type", "JURY_VOTE")
                    put("voterId", voterId)
                    put("targetId", targetId)
                }.toString()
                OnlineManager.sendCommandToHost(cmd)
            }
        }
    }

    private fun castJuryVote(voterId: String, targetId: String) {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return

        var shouldResolveTally = false
        _roomState.update { current ->
            val voter = current.players.find { it.id == voterId }
            if (voter == null || voter.isAlive) return@update current

            val newJVotes = current.juryVotes.toMutableMap()
            newJVotes[voterId] = targetId
            val updatedState = current.copy(juryVotes = newJVotes)
            val jurySize = updatedState.players.count { !it.isAlive }

            if (newJVotes.size >= jurySize) {
                shouldResolveTally = true
            } else {
                OnlineManager.broadcastStateToClients(updatedState)
            }
            updatedState
        }

        if (shouldResolveTally) {
            resolveJuryVotingTally()
        }
    }

    private fun resolveVotingTally() {
        stopTimer()
        val state = _roomState.value
        val voteCounts = mutableMapOf<String, Int>()
        state.votes.values.forEach { targetId ->
            voteCounts[targetId] = voteCounts.getOrDefault(targetId, 0) + 1
        }
        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        val tiedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()

        _roomState.update { current ->
            val newState = if (tiedPlayers.size >= 2 && voteCounts.isNotEmpty()) {
                val tiedNames = current.players.filter { it.id in tiedPlayers }.joinToString(" و ") { it.name }
                current.copy(
                    phase = GamePhase.VOTE_RESULT,
                    tiedVotePlayers = tiedPlayers,
                    lastEliminatedResult = "حصل تعادل في الأصوات بين ($tiedNames)! محدش خرج وهنعيد التصويت تاني."
                )
            } else {
                val targetId = tiedPlayers.firstOrNull()
                var eliminatedPlayer: Player? = null
                if (targetId != null) {
                    val currentPlayers = current.players.map { player ->
                        if (player.id == targetId) {
                            val updated = player.copy(isAlive = false)
                            eliminatedPlayer = updated
                            updated
                        } else {
                            player
                        }
                    }
                    val isMafia = eliminatedPlayer?.isMafia == true
                    val roleStr = if (isMafia) "مجرم" else "بريء"
                    val resultText = "${eliminatedPlayer?.name} خرج وكان $roleStr"
                    current.copy(
                        phase = GamePhase.VOTE_RESULT,
                        players = currentPlayers,
                        tiedVotePlayers = emptyList(),
                        lastEliminatedResult = resultText
                    )
                } else {
                    current.copy(
                        phase = GamePhase.VOTE_RESULT,
                        tiedVotePlayers = emptyList(),
                        lastEliminatedResult = "محدش صوّت ومحدش خرج!"
                    )
                }
            }

            if (newState.mode == "ONLINE") {
                OnlineManager.broadcastStateToClients(newState)
            }
            newState
        }
    }

    fun confirmVoteResultAndProceed() {
        val state = _roomState.value
        if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
        playTransitionSound()
        if (state.tiedVotePlayers.isNotEmpty()) {
            _roomState.update { current ->
                val updatedState = current.copy(
                    phase = GamePhase.VOTING,
                    votes = emptyMap()
                )
                if (updatedState.mode == "ONLINE") {
                    OnlineManager.broadcastStateToClients(updatedState)
                }
                updatedState
            }
            startTimer(state.settings.votingTimeMinutes * 60) {
                resolveVotingTally()
            }
        } else {
            val lastEliminated = state.players.find { !it.isAlive && state.lastEliminatedResult.contains(it.name) }
            checkEndgameConditions(lastEliminated)
        }
    }

    private fun resolveJuryVotingTally() {
        val state = _roomState.value
        val voteCounts = mutableMapOf<String, Int>()
        state.juryVotes.values.forEach { targetId ->
            voteCounts[targetId] = voteCounts.getOrDefault(targetId, 0) + 1
        }
        val sortedVotes = voteCounts.entries.sortedByDescending { it.value }
        val finalAccusedEntry = sortedVotes.firstOrNull()

        _roomState.update { current ->
            val newState = if (finalAccusedEntry != null) {
                val accusedId = finalAccusedEntry.key
                val accusedPlayer = current.players.find { it.id == accusedId }
                if (accusedPlayer != null && accusedPlayer.isMafia) {
                    current.copy(phase = GamePhase.ENDGAME, winnerSide = "INNOCENTS")
                } else {
                    current.copy(phase = GamePhase.ENDGAME, winnerSide = "MAFIA")
                }
            } else {
                current.copy(phase = GamePhase.ENDGAME, winnerSide = "MAFIA")
            }

            if (newState.mode == "ONLINE") {
                OnlineManager.broadcastStateToClients(newState)
            }
            newState
        }
    }

    private fun checkEndgameConditions(justEliminated: Player?) {
        _roomState.update { state ->
            val alivePlayers = state.players.filter { it.isAlive }
            val mafiaAlive = alivePlayers.count { it.isMafia }
            val innocentAlive = alivePlayers.size - mafiaAlive

            val newState = when {
                mafiaAlive == 0 -> {
                    state.copy(phase = GamePhase.ENDGAME, winnerSide = "INNOCENTS")
                }
                innocentAlive == 0 -> {
                    state.copy(phase = GamePhase.ENDGAME, winnerSide = "MAFIA")
                }
                alivePlayers.size == 2 -> {
                    val finalEvidenceIndex = ((state.currentCase?.evidenceList?.size)?.minus(1))?.coerceAtLeast(0) ?: 0
                    state.copy(
                        phase = GamePhase.EVIDENCE_ROUND,
                        currentEvidenceIndex = finalEvidenceIndex,
                        votes = emptyMap(),
                        juryVotes = emptyMap()
                    )
                }
                else -> {
                    val nextEvidenceIndex = (state.currentEvidenceIndex + 1) % (state.currentCase?.evidenceList?.size ?: 6)
                    state.copy(
                        phase = GamePhase.EVIDENCE_ROUND,
                        currentEvidenceIndex = nextEvidenceIndex,
                        votes = emptyMap()
                    )
                }
            }

            if (newState.mode == "ONLINE") {
                OnlineManager.broadcastStateToClients(newState)
            }
            newState
        }
    }

    private fun startTimer(seconds: Int, onComplete: () -> Unit) {
        stopTimer()
        _roomState.update { it.copy(timerTotalSeconds = seconds, timerSecondsLeft = seconds) }

        val isOnline = _roomState.value.mode == "ONLINE"
        val isHost = _roomState.value.hostId == myPlayerId.value

        if (!isOnline || isHost) {
            timerJob = viewModelScope.launch {
                var remaining = seconds
                while (remaining > 0 && isActive) {
                    delay(1000)
                    remaining--
                    val newRemaining = remaining
                    _roomState.update { current ->
                        current.copy(timerSecondsLeft = newRemaining)
                    }
                    if (isOnline && (remaining % 5 == 0 || remaining <= 10)) {
                        OnlineManager.broadcastStateToClients(_roomState.value)
                    }
                }
                if (isActive) onComplete()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun transitionToPhase(newPhase: GamePhase) {
        _roomState.update { current ->
            val updatedState = current.copy(phase = newPhase)
            if (updatedState.mode == "ONLINE") {
                OnlineManager.broadcastStateToClients(updatedState)
            }
            updatedState
        }
    }

    fun updateSettings(discussionMins: Int, votingMins: Int, music: Boolean, vol: Float) {
        val updatedSettings = GameSettings(
            discussionTimeMinutes = discussionMins,
            votingTimeMinutes = votingMins,
            isMusicEnabled = music,
            volume = vol
        )
        _roomState.update { current ->
            val updatedState = current.copy(settings = updatedSettings)
            if (updatedState.mode == "ONLINE") {
                OnlineManager.broadcastStateToClients(updatedState)
            }
            updatedState
        }
    }

    fun playAgain() {
        stopTimer()
        _roomState.update { it.copy(currentCase = null) }
        executeStartInvestigationGame()
    }

    fun playAgainWithActivity(activity: Activity, onNoInternet: () -> Unit) {
        stopTimer()
        checkHeartsAndProceed(activity, onNoInternet) {
            _roomState.update { it.copy(currentCase = null) }
            executeStartInvestigationGame()
        }
    }

    fun resetToMainMenu() {
        stopTimer()
        isVoiceChatActive = false
        try { webRtcManager.stopVoiceChat() } catch(e: Exception){}
        OnlineManager.stopHost()
        OnlineManager.disconnectFromHost()
        val currentSettings = _roomState.value.settings
        _roomState.value = RoomState(settings = currentSettings)
        loadRewardedAdInternal()
        loadInterstitialAdInternal()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        isVoiceChatActive = false
        webRtcManager.release()
        OnlineManager.stopHost()
        OnlineManager.disconnectFromHost()
    }

    fun selectCustomCase(customCase: Case?) {
        _roomState.update { currentState ->
            currentState.copy(
                currentCase = customCase
            )
        }
    }
}