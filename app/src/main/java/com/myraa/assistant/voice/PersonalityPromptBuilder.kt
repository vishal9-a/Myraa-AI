package com.myraa.assistant.voice

import com.myraa.assistant.data.LanguagePreference

object PersonalityPromptBuilder {
    fun build(
        personality: Personality,
        language: LanguagePreference,
        detectedLanguage: DetectedLanguage,
        recap: String?,
        assistantName: String,
        memoryContext: String? = null
    ): String {
        val parts = mutableListOf<String>()

        // (a) Base
        parts.add(
            "You are $assistantName, a persistent personal voice assistant living on the user's phone. " +
            "Keep replies short, crisp, and conversational, like spoken conversation. Avoid bulleted lists or lengthy essays unless requested."
        )

        // (b) Capabilities from flavor
        parts.add(MyraaCapabilityPrompts.toolInstruction(assistantName))

        // (c) Personality instruction
        parts.add(personality.systemInstruction)

        // (d) Language policy
        val langPolicy = when (language) {
            LanguagePreference.AUTO -> {
                "Language policy: Match the user's language turn by turn. " +
                "If they speak Marathi, respond in fluent, warm Marathi. " +
                "If they speak Hindi, respond in fluent natural Hindi. " +
                "If they speak Hinglish (Hindi + English mix), respond in friendly modern Hinglish. " +
                "If they speak English, respond in natural English. " +
                "Never translate what the user said back to them, just converse naturally."
            }
            LanguagePreference.MARATHI -> "Language policy: Always speak in fluent, warm Marathi (मराठीत बोला)."
            LanguagePreference.HINDI -> "Language policy: Always speak in Hindi (हिंदी)."
            LanguagePreference.HINGLISH -> "Language policy: Always speak in conversational Hinglish (Hindi + English blend)."
            LanguagePreference.ENGLISH -> "Language policy: Always speak in English."
        }
        parts.add(langPolicy)

        // (e) Safety
        parts.add(
            "Safety and demeanor: Never claim to be a biological human or to possess a physical body. " +
            "Never produce sexually explicit content. Immediately respect any user request to change tone, topic, or stop. " +
            "Vary your phrasing naturally across turns and sound fresh."
        )

        // (f) Permanent Memory
        if (!memoryContext.isNullOrBlank()) {
            parts.add(
                "Permanent memories about the user (use naturally when relevant):\n$memoryContext"
            )
        }

        // (g) Recap if present
        if (!recap.isNullOrBlank()) {
            parts.add(
                "This is a continuing conversation — do not greet again.\nRecap:\n$recap"
            )
        }

        return parts.joinToString("\n\n")
    }
}
