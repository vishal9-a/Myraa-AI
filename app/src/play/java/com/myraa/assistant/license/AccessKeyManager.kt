package com.myraa.assistant.license

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LicenseInfo(
    val active: Boolean = true,
    val plan: String = "Play Store",
    val expiresAt: Long = 0L,
    val statusText: String = "Active",
    val blocked: Boolean = false,
    val hasRememberedKey: Boolean = false
)

object AccessKeyManager {
    private val _info = MutableStateFlow(LicenseInfo())
    val info: StateFlow<LicenseInfo> = _info.asStateFlow()

    fun refreshInfo(context: Context) {}
    suspend fun activate(context: Context, key: String): Boolean = true
    suspend fun liveCheck(context: Context) {}
    fun isAccessAllowed(): Boolean = true
}
