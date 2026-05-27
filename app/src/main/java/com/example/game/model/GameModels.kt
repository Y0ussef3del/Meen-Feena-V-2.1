package com.example.game.model

import org.json.JSONArray
import org.json.JSONObject

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

data class GameCharacter(
    val name: String = "",
    val occupation: String = "",
    val traits: String = "",
    val hiddenMotive: String = ""
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("name", name)
        put("occupation", occupation)
        put("traits", traits)
        put("hiddenMotive", hiddenMotive)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): GameCharacter = GameCharacter(
            name = obj.optString("name", ""),
            occupation = obj.optString("occupation", ""),
            traits = obj.optString("traits", ""),
            hiddenMotive = obj.optString("hiddenMotive", "")
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
    val character: GameCharacter? = null
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isMafia", isMafia)
        put("isAlive", isAlive)
        put("isConnected", isConnected)
        put("avatarId", avatarId)
        put("character", character?.toJsonObject())
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Player = Player(
            id = json.getString("id"),
            name = json.getString("name"),
            isMafia = json.optBoolean("isMafia", false),
            isAlive = json.optBoolean("isAlive", true),
            isConnected = json.optBoolean("isConnected", true),
            avatarId = json.optInt("avatarId", 0),
            character = json.optJSONObject("character")?.let { GameCharacter.fromJsonObject(it) }
        )
    }
}

data class Case(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val explanation: String = "",
    val characters: List<GameCharacter> = emptyList()
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("description", description)
        put("explanation", explanation)
        val array = JSONArray()
        characters.forEach { array.put(it.toJsonObject()) }
        put("characters", array)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): Case {
            val charsList = mutableListOf<GameCharacter>()
            val charsArray = obj.optJSONArray("characters")
            if (charsArray != null) {
                for (i in 0 until charsArray.length()) {
                    obj.optJSONObject(i)?.let { charsList.add(GameCharacter.fromJsonObject(it)) }
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

// Replace RoomState completely in com.example.game.model.GameModels.kt
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
    val lastEliminatedResult: String = "",
    val lastEliminatedPlayer: Player? = null
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
            lastEliminatedPlayer?.let { put("lastEliminatedPlayer", it.toJsonObject()) }

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
            val playersArr = root.optJSONArray("players")
            if (playersArr != null) {
                for (i in 0 until playersArr.length()) {
                    playersList.add(Player.fromJsonObject(playersArr.getJSONObject(i)))
                }
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

            val eliminatedObj = root.optJSONObject("lastEliminatedPlayer")
            val eliminatedPlayer = eliminatedObj?.let { Player.fromJsonObject(it) }

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
                lastEliminatedResult = lastResult,
                lastEliminatedPlayer = eliminatedPlayer
            )
        }
    }
}