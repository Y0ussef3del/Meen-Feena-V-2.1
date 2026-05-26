package com.example.game.model

import org.json.JSONArray
import org.json.JSONObject

// إعدادات اللعبة
data class GameSettings(
    val discussionTimeMinutes: Int = 2,
    val votingTimeMinutes: Int = 1,
    val isMusicEnabled: Boolean = true,
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
                discussionTimeMinutes = json.optInt("discussionTimeMinutes", 2),
                votingTimeMinutes = json.optInt("votingTimeMinutes", 1),
                isMusicEnabled = json.optBoolean("isMusicEnabled", true),
                volume = json.optDouble("volume", 0.5).toFloat()
            )
        }
    }
}

// بيانات اللاعب
data class Player(
    val id: String,
    val name: String,
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
            if (character != null) {
                put("character", character.toJsonObject())
            }
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Player {
            val charObj = json.optJSONObject("character")
            val character = if (charObj != null) Character.fromJsonObject(charObj) else null
            return Player(
                id = json.optString("id", ""),
                name = json.optString("name", ""),
                isMafia = json.optBoolean("isMafia", false),
                isAlive = json.optBoolean("isAlive", true),
                isConnected = json.optBoolean("isConnected", true),
                avatarId = json.optInt("avatarId", 0),
                character = character
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
    val relevantHistory: String
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
                relevantHistory = json.optString("hiddenTrait", json.optString("relevantHistory", "سجل خالي من السوابق"))
            )
        }
    }
}

// بيانات القضية بالكامل
data class Case(
    val title: String,
    val location: String,
    val time: String,
    val victim: String,
    val victimProfile: String,
    val description: String,
    val characters: List<Character>,
    val evidenceList: List<String>,
    val hint: String = "",
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
            put("hint", hint)
            put("explanation", explanation)
            
            val charArray = JSONArray()
            characters.forEach { charArray.put(it.toJsonObject()) }
            put("characters", charArray)
            
            val evArray = JSONArray()
            evidenceList.forEach { evArray.put(it) }
            put("evidenceList", evArray)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Case {
            val charList = mutableListOf<Character>()
            val charArray = json.optJSONArray("characters")
            if (charArray != null) {
                for (i in 0 until charArray.length()) {
                    charList.add(Character.fromJsonObject(charArray.getJSONObject(i)))
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
                title = json.optString("title", ""),
                location = json.optString("location", ""),
                time = json.optString("time", ""),
                victim = json.optString("victim", ""),
                victimProfile = json.optString("victimProfile", ""),
                description = json.optString("description", ""),
                characters = charList,
                evidenceList = evList,
                hint = json.optString("hint", ""),
                explanation = json.optString("explanation", "")
            )
        }
    }
}

// جميع مراحل اللعبة متطابقة تماماً مع الـ ViewModel والـ UI Navigation
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

// حالة الغرفة المشتركة والربط عبر الشبكة المحلية
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
        return toJsonObject().toString()
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("roomId", roomId)
            put("mode", mode)
            put("hostId", hostId)
            put("phase", phase.name)
            
            val pArray = JSONArray()
            players.forEach { pArray.put(it.toJsonObject()) }
            put("players", pArray)
            
            if (currentCase != null) {
                put("currentCase", currentCase.toJsonObject())
            }
            
            put("currentEvidenceIndex", currentEvidenceIndex)
            put("activePassPlayerIndex", activePassPlayerIndex)
            put("rulesRevealed", rulesRevealed)
            put("timerSecondsLeft", timerSecondsLeft)
            put("timerTotalSeconds", timerTotalSeconds)
            
            val vObj = JSONObject()
            votes.forEach { (k, v) -> vObj.put(k, v) }
            put("votes", vObj)
            
            val jObj = JSONObject()
            juryVotes.forEach { (k, v) -> jObj.put(k, v) }
            put("juryVotes", jObj)
            
            put("settings", settings.toJsonObject())
            put("gameNumber", gameNumber)
            put("winnerSide", winnerSide)
            
            val tvArray = JSONArray()
            tiedVotePlayers.forEach { tvArray.put(it) }
            put("tiedVotePlayers", tvArray)
            
            put("lastEliminatedResult", lastEliminatedResult)
        }
    }

    companion object {
        fun fromSharedJsonString(jsonString: String): RoomState {
            val root = JSONObject(jsonString)
            return fromJsonObject(root)
        }

        fun fromJsonObject(jsonString: String): RoomState {
            val root = JSONObject(jsonString)
            return fromJsonObject(root)
        }

        fun fromJsonObject(root: JSONObject): RoomState {
            val playersList = mutableListOf<Player>()
            val pArray = root.optJSONArray("players")
            if (pArray != null) {
                for (i in 0 until pArray.length()) {
                    playersList.add(Player.fromJsonObject(pArray.getJSONObject(i)))
                }
            }

            val caseObj = root.optJSONObject("currentCase")
            val case = if (caseObj != null) Case.fromJsonObject(caseObj) else null

            val votesMap = mutableMapOf<String, String>()
            val vObj = root.optJSONObject("votes")
            if (vObj != null) {
                val keys = vObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    votesMap[key] = vObj.getString(key)
                }
            }

            val jVotesMap = mutableMapOf<String, String>()
            val jObj = root.optJSONObject("juryVotes")
            if (jObj != null) {
                val keys = jObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    jVotesMap[key] = jObj.getString(key)
                }
            }

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