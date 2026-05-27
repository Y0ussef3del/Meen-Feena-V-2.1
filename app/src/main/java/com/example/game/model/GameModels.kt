package com.example.game.model

import org.json.JSONArray
import org.json.JSONObject

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
            character?.let { put("character", it.toJsonObject()) }
        }
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