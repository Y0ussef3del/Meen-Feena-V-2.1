package com.example.game.data

// Change the old model import to target the unified Case structure inside your ViewModel file
import com.example.game.viewmodel.Case
import org.json.JSONArray
import kotlin.random.Random

object CaseRepository {
    private var cachedCases: List<Case> = emptyList()

    fun loadCasesFromJson(jsonString: String) {
        val casesList = mutableListOf<Case>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                // Calls Case.fromJsonObject from com.example.game.viewmodel.Case
                val caseItem = Case.fromJsonObject(jsonObject)
                casesList.add(caseItem)
            }
            cachedCases = casesList
        } catch (e: Exception) {
            e.printStackTrace()
            cachedCases = emptyList()
        }
    }

    fun getUniqueCase(completedCaseTitles: Set<String>, playerCount: Int): Case? {
        val available = cachedCases.filter { it.title !in completedCaseTitles }
        val pool = if (available.isNotEmpty()) available else cachedCases
        
        // Filter out cases where the characters count matches the local players count exactly
        val matchingCases = pool.filter { it.characters.size == playerCount }
        
        return if (matchingCases.isNotEmpty()) {
            matchingCases.random(Random(System.currentTimeMillis()))
        } else {
            null
        }
    }

    fun getAllCases(): List<Case> = cachedCases
}