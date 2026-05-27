package com.example.game.model

import org.json.JSONArray
import org.json.JSONObject

data class GameSettings(
    val discussionTimeMinutes: Int = 2,
    val votingTimeMinutes: Int = 1,
    val isMusicEnabled: Boolean = true,
    val volume: Float = 0.5f
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("discussionTimeMinutes", discussionTimeMinutes)
        put("votingTimeMinutes", votingTimeMinutes)
        put("isMusicEnabled", isMusicEnabled)
        put("volume", volume.toDouble())
    }

    companion object {
        fun fromJsonObject(json: JSONObject): GameSettings = GameSettings(
            discussionTimeMinutes = json.optInt("discussionTimeMinutes", 2),
            votingTimeMinutes = json.optInt("votingTimeMinutes", 1),
            isMusicEnabled = json.optBoolean("isMusicEnabled", true),
            volume = json.optDouble("volume", 0.5).toFloat()
        )
    }
}

data class Player(
    val id: String,
    val name: String,
    val isMafia: Boolean = false,
    val isAlive: Boolean = true,
    val isConnected: Boolean = true,
    val avatarId: Int = 0,
    val character: Character? = null
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isMafia", isMafia)
        put("isAlive", isAlive)
        put("isConnected", isConnected)
        put("avatarId", avatarId)
        character?.let { put("character", it.toJsonObject()) }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Player {
            val charJson = json.optJSONObject("character")
            return Player(
                id = json.getString("id"),
                name = json.getString("name"),
                isMafia = json.optBoolean("isMafia", false),
                isAlive = json.optBoolean("isAlive", true),
                isConnected = json.optBoolean("isConnected", true),
                avatarId = json.optInt("avatarId", 0),
                character = charJson?.let { Character.fromJsonObject(it) }
            )
        }
    }
}

data class Character(
    val name: String,
    val age: Int,
    val occupation: String,
    val background: String,
    val traits: String,
    val hiddenMotive: String,
    val fullName: String,
    val personalitySummary: String,
    val socialStatus: String,
    val relationshipToVictim: String,
    val relationshipToOtherSuspects: String,
    val possibleMotive: String,
    val relevantHistory: String,
    val isMafia: Boolean
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("name", name)
        put("age", age)
        put("job", occupation)
        put("description", background)
        put("personality", traits)
        put("motive", hiddenMotive)
        put("fullName", fullName)
        put("personalitySummary", personalitySummary)
        put("financialStatus", socialStatus)
        put("relation", relationshipToVictim)
        put("notes", relationshipToOtherSuspects)
        put("possibleMotive", possibleMotive)
        put("hiddenTrait", relevantHistory)
        put("ismafia", isMafia)
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Character {
            val n = json.optString("name", "مجهول")
            val tr = json.optString("personality", json.optString("traits", "غامض"))
            val hm = json.optString("motive", json.optString("hiddenMotive", "غير معروف"))
            return Character(
                name = n,
                age = json.optInt("age", 30),
                occupation = json.optString("job", json.optString("occupation", "مجهول")),
                background = json.optString("description", json.optString("background", "")),
                traits = tr,
                hiddenMotive = hm,
                fullName = json.optString("fullName", n),
                personalitySummary = json.optString("personalitySummary", tr),
                socialStatus = json.optString("financialStatus", "متوسط الحال"),
                relationshipToVictim = json.optString("relation", json.optString("relationshipToVictim", "مجهول")),
                relationshipToOtherSuspects = json.optString("notes", json.optString("relationshipToOtherSuspects", "")),
                possibleMotive = json.optString("possibleMotive", hm),
                relevantHistory = json.optString("hiddenTrait", json.optString("relevantHistory", "سجل خالي من السوابق")),
                isMafia = json.optBoolean("ismafia", false)
            )
        }
    }
}

data class Case(
    val title: String,
    val location: String,
    val time: String,
    val victim: String,
    val victimProfile: String,
    val description: String,
    val characters: List<Character>,
    val evidenceList: List<String>,
    val suspicionDistribution: String,
    val hint: String,
    val explanation: String = ""
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("title", title)
        put("location", location)
        put("time", time)
        put("victim", victim)
        put("victimProfile", victimProfile)
        put("description", description)
        put("suspicionDistribution", suspicionDistribution)
        put("hint", hint)
        put("explanation", explanation)
        put("characters", JSONArray().apply { characters.forEach { put(it.toJsonObject()) } })
        put("evidenceList", JSONArray().apply { evidenceList.forEach { put(it) } })
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Case {
            val chars = mutableListOf<Character>()
            json.optJSONArray("characters")?.let { arr ->
                for (i in 0 until arr.length()) chars.add(Character.fromJsonObject(arr.getJSONObject(i)))
            }
            val ev = mutableListOf<String>()
            json.optJSONArray("evidenceList")?.let { arr ->
                for (i in 0 until arr.length()) ev.add(arr.getString(i))
            }
            return Case(
                title = json.getString("title"),
                location = json.getString("location"),
                time = json.getString("time"),
                victim = json.getString("victim"),
                victimProfile = json.getString("victimProfile"),
                description = json.getString("description"),
                characters = chars,
                evidenceList = ev,
                suspicionDistribution = json.optString("suspicionDistribution", ""),
                hint = json.optString("hint", ""),
                explanation = json.optString("explanation", "")
            )
        }
    }
}

