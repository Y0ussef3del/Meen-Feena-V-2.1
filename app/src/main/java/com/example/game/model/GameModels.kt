package com.example.game.model

import org.json.JSONArray
import org.json.JSONObject

// Game settings
data class GameSettings(
    val discussionTimeMinutes: Int = 4, // تم التعديل لـ 4 دقائق افتراضياً
    val votingTimeMinutes: Int = 4,     // تم التعديل لـ 4 دقائق افتراضياً
    val isMusicEnabled: Boolean = false,
    val volume: Float = 0.5f
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("discussionTimeMinutes", discussionTimeMinutes)
            put("votingTimeMinutes", votingTimeMinutes)
            put("isMusicEnabled", isMusicEnabled)
            put("volume", volume.toDouble())
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): GameSettings {
            return GameSettings(
                discussionTimeMinutes = json.optInt("discussionTimeMinutes", 4),
                votingTimeMinutes = json.optInt("votingTimeMinutes", 4),
                isMusicEnabled = json.optBoolean("isMusicEnabled", false),
                volume = json.optDouble("volume", 0.5).toFloat()
            )
        }
    }
}

// Player details
data class Player(
    val id: String = "",
    val name: String = "",
    val isMafia: Boolean = false,
    val isAlive: Boolean = true,
    val isConnected: Boolean = true,
    val avatarId: Int = 0,
    val character: Character? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("isMafia", isMafia)
            put("isAlive", isAlive)
            put("isConnected", isConnected)
            put("avatarId", avatarId)
            character?.let { put("character", it.toJsonObject()) }
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Player {
            val charJson = json.optJSONObject("character")
            return Player(
                id = json.optString("id", ""),
                name = json.optString("name", ""),
                isMafia = json.optBoolean("isMafia", false),
                isAlive = json.optBoolean("isAlive", true),
                isConnected = json.optBoolean("isConnected", true),
                avatarId = json.optInt("avatarId", 0),
                character = charJson?.let { Character.fromJsonObject(it) }
            )
        }
    }
}

// بيانات الشخصية داخل القضية
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
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
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

// Game Case including deep evidence progress
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
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("title", title)
            put("location", location)
            put("time", time)
            put("victim", victim)
            put("victimProfile", victimProfile)
            put("description", description)
            put("suspicionDistribution", suspicionDistribution)
            put("hint", hint)
            put("explanation", explanation)

            val charsArray = JSONArray()
            characters.forEach { charsArray.put(it.toJsonObject()) }
            put("characters", charsArray)

            val evArray = JSONArray()
            evidenceList.forEach { evArray.put(it) }
            put("evidenceList", evArray)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Case {
            val charsList = mutableListOf<Character>()
            val charsArray = json.optJSONArray("characters")
            if (charsArray != null) {
                for (i in 0 until charsArray.length()) {
                    charsList.add(Character.fromJsonObject(charsArray.getJSONObject(i)))
                }
            }

            val evList = mutableListOf<String>()
            val evArray = json.optJSONArray("evidenceList")
            if (evArray != null) {
                for (i in 0 until evArray.length()) {
                    evList.add(evArray.getString(i))
                }
            }

            return Case(
                title = json.optString("title", "قضية غامضة"),
                location = json.optString("location", ""),
                time = json.optString("time", ""),
                victim = json.optString("victim", ""),
                victimProfile = json.optString("victimProfile", ""),
                description = json.optString("description", ""),
                characters = charsList,
                evidenceList = evList,
                suspicionDistribution = json.optString("suspicionDistribution", ""),
                hint = json.optString("hint", ""),
                explanation = json.optString("explanation", "")
            )
        }
    }
}

// Current game phase enumeration
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

// The comprehensive shared room game state
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
    fun toSharedJsonString(): String {
        val root = JSONObject().apply {
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

            val tvArray = JSONArray()
            tiedVotePlayers.forEach { tvArray.put(it) }
            put("tiedVotePlayers", tvArray)

            currentCase?.let { put("currentCase", it.toJsonObject()) }

            val playersArray = JSONArray()
            players.forEach { playersArray.put(it.toJsonObject()) }
            put("players", playersArray)

            val votesObj = JSONObject()
            votes.forEach { (k, v) -> votesObj.put(k, v) }
            put("votes", votesObj)

            val jVotesObj = JSONObject()
            juryVotes.forEach { (k, v) -> jVotesObj.put(k, v) }
            put("juryVotes", jVotesObj)
        }
        return root.toString()
    }

    companion object {
        fun fromSharedJsonString(jsonStr: String): RoomState {
            val root = JSONObject(jsonStr)
            val playersList = mutableListOf<Player>()
            val playersArr = root.getJSONArray("players")
            for (i in 0 until playersArr.length()) {
                playersList.add(Player.fromJsonObject(playersArr.getJSONObject(i)))
            }

            val votesMap = mutableMapOf<String, String>()
            val votesObj = root.optJSONObject("votes")
            if (votesObj != null) {
                val keys = votesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    votesMap[k] = votesObj.getString(k)
                }
            }

            val jVotesMap = mutableMapOf<String, String>()
            val jVotesObj = root.optJSONObject("juryVotes")
            if (jVotesObj != null) {
                val keys = jVotesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    jVotesMap[k] = jVotesObj.getString(k)
                }
            }

            val caseObj = root.optJSONObject("currentCase")
            val case = caseObj?.let { Case.fromJsonObject(it) }

            val settingsObj = root.optJSONObject("settings")
            val settings = if (settingsObj != null) GameSettings.fromJsonObject(settingsObj) else GameSettings()

            val lastResult = root.optString("lastEliminatedResult", "")
            val tvList = mutableListOf<String>()
            val tvArray = root.optJSONArray("tiedVotePlayers")
            if (tvArray != null) {
                for (i in 0 until tvArray.length()) {
                    tvList.add(tvArray.getString(i))
                }
            }

            return RoomState(
                roomId = root.optString("roomId", ""),
                mode = root.optString("mode", "PASS_AND_PLAY"),
                hostId = root.optString("hostId", ""),
                phase = GamePhase.valueOf(root.optString("phase", GamePhase.LOBBY.name)),
                players = playersList,
                currentCase = case,
                currentEvidenceIndex = root.optInt("currentEvidenceIndex", 0),
                activePassPlayerIndex = root.optInt("activePassPlayerIndex", 0),
                rulesRevealed = root.optBoolean("rulesRevealed", false),
                timerSecondsLeft = root.optInt("timerSecondsLeft", 0),
                timerTotalSeconds = root.optInt("timerTotalSeconds", 0),
                votes = votesMap,
                juryVotes = jVotesMap,
                settings = settings,
                gameNumber = root.optInt("gameNumber", 0),
                winnerSide = root.optString("winnerSide", ""),
                tiedVotePlayers = tvList,
                lastEliminatedResult = lastResult
            )
        }
    }
}