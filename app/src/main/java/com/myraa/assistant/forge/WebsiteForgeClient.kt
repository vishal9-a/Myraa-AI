package com.myraa.assistant.forge

import android.content.Context
import com.myraa.assistant.voice.GeminiTextClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class ForgeStreamEvent {
    data class Chunk(val text: String) : ForgeStreamEvent()
    data class Complete(val file: File, val totalChars: Int) : ForgeStreamEvent()
    data class Error(val message: String) : ForgeStreamEvent()
}

class WebsiteForgeClient(
    private val context: Context,
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun streamBuild(prompt: String, isEdit: Boolean = false, currentHtml: String = ""): Flow<ForgeStreamEvent> = flow {
        if (apiKey.isBlank()) {
            emit(ForgeStreamEvent.Error("Missing Gemini API Key. Please add it in Settings."))
            return@flow
        }

        val model = GeminiTextClient.resolveModel(apiKey)
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/$model:streamGenerateContent?alt=sse"

        val systemPrompt = if (isEdit) {
            ForgeSystemPrompt.forEdit(currentHtml, prompt)
        } else {
            ForgeSystemPrompt.forNewSite(prompt)
        }

        var attempt = 0
        var fullHtmlBuilder = StringBuilder()
        var retryWithHint = false

        while (attempt < 2) {
            attempt++
            fullHtmlBuilder = StringBuilder()

            val root = JSONObject()
            val contents = JSONArray()
            val userContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray().apply {
                    val promptText = if (retryWithHint) {
                        "$systemPrompt\n\nImportant: Write completely original markup, copy and code; finish the whole page and ensure all tags close properly."
                    } else {
                        systemPrompt
                    }
                    put(JSONObject().apply { put("text", promptText) })
                }
                put("parts", parts)
            }
            contents.put(userContent)
            root.put("contents", contents)

            // Thinking budget optimization for 2.5 models
            if (model.contains("2.5")) {
                val genConfig = JSONObject()
                val thinkingConfig = JSONObject().apply { put("thinkingBudget", 0) }
                genConfig.put("thinkingConfig", thinkingConfig)
                root.put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", apiKey)
                .post(root.toString().toRequestBody(jsonMedia))
                .build()

            var isOverloaded = false
            var finishReason: String? = null

            try {
                val call = client.newCall(request)
                val response = call.execute()

                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 503 || code == 429) {
                        isOverloaded = true
                    } else {
                        emit(ForgeStreamEvent.Error("Gemini error (HTTP $code)"))
                        return@flow
                    }
                } else {
                    val body = response.body
                    if (body == null) {
                        emit(ForgeStreamEvent.Error("Empty stream received from Gemini."))
                        return@flow
                    }

                    val reader = BufferedReader(InputStreamReader(body.byteStream()))
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        if (!currentCoroutineContext().isActive) {
                            call.cancel()
                            return@flow
                        }

                        val l = line?.trim().orEmpty()
                        if (l.startsWith("data:")) {
                            val textChunk = SseParser.extractText(l)
                            if (textChunk != null) {
                                fullHtmlBuilder.append(textChunk)
                                emit(ForgeStreamEvent.Chunk(textChunk))
                            }
                            val reason = SseParser.extractFinishReason(l)
                            if (reason != null) finishReason = reason
                        }
                    }
                }
            } catch (e: Exception) {
                if (attempt == 1) {
                    delay(4000)
                    continue
                } else {
                    emit(ForgeStreamEvent.Error("Network error: ${e.message}"))
                    return@flow
                }
            }

            if (isOverloaded && attempt == 1) {
                delay(4000)
                continue
            }

            val rawHtml = fullHtmlBuilder.toString()
            val cleanedHtml = ForgeHtml.stripFences(rawHtml)

            // Check if output is too short or truncated
            if ((cleanedHtml.length < 1500 || !cleanedHtml.contains("</html>", ignoreCase = true)) && attempt == 1) {
                retryWithHint = true
                delay(2000)
                continue
            }

            val finalHtml = if (cleanedHtml.length >= 14000 && !cleanedHtml.contains("</html>", ignoreCase = true)) {
                ForgeHtml.withSafetyNet(ForgeHtml.repairTruncated(cleanedHtml))
            } else if (cleanedHtml.contains("</html>", ignoreCase = true)) {
                ForgeHtml.withSafetyNet(cleanedHtml)
            } else {
                emit(ForgeStreamEvent.Error("Generation ended prematurely. Please retry."))
                return@flow
            }

            // Save to filesDir/Websites/website_<timestamp>.html
            val websitesDir = File(context.filesDir, "Websites")
            if (!websitesDir.exists()) websitesDir.mkdirs()
            val targetFile = File(websitesDir, "website_${System.currentTimeMillis()}.html")
            targetFile.writeText(finalHtml)

            emit(ForgeStreamEvent.Complete(targetFile, finalHtml.length))
            return@flow
        }
    }.flowOn(Dispatchers.IO)
}