enum class GamePhase {
    LOBBY, ROLE_REVEAL, CASE_INTRO, EVIDENCE_ROUND, DISCUSSION, VOTING, VOTE_RESULT, JURY_ROUND, ENDGAME
}

data class RoomState(
    val roomId: String = "",
    val mode: String = "PASS_AND_PLAY",
    val hostId: String = "",
    val phase: GamePhase = GamePhase.LOBBY,
    val players: List<Player> = emptyList(),
    val currentCase: Case? = null,
    val currentEvidenceIndex: Int = 0,
    val activePassPlayerIndex: Int = 0,
    val rulesRevealed: Boolean = false,
    val timerSecondsLeft: Int = 0,
    val timerTotalSeconds: Int = 0,
    val votes: Map<String, String> = emptyMap(),
    val juryVotes: Map<String, String> = emptyMap(),
    val settings: GameSettings = GameSettings(),
    val gameNumber: Int = 0,
    val winnerSide: String = "",
    val tiedVotePlayers: List<String> = emptyList(),
    val lastEliminatedResult: String = ""
) {
    fun toSharedJsonString(): String = JSONObject().apply {
        put("roomId", roomId)
        put("mode", mode)
        put("hostId", hostId)
        put("phase", phase.name)
        put("currentEvidenceIndex", currentEvidenceIndex)
        put("activePassPlayerIndex", activePassPlayerIndex)
        put("rulesRevealed", rulesRevealed)
        put("timerSecondsLeft", timerSecondsLeft)
        put("timerTotalSeconds", timerTotalSeconds)
        put("gameNumber", gameNumber)
        put("winnerSide", winnerSide)
        put("lastEliminatedResult", lastEliminatedResult)
        put("settings", settings.toJsonObject())
        put("tiedVotePlayers", JSONArray().apply { tiedVotePlayers.forEach { put(it) } })
        currentCase?.let { put("currentCase", it.toJsonObject()) }
        put("players", JSONArray().apply { players.forEach { put(it.toJsonObject()) } })
        put("votes", JSONObject().apply { votes.forEach { (k, v) -> put(k, v) } })
        put("juryVotes", JSONObject().apply { juryVotes.forEach { (k, v) -> put(k, v) } })
    }.toString()

    companion object {
        fun fromSharedJsonString(jsonStr: String): RoomState {
            val root = JSONObject(jsonStr)
            val playersList = mutableListOf<Player>()
            root.getJSONArray("players").let { arr ->
                for (i in 0 until arr.length()) playersList.add(Player.fromJsonObject(arr.getJSONObject(i)))
            }
            val votesMap = mutableMapOf<String, String>()
            root.optJSONObject("votes")?.let { obj ->
                obj.keys().forEach { key -> votesMap[key] = obj.getString(key) }
            }
            val juryMap = mutableMapOf<String, String>()
            root.optJSONObject("juryVotes")?.let { obj ->
                obj.keys().forEach { key -> juryMap[key] = obj.getString(key) }
            }
            val caseObj = root.optJSONObject("currentCase")?.let { Case.fromJsonObject(it) }
            val settingsObj = root.optJSONObject("settings")?.let { GameSettings.fromJsonObject(it) } ?: GameSettings()
            val tvList = mutableListOf<String>()
            root.optJSONArray("tiedVotePlayers")?.let { arr ->
                for (i in 0 until arr.length()) tvList.add(arr.getString(i))
            }
            return RoomState(
                roomId = root.optString("roomId", ""),
                mode = root.optString("mode", "PASS_AND_PLAY"),
                hostId = root.optString("hostId", ""),
                phase = GamePhase.valueOf(root.optString("phase", GamePhase.LOBBY.name)),
                players = playersList,
                currentCase = caseObj,
                currentEvidenceIndex = root.optInt("currentEvidenceIndex", 0),
                activePassPlayerIndex = root.optInt("activePassPlayerIndex", 0),
                rulesRevealed = root.optBoolean("rulesRevealed", false),
                timerSecondsLeft = root.optInt("timerSecondsLeft", 0),
                timerTotalSeconds = root.optInt("timerTotalSeconds", 0),
                votes = votesMap,
                juryVotes = juryMap,
                settings = settingsObj,
                gameNumber = root.optInt("gameNumber", 0),
                winnerSide = root.optString("winnerSide", ""),
                tiedVotePlayers = tvList,
                lastEliminatedResult = root.optString("lastEliminatedResult", "")
            )
        }
    }
}