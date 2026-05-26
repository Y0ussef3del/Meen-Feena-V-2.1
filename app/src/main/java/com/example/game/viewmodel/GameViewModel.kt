package com.example.game.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.game.audio.MysteryAudioPlayer

// ==========================================
// 1. GAME DATA STRUCTURES & ENUMS 
// ==========================================

enum class GamePhase {
    LOBBY,
    ROLE_REVEAL,
    CASE_INTRO,
    EVIDENCE_ROUND,
    DISCUSSION,
    VOTING,
    VOTE_RESULT,
    JURY_ROUND,
    ENDGAME
}

data class GameCharacter(
    val name: String = "",
    val occupation: String = "",
    val traits: String = "",
    val hiddenMotive: String = ""
)

data class Player(
    val id: String,
    val name: String,
    val avatarId: Int,
    var isAlive: Boolean = true,
    var isMafia: Boolean = false,
    var character: GameCharacter? = null
)

data class Case(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val explanation: String = "",
    val characters: List<GameCharacter> = emptyList()
)

data class RoomState(
    val roomId: String = "12345",
    val hostId: String = "player_local",
    val phase: GamePhase = GamePhase.LOBBY,
    val players: List<Player> = emptyList(),
    val mode: String = "PASS_AND_PLAY", // PASS_AND_PLAY or LAN
    val activePassPlayerIndex: Int = 0,
    val currentCase: Case? = null,
    val votes: Map<String, String> = emptyMap(),      // VoterID -> TargetPlayerID
    val juryVotes: Map<String, String> = emptyMap(),  // EliminatedPlayerID -> RemainingPlayerID
    val tiedVotePlayers: List<String> = emptyList(),
    val winnerSide: String = "",                      // MAFIA or INNOCENT
    val discussionDurationMins: Int = 2
)

// ==========================================
// 2. MAIN VIEWMODEL IMPLEMENTATION
// ==========================================

class GameViewModel : ViewModel() {

    // Identity tracking for multiplayer components
    val myPlayerId = MutableStateFlow("player_local")
    val myPlayerName = MutableStateFlow("المحقق الأصلي")

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    // Configuration states accessible by Settings dialogs
    private val _discussionDurationMins = MutableStateFlow(2)
    val discussionDurationMins: StateFlow<Int> = _discussionDurationMins.asStateFlow()

    init {
        // Initialize an example mock scenario in case JSON files are empty
        setupDefaultMockCase()
    }

    private fun setupDefaultMockCase() {
        val mockCharacters = listOf(
            GameCharacter("الدكتور سامح", "طبيب جراح", "عصبي وغامض", "كان يريد التخلص من الضحية بسبب سر طبي قديم"),
            GameCharacter("المهندس كريم", "مهندس معمار", "هادئ وملاحظ", "الضحية ابتزته بمبالغ مالية ضخمة"),
            GameCharacter("الأستاذة فريدة", "محامية العائلة", "ذكية وسريعة الرد", "أنت بريء حاول تكتشف المجرم الحقيقي"),
            GameCharacter("الحارس عثمان", "حارس الفيلا", "قوي وبسيط", "رأى الجريمة ولكنه خائف من التحدث")
        )
        val defaultCase = Case(
            id = "case_01",
            title = "جريمة في القصر الملعون",
            description = "تم العثور على صاحب القصر مقتولاً داخل مكتبه المغلق من الداخل. الساعة كانت تشير إلى الحادية عشر مساءً، وهناك 4 مشتبه بهم تواجدوا في محيط الجريمة.",
            explanation = "الحقيقة الكاملة هي أن الدكتور سامح استغل معرفته الطبية لتزييف وقت الوفاة الحقيقي قبل إغلاق الغرفة تلقائياً ليخرج بريئاً أمام المحققين!",
            characters = mockCharacters
        )
        _roomState.value = _roomState.value.copy(currentCase = defaultCase)
    }

    fun updateDiscussionTimer(minutes: Int) {
        val boundedMins = minutes.coerceIn(1, 10)
        _discussionDurationMins.value = boundedMins
        _roomState.value = _roomState.value.copy(discussionDurationMins = boundedMins)
    }

    // Pass and Play management setup
    fun setupPassAndPlayGame() {
        _roomState.value = RoomState(
            roomId = (10000..99999).random().toString(),
            hostId = "player_local",
            mode = "PASS_AND_PLAY",
            phase = GamePhase.LOBBY,
            currentCase = _roomState.value.currentCase
        )
    }

    fun addLocalLobbyPlayer(name: String) {
        val currentState = _roomState.value
        if (currentState.players.size >= 6) return
        
        val nextAvatarId = currentState.players.size + 1
        val newPlayer = Player(
            id = "player_${System.currentTimeMillis()}_$nextAvatarId",
            name = name,
            avatarId = nextAvatarId
        )
        _roomState.value = currentState.copy(
            players = currentState.players + newPlayer
        )
    }

    fun removePlayerFromLobby(id: String) {
        val currentState = _roomState.value
        _roomState.value = currentState.copy(
            players = currentState.players.filter { it.id != id }
        )
    }

    fun startInvestigationGame() {
        val currentState = _roomState.value
        val totalPlayers = currentState.players.size
        if (totalPlayers < 4) return

        // Shuffle and assign Mafia/Criminal Roles
        // 4 Players = 1 Mafia, 5+ Players = 2 Mafia
        val totalMafiaNeeded = if (totalPlayers <= 4) 1 else 2
        val shuffledIndices = currentState.players.indices.shuffled()
        val mafiaIndices = shuffledIndices.take(totalMafiaNeeded).toSet()

        val activeCase = currentState.currentCase ?: return
        
        val assignedPlayers = currentState.players.mapIndexed { idx, player ->
            val isMafiaRole = mafiaIndices.contains(idx)
            val characterAssigned = activeCase.characters.getOrNull(idx % activeCase.characters.size)
            player.copy(
                isAlive = true,
                isMafia = isMafiaRole,
                character = characterAssigned
            )
        }

        _roomState.value = currentState.copy(
            players = assignedPlayers,
            activePassPlayerIndex = 0,
            phase = GamePhase.ROLE_REVEAL,
            votes = emptyMap(),
            juryVotes = emptyMap(),
            tiedVotePlayers = emptyList()
        )
    }

    fun confirmSecretsRevealed() {
        val currentState = _roomState.value
        val nextIndex = currentState.activePassPlayerIndex + 1
        
        if (currentState.mode == "PASS_AND_PLAY" && nextIndex < currentState.players.size) {
            _roomState.value = currentState.copy(activePassPlayerIndex = nextIndex)
        } else {
            // All players checked their role envelopes -> advance to the story intro phase
            _roomState.value = currentState.copy(
                activePassPlayerIndex = 0,
                phase = GamePhase.CASE_INTRO
            )
        }
    }

    fun startCaseInvestigationIntro() {
        _roomState.value = _roomState.value.copy(phase = GamePhase.EVIDENCE_ROUND)
    }

    fun advanceFromEvidenceToDiscussion() {
        _roomState.value = _roomState.value.copy(phase = GamePhase.DISCUSSION)
    }

    fun advanceFromDiscussionToVoting() {
        _roomState.value = _roomState.value.copy(
            phase = GamePhase.VOTING,
            activePassPlayerIndex = 0
        )
    }

    fun advanceToJuryRound() {
        _roomState.value = _roomState.value.copy(
            phase = GamePhase.JURY_ROUND,
            activePassPlayerIndex = 0
        )
    }

    fun submitVote(targetId: String) {
        val currentState = _roomState.value
        val activeVoters = if (currentState.tiedVotePlayers.isNotEmpty()) {
            currentState.players.filter { it.isAlive && it.id !in currentState.tiedVotePlayers }
        } else {
            currentState.players.filter { it.isAlive }
        }

        if (currentState.mode == "PASS_AND_PLAY") {
            val updatedVotes = currentState.votes.toMutableMap()
            val currentVoter = activeVoters.getOrNull(currentState.activePassPlayerIndex)
            
            if (currentVoter != null) {
                updatedVotes[currentVoter.id] = targetId
                val nextIndex = currentState.activePassPlayerIndex + 1
                
                if (nextIndex < activeVoters.size) {
                    _roomState.value = currentState.copy(
                        votes = updatedVotes,
                        activePassPlayerIndex = nextIndex
                    )
                } else {
                    tallyVotesAndProceed(updatedVotes)
                }
            }
        } else {
            // LAN Multi-device mode handling
            val voterId = myPlayerId.value
            val updatedVotes = currentState.votes.toMutableMap()
            updatedVotes[voterId] = targetId
            _roomState.value = currentState.copy(votes = updatedVotes)
            
            if (isHost() && updatedVotes.size >= activeVoters.size) {
                tallyVotesAndProceed(updatedVotes)
            }
        }
    }

    private fun tallyVotesAndProceed(allVotes: Map<String, String>) {
        val currentState = _roomState.value
        val voteCounts = mutableMapOf<String, Int>()
        allVotes.values.forEach { targetId ->
            voteCounts[targetId] = (voteCounts[targetId] ?: 0) + 1
        }

        if (voteCounts.isEmpty()) {
            goToNextRoundOrJuryScreen()
            return
        }

        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        val highestVotedPlayers = voteCounts.filter { it.value == maxVotes }.keys.toList()

        if (highestVotedPlayers.size > 1) {
            // Tie breaker condition detected
            _roomState.value = currentState.copy(
                tiedVotePlayers = highestVotedPlayers,
                votes = emptyMap(),
                phase = GamePhase.VOTE_RESULT
            )
        } else {
            // A clear candidate is eliminated by popular choice
            val eliminatedId = highestVotedPlayers.first()
            val updatedPlayers = currentState.players.map { player ->
                if (player.id == eliminatedId) player.copy(isAlive = false) else player
            }

            _roomState.value = currentState.copy(
                players = updatedPlayers,
                tiedVotePlayers = emptyList(),
                votes = emptyMap(),
                phase = GamePhase.VOTE_RESULT
            )
        }
    }

