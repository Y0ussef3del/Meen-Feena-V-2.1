package com.example.game.viewmodel

import android.app.Application
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

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    val myPlayerId = MutableStateFlow("player_local")
    val myPlayerName = MutableStateFlow("مكافح الجريمة")
    val newLobbyPlayerName = MutableStateFlow("")

    init {
        loadDynamicGameCase()
    }

    fun loadDynamicGameCase() {
        val jsonCases = CaseRepository.getAllCases()
        if (jsonCases.isNotEmpty()) {
            _roomState.value = _roomState.value.copy(currentCase = jsonCases.random())
        } else {
            setupDefaultMockCase()
        }
    }

    private fun setupDefaultMockCase() {
        val mockCharacters = listOf(
            GameCharacter("الدكتور سامح", "طبيب جراح", "عصبي وغامض", "قتل الضحية بسبب سر طبي قديم"),
            GameCharacter("المهندس كريم", "مهندس معمار", "هادئ وملاحظ", "الضحية ابتزته بمبالغ مالية ضخمة"),
            GameCharacter("الأستاذة فريدة", "محامية العائلة", "ذكية وسريعة الرد", "أنت بريء حاول تكتشف المجرم الحقيقي"),
            GameCharacter("الحارس عثمان", "حارس الفيلا", "قوي وبسيط", "رأى الجريمة ولكنه خائف من التحدث")
        )
        val defaultCase = Case(
            id = "case_fallback",
            title = "جريمة في القصر الملعون",
            description = "تم العثور على صاحب القصر مقتولاً داخل مكتبه المغلق من الداخل.",
            explanation = "الحقيقة الكاملة هي أن الدكتور سامح استغل معرفته الطبية لتزييف وقت الوفاة الحقيقي!",
            characters = mockCharacters
        )
        _roomState.value = _roomState.value.copy(currentCase = defaultCase)
    }

    fun updateDiscussionTimer(minutes: Int) {
        val currentSettings = _roomState.value.settings.copy(discussionTimeMinutes = minutes.coerceIn(1, 10))
        _roomState.value = _roomState.value.copy(settings = currentSettings)
    }

    fun setupPassAndPlayGame() {
        val currentCaseData = _roomState.value.currentCase
        _roomState.value = RoomState(
            roomId = (10000..99999).random().toString(),
            hostId = "player_local",
            mode = "PASS_AND_PLAY",
            phase = GamePhase.LOBBY,
            currentCase = currentCaseData
        )
    }

    fun addLocalLobbyPlayer(name: String) {
        val currentState = _roomState.value
        if (currentState.players.size >= 8 || name.isBlank()) return
        
        val nextAvatarId = currentState.players.size + 1
        val newPlayer = Player(
            id = "player_${System.currentTimeMillis()}",
            name = name,
            avatarId = nextAvatarId
        )
        _roomState.value = currentState.copy(players = currentState.players + newPlayer)
    }

    fun removePlayerFromLobby(id: String) {
        val currentState = _roomState.value
        _roomState.value = currentState.copy(players = currentState.players.filter { it.id != id })
    }

    fun startInvestigationGame() {
        val currentState = _roomState.value
        val totalPlayers = currentState.players.size
        if (totalPlayers < 4) return

        val runtimeCase = CaseRepository.getUniqueCase(emptySet(), totalPlayers) ?: currentState.currentCase
        val totalMafiaNeeded = if (totalPlayers <= 4) 1 else 2
        val shuffledIndices = currentState.players.indices.shuffled()
        val mafiaIndices = shuffledIndices.take(totalMafiaNeeded).toSet()

        val assignedPlayers = currentState.players.mapIndexed { idx, player ->
            player.copy(
                isAlive = true,
                isMafia = mafiaIndices.contains(idx),
                character = runtimeCase?.characters?.getOrNull(idx % (runtimeCase.characters.size.coerceAtLeast(1)))
            )
        }

        _roomState.value = currentState.copy(
            currentCase = runtimeCase,
            players = assignedPlayers,
            activePassPlayerIndex = 0,
            phase = GamePhase.ROLE_REVEAL,
            votes = emptyMap(),
            juryVotes = emptyMap(),
            tiedVotePlayers = emptyList(),
            lastEliminatedPlayer = null
        )
    }

    fun confirmSecretsRevealed() {
        val currentState = _roomState.value
        val nextIndex = currentState.activePassPlayerIndex + 1
        
        if (currentState.mode == "PASS_AND_PLAY" && nextIndex < currentState.players.size) {
            _roomState.value = currentState.copy(activePassPlayerIndex = nextIndex)
        } else {
            _roomState.value = currentState.copy(activePassPlayerIndex = 0, phase = GamePhase.CASE_INTRO)
        }
    }

    fun startCaseInvestigationIntro() = transitionToPhase(GamePhase.EVIDENCE_ROUND)
    fun advanceFromEvidenceToDiscussion() = transitionToPhase(GamePhase.DISCUSSION)
    fun advanceFromDiscussionToVoting() {
        _roomState.value = _roomState.value.copy(phase = GamePhase.VOTING, activePassPlayerIndex = 0)
    }

    fun submitVote(targetId: String) {
        val currentState = _roomState.value
        val activeVoters = currentState.players.filter { it.isAlive }

        if (currentState.mode == "PASS_AND_PLAY") {
            val updatedVotes = currentState.votes.toMutableMap()
            val currentVoter = activeVoters.getOrNull(currentState.activePassPlayerIndex)
            
            if (currentVoter != null) {
                updatedVotes[currentVoter.id] = targetId
                val nextIndex = currentState.activePassPlayerIndex + 1
                
                if (nextIndex < activeVoters.size) {
                    _roomState.value = currentState.copy(votes = updatedVotes, activePassPlayerIndex = nextIndex)
                } else {
                    tallyVotesAndProceed(updatedVotes)
                }
            }
        } else {
            val updatedVotes = currentState.votes.toMutableMap()
            updatedVotes[myPlayerId.value] = targetId
            _roomState.value = currentState.copy(votes = updatedVotes)
            if (isHost() && updatedVotes.size >= activeVoters.size) {
                tallyVotesAndProceed(updatedVotes)
            }
        }
    }

    private fun tallyVotesAndProceed(allVotes: Map<String, String>) {
        val currentState = _roomState.value
        val voteCounts = mutableMapOf<String, Int>()
        allVotes.values.forEach { targetId -> voteCounts[targetId] = (voteCounts[targetId] ?: 0) + 1 }

        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        val highestVotedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()

        if (highestVotedPlayers.size > 1) {
            _roomState.value = currentState.copy(
                tiedVotePlayers = highestVotedPlayers,
                votes = emptyMap(),
                phase = GamePhase.VOTE_RESULT,
                lastEliminatedPlayer = null
            )
        } else if (highestVotedPlayers.isNotEmpty()) {
            val eliminatedId = highestVotedPlayers.first()
            var lastEliminated: Player? = null
            val updatedPlayers = currentState.players.map { player ->
                if (player.id == eliminatedId) {
                    val killed = player.copy(isAlive = false)
                    lastEliminated = killed
                    killed
                } else player
            }

            _roomState.value = currentState.copy(
                players = updatedPlayers,
                tiedVotePlayers = emptyList(),
                votes = emptyMap(),
                phase = GamePhase.VOTE_RESULT,
                lastEliminatedPlayer = lastEliminated
            )
        } else {
            goToNextRoundOrJuryScreen()
        }
    }

    fun confirmVoteResultAndProceed() = goToNextRoundOrJuryScreen()

    private fun goToNextRoundOrJuryScreen() {
        val currentState = _roomState.value
        val alivePlayers = currentState.players.filter { it.isAlive }
        val aliveMafiaCount = alivePlayers.count { it.isMafia }

        if (aliveMafiaCount == 0) {
            _roomState.value = currentState.copy(phase = GamePhase.ENDGAME, winnerSide = "INNOCENT")
            return
        }
        
        if (alivePlayers.size <= 2) {
            _roomState.value = currentState.copy(phase = GamePhase.JURY_ROUND, activePassPlayerIndex = 0, juryVotes = emptyMap())
        } else {
            _roomState.value = currentState.copy(phase = GamePhase.EVIDENCE_ROUND)
        }
    }

    fun submitJuryVote(targetId: String) {
        val currentState = _roomState.value
        val deadPlayers = currentState.players.filter { !it.isAlive }

        val updatedJuryVotes = currentState.juryVotes.toMutableMap()
        val currentVoter = deadPlayers.getOrNull(currentState.activePassPlayerIndex)
        
        if (currentVoter != null) {
            updatedJuryVotes[currentVoter.id] = targetId
            val nextIndex = currentState.activePassPlayerIndex + 1
            
            if (nextIndex < deadPlayers.size) {
                _roomState.value = currentState.copy(juryVotes = updatedJuryVotes, activePassPlayerIndex = nextIndex)
            } else {
                val tally = mutableMapOf<String, Int>()
                updatedJuryVotes.values.forEach { tally[it] = (tally[it] ?: 0) + 1 }
                val finalConvictedId = tally.maxByOrNull { it.value }?.key ?: targetId
                
                val isMafia = currentState.players.find { it.id == finalConvictedId }?.isMafia == true
                _roomState.value = currentState.copy(
                    phase = GamePhase.ENDGAME, 
                    winnerSide = if (isMafia) "INNOCENT" else "MAFIA"
                )
            }
        }
    }

    private fun transitionToPhase(newPhase: GamePhase) {
        _roomState.value = _roomState.value.copy(phase = newPhase)
    }

    fun isHost(): Boolean = _roomState.value.hostId == myPlayerId.value
    fun playAgain() = startInvestigationGame()
    fun resetToMainMenu() { _roomState.value = RoomState(phase = GamePhase.LOBBY) }
}