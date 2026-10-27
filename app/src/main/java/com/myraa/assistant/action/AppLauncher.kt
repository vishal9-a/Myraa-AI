package com.myraa.assistant.action

import android.content.Context
import android.content.Intent

object AppLauncher {
    fun findApp(query: String, apps: Map<String, String>): String? {
        val q = query.trim().lowercase()
        if (q.isEmpty() || apps.isEmpty()) return null

        // 1. Exact match
        apps[q]?.let { return it }

        // 2. Candidates where key length >= 3
        val queryWords = q.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val candidates = apps.filter { (label, _) ->
            if (label.length < 3) return@filter false
            if (label.startsWith(q) || q.startsWith(label)) return@filter true

            val labelWords = label.split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (queryWords.isNotEmpty() && queryWords.all { qw ->
                labelWords.any { lw -> lw.startsWith(qw) }
            }) {
                return@filter true
            }
            false
        }

        if (candidates.isEmpty()) return null

        // Pick the candidate with closest label length to query
        val best = candidates.minByOrNull { Math.abs(it.key.length - q.length) }
        return best?.value
    }

    fun launch(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
