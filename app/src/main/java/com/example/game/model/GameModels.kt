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

data class RoomState(
    val roomId: String = "12345",
    val hostId: String = "player_local",
    val phase: GamePhase = GamePhase.LOBBY,
    val players: List<Player> = emptyList(),
    val mode: String = "PASS_AND_PLAY",
    val activePassPlayerIndex: Int = 0,
    val currentCase: Case? = null,
    val votes: Map<String, String> = emptyMap(),
    val juryVotes: Map<String, String> = emptyMap(),
    val tiedVotePlayers: List<String> = emptyList(),
    val winnerSide: String = "",
    val settings: GameSettings = GameSettings(),
    val lastEliminatedPlayer: Player? = null
)