    fun confirmVoteResultAndProceed() {
        goToNextRoundOrJuryScreen()
    }

    private fun goToNextRoundOrJuryScreen() {
        val currentState = _roomState.value
        val alivePlayers = currentState.players.filter { it.isAlive }
        val aliveMafiaCount = alivePlayers.count { it.isMafia }

        // Endgame Win conditions check
        if (aliveMafiaCount == 0) {
            _roomState.value = currentState.copy(phase = GamePhase.ENDGAME, winnerSide = "INNOCENT")
            return
        }
        
        if (alivePlayers.size <= 2) {
            // CRITICAL REQUIREMENT MATCH: If down to final round (2-3 players left) and mafia is still alive,
            // we stop active direct execution and transition to the JURY ROUND (All dead players vote).
            _roomState.value = currentState.copy(
                phase = GamePhase.JURY_ROUND,
                activePassPlayerIndex = 0,
                juryVotes = emptyMap()
            )
        } else {
            // Keep inspecting clues in additional cycle rounds
            _roomState.value = currentState.copy(phase = GamePhase.EVIDENCE_ROUND)
        }
    }

    // ==========================================
    // ELIMINATED PLAYERS / JURY VOTING SYSTEM
    // ==========================================
    fun submitJuryVote(targetId: String) {
        val currentState = _roomState.value
        val deadPlayers = currentState.players.filter { !it.isAlive }
        
        if (deadPlayers.isEmpty()) {
            // Safety guard fallback if nobody is dead yet
            val alivePlayers = currentState.players.filter { it.isAlive }
            determineFinalGameWinner(alivePlayers.firstOrNull { it.isMafia }?.id ?: targetId)
            return
        }

        if (currentState.mode == "PASS_AND_PLAY") {
            val updatedJuryVotes = currentState.juryVotes.toMutableMap()
            val currentVoter = deadPlayers.getOrNull(currentState.activePassPlayerIndex)
            
            if (currentVoter != null) {
                updatedJuryVotes[currentVoter.id] = targetId
                val nextIndex = currentState.activePassPlayerIndex + 1
                
                if (nextIndex < deadPlayers.size) {
                    _roomState.value = currentState.copy(
                        juryVotes = updatedJuryVotes,
                        activePassPlayerIndex = nextIndex
                    )
                } else {
                    tallyJuryVotesAndEndGame(updatedJuryVotes)
                }
            }
        } else {
            // Network mode logic
            val voterId = myPlayerId.value
            val updatedJuryVotes = currentState.juryVotes.toMutableMap()
            updatedJuryVotes[voterId] = targetId
            _roomState.value = currentState.copy(juryVotes = updatedJuryVotes)
            
            if (isHost() && updatedJuryVotes.size >= deadPlayers.size) {
                tallyJuryVotesAndEndGame(updatedJuryVotes)
            }
        }
    }

    private fun tallyJuryVotesAndEndGame(juryVotes: Map<String, String>) {
        val tally = mutableMapOf<String, Int>()
        juryVotes.values.forEach { targetId ->
            tally[targetId] = (tally[targetId] ?: 0) + 1
        }
        
        val maxVotes = tally.values.maxOrNull() ?: 0
        val finalConvictedId = tally.filter { it.value == maxVotes }.keys.firstOrNull() ?: ""
        
        determineFinalGameWinner(finalConvictedId)
    }

    private fun determineFinalGameWinner(convictedId: String) {
        val currentState = _roomState.value
        val convictedPlayer = currentState.players.find { it.id == convictedId }

        if (convictedPlayer != null && convictedPlayer.isMafia) {
            // Jury successfully detected and convicted the mafia actor!
            _roomState.value = currentState.copy(phase = GamePhase.ENDGAME, winnerSide = "INNOCENT")
        } else {
            // Mafia survived or innocent suspect framed by the dead crew!
            _roomState.value = currentState.copy(phase = GamePhase.ENDGAME, winnerSide = "MAFIA")
        }
    }

    // Networking connectivity skeletons
    fun startLanHost(title: String) {
        _roomState.value = RoomState(
            roomId = "55555",
            hostId = myPlayerId.value,
            mode = "LAN",
            phase = GamePhase.LOBBY
        )
    }

    fun joinLanHostByCode(code: String, name: String): Boolean {
        _roomState.value = RoomState(
            roomId = code,
            hostId = "remote_host",
            mode = "LAN",
            phase = GamePhase.LOBBY
        )
        return true
    }

    fun joinLanHost(ip: String, name: String) {
        _roomState.value = RoomState(roomId = "77777", hostId = "remote", mode = "LAN", phase = GamePhase.LOBBY)
    }

    fun playAgain() {
        startInvestigationGame()
    }

    fun resetToMainMenu() {
        _roomState.value = RoomState(phase = GamePhase.LOBBY)
    }

    fun isHost(): Boolean {
        return _roomState.value.hostId == myPlayerId.value
    }
}