package com.myraa.assistant.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object InstalledAppLabelCache {
    private const val TTL_MS = 15 * 60 * 1000L // 15 minutes

    private val mutex = Mutex()
    private var cachedMap: Map<String, String> = emptyMap()
    private var lastLoadedTime = 0L
    private val scope = CoroutineScope(Dispatchers.IO)

    fun prewarm(context: Context) {
        scope.launch {
            get(context)
        }
    }

    suspend fun get(context: Context): Map<String, String> {
        val now = System.currentTimeMillis()
        if (cachedMap.isNotEmpty() && (now - lastLoadedTime) < TTL_MS) {
            return cachedMap
        }

        return mutex.withLock {
            if (cachedMap.isNotEmpty() && (System.currentTimeMillis() - lastLoadedTime) < TTL_MS) {
                return@withLock cachedMap
            }
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = try {
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            } catch (_: Exception) {
                try {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, 0)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val map = mutableMapOf<String, String>()
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                val label = try {
                    info.loadLabel(pm).toString().trim().lowercase()
                } catch (_: Exception) {
                    ""
                }
                if (label.isNotEmpty() && pkg.isNotEmpty()) {
                    map[label] = pkg
                }
            }
            cachedMap = map
            lastLoadedTime = System.currentTimeMillis()
            map
        }
    }
}
