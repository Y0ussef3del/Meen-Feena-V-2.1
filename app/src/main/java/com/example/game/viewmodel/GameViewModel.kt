package com.example.game.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.data.CaseRepository
import org.json.JSONArray
import org.json.JSONObject

// ==========================================
// 1. GAME DATA STRUCTURES & MODELS
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
) {
    companion object {
        fun fromJsonObject(obj: JSONObject): GameCharacter {
            return GameCharacter(
                name = obj.optString("name", ""),
                occupation = obj.optString("occupation", ""),
                traits = obj.optString("traits", ""),
                hiddenMotive = obj.optString("hiddenMotive", "")
            )
        }
    }
}

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
) {
    companion object {
        fun fromJsonObject(obj: JSONObject): Case {
            val charsList = mutableListOf<GameCharacter>()
            val charsArray = obj.optJSONArray("characters")
            if (charsArray != null) {
                for (i in 0 until charsArray.length()) {
                    val charObj = charsArray.optJSONObject(i)
                    if (charObj != null) {
                        charsList.add(GameCharacter.fromJsonObject(charObj))
                    }
                }
            }
            return Case(
                id = obj.optString("id", ""),
                title = obj.optString("title", ""),
                description = obj.optString("description", ""),
                explanation = obj.optString("explanation", ""),
                characters = charsList
            )
        }
    }
}

data class RoomState(
    val roomId: String = "12345",
    val hostId: String = "player_local",
    val phase: GamePhase = GamePhase.LOBBY,
    val players: List<Player> = emptyList(),
    val mode: String = "PASS_AND_PLAY", // PASS_AND_PLAY or LAN
    val activePassPlayerIndex: Int = 0,
    val currentCase: Case? = null,
    val votes: Map<String, String> = emptyMap(),       // VoterID -> TargetPlayerID
    val juryVotes: Map<String, String> = emptyMap(),   // Eliminated -> Remaining Target
    val tiedVotePlayers: List<String> = emptyList(),
    val winnerSide: String = "",                       // MAFIA or INNOCENT
    val discussionDurationMins: Int = 2,
    // EXPLICIT TRACKER: Holds the reference to the last eliminated player to reveal their true identity
    val lastEliminatedPlayer: Player? = null
)

// ==========================================
// 2. MAIN VIEWMODEL IMPLEMENTATION
// ==========================================

class GameViewModel : ViewModel() {

    val myPlayerId = MutableStateFlow("player_local")
    val myPlayerName = MutableStateFlow("المحقق")

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    private val _discussionDurationMins = MutableStateFlow(2)
    val discussionDurationMins: StateFlow<Int> = _discussionDurationMins.asStateFlow()

    init {
        loadDynamicGameCase()
    }

