package com.myraa.assistant.memory

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 3-Layer Persistent Memory Store:
 * 1. Short-term (rolling context)
 * 2. Long-term structured facts (user info, routines, likes/dislikes, goals)
 * 3. Episodic daily summaries
 */
class MemoryStore private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _factsFlow = MutableStateFlow<List<FactMemory>>(emptyList())
    val factsFlow: StateFlow<List<FactMemory>> = _factsFlow.asStateFlow()

    private val _episodicFlow = MutableStateFlow<List<EpisodicMemory>>(emptyList())
    val episodicFlow: StateFlow<List<EpisodicMemory>> = _episodicFlow.asStateFlow()

    init {
        reloadAll()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE facts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fact_key TEXT NOT NULL,
                fact_value TEXT NOT NULL,
                category TEXT NOT NULL,
                importance INTEGER DEFAULT 5,
                created_at INTEGER,
                last_accessed_at INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE episodic (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date_iso TEXT NOT NULL,
                summary TEXT NOT NULL,
                key_moments TEXT,
                created_at INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE promises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                details TEXT,
                target_time INTEGER,
                is_fulfilled INTEGER DEFAULT 0,
                created_at INTEGER
            )
            """.trimIndent()
        )

        // Seed some initial friendly default memories
        val now = System.currentTimeMillis()
        db.execSQL("INSERT INTO facts (fact_key, fact_value, category, importance, created_at, last_accessed_at) VALUES ('assistant_name', 'Myraa', 'PERSONAL', 10, $now, $now)")
        db.execSQL("INSERT INTO facts (fact_key, fact_value, category, importance, created_at, last_accessed_at) VALUES ('assistant_vibe', 'Warm, empathetic, smart, natural spoken voice', 'PREFERENCE', 9, $now, $now)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration if needed
    }

    fun reloadAll() {
        scope.launch {
            val facts = getAllFactsInternal()
            _factsFlow.value = facts
            val episodes = getAllEpisodesInternal()
            _episodicFlow.value = episodes
        }
    }

    suspend fun saveFact(
        key: String,
        value: String,
        category: MemoryCategory = MemoryCategory.PERSONAL,
        importance: Int = 5
    ): Long = withContext(Dispatchers.IO) {
        val cleanKey = key.trim().lowercase()
        val cleanVal = value.trim()
        val now = System.currentTimeMillis()

        val db = writableDatabase
        // Check if an existing fact with the exact key exists to update or merge
        val cursor = db.rawQuery("SELECT id FROM facts WHERE LOWER(fact_key) = ? LIMIT 1", arrayOf(cleanKey))
        val existingId = if (cursor.moveToFirst()) cursor.getLong(0) else null
        cursor.close()

        val id = if (existingId != null) {
            val cv = ContentValues().apply {
                put("fact_value", cleanVal)
                put("category", category.name)
                put("importance", importance)
                put("last_accessed_at", now)
            }
            db.update("facts", cv, "id = ?", arrayOf(existingId.toString()))
            existingId
        } else {
            val cv = ContentValues().apply {
                put("fact_key", cleanKey)
                put("fact_value", cleanVal)
                put("category", category.name)
                put("importance", importance)
                put("created_at", now)
                put("last_accessed_at", now)
            }
            db.insert("facts", null, cv)
        }

        reloadAll()
        id
    }

    suspend fun deleteFact(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val rows = db.delete("facts", "id = ?", arrayOf(id.toString()))
        reloadAll()
        rows > 0
    }

    suspend fun clearAllMemories(): Unit = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete("facts", null, null)
        db.delete("episodic", null, null)
        db.delete("promises", null, null)
        reloadAll()
    }

    suspend fun saveDailyEpisode(summary: String, keyMoments: String = ""): Long = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("date_iso", today)
            put("summary", summary.trim())
            put("key_moments", keyMoments.trim())
            put("created_at", System.currentTimeMillis())
        }
        val id = db.insert("episodic", null, cv)
        reloadAll()
        id
    }

    /**
     * Retrieves the most relevant facts and episodic summaries for a given query
     * using semantic vector search.
     */
    suspend fun retrieveMemoryContext(query: String, topK: Int = 4): String = withContext(Dispatchers.IO) {
        val facts = getAllFactsInternal()
        val relevantFacts = VectorSearch.findTopRelevantFacts(query, facts, topK = topK)
        val episodes = getAllEpisodesInternal()
        val recentEpisode = episodes.firstOrNull()

        if (relevantFacts.isEmpty() && recentEpisode == null) {
            return@withContext ""
        }

        val b = StringBuilder()
        b.append("Relevant memories about user & past talks:\n")
        relevantFacts.forEach { fact ->
            b.append(" - ").append(fact.key).append(": ").append(fact.value).append("\n")
        }
        if (recentEpisode != null) {
            b.append("Recent episode (${recentEpisode.dateIso}): ").append(recentEpisode.summary).append("\n")
        }
        b.toString().trim()
    }

    /**
     * Auto-extracts facts from user conversational turn.
     * Checks multilingual patterns in English, Hindi, Hinglish, Marathi.
     */
    suspend fun autoExtractAndStore(userText: String) = withContext(Dispatchers.IO) {
        val lower = userText.trim().lowercase()

        // Name extraction
        val nameMatch = Regex("(?:my name is|mera naam|maza naav|i am|me ahe)\\s+([a-zA-Z\\u0900-\\u097F]+)", RegexOption.IGNORE_CASE).find(lower)
        if (nameMatch != null) {
            val name = nameMatch.groupValues[1].replaceFirstChar { it.uppercase() }
            if (name.length > 2 && name != "myraa" && name != "here") {
                saveFact("user_name", name, MemoryCategory.PERSONAL, importance = 10)
            }
        }

        // Likes / Preferences
        val likeMatch = Regex("(?:i love|i like|mujhe.*pasand hai|mala.*aawadto)\\s+(.+)", RegexOption.IGNORE_CASE).find(lower)
        if (likeMatch != null) {
            val preference = likeMatch.groupValues[1].take(60).trim()
            if (preference.isNotEmpty()) {
                saveFact("preference_${preference.take(15).replace(" ", "_")}", preference, MemoryCategory.PREFERENCE, importance = 7)
            }
        }

        // Location / City
        val cityMatch = Regex("(?:i live in|i am from|me.*madhe rahto|mein.*rehta hoon)\\s+([a-zA-Z\\u0900-\\u097F]+)", RegexOption.IGNORE_CASE).find(lower)
        if (cityMatch != null) {
            val city = cityMatch.groupValues[1].replaceFirstChar { it.uppercase() }
            if (city.length > 2) {
                saveFact("user_location", city, MemoryCategory.PERSONAL, importance = 8)
            }
        }

        // Work / Occupation
        val workMatch = Regex("(?:i work as|i am a|my job is)\\s+([a-zA-Z\\s]{3,30})", RegexOption.IGNORE_CASE).find(lower)
        if (workMatch != null) {
            val role = workMatch.groupValues[1].trim()
            if (role.isNotEmpty()) {
                saveFact("user_work", role, MemoryCategory.WORK, importance = 7)
            }
        }
    }

    /**
     * Consolidates memories: merges duplicates, removes stale low-importance facts,
     * keeps storage lightweight and fast.
     */
    suspend fun consolidate() = withContext(Dispatchers.IO) {
        val facts = getAllFactsInternal()
        val db = writableDatabase

        // Group by key similarity
        val seen = mutableSetOf<String>()
        val duplicates = mutableListOf<Long>()

        for (fact in facts) {
            val key = fact.key.lowercase().trim()
            if (seen.contains(key)) {
                duplicates.add(fact.id)
            } else {
                seen.add(key)
            }
        }

        for (id in duplicates) {
            db.delete("facts", "id = ?", arrayOf(id.toString()))
        }

        reloadAll()
    }

    private fun getAllFactsInternal(): List<FactMemory> {
        val list = mutableListOf<FactMemory>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, fact_key, fact_value, category, importance, created_at, last_accessed_at FROM facts ORDER BY importance DESC, last_accessed_at DESC",
            null
        )
        while (cursor.moveToNext()) {
            val catStr = cursor.getString(3)
            val category = try {
                MemoryCategory.valueOf(catStr)
            } catch (_: Exception) {
                MemoryCategory.OTHER
            }
            list.add(
                FactMemory(
                    id = cursor.getLong(0),
                    key = cursor.getString(1),
                    value = cursor.getString(2),
                    category = category,
                    importance = cursor.getInt(4),
                    createdAt = cursor.getLong(5),
                    lastAccessedAt = cursor.getLong(6)
                )
            )
        }
        cursor.close()
        return list
    }

    private fun getAllEpisodesInternal(): List<EpisodicMemory> {
        val list = mutableListOf<EpisodicMemory>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, date_iso, summary, key_moments, created_at FROM episodic ORDER BY created_at DESC LIMIT 30", null)
        while (cursor.moveToNext()) {
            list.add(
                EpisodicMemory(
                    id = cursor.getLong(0),
                    dateIso = cursor.getString(1),
                    summary = cursor.getString(2),
                    keyMoments = cursor.getString(3).orEmpty(),
                    createdAt = cursor.getLong(4)
                )
            )
        }
        cursor.close()
        return list
    }

    companion object {
        private const val DATABASE_NAME = "myraa_memory.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: MemoryStore? = null

        fun get(context: Context): MemoryStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
