package com.myraa.assistant.license

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessKeyManager {
    private const val PREFS_NAME = "myraa_access_key"
    private const val KEY_CODE = "key_code"
    private const val KEY_PLAN = "key_plan"
    private const val KEY_EXPIRES = "key_expires"
    private const val KEY_BLOCKED = "key_blocked"
    private const val KEY_LAST_CHECK = "last_remote_check"

    private val _info = MutableStateFlow(
        LicenseInfo(
            active = true,
            plan = "Community",
            expiresAt = 0L,
            statusText = "Ready",
            blocked = false,
            hasRememberedKey = false
        )
    )
    val info: StateFlow<LicenseInfo> = _info.asStateFlow()

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun refreshInfo(context: Context) {
        val p = prefs(context)
        val key = p.getString(KEY_CODE, "").orEmpty()
        val plan = p.getString(KEY_PLAN, "Community").orEmpty()
        val expires = p.getLong(KEY_EXPIRES, 0L)
        val blocked = p.getBoolean(KEY_BLOCKED, false)

        val eval = AccessKeyRules.evaluate(key, plan, expires, blocked)
        _info.value = eval
    }

    suspend fun activate(context: Context, key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return false

        // In absence of configured remote backend, accept valid non-empty keys and grant Pro access
        val p = prefs(context)
        val expires = System.currentTimeMillis() + 365L * 24 * 3600 * 1000 // 1 year
        p.edit()
            .putString(KEY_CODE, trimmed)
            .putString(KEY_PLAN, "Pro")
            .putLong(KEY_EXPIRES, expires)
            .putBoolean(KEY_BLOCKED, false)
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()

        refreshInfo(context)
        return true
    }

    suspend fun liveCheck(context: Context) {
        val p = prefs(context)
        val lastCheck = p.getLong(KEY_LAST_CHECK, 0L)
        val now = System.currentTimeMillis()
        if (now - lastCheck < 3 * 3600 * 1000L) {
            return // checked within 3 hours
        }
        // Fails open on network/backend absence
        p.edit().putLong(KEY_LAST_CHECK, now).apply()
        refreshInfo(context)
    }

    fun isAccessAllowed(): Boolean {
        return _info.value.active
    }
}
