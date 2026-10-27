package com.myraa.assistant.voice

enum class DetectedLanguage {
    MARATHI,
    HINDI,
    HINGLISH,
    ENGLISH,
    UNKNOWN
}

object LanguageDetector {
    private val MARATHI_MARKERS = setOf(
        "kay", "ahe", "kasa", "kashi", "karto", "karte", "zala", "zali",
        "sang", "karaycha", "karaychi", "thamba", "ughad", "dakhav", "bol",
        "madhe", "tula", "mala", "aawaj", "lav", "pahije", "kadhi", "pan",
        "ani", "donhi", "swatachya", "haluu", "jaast", "kasa", "kashi", "kuthe",
        "te", "ti", "to", "tyacha", "tiche", "amcha", "tumcha", "aaho", "baga"
    )

    private val MARATHI_DEVANAGARI = setOf(
        "आहे", "काय", "कसा", "कशी", "झाला", "झाली", "सांग", "नाही", "हो",
        "चालू", "कर", "करायचं", "थंब", "बोल", "उघड", "दाखव", "लाव", "द्या",
        "पाहिजे", "मराठी", "आवाज", "हळू", "जास्त", "मध्ये", "मला", "तुला"
    )

    private val HINGLISH_MARKERS = setOf(
        "hai", "hain", "ho", "kya", "kyun", "kaise", "nahi", "haan",
        "mujhe", "tumhe", "aap", "mera", "yaar", "bhai", "accha", "theek",
        "matlab", "karna", "karo", "raha", "chahiye", "batao", "suno", "dekho",
        "abhi", "phir", "lekin", "bahut", "thoda", "kuch", "sab", "wala",
        "meri", "mere", "hum", "tum", "apna", "apne", "apni", "hoga", "hogi",
        "dena", "karo", "de", "lo", "bolo", "pe", "ka", "ki", "ke", "ko", "se"
    )

    fun detect(text: String?): DetectedLanguage {
        if (text.isNullOrBlank()) return DetectedLanguage.UNKNOWN

        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) {
            val words = text.split(Regex("\\s+"))
            if (words.any { it in MARATHI_DEVANAGARI }) {
                return DetectedLanguage.MARATHI
            }
            return DetectedLanguage.HINDI
        }

        val words = text.lowercase()
            .split(Regex("[^a-zA-Z']+"))
            .filter { it.isNotBlank() }

        if (words.isEmpty()) return DetectedLanguage.UNKNOWN

        val marathiHits = words.count { it in MARATHI_MARKERS }
        if (marathiHits >= 1) {
            return DetectedLanguage.MARATHI
        }

        val hits = words.count { it in HINGLISH_MARKERS }

        return if (words.size <= 4) {
            if (hits >= 1) DetectedLanguage.HINGLISH else DetectedLanguage.ENGLISH
        } else {
            val ratio = hits.toDouble() / words.size.toDouble()
            if (hits >= 2 || ratio >= 0.15) {
                DetectedLanguage.HINGLISH
            } else {
                DetectedLanguage.ENGLISH
            }
        }
    }
}
