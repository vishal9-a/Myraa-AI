package com.myraa.assistant.memory

enum class MemoryCategory {
    PERSONAL,       // Name, age, birthday, location
    PREFERENCE,     // Food, music, tea/coffee, favorite colors, habits
    ROUTINE,        // Morning/night schedule, commute, workouts
    RELATIONSHIP,   // Family, friends, colleagues, pets
    GOAL,           // Projects, fitness goals, study targets
    WORK,           // Job role, company, team, skills
    PROMISE,        // Commitments made by Myraa or user
    OTHER
}

data class FactMemory(
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: MemoryCategory = MemoryCategory.PERSONAL,
    val importance: Int = 5, // 1 to 10
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

data class EpisodicMemory(
    val id: Long = 0,
    val dateIso: String,
    val summary: String,
    val keyMoments: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class PromiseMemory(
    val id: Long = 0,
    val title: String,
    val details: String,
    val targetTime: Long = 0L,
    val isFulfilled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
