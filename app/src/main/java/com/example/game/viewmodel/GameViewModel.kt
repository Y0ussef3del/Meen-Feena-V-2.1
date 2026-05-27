package com.example.game.viewmodel

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.data.CaseRepository
import com.example.game.model.*
import com.example.game.network.LanManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.*

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "GameViewModel"

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    val myPlayerId = MutableStateFlow("")
    val myPlayerName = MutableStateFlow("مكافح الجريمة")

    private val completedCaseTitles = mutableSetOf<String>()
    val newLobbyPlayerName = MutableStateFlow("")
    private var timerJob: Job? = null

    // Authoritative structures maintained locally on Host device
    private val hostClientKeys = mutableMapOf<String, String>()
    private var localSessionKey = ""
    private var hostAuthoritativePlayers: List<Player> = emptyList()
    private val readyPlayersForCaseReveal = mutableSetOf<String>()

    init {
        setupLanListeners()
        viewModelScope.launch {
            roomState.collect { state ->
                MysteryAudioPlayer.setVolume(state.settings.volume)
                if (state.settings.isMusicEnabled) {
                    MysteryAudioPlayer.startMusic()
                } else {
                    MysteryAudioPlayer.stopMusic()
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
                    val pName = json.getString("playerName")
                    val deviceId = json.getString("deviceId")
                    addLanClientPlayer(deviceId, pName)
                }
                "SHARE_KEY" -> {
                    val deviceId = json.getString("deviceId")
                    val clientKey = json.getString("clientKey")
                    hostClientKeys[deviceId] = clientKey
                    Log.d(TAG, "Authoritative handoff completed for encryption key: $deviceId")
                }
                "REVEAL_SECRET" -> {
                    val playerId = json.getString("playerId")
                    handlePlayerRevealedRole(playerId)
                }
                "VOTE" -> {
                    val voterId = json.getString("voterId")
                    val targetId = json.getString("targetId")
                    castVote(voterId, targetId)
                }
                "JURY_VOTE" -> {
                    val voterId = json.getString("voterId")
                    val targetId = json.getString("targetId")
                    castJuryVote(voterId, targetId)
                }
                "CLIENT_LEAVE" -> {
                    val deviceId = json.getString("deviceId")
                    removePlayerFromLobby(deviceId)
                }
                "STATE_UPDATE" -> {
                    val stateJsonStr = json.getString("data")
                    var updatedState = RoomState.fromSharedJsonString(stateJsonStr)
                    
                    // Secure Role Verification layer for clients
                    val myId = myPlayerId.value
                    val encryptedRole = updatedState.encryptedRoles[myId]
                    if (!encryptedRole.isNullOrEmpty()) {
                        val key = localSessionKey
                        val decryptedCharJson = decipherCode(encryptedRole, key)
                        if (decryptedCharJson.isNotEmpty()) {
                            try {
                                val charObj = Character.fromJsonObject(JSONObject(decryptedCharJson))
                                updatedState = updatedState.copy(
                                    players = updatedState.players.map { p ->
                                        if (p.id == myId) p.copy(character = charObj, isMafia = charObj.isMafia) else p
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Decryption mapping failure", e)
                            }
                        }
                    }
                    _roomState.value = updatedState.copy(roomId = _roomState.value.roomId)
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
        _roomState.value = RoomState(
            roomId = "PASS_AND_PLAY_ROOM",
            mode = "PASS_AND_PLAY",
            hostId = "LOCAL_HOST",
            phase = GamePhase.LOBBY,
            players = listOf(
                Player("p1", "يوسف", avatarId = 1),
                Player("p2", "عادل", avatarId = 2),
                Player("p3", "محمد", avatarId = 3),
                Player("p4", "جمال", avatarId = 4)
            )
        )
        myPlayerId.value = "p1"
    }

    fun addLocalLobbyPlayer(name: String) {
        if (name.isBlank()) return
        playSelection()
        val currentPlayers = _roomState.value.players.toMutableList()
        if (currentPlayers.size >= 6) return
        val newId = "p${currentPlayers.size + 1}"
        currentPlayers.add(Player(newId, name, avatarId = (currentPlayers.size % 6) + 1))
        _roomState.value = _roomState.value.copy(players = currentPlayers)
    }

    fun removePlayerFromLobby(id: String) {
        playWarning()
        val currentPlayers = _roomState.value.players.filter { it.id != id }
        _roomState.value = _roomState.value.copy(players = currentPlayers)
        if (_roomState.value.mode == "LAN") {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    fun startLanHost(hostPlayerName: String) {
        stopTimer()
        playSuccess()
        val deviceId = LanManager.localDeviceId
        myPlayerId.value = deviceId
        myPlayerName.value = hostPlayerName
        localSessionKey = UUID.randomUUID().toString().substring(0, 8)
        val roomCode = (Random().nextInt(90000) + 1000).toString().padStart(5, '0')
        _roomState.value = RoomState(
            roomId = roomCode,
            mode = "LAN",
            hostId = deviceId,
            phase = GamePhase.LOBBY,
            players = listOf(Player(deviceId, hostPlayerName, avatarId = 1))
        )
        LanManager.startHost(hostPlayerName, roomCode)
    }

    fun joinLanHost(hostIp: String, playerName: String) {
        stopTimer()
        playSelection()
        val deviceId = LanManager.localDeviceId
        myPlayerId.value = deviceId
        myPlayerName.value = playerName
        localSessionKey = UUID.randomUUID().toString().substring(0, 8)
        _roomState.value = RoomState(mode = "LAN", phase = GamePhase.LOBBY)
        LanManager.connectToHost(hostIp, playerName, deviceId)

        // Asynchronously share generated secret key with the host right after socket link
        viewModelScope.launch {
            delay(600)
            val cmd = JSONObject().apply {
                put("type", "SHARE_KEY")
                put("deviceId", deviceId)
                put("clientKey", localSessionKey)
            }.toString()
            LanManager.sendCommandToHost(cmd)
        }
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
        val currentPlayers = _roomState.value.players.toMutableList()
        val existingIndex = currentPlayers.indexOfFirst { it.id == deviceId }
        if (existingIndex >= 0) {
            currentPlayers[existingIndex] = currentPlayers[existingIndex].copy(isConnected = true)
        } else {
            if (currentPlayers.size >= 6) return
            val finalizedName = if (currentPlayers.any { it.name.equals(name, ignoreCase = true) }) {
                "$name (${currentPlayers.size + 1})"
            } else {
                name
            }
            currentPlayers.add(Player(deviceId, finalizedName, avatarId = (currentPlayers.size % 6) + 1))
        }
        _roomState.value = _roomState.value.copy(players = currentPlayers)
        LanManager.broadcastStateToClients(_roomState.value)
    }

    fun startInvestigationGame() {
        val state = _roomState.value
        val playersCount = state.players.size
        if (playersCount < 4 || playersCount > 6) {
            playError()
            return
        }
        playTransitionSound()

        // BUG #1 FIX: Strictly fetch via player validation, return if null (no fallbacks)
        val selectedCase = CaseRepository.getUniqueCase(completedCaseTitles, playersCount)
        if (selectedCase == null) {
            Log.e(TAG, "startInvestigationGame aborted: No matching case found for exactly $playersCount players.")
            playError()
            return
        }

        completedCaseTitles.add(selectedCase.title)
        val shuffledCharacters = selectedCase.characters.shuffled()
        
        val authoritativePlayers = state.players.mapIndexed { index, player ->
            val assignedCharacter = shuffledCharacters.getOrNull(index)
            val isPlayerMafia = assignedCharacter?.isMafia == true
            player.copy(
                isMafia = isPlayerMafia,
                character = assignedCharacter,
                isAlive = true,
                isConnected = true
            )
        }
        hostAuthoritativePlayers = authoritativePlayers

        synchronized(readyPlayersForCaseReveal) {
            readyPlayersForCaseReveal.clear()
        }

        val encryptedRolesMap = mutableMapOf<String, String>()
        val hostId = state.hostId.ifBlank { myPlayerId.value }

        // Mask internal information completely before transmitting across shared networks
        val updatedPlayers = authoritativePlayers.map { player ->
            if (state.mode == "LAN") {
                val charJson = player.character?.toJsonObject()?.toString() ?: ""
                if (player.id != hostId) {
                    val clientKey = hostClientKeys[player.id] ?: ""
                    if (charJson.isNotEmpty() && clientKey.isNotEmpty()) {
                        encryptedRolesMap[player.id] = cipherCode(charJson, clientKey)
                    }
                    player.copy(character = null, isMafia = false)
                } else {
                    player // Host preserves local read authorization
                }
            } else {
                player
            }
        }

        _roomState.value = state.copy(
            phase = GamePhase.ROLE_REVEAL,
            players = updatedPlayers,
            currentCase = selectedCase,
            currentEvidenceIndex = 0,
            activePassPlayerIndex = 0,
            rulesRevealed = false,
            votes = emptyMap(),
            juryVotes = emptyMap(),
            winnerSide = "",
            encryptedRoles = encryptedRolesMap
        )

        if (_roomState.value.mode == "LAN") {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    fun revealNextPassPlayerSecrets() {
        playRevealSound()
        _roomState.value = _roomState.value.copy(rulesRevealed = true)
    }

    fun confirmSecretsRevealed() {
        playSelection()
        val state = _roomState.value
        if (state.mode == "PASS_AND_PLAY") {
            val nextIndex = state.activePassPlayerIndex + 1
            if (nextIndex < state.players.size) {
                _roomState.value = state.copy(activePassPlayerIndex = nextIndex, rulesRevealed = false)
            } else {
                transitionToPhase(GamePhase.CASE_INTRO)
            }
        } else {
            val myId = myPlayerId.value
            if (state.hostId == myId) {
                handlePlayerRevealedRole(myId)
            } else {
                val cmd = JSONObject().apply {
                    put("type", "REVEAL_SECRET")
                    put("playerId", myId)
                }.toString()
                LanManager.sendCommandToHost(cmd)
            }
        }
    }

    private fun handlePlayerRevealedRole(playerId: String) {
        val state = _roomState.value
        if (state.mode != "LAN" || state.hostId != myPlayerId.value) return

        synchronized(readyPlayersForCaseReveal) {
            readyPlayersForCaseReveal.add(playerId)
            val expectedIds = state.players.map { it.id }.toSet()
            if (readyPlayersForCaseReveal.containsAll(expectedIds)) {
                readyPlayersForCaseReveal.clear()
                transitionToPhase(GamePhase.CASE_INTRO)
            }
        }
    }

    fun skipRoleRevealToCaseIntro() {
        playTransitionSound()
        transitionToPhase(GamePhase.CASE_INTRO)
    }

    fun startCaseInvestigationIntro() {
        playTransitionSound()
        _roomState.value = _roomState.value.copy(currentEvidenceIndex = 0)
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
        val firstAliveIndex = state.players.indexOfFirst { it.isAlive }
        _roomState.value = state.copy(
            votes = emptyMap(),
            activePassPlayerIndex = if (firstAliveIndex != -1) firstAliveIndex else 0
        )
        transitionToPhase(GamePhase.VOTING)
        startTimer(_roomState.value.settings.votingTimeMinutes * 60) {
            resolveVotingTally()
        }
    }

    fun submitVote(targetId: String) {
        playVoteSound()
        val state = _roomState.value
        val voterId = myPlayerId.value
        
        if (state.mode == "PASS_AND_PLAY") {
            val currentVoter = state.players.getOrNull(state.activePassPlayerIndex) ?: return
            if (!currentVoter.isAlive) return
            
            val target = state.players.find { it.id == targetId }
            if (target == null || !target.isAlive) return

            val newVotes = state.votes.toMutableMap()
            newVotes[currentVoter.id] = targetId
            
            var nextIndex = state.activePassPlayerIndex + 1
            while (nextIndex < state.players.size && !state.players[nextIndex].isAlive) {
                nextIndex++
            }
            if (nextIndex < state.players.size) {
                _roomState.value = state.copy(votes = newVotes, activePassPlayerIndex = nextIndex)
            } else {
                _roomState.value = state.copy(votes = newVotes)
                resolveVotingTally()
            }
        } else {
            // BUG #2 UI LEAK PROOFING: Verify voting permission on local client before messaging host
            val localMe = state.players.find { it.id == voterId }
            if (localMe == null || !localMe.isAlive) return

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
        val state = _roomState.value
        val voter = state.players.find { it.id == voterId }
        val target = state.players.find { it.id == targetId }
        
        // BUG #2 FIX: Ignore dead voters or invalid target choices explicitly
        if (voter == null || !voter.isAlive || target == null || !target.isAlive) {
            Log.w(TAG, "Rejected unauthorized vote from $voterId to $targetId")
            return
        }
        if (state.votes.containsKey(voterId)) {
            Log.w(TAG, "Duplicate vote rejected from: $voterId")
            return
        }

        val newVotes = state.votes.toMutableMap()
        newVotes[voterId] = targetId
        _roomState.value = state.copy(votes = newVotes)
        
        val alivePlayersCount = state.players.count { it.isAlive }
        if (newVotes.size >= alivePlayersCount) {
            resolveVotingTally()
        } else {
            LanManager.broadcastStateToClients(_roomState.value)
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
            _roomState.value = state.copy(juryVotes = newJVotes)
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
        val state = _roomState.value
        val newJVotes = state.juryVotes.toMutableMap()
        newJVotes[voterId] = targetId
        _roomState.value = state.copy(juryVotes = newJVotes)
        val jurySize = state.players.count { !it.isAlive }
        if (newJVotes.size >= jurySize) {
            resolveJuryVotingTally()
        } else {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    private fun resolveVotingTally() {
        stopTimer()
        val state = _roomState.value
        val voteCounts = mutableMapOf<String, Int>()
        
        // Count votes cast strictly by alive users
        state.votes.forEach { (voterId, targetId) ->
            val voter = state.players.find { it.id == voterId }
            if (voter != null && voter.isAlive) {
                voteCounts[targetId] = voteCounts.getOrDefault(targetId, 0) + 1
            }
        }
        
        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        val tiedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()
        
        if (tiedPlayers.size >= 2 && voteCounts.isNotEmpty()) {
            val tiedNames = state.players.filter { it.id in tiedPlayers }.joinToString(" و ") { it.name }
            _roomState.value = state.copy(
                phase = GamePhase.VOTE_RESULT,
                tiedVotePlayers = tiedPlayers,
                lastEliminatedResult = "حصل تعادل في الأصوات بين ($tiedNames)! محدش خرج وهنعيد التصويت تاني."
            )
        } else {
            val targetId = tiedPlayers.firstOrNull()
            var eliminatedPlayer: Player? = null
            if (targetId != null) {
                val currentPlayers = state.players.map { player ->
                    if (player.id == targetId) {
                        val updated = player.copy(isAlive = false)
                        eliminatedPlayer = updated
                        updated
                    } else {
                        player
                    }
                }
                
                val isMafia = if (state.mode == "LAN") {
                    hostAuthoritativePlayers.find { it.id == targetId }?.isMafia == true
                } else {
                    eliminatedPlayer?.isMafia == true
                }
                
                val roleStr = if (isMafia) "مافيا" else "بريء"
                val resultText = "${eliminatedPlayer?.name} خرج وكان $roleStr"
                
                _roomState.value = state.copy(
                    phase = GamePhase.VOTE_RESULT,
                    players = currentPlayers,
                    tiedVotePlayers = emptyList(),
                    lastEliminatedResult = resultText
                )
            } else {
                _roomState.value = state.copy(
                    phase = GamePhase.VOTE_RESULT,
                    tiedVotePlayers = emptyList(),
                    lastEliminatedResult = "محدش صوّت ومحدش خرج!"
                )
            }
        }
        if (state.mode == "LAN") {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    fun confirmVoteResultAndProceed() {
        playTransitionSound()
        val state = _roomState.value
        if (state.tiedVotePlayers.isNotEmpty()) {
            _roomState.value = state.copy(
                phase = GamePhase.VOTING,
                votes = emptyMap()
            )
            if (state.mode == "LAN") {
                LanManager.broadcastStateToClients(_roomState.value)
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
        if (finalAccusedEntry != null) {
            val accusedId = finalAccusedEntry.key
            val accusedPlayer = state.players.find { it.id == accusedId }
            if (accusedPlayer != null) {
                val isMafia = if (state.mode == "LAN") {
                    hostAuthoritativePlayers.find { it.id == accusedId }?.isMafia == true
                } else {
                    accusedPlayer.isMafia
                }
                if (isMafia) {
                    transitionToEndgame("INNOCENTS")
                } else {
                    transitionToEndgame("MAFIA")
                }
                return
            }
        }
        transitionToEndgame("MAFIA")
    }

    /**
     * Requirement Bug #2 & Phase 8: Intercepts game termination sequence when 2 alive players remain.
     */
    private fun checkEndgameConditions(justEliminated: Player?) {
        val state = _roomState.value
        val alivePlayers = state.players.filter { it.isAlive }
        val mafiaAlive = alivePlayers.count { p ->
            if (state.mode == "LAN") {
                hostAuthoritativePlayers.find { it.id == p.id }?.isMafia == true
            } else {
                p.isMafia
            }
        }
        val innocentAlive = alivePlayers.size - mafiaAlive
        Log.d(TAG, "Tally outcomes: Total alive = ${alivePlayers.size}, Mafia alive = $mafiaAlive, Innocents alive = $innocentAlive")
        
        var targetPhase = state.phase
        var targetWinner = ""
        var targetEvidenceIndex = state.currentEvidenceIndex
        
        when {
            mafiaAlive == 0 -> {
                targetPhase = GamePhase.ENDGAME
                targetWinner = "INNOCENTS"
            }
            alivePlayers.size == 2 -> {
                // BUG #2 CRITICAL ROUTING CHANGE: Sequence Final Clue (Evidence Index max) -> Discussion -> Voting
                val finalEvidenceIndex = ((state.currentCase?.evidenceList?.size)?.minus(1))?.coerceAtLeast(0) ?: 0
                targetPhase = GamePhase.EVIDENCE_ROUND
                targetEvidenceIndex = finalEvidenceIndex
            }
            mafiaAlive >= innocentAlive -> {
                targetPhase = GamePhase.ENDGAME
                targetWinner = "MAFIA"
            }
            else -> {
                targetPhase = GamePhase.EVIDENCE_ROUND
                targetEvidenceIndex = (state.currentEvidenceIndex + 1) % (state.currentCase?.evidenceList?.size ?: 6)
            }
        }
        
        // Unmask identities safely if targetPhase matches endgame constraints
        val finalPlayers = if (targetPhase == GamePhase.ENDGAME && state.mode == "LAN") {
            hostAuthoritativePlayers.map { authP ->
                val matchingAlive = state.players.find { it.id == authP.id }
                authP.copy(isAlive = matchingAlive?.isAlive ?: authP.isAlive)
            }
        } else {
            state.players
        }

        _roomState.value = state.copy(
            phase = targetPhase,
            players = finalPlayers,
            currentEvidenceIndex = targetEvidenceIndex,
            votes = emptyMap(),
            winnerSide = targetWinner
        )
        
        if (_roomState.value.mode == "LAN") {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    private fun transitionToEndgame(winner: String) {
        val state = _roomState.value
        val finalPlayers = if (state.mode == "LAN") {
            hostAuthoritativePlayers.map { authP ->
                val matchingAlive = state.players.find { it.id == authP.id }
                authP.copy(isAlive = matchingAlive?.isAlive ?: authP.isAlive)
            }
        } else {
            state.players
        }
        _roomState.value = state.copy(phase = GamePhase.ENDGAME, winnerSide = winner, players = finalPlayers)
        if (_roomState.value.mode == "LAN") {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    private fun startTimer(seconds: Int, onComplete: () -> Unit) {
        stopTimer()
        _roomState.value = _roomState.value.copy(timerTotalSeconds = seconds, timerSecondsLeft = seconds)
        timerJob = viewModelScope.launch {
            while (_roomState.value.timerSecondsLeft > 0) {
                delay(1000)
                val updatedSeconds = _roomState.value.timerSecondsLeft - 1
                _roomState.value = _roomState.value.copy(timerSecondsLeft = updatedSeconds)
                if (_roomState.value.mode == "LAN") {
                    LanManager.broadcastStateToClients(_roomState.value)
                }
            }
            onComplete()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun transitionToPhase(newPhase: GamePhase) {
        val state = _roomState.value
        val finalPlayers = if (newPhase == GamePhase.ENDGAME && state.mode == "LAN") {
            hostAuthoritativePlayers.map { authP ->
                val matchingAlive = state.players.find { it.id == authP.id }
                authP.copy(isAlive = matchingAlive?.isAlive ?: authP.isAlive)
            }
        } else {
            state.players
        }
        _roomState.value = state.copy(phase = newPhase, players = finalPlayers)
        if (_roomState.value.mode == "LAN") {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    fun updateSettings(discussionMins: Int, votingMins: Int, music: Boolean, vol: Float) {
        val state = _roomState.value
        val updatedSettings = GameSettings(
            discussionTimeMinutes = discussionMins,
            votingTimeMinutes = votingMins,
            isMusicEnabled = music,
            volume = vol
        )
        _roomState.value = state.copy(settings = updatedSettings)
        if (state.mode == "LAN") {
            LanManager.broadcastStateToClients(_roomState.value)
        }
    }

    fun playAgain() {
        stopTimer()
        startInvestigationGame()
    }

    fun resetToMainMenu() {
        stopTimer()
        LanManager.stopDiscovery()
        LanManager.stopHost()
        _roomState.value = RoomState()
        hostClientKeys.clear()
        localSessionKey = ""
        hostAuthoritativePlayers = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        LanManager.stopHost()
        LanManager.stopDiscovery()
    }

    // Cryptographic Helpers utilizing android.util.Base64 safely (XOR Bitwise Obfuscation)
    private fun cipherCode(text: String, key: String): String {
        if (key.isEmpty()) return text
        val result = StringBuilder()
        for (i in text.indices) {
            result.append((text[i].code xor key[i % key.length].code).toChar())
        }
        return Base64.encodeToString(result.toString().toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decipherCode(encryptedB64: String, key: String): String {
        if (key.isEmpty()) return ""
        return try {
            val decodedBytes = Base64.decode(encryptedB64, Base64.NO_WRAP)
            val text = String(decodedBytes, StandardCharsets.UTF_8)
            val result = StringBuilder()
            for (i in text.indices) {
                result.append((text[i].code xor key[i % key.length].code).toChar())
            }
            result.toString()
        } catch (e: Exception) {
            ""
        }
    }
}