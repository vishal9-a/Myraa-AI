package com.myraa.assistant.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.myraa.assistant.voice.Personality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ConversationSummary(
    val id: String,
    val title: String,
    val timestamp: Long,
    val lastMessage: String
)

class NovaAppState(context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)

    var themeMode by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.THEME_MODE, "DARK")
    )
        private set

    var reducedMotion by mutableStateOf(
        NovaPreferences.getBoolean(context, NovaPreferences.Keys.REDUCED_MOTION, false)
    )
        private set

    var orbStyle by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.ORB_STYLE, "NOVA")
    )
        private set

    var personality by mutableStateOf(
        Personality.fromName(NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name))
    )
        private set

    var assistantName by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME).ifBlank { AssistantBrand.NAME }
    )
        private set

    var userName by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.USER_NAME, "")
    )
        private set

    var language by mutableStateOf(
        LanguagePreference.fromName(NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name))
    )
        private set

    var voiceName by mutableStateOf(
        NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede").ifBlank { "Aoede" }
    )
        private set

    var speechSpeed by mutableFloatStateOf(
        NovaPreferences.getFloat(context, NovaPreferences.Keys.SPEECH_SPEED, 1.0f)
    )
        private set

    var voiceVolume by mutableFloatStateOf(
        NovaPreferences.getFloat(context, NovaPreferences.Keys.VOICE_VOLUME, 1.0f)
    )
        private set

    var hasOnboarded by mutableStateOf(
        NovaPreferences.getBoolean(context, NovaPreferences.Keys.HAS_ONBOARDED, false)
    )
        private set

    var conversations by mutableStateOf<List<ConversationSummary>>(emptyList())
        private set

    fun applyThemeMode(context: Context, mode: String) {
        themeMode = mode
        NovaPreferences.putString(context, NovaPreferences.Keys.THEME_MODE, mode)
    }

    fun applyReducedMotion(context: Context, enabled: Boolean) {
        reducedMotion = enabled
        NovaPreferences.putBoolean(context, NovaPreferences.Keys.REDUCED_MOTION, enabled)
    }

    fun applyOrbStyle(context: Context, style: String) {
        orbStyle = style
        NovaPreferences.putString(context, NovaPreferences.Keys.ORB_STYLE, style)
    }

    fun applyPersonality(context: Context, p: Personality) {
        personality = p
        scope.launch {
            PersonalityRepository.setPersonality(context, p)
        }
    }

    fun applyAssistantName(context: Context, name: String) {
        val clean = name.trim().ifEmpty { AssistantBrand.NAME }
        assistantName = clean
        scope.launch {
            PersonalityRepository.setAssistantName(context, clean)
        }
    }

    fun applyUserName(context: Context, name: String) {
        userName = name.trim()
        NovaPreferences.putString(context, NovaPreferences.Keys.USER_NAME, userName)
    }

    fun applyLanguage(context: Context, lang: LanguagePreference) {
        language = lang
        scope.launch {
            PersonalityRepository.setLanguage(context, lang)
        }
    }

    fun applyVoiceName(context: Context, v: String) {
        val clean = v.trim().ifEmpty { "Aoede" }
        voiceName = clean
        scope.launch {
            PersonalityRepository.setVoiceName(context, clean)
        }
    }

    fun applySpeechSpeed(context: Context, speed: Float) {
        speechSpeed = speed
        NovaPreferences.putFloat(context, NovaPreferences.Keys.SPEECH_SPEED, speed)
    }

    fun applyVoiceVolume(context: Context, volume: Float) {
        voiceVolume = volume
        NovaPreferences.putFloat(context, NovaPreferences.Keys.VOICE_VOLUME, volume)
    }

    fun applyHasOnboarded(context: Context, done: Boolean) {
        hasOnboarded = done
        NovaPreferences.putBoolean(context, NovaPreferences.Keys.HAS_ONBOARDED, done)
    }

    fun addConversation(item: ConversationSummary) {
        conversations = listOf(item) + conversations
    }

    fun removeConversation(id: String) {
        conversations = conversations.filter { it.id != id }
    }

    fun clearConversations() {
        conversations = emptyList()
    }
}

@Composable
fun rememberNovaAppState(context: Context): NovaAppState {
    return remember { NovaAppState(context) }
}
