package com.myraa.assistant.offline

enum class VoiceProfile(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val pitch: Float,
    val speed: Float,
    val pauseDurationMs: Long,
    val previewTextMarathi: String,
    val previewTextEnglish: String
) {
    CUTE_GIRL(
        id = "cute_girl",
        displayName = "Cute Girl (गोड तरुणी - Default)",
        subtitle = "गोड, तरुण मुलीचा आवाज · 1.08x speed · 1.18x pitch · Micro-pauses",
        pitch = 1.18f,
        speed = 1.08f,
        pauseDurationMs = 70L,
        previewTextMarathi = "नमस्कार! मी मायरा, तुमची गोड वैयक्तिक AI सोबती!",
        previewTextEnglish = "Hi there! I am Myraa, your sweet voice companion!"
    ),
    SOFT_GIRL(
        id = "soft_girl",
        displayName = "Soft Girl (सौम्य, आपुलकीची)",
        subtitle = "शांत, काळजीपूर्वक बोलणारा आवाज · 0.96x speed · 1.06x pitch",
        pitch = 1.06f,
        speed = 0.96f,
        pauseDurationMs = 120L,
        previewTextMarathi = "नमस्कार! मी मायरा. मी शांतपणे आणि आपुलकीने तुमच्यासोबत बोलेन.",
        previewTextEnglish = "Hello! I am Myraa, speaking gently and softly."
    ),
    MALE(
        id = "male",
        displayName = "Male (धीरगंभीर पुरुष)",
        subtitle = "स्पष्ट, धीरगंभीर पुरुष आवाज · 1.0x speed · 0.82x pitch",
        pitch = 0.82f,
        speed = 1.0f,
        pauseDurationMs = 90L,
        previewTextMarathi = "नमस्कार, मी मायरा असिस्टंट. मी सर्व कामे अचूकपणे पूर्ण करेन.",
        previewTextEnglish = "Hello, I am Myraa assistant. Ready to handle all tasks."
    );

    companion object {
        fun fromId(id: String?): VoiceProfile {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CUTE_GIRL
        }
    }
}
