package com.myraa.assistant.data

enum class LanguagePreference(
    val displayName: String,
    val description: String
) {
    AUTO("Auto Detect", "follows whatever you speak"),
    MARATHI("Marathi", "मराठीत संभाषण"),
    HINDI("Hindi", "हिंदी में बातचीत"),
    HINGLISH("Hinglish", "Hindi and English mix"),
    ENGLISH("English", "Conversational English");

    companion object {
        fun fromName(name: String?): LanguagePreference {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}
