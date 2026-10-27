package com.myraa.assistant.data

import android.content.Context
import android.content.SharedPreferences

object ApiKeyStore {
    private const val PREFS_NAME = "nova_prefs"
    private const val KEY_GEMINI_API = "gemini_api_key"

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getKey(context: Context): String {
        return prefs(context).getString(KEY_GEMINI_API, "")?.trim().orEmpty()
    }

    fun saveKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_GEMINI_API, key.trim()).apply()
    }

    fun hasKey(context: Context): Boolean {
        return getKey(context).isNotEmpty()
    }
}
