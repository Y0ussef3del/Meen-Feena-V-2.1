package com.example.game.data

import android.content.Context
import android.net.Uri
import com.example.game.model.Case
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections

object CaseRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    @Volatile
    private var file: File? = null
    private val mutex = Mutex()

    private val defaultCases = Collections.synchronizedList(mutableListOf<Case>())
    private val customCases = Collections.synchronizedList(mutableListOf<Case>())

    fun init(context: Context) {
        if (file == null) {
            synchronized(this) {
                if (file == null) {
                    val targetFile = File(context.applicationContext.filesDir, "user_cases.json")
                    file = targetFile
                    if (targetFile.exists()) {
                        try {
                            val text = targetFile.readText()
                            if (text.isNotBlank()) {
                                val loaded = json.decodeFromString(
                                    kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                                    text
                                )
                                synchronized(customCases) {
                                    customCases.clear()
                                    customCases.addAll(loaded)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun loadCasesFromJson(jsonString: String) {
        try {
            val loaded = JSONObjectToCaseList(jsonString)
            synchronized(defaultCases) {
                defaultCases.clear()
                defaultCases.addAll(loaded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDefaultCases(): List<Case> = synchronized(defaultCases) { defaultCases.toList() }

    fun getUniqueCase(completedTitles: Set<String>, playersCount: Int): Case? {
        val allAvailableCases = synchronized(customCases) { customCases.toList() } + synchronized(defaultCases) { defaultCases.toList() }
        val matchingCases = allAvailableCases.filter { it.characters.size == playersCount }

        if (matchingCases.isEmpty()) return null

        val available = matchingCases.filter { it.title !in completedTitles }

        return if (available.isNotEmpty()) {
            available.random()
        } else {
            matchingCases.random()
        }
    }

    private fun JSONObjectToCaseList(jsonString: String): List<Case> {
        val list = mutableListOf<Case>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                list.add(Case.fromJsonObject(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    suspend fun loadAllCustomCases(): List<Case> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val targetFile = file ?: return@withContext synchronized(customCases) { customCases.toList() }
            if (!targetFile.exists()) return@withContext synchronized(customCases) { customCases.toList() }
            return@withContext try {
                val text = targetFile.readText()
                if (text.isNotBlank()) {
                    val loaded = json.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                        text
                    )
                    synchronized(customCases) {
                        customCases.clear()
                        customCases.addAll(loaded)
                    }
                    loaded
                } else {
                    synchronized(customCases) { customCases.toList() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                synchronized(customCases) { customCases.toList() }
            }
        }
    }

    suspend fun saveCase(newCase: Case) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val targetFile = file ?: return@withContext

            val currentCases = if (targetFile.exists() && targetFile.readText().isNotBlank()) {
                try {
                    json.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                        targetFile.readText()
                    ).toMutableList()
                } catch (e: Exception) {
                    synchronized(customCases) { customCases.toMutableList() }
                }
            } else {
                synchronized(customCases) { customCases.toMutableList() }
            }

            currentCases.removeAll { it.id == newCase.id || it.title == newCase.title }
            currentCases.add(newCase)

            val jsonString = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                currentCases
            )
            targetFile.writeText(jsonString)

            synchronized(customCases) {
                customCases.removeAll { it.id == newCase.id || it.title == newCase.title }
                customCases.add(newCase)
            }
        }
    }

    suspend fun deleteCase(caseId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val targetFile = file ?: return@withContext
            val currentCases = synchronized(customCases) { customCases.toMutableList() }
            currentCases.removeAll { it.id == caseId }

            targetFile.writeText(
                json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                    currentCases
                )
            )

            synchronized(customCases) {
                customCases.removeAll { it.id == caseId }
            }
        }
    }

    suspend fun importCases(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            init(context)
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (!content.isNullOrEmpty()) {
                val importedList = mutableListOf<Case>()

                try {
                    val trimmed = content.trim()
                    if (trimmed.startsWith("[")) {
                        val array = JSONArray(trimmed)
                        for (i in 0 until array.length()) {
                            importedList.add(Case.fromJsonObject(array.getJSONObject(i)))
                        }
                    } else if (trimmed.startsWith("{")) {
                        importedList.add(Case.fromJsonObject(JSONObject(trimmed)))
                    } else {
                        val imported = json.decodeFromString(
                            kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                            content
                        )
                        importedList.addAll(imported)
                    }
                } catch (e: Exception) {
                    try {
                        val imported = json.decodeFromString(
                            kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                            content
                        )
                        importedList.addAll(imported)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                if (importedList.isNotEmpty()) {
                    mutex.withLock {
                        val currentCases = synchronized(customCases) { customCases.toMutableList() }

                        importedList.forEach { importedCase ->
                            val finalCase = if (importedCase.id.isBlank()) {
                                importedCase.copy(id = java.util.UUID.randomUUID().toString())
                            } else {
                                importedCase
                            }
                            currentCases.removeAll { it.id == finalCase.id || it.title == finalCase.title }
                            currentCases.add(finalCase)
                        }

                        file?.writeText(
                            json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(Case.serializer()),
                                currentCases
                            )
                        )

                        synchronized(customCases) {
                            customCases.clear()
                            customCases.addAll(currentCases)
                        }
                    }
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}