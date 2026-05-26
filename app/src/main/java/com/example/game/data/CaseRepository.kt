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

    fun getUniqueCase(completedCaseTitles: Set<String>, playerCount: Int): Case? = synchronized(this) {
        val available = cachedCases.filter { it.title !in completedCaseTitles }
        val pool = if (available.isNotEmpty()) available else cachedCases
        
        if (pool.isEmpty()) return null

        // تصفية مرنة: اختيار القضايا التي تملك عدداً كافياً من الشخصيات لاستيعاب اللاعبين
        val matchingCases = pool.filter { it.characters.size >= playerCount }
        
        return if (matchingCases.isNotEmpty()) {
            matchingCases.random(Random(System.currentTimeMillis()))
        } else {
            // كخيار احتياطي لمنع توقف زر ابدأ: نختار أي قضية متاحة إذا لم يتطابق العدد بدقة
            pool.random(Random(System.currentTimeMillis()))
        }
    }

    fun getAllCases(): List<Case> = cachedCases
}

