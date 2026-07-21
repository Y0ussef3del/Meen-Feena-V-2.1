package com.example.game.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.data.CaseRepository
import com.example.game.model.*
import com.example.game.network.LanManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
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
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "GameViewModel"

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    val myPlayerId = MutableStateFlow("")
    val myPlayerName = MutableStateFlow("مكافح الجريمة")

    private val completedCaseTitles = Collections.synchronizedSet(mutableSetOf<String>())
    val newLobbyPlayerName = MutableStateFlow("")
    private var timerJob: Job? = null

    private var rewardedAd: RewardedAd? = null
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_HEARTS = 1 // الحد الأقصى اليومي للقلوب المجانية
    }

    init {
        CaseRepository.init(application)
        setupLanListeners()
        checkAndResetDailyHearts()
        loadRewardedAdInternal()

        viewModelScope.launch {
            var lastVolume: Float? = null
            roomState.collect { state ->
                if (lastVolume != state.settings.volume) {
                    lastVolume = state.settings.volume
                    MysteryAudioPlayer.setVolume(state.settings.volume)
                }
            }
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
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

        if (lastPlayDate.isNullOrEmpty()) {
            sharedPreferences.edit()
                .putString("last_play_date", todayStr)
                .putInt("hearts_count", MAX_HEARTS)
                .apply()
        } else if (todayStr != lastPlayDate) {
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
        if (!isNetworkAvailable(getApplication())) {
            rewardedAd = null
            return
        }
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(getApplication(), "ca-app-pub-6722529223110069/2125092694", adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Failed to load rewarded ad: ${error.message}")
                }
            })
    }

    fun showAdToEarnHeart(activity: Activity, onFinished: (Boolean) -> Unit) {
        if (!isNetworkAvailable(activity)) {
            onFinished(false)
            return
        }

        rewardedAd?.let { ad ->
            ad.show(activity) { _ ->
                saveLocalHeartsCount(MAX_HEARTS)
                loadRewardedAdInternal()
                onFinished(true)
            }
        } ?: run {
            saveLocalHeartsCount(MAX_HEARTS)
            loadRewardedAdInternal()
            onFinished(true)
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

    fun playButtonClick() { MysteryAudioPlayer.playSelection() }
    fun playSelection() { MysteryAudioPlayer.playSelection() }
    fun playSuccess() { MysteryAudioPlayer.playSuccess() }
    fun playError() { MysteryAudioPlayer.playError() }
    fun playWarning() { MysteryAudioPlayer.playWarning() }
    fun playVoteSound() { MysteryAudioPlayer.playVote() }
    fun playTransitionSound() { MysteryAudioPlayer.playTransition() }
    fun playRevealSound() { MysteryAudioPlayer.playReveal() }

    private fun setupLanListeners() {
        viewModelScope.launch {
            LanManager.discoveredHosts.collect { hosts ->
                Log.d(TAG, "Discovered LAN Hosts updated: $hosts")
            }
        }
        viewModelScope.launch {
            LanManager.incomingCommands.collect { (sourceId, msg) ->
                handleIncomingLanMessage(sourceId, msg)
            }
        }
    }

    private fun handleIncomingLanMessage(source: String, msg: String) {
        try {
            val json = JSONObject(msg)
            val type = json.optString("type")
            Log.d(TAG, "Incoming TCP command [$type] from $source")
            when (type) {
                "JOIN" -> {
                    val pName = json.optString("playerName", "لاعب")
                    val deviceId = json.optString("deviceId", UUID.randomUUID().toString())
                    addLanClientPlayer(deviceId, pName)
                }
                "REVEAL_SECRET" -> {
                    val playerId = json.optString("playerId", "")
                    handlePlayerRevealedRole(playerId)
                }
                "VOTE" -> {
                    val voterId = json.optString("voterId", "")
                    val targetId = json.optString("targetId", "")
                    if (voterId.isNotEmpty()) castVote(voterId, targetId)
                }
                "JURY_VOTE" -> {
                    val voterId = json.optString("voterId", "")
                    val targetId = json.optString("targetId", "")
                    if (voterId.isNotEmpty()) castJuryVote(voterId, targetId)
                }
                "CLIENT_LEAVE" -> {
                    val deviceId = json.optString("deviceId", "")
                    if (deviceId.isNotEmpty()) removePlayerFromLobby(deviceId)
                }
                "STATE_UPDATE" -> {
                    val stateJsonStr = json.optString("data", "")
                    if (stateJsonStr.isNotEmpty()) {
                        val updatedState = RoomState.fromSharedJsonString(stateJsonStr)
                        _roomState.update { current -> updatedState.copy(roomId = current.roomId) }
                    }
                }
                "HOST_DISCONNECTED" -> {
                    LanManager.disconnectFromHost()
                    resetToMainMenu()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing LAN packet", e)
        }
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
            if (updatedState.mode == "LAN") {
                LanManager.broadcastStateToClients(updatedState)
            }
            updatedState
        }
    }

    fun startLanHost(hostPlayerName: String) {
        stopTimer()
        playSuccess()
        val deviceId = LanManager.localDeviceId
        myPlayerId.value = deviceId
        myPlayerName.value = hostPlayerName
        val currentSettings = _roomState.value.settings
        val roomCode = (Random.nextInt(90000) + 10000).toString()
        val newState = RoomState(
            roomId = roomCode,
            mode = "LAN",
            hostId = deviceId,
            phase = GamePhase.LOBBY,
            players = listOf(Player(deviceId, hostPlayerName, avatarId = 1)),
            settings = currentSettings,
            heartsCount = getLocalHeartsCount()
        )
        _roomState.value = newState
        LanManager.startHost(hostPlayerName, roomCode)
    }

    fun joinLanHost(hostIp: String, playerName: String) {
        stopTimer()
        playSelection()
        val deviceId = LanManager.localDeviceId
        myPlayerId.value = deviceId
        myPlayerName.value = playerName
        val currentSettings = _roomState.value.settings
        _roomState.value = RoomState(mode = "LAN", phase = GamePhase.LOBBY, settings = currentSettings)
        LanManager.connectToHost(hostIp, playerName, deviceId)
    }

    fun joinLanHostByCode(roomCode: String, playerName: String): Boolean {
        val cleanCode = roomCode.trim()
        val hosts = LanManager.discoveredHosts.value
        val hostEntry = hosts.entries.find { entry ->
            val parts = entry.value.split("|")
            val rCode = parts.getOrNull(1)
            rCode != null && rCode.equals(cleanCode, ignoreCase = true)
        }
        if (hostEntry != null) {
            joinLanHost(hostEntry.key, playerName)
            return true
        }
        return false
    }

    private fun addLanClientPlayer(deviceId: String, name: String) {
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
            LanManager.broadcastStateToClients(updatedState)
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
            ?: CaseRepository.getUniqueCase(completedCaseTitles, playersCount)

        var updatedPlayers = state.players
        selectedCase?.let { case ->
            completedCaseTitles.add(case.title)
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
            Log.e(TAG, "No suitable case found.")
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
            if (newState.mode == "LAN") {
                LanManager.broadcastStateToClients(newState)
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
            val cmd = JSONObject().apply {
                put("type", "REVEAL_SECRET")
                put("playerId", myPlayerId.value)
            }.toString()
            LanManager.sendCommandToHost(cmd)
        }
    }

    private fun handlePlayerRevealedRole(playerId: String) { }

    fun skipRoleRevealToCaseIntro() {
        playTransitionSound()
        transitionToPhase(GamePhase.CASE_INTRO)
    }

    fun startCaseInvestigationIntro() {
        playTransitionSound()
        _roomState.update { it.copy(currentEvidenceIndex = 0) }
        transitionToPhase(GamePhase.EVIDENCE_ROUND)
    }

    fun advanceFromEvidenceToDiscussion() {
        playTransitionSound()
        transitionToPhase(GamePhase.DISCUSSION)
        startTimer(_roomState.value.settings.discussionTimeMinutes * 60) {
            advanceFromDiscussionToVoting()
        }
    }

    fun advanceFromDiscussionToVoting() {
        stopTimer()
        playTransitionSound()
        val state = _roomState.value
        val aliveCount = state.players.count { it.isAlive }

        if (aliveCount == 2) {
            _roomState.update {
                it.copy(
                    juryVotes = emptyMap(),
                    phase = GamePhase.JURY_ROUND
                )
            }
            startTimer(state.settings.votingTimeMinutes * 60) {
                resolveJuryVotingTally()
            }
        } else {
            val firstAliveIndex = state.players.indexOfFirst { it.isAlive }
            _roomState.update {
                it.copy(
                    votes = emptyMap(),
                    activePassPlayerIndex = if (firstAliveIndex != -1) firstAliveIndex else 0,
                    phase = GamePhase.VOTING
                )
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
                LanManager.sendCommandToHost(cmd)
            }
        }
    }

    private fun castVote(voterId: String, targetId: String) {
        _roomState.update { state ->
            val newVotes = state.votes.toMutableMap()
            newVotes[voterId] = targetId
            val updatedState = state.copy(votes = newVotes)
            val alivePlayersCount = updatedState.players.count { it.isAlive }
            if (newVotes.size >= alivePlayersCount) {
                resolveVotingTally()
            } else {
                LanManager.broadcastStateToClients(updatedState)
            }
            updatedState
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
                LanManager.sendCommandToHost(cmd)
            }
        }
    }

    private fun castJuryVote(voterId: String, targetId: String) {
        _roomState.update { state ->
            val newJVotes = state.juryVotes.toMutableMap()
            newJVotes[voterId] = targetId
            val updatedState = state.copy(juryVotes = newJVotes)
            val jurySize = updatedState.players.count { !it.isAlive }
            if (newJVotes.size >= jurySize) {
                resolveJuryVotingTally()
            } else {
                LanManager.broadcastStateToClients(updatedState)
            }
            updatedState
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

            if (newState.mode == "LAN") {
                LanManager.broadcastStateToClients(newState)
            }
            newState
        }
    }

    fun confirmVoteResultAndProceed() {
        playTransitionSound()
        val state = _roomState.value
        if (state.tiedVotePlayers.isNotEmpty()) {
            _roomState.update { current ->
                val updatedState = current.copy(
                    phase = GamePhase.VOTING,
                    votes = emptyMap()
                )
                if (updatedState.mode == "LAN") {
                    LanManager.broadcastStateToClients(updatedState)
                }
                updatedState
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

            if (newState.mode == "LAN") {
                LanManager.broadcastStateToClients(newState)
            }
            newState
        }
    }

    private fun checkEndgameConditions(justEliminated: Player?) {
        _roomState.update { state ->
            val alivePlayers = state.players.filter { it.isAlive }
            val mafiaAlive = alivePlayers.count { it.isMafia }
            val innocentAlive = alivePlayers.size - mafiaAlive
            Log.d(TAG, "Tally outcomes: Total alive = ${alivePlayers.size}, Mafia alive = $mafiaAlive, Innocents alive = $innocentAlive")

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

            if (newState.mode == "LAN") {
                LanManager.broadcastStateToClients(newState)
            }
            newState
        }
    }

    private fun startTimer(seconds: Int, onComplete: () -> Unit) {
        stopTimer()
        _roomState.update { it.copy(timerTotalSeconds = seconds, timerSecondsLeft = seconds) }

        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                val newRemaining = remaining
                _roomState.update { current ->
                    current.copy(timerSecondsLeft = newRemaining)
                }
                if (_roomState.value.mode == "LAN" && (remaining % 5 == 0 || remaining <= 10)) {
                    LanManager.broadcastStateToClients(_roomState.value)
                }
            }
            if (isActive) onComplete()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun transitionToPhase(newPhase: GamePhase) {
        _roomState.update { current ->
            val updatedState = current.copy(phase = newPhase)
            if (updatedState.mode == "LAN") {
                LanManager.broadcastStateToClients(updatedState)
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
            if (updatedState.mode == "LAN") {
                LanManager.broadcastStateToClients(updatedState)
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
        LanManager.stopDiscovery()
        LanManager.stopHost()
        val currentSettings = _roomState.value.settings
        _roomState.value = RoomState(settings = currentSettings)
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        LanManager.stopHost()
        LanManager.stopDiscovery()
    }

    fun selectCustomCase(customCase: Case) {
        _roomState.update { currentState ->
            currentState.copy(
                currentCase = customCase
            )
        }
    }
}