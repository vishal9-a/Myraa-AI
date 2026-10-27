package com.myraa.assistant.forge

import org.json.JSONObject

object SseParser {
    fun extractText(dataLine: String): String? {
        val payload = dataLine.removePrefix("data:").trim()
        if (payload.isEmpty() || payload == "[DONE]") return null

        return try {
            val json = JSONObject(payload)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            parts.getJSONObject(0).optString("text", null)
        } catch (_: Exception) {
            null
        }
    }

    fun extractFinishReason(dataLine: String): String? {
        val payload = dataLine.removePrefix("data:").trim()
        if (payload.isEmpty() || payload == "[DONE]") return null

        return try {
            val json = JSONObject(payload)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            candidates.getJSONObject(0).optString("finishReason", null)
        } catch (_: Exception) {
            null
        }
    }

    fun extractError(dataLine: String): String? {
        val payload = dataLine.removePrefix("data:").trim()
        if (payload.isEmpty()) return null

        return try {
            val json = JSONObject(payload)
            json.optJSONObject("error")?.optString("message", null)
        } catch (_: Exception) {
            null
        }
    }
}
