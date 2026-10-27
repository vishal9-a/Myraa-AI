package com.myraa.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.myraa.assistant.voice.Personality
import com.myraa.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.assistantDataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_settings")

object PersonalityRepository {
    private val KEY_PERSONALITY = stringPreferencesKey(NovaPreferences.Keys.PERSONALITY)
    private val KEY_LANGUAGE = stringPreferencesKey(NovaPreferences.Keys.LANGUAGE)
    private val KEY_ASSISTANT_NAME = stringPreferencesKey(NovaPreferences.Keys.ASSISTANT_NAME)
    private val KEY_VOICE_NAME = stringPreferencesKey(NovaPreferences.Keys.VOICE_NAME)

    fun personalityFlow(context: Context): Flow<Personality> {
        return context.assistantDataStore.data.map { prefs ->
            val stored = prefs[KEY_PERSONALITY]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
            Personality.fromName(stored)
        }
    }

    suspend fun getPersonality(context: Context): Personality {
        return try {
            val prefs = context.assistantDataStore.data.first()
            val stored = prefs[KEY_PERSONALITY]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
            Personality.fromName(stored)
        } catch (_: Exception) {
            val stored = NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
            Personality.fromName(stored)
        }
    }

    suspend fun setPersonality(context: Context, personality: Personality) {
        NovaPreferences.putString(context, NovaPreferences.Keys.PERSONALITY, personality.name)
        try {
            context.assistantDataStore.edit { prefs ->
                prefs[KEY_PERSONALITY] = personality.name
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }

    fun languageFlow(context: Context): Flow<LanguagePreference> {
        return context.assistantDataStore.data.map { prefs ->
            val stored = prefs[KEY_LANGUAGE]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
            LanguagePreference.fromName(stored)
        }
    }

    suspend fun getLanguage(context: Context): LanguagePreference {
        return try {
            val prefs = context.assistantDataStore.data.first()
            val stored = prefs[KEY_LANGUAGE]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
            LanguagePreference.fromName(stored)
        } catch (_: Exception) {
            val stored = NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
            LanguagePreference.fromName(stored)
        }
    }

    suspend fun setLanguage(context: Context, language: LanguagePreference) {
        NovaPreferences.putString(context, NovaPreferences.Keys.LANGUAGE, language.name)
        try {
            context.assistantDataStore.edit { prefs ->
                prefs[KEY_LANGUAGE] = language.name
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }

    fun assistantNameFlow(context: Context): Flow<String> {
        return context.assistantDataStore.data.map { prefs ->
            prefs[KEY_ASSISTANT_NAME]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
        }
    }

    suspend fun getAssistantName(context: Context): String {
        return try {
            val prefs = context.assistantDataStore.data.first()
            prefs[KEY_ASSISTANT_NAME]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
        } catch (_: Exception) {
            NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
        }
    }

    suspend fun setAssistantName(context: Context, name: String) {
        val trimmed = name.trim().ifEmpty { AssistantBrand.NAME }
        NovaPreferences.putString(context, NovaPreferences.Keys.ASSISTANT_NAME, trimmed)
        try {
            context.assistantDataStore.edit { prefs ->
                prefs[KEY_ASSISTANT_NAME] = trimmed
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }

    fun voiceNameFlow(context: Context): Flow<String> {
        return context.assistantDataStore.data.map { prefs ->
            prefs[KEY_VOICE_NAME]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
        }
    }

    suspend fun getVoiceName(context: Context): String {
        return try {
            val prefs = context.assistantDataStore.data.first()
            prefs[KEY_VOICE_NAME]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
        } catch (_: Exception) {
            NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
        }
    }

    suspend fun setVoiceName(context: Context, voiceName: String) {
        val trimmed = voiceName.trim().ifEmpty { "Aoede" }
        NovaPreferences.putString(context, NovaPreferences.Keys.VOICE_NAME, trimmed)
        try {
            context.assistantDataStore.edit { prefs ->
                prefs[KEY_VOICE_NAME] = trimmed
            }
        } catch (_: Exception) {}
        VoiceSessionManager.refreshInstructions(context)
    }
}
