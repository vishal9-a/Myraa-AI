package com.myraa.assistant.forge

object ForgeIntent {
    private val SUBJECT_KEYWORDS = setOf(
        "website", "web page", "webpage", "landing page", "portfolio", "site", "web app"
    )
    private val ACTION_KEYWORDS = setOf(
        "build", "create", "make", "bana", "banao", "generate", "design", "develop", "banaye"
    )

    fun extract(text: String): String? {
        val trimmed = text.trim()
        val lower = trimmed.lowercase()

        // Ignore mere informational questions
        if (lower.startsWith("what is a website") || lower.startsWith("how to build") || lower.contains("kya hoti hai")) {
            return null
        }

        val hasSubject = SUBJECT_KEYWORDS.any { lower.contains(it) }
        val hasAction = ACTION_KEYWORDS.any { lower.contains(it) }

        if (hasSubject && hasAction) {
            return trimmed
        }
        return null
    }
}
