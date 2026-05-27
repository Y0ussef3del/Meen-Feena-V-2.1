package com.example.game.data

import com.example.game.model.Case
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
        val matchingCases = pool.filter { it.characters.size >= playerCount }
        
        return if (matchingCases.isNotEmpty()) {
            matchingCases.random(Random(System.currentTimeMillis()))
        } else {
            null
        }
    }

    fun getAllCases(): List<Case> = cachedCases
}