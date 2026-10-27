package com.myraa.assistant.voice

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatTurn(
    val isUser: Boolean,
    val text: String
)

sealed class ChatReplyResult {
    data class Success(val text: String) : ChatReplyResult()
    data class Error(val message: String) : ChatReplyResult()
}

object GeminiTextClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedModel: String? = null

    internal val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    internal fun resolveModel(apiKey: String): String {
        cachedModel?.let { return it }

        val url = "$BASE_URL/models?key=$apiKey"
        val request = Request.Builder().url(url).get().build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val models = json.optJSONArray("models") ?: JSONArray()

                var fallbackModel: String? = null
                var matchedModel: String? = null

                for (i in 0 until models.length()) {
                    val m = models.getJSONObject(i)
                    val name = m.optString("name", "")
                    val methods = m.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                    var canGenerate = false
                    for (j in 0 until methods.length()) {
                        if (methods.getString(j) == "generateContent") {
                            canGenerate = true
                            break
                        }
                    }

                    if (canGenerate) {
                        if (fallbackModel == null) {
                            fallbackModel = name
                        }
                        val lower = name.lowercase()
                        if (lower.contains("flash") &&
                            !lower.contains("live") &&
                            !lower.contains("image") &&
                            !lower.contains("tts")
                        ) {
                            matchedModel = name
                            break
                        }
                    }
                }

                val resolved = matchedModel ?: fallbackModel ?: "models/gemini-2.0-flash"
                cachedModel = resolved
                return resolved
            }
        } catch (_: Exception) {}

        val defaultFallback = "models/gemini-2.0-flash"
        cachedModel = defaultFallback
        return defaultFallback
    }

    fun reply(
        apiKey: String,
        systemInstruction: String,
        history: List<ChatTurn>
    ): ChatReplyResult {
        if (apiKey.isBlank()) {
            return ChatReplyResult.Error("API key is missing. Please enter your Gemini API key in Settings.")
        }
        if (history.isEmpty()) {
            return ChatReplyResult.Error("No conversation history provided.")
        }

        val model = resolveModel(apiKey)
        val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"

        val root = JSONObject()

        // System Instruction
        if (systemInstruction.isNotBlank()) {
            val sysObj = JSONObject()
            val parts = JSONArray()
            parts.put(JSONObject().apply { put("text", systemInstruction) })
            sysObj.put("parts", parts)
            root.put("systemInstruction", sysObj)
        }

        // Contents history
        val contents = JSONArray()
        for (turn in history) {
            val turnObj = JSONObject()
            turnObj.put("role", if (turn.isUser) "user" else "model")
            val parts = JSONArray()
            parts.put(JSONObject().apply { put("text", turn.text) })
            turnObj.put("parts", parts)
            contents.put(turnObj)
        }
        root.put("contents", contents)

        val requestBody = root.toString().toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty()
                val reason = try {
                    val errJson = JSONObject(errBody)
                    errJson.optJSONObject("error")?.optString("message", "HTTP ${response.code}") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                ChatReplyResult.Error(reason)
            } else {
                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    ChatReplyResult.Error("Empty response from Gemini.")
                } else {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
                    if (text.isBlank()) {
                        ChatReplyResult.Error("Gemini returned an empty reply.")
                    } else {
                        ChatReplyResult.Success(text)
                    }
                }
            }
        } catch (_: IOException) {
            ChatReplyResult.Error("Couldn't reach Gemini. Please check your internet connection.")
        } catch (e: Exception) {
            ChatReplyResult.Error("Failed to get response: ${e.message}")
        }
    }
}
