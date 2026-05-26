package com.example.game.data

import com.example.game.model.Case
import com.example.game.model.Character
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

object CaseRepository {
    
    // قائمة ديناميكية لتخزين القضايا بعد قراءتها من ملف الـ JSON
    private var cachedCases: List<Case> = emptyList()

    /**
     * يقوم بتحميل القضايا وتحليلها من نص JSON وتخزينها في الـ cache.
     * تم اعتماده بناءً على نفس أسلوب التخزين والمعالجة في GameSettings و Player.
     */
    fun loadCasesFromJson(jsonString: String) {
        val casesList = mutableListOf<Case>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                // إنشاء كائن القضية باستخدام optString لضمان عدم حدوث Crash وتوفير قيم افتراضية
                val caseItem = Case(
                    title = jsonObject.optString("title", ""),
                    location = jsonObject.optString("location", ""),
                    time = jsonObject.optString("time", ""),
                    victim = jsonObject.optString("victim", ""),
                    victimProfile = jsonObject.optString("victimProfile", ""),
                    description = jsonObject.optString("description", ""),
                    hint = jsonObject.optString("hint", ""),
                    explanation = jsonObject.optString("explanation", "")
                )
                casesList.add(caseItem)
            }
            cachedCases = casesList
        } catch (e: Exception) {
            e.printStackTrace()
            // في حالة حدوث خطأ في القراءة تظل القائمة فارغة لحماية التطبيق من الانهيار
            cachedCases = emptyList()
        }
    }

    /**
     * جلب قضية عشوائية لم يلعبها المستخدم من قبل بناءً على العناوين المكتملة.
     */
    fun getUniqueCase(completedCaseTitles: Set<String>, playerCount: Int): Case? {
        val available = cachedCases.filter { it.title !in completedCaseTitles }
        if (available.isEmpty()) {
            return null
        }
        val randomIndex = Random.nextInt(available.size)
        return available[randomIndex]
    }

    /**
     * ميثود مساعدة للحصول على جميع القضايا المحملة حالياً (لأغراض الفحص أو التطوير).
     */
    fun getAllCases(): List<Case> {
        return cachedCases
    }
}