    /**
     * Fix 1: Pulls live parsed items loaded dynamically from JSON instead of forcing a default fallback.
     */
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
            GameCharacter("الدكتور سامح", "طبيب جراح", "عصبي وغامض", "كان يريد التخلص من الضحية بسبب سر طبي قديم"),
            GameCharacter("المهندس كريم", "مهندس معمار", "هادئ وملاحظ", "الضحية ابتزته بمبالغ مالية ضخمة"),
            GameCharacter("الأستاذة فريدة", "محامية العائلة", "ذكية وسريعة الرد", "أنت بريء حاول تكتشف المجرم الحقيقي"),
            GameCharacter("الحارس عثمان", "حارس الفيلا", "قوي وبسيط", "رأى الجريمة ولكنه خائف من التحدث")
        )
        val defaultCase = Case(
            id = "case_fallback",
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
        if (currentState.players.size >= 8) return
        
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

        // Dynamic case injection logic linked against current layout selection constraints
        val runtimeCase = CaseRepository.getUniqueCase(emptySet(), totalPlayers) ?: currentState.currentCase

        val totalMafiaNeeded = if (totalPlayers <= 4) 1 else 2
        val shuffledIndices = currentState.players.indices.shuffled()
        val mafiaIndices = shuffledIndices.take(totalMafiaNeeded).toSet()

        val assignedPlayers = currentState.players.mapIndexed { idx, player ->
            val isMafiaRole = mafiaIndices.contains(idx)
            val characterAssigned = runtimeCase?.characters?.getOrNull(idx % runtimeCase.characters.size)
            player.copy(
                isAlive = true,
                isMafia = isMafiaRole,
                character = characterAssigned
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
            // LAN Synchronization handling
            val voterId = myPlayerId.value
            val updatedVotes = currentState.votes.toMutableMap()
            updatedVotes[voterId] = targetId
            _roomState.value = currentState.copy(votes = updatedVotes)
            
            if (isHost() && updatedVotes.size >= activeVoters.size) {
                tallyVotesAndProceed(updatedVotes)
            }
        }
    }

    /**
     * Fix 2: Captures the precise attributes of the eliminated target and exposes them to the state architecture.
     */
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
            // Tie condition encountered
            _roomState.value = currentState.copy(
                tiedVotePlayers = highestVotedPlayers,
                votes = emptyMap(),
                phase = GamePhase.VOTE_RESULT,
                lastEliminatedPlayer = null
            )
        } else {
            // An isolated player was targeted and voted out
            val eliminatedId = highestVotedPlayers.first()
            var eliminatedTargetCopy: Player? = null

            val updatedPlayers = currentState.players.map { player ->
                if (player.id == eliminatedId) {
                    val killed = player.copy(isAlive = false)
                    eliminatedTargetCopy = killed // Save full reference with accurate identity tags
                    killed
                } else {
                    player
                }
            }

            _roomState.value = currentState.copy(
                players = updatedPlayers,
                tiedVotePlayers = emptyList(),
                votes = emptyMap(),
                phase = GamePhase.VOTE_RESULT,
                lastEliminatedPlayer = eliminatedTargetCopy
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

        if (aliveMafiaCount == 0) {
            _roomState.value = currentState.copy(phase = GamePhase.ENDGAME, winnerSide = "INNOCENT")
            return
        }
        
        if (alivePlayers.size <= 2) {
            // If down to 2 players, advance to the final Jury Round where eliminated players break the tie
            _roomState.value = currentState.copy(
                phase = GamePhase.JURY_ROUND,
                activePassPlayerIndex = 0,
                juryVotes = emptyMap()
            )
        } else {
            // Re-cycle loop back to clues tracking phase
            _roomState.value = currentState.copy(phase = GamePhase.EVIDENCE_ROUND)
        }
    }

    // ==========================================
    // 3. JURY / ELIMINATED PLAYERS VOTING SYSTEM
    // ==========================================
    fun submitJuryVote(targetId: String) {
        val currentState = _roomState.value
        val deadPlayers = currentState.players.filter { !it.isAlive }
        
        if (deadPlayers.isEmpty()) {
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
            _roomState.value = currentState.copy(phase = GamePhase.ENDGAME, winnerSide = "INNOCENT")
        } else {
            _roomState.value = currentState.copy(phase = GamePhase.ENDGAME, winnerSide = "MAFIA")
        }
    }

    // ==========================================
    // 4. LAN SKELETON HANDLERS
    // ==========================================
    fun startLanHost(title: String) {
        val currentCaseData = _roomState.value.currentCase
        _roomState.value = RoomState(
            roomId = (1000..9999).random().toString(),
            hostId = myPlayerId.value,
            mode = "LAN",
            phase = GamePhase.LOBBY,
            currentCase = currentCaseData
        )
    }

    fun joinLanHostByCode(code: String, name: String): Boolean {
        _roomState.value = RoomState(
            roomId = code,
            hostId = "remote_host_id",
            mode = "LAN",
            phase = GamePhase.LOBBY
        )
        return true
    }

    fun joinLanHost(ip: String, name: String) {
        _roomState.value = RoomState(roomId = "9999", hostId = "remote_ip_id", mode = "LAN", phase = GamePhase.LOBBY)
    }

    fun playAgain() {
        loadDynamicGameCase()
        startInvestigationGame()
    }

    fun resetToMainMenu() {
        _roomState.value = RoomState(phase = GamePhase.LOBBY)
        loadDynamicGameCase()
    }

    fun isHost(): Boolean {
        return _roomState.value.hostId == myPlayerId.value
    }
}