package com.example.game.data

import android.util.Log
import com.example.game.model.Case
import org.json.JSONArray
import kotlin.random.Random

object CaseRepository {
    private const val TAG = "CaseRepository"
    private var cachedCases: List<Case> = emptyList()

    fun loadCasesFromJson(jsonString: String) {
        if (jsonString.isBlank()) {
            Log.e(TAG, "loadCasesFromJson failed: Provided JSON string is empty or blank.")
            cachedCases = emptyList()
            return
        }

        val casesList = mutableListOf<Case>()
        val seenTitles = mutableSetOf<String>()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.optJSONObject(i) ?: continue
                try {
                    val title = jsonObject.optString("title", "").trim()
                    if (title.isEmpty()) {
                        Log.w(TAG, "Malformed entry at index $i: Missing required 'title' field. Skipping.")
                        continue
                    }
                    if (seenTitles.contains(title)) {
                        Log.w(TAG, "Duplicate entry detected for case title: '$title'. Skipping.")
                        continue
                    }

                    // Parse through safe model layer
                    val caseItem = Case.fromJsonObject(jsonObject)
                    
                    // Defensive validation on player/character layout
                    if (caseItem.characters.isEmpty()) {
                        Log.w(TAG, "Invalid configuration for case '$title': Character list is empty. Skipping.")
                        continue
                    }

                    casesList.add(caseItem)
                    seenTitles.add(title)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception encountered parsing entry at index $i: ${e.message}", e)
                }
            }
            cachedCases = casesList
            Log.d(TAG, "Successfully validated and cached ${cachedCases.size} game cases.")
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure reading JSON array structure: ${e.message}", e)
            cachedCases = emptyList()
        }
    }

    /**
     * Requirement Bug #1: Returns a random case matching the exact player count, or null if missing.
     */
    fun getCaseByPlayerCount(playerCount: Int): Case? {
        if (playerCount <= 0) {
            Log.w(TAG, "getCaseByPlayerCount requested with an invalid count: $playerCount")
            return null
        }
        val matchingCases = cachedCases.filter { it.characters.size == playerCount }
        return if (matchingCases.isNotEmpty()) {
            matchingCases.random(Random(System.currentTimeMillis()))
        } else {
            Log.w(TAG, "No exact match found in JSON cases database for player count: $playerCount")
            null
        }
    }

    fun getUniqueCase(completedCaseTitles: Set<String>, playerCount: Int): Case? {
        if (playerCount <= 0) return null

        // Filter database strictly by matching player configuration first
        val matchingCountCases = cachedCases.filter { it.characters.size == playerCount }
        if (matchingCountCases.isEmpty()) {
            Log.w(TAG, "No cases match player count criteria ($playerCount). Returning null.")
            return null
        }

        // Isolate remaining unplayed cases
        val available = matchingCountCases.filter { it.title !in completedCaseTitles }
        
        // Mandatory behavior: If everything was completed, reset pool using matching player counts ONLY
        val pool = if (available.isNotEmpty()) available else matchingCountCases
        
        return pool.random(Random(System.currentTimeMillis()))
    }

    fun getAllCases(): List<Case> = cachedCases
}