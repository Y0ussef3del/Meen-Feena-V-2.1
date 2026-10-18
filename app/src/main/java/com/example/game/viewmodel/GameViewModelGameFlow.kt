package com.example.game.viewmodel

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.example.game.data.CaseRepository
import com.example.game.model.*
import com.example.game.network.OnlineManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

fun GameViewModel.startInvestigationGame() {
    executeStartInvestigationGame()
}

fun GameViewModel.startInvestigationGameWithActivity(activity: Activity, onNoInternet: () -> Unit) {
    checkHeartsAndProceed(activity, onNoInternet) {
        executeStartInvestigationGame()
    }
}

internal fun GameViewModel.executeStartInvestigationGame() {
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

fun GameViewModel.revealNextPassPlayerSecrets() {
    playRevealSound()
    _roomState.update { it.copy(rulesRevealed = true) }
}

fun GameViewModel.confirmSecretsRevealed() {
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

internal fun GameViewModel.handlePlayerRevealedRole(playerId: String) {}

fun GameViewModel.skipRoleRevealToCaseIntro() {
    val state = _roomState.value
    if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
    playTransitionSound()
    transitionToPhase(GamePhase.CASE_INTRO)
}

fun GameViewModel.startCaseInvestigationIntro() {
    val state = _roomState.value
    if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
    playTransitionSound()
    _roomState.update { it.copy(currentEvidenceIndex = 0) }
    transitionToPhase(GamePhase.EVIDENCE_ROUND)
}

fun GameViewModel.advanceFromEvidenceToDiscussion() {
    val state = _roomState.value
    if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
    playTransitionSound()
    transitionToPhase(GamePhase.DISCUSSION)
    startTimer(_roomState.value.settings.discussionTimeMinutes * 60) {
        advanceFromDiscussionToVoting()
    }
}

fun GameViewModel.advanceFromDiscussionToVoting() {
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

fun GameViewModel.submitVote(targetId: String) {
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

internal fun GameViewModel.castVote(voterId: String, targetId: String) {
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

fun GameViewModel.submitJuryVote(targetId: String) {
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

internal fun GameViewModel.castJuryVote(voterId: String, targetId: String) {
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

internal fun GameViewModel.resolveVotingTally() {
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

fun GameViewModel.confirmVoteResultAndProceed() {
    val state = _roomState.value
    if (state.mode == "ONLINE" && state.hostId != myPlayerId.value) return
    playTransitionSound()
    if (state.tiedVotePlayers.isNotEmpty()) {
        val firstAliveIndex = state.players.indexOfFirst { it.isAlive }
        _roomState.update { current ->
            val updatedState = current.copy(
                phase = GamePhase.VOTING,
                votes = emptyMap(),
                activePassPlayerIndex = if (firstAliveIndex != -1) firstAliveIndex else 0
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

internal fun GameViewModel.resolveJuryVotingTally() {
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

internal fun GameViewModel.checkEndgameConditions(justEliminated: Player?) {
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

internal fun GameViewModel.startTimer(seconds: Int, onComplete: () -> Unit) {
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

internal fun GameViewModel.stopTimer() {
    timerJob?.cancel()
    timerJob = null
}

internal fun GameViewModel.transitionToPhase(newPhase: GamePhase) {
    _roomState.update { current ->
        val updatedState = current.copy(phase = newPhase)
        if (updatedState.mode == "ONLINE") {
            OnlineManager.broadcastStateToClients(updatedState)
        }
        updatedState
    }
}

fun GameViewModel.updateSettings(discussionMins: Int, votingMins: Int, music: Boolean, vol: Float) {
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

fun GameViewModel.playAgain() {
    stopTimer()
    _roomState.update { it.copy(currentCase = null) }
    executeStartInvestigationGame()
}

fun GameViewModel.playAgainWithActivity(activity: Activity, onNoInternet: () -> Unit) {
    stopTimer()
    checkHeartsAndProceed(activity, onNoInternet) {
        _roomState.update { it.copy(currentCase = null) }
        executeStartInvestigationGame()
    }
}

fun GameViewModel.resetToMainMenu() {
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

fun GameViewModel.selectCustomCase(customCase: Case?) {
    _roomState.update { currentState ->
        currentState.copy(
            currentCase = customCase
        )
    }
}