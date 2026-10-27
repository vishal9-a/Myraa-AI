package com.myraa.assistant.voice

import android.util.Base64
import com.myraa.assistant.action.MyraaToolCatalog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val apiKey: String,
    private val listener: Listener
) {
    interface Listener {
        fun onSetupComplete()
        fun onAudioChunk(bytes: ByteArray)
        fun onInputTranscript(text: String)
        fun onOutputTranscript(text: String)
        fun onToolCall(id: String, name: String, args: JSONObject)
        fun onTurnComplete()
        fun onInterrupted()
        fun onError(message: String)
        fun onClosed()
    }

    companion object {
        const val MODEL = "models/gemini-2.0-flash-exp"
        private const val WS_BASE_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    private var webSocket: WebSocket? = null
    private val deduplicator = ToolCallDeduplicator()

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect(systemInstruction: String, voiceName: String = "Aoede") {
        if (apiKey.isBlank()) {
            listener.onError("Missing Gemini API Key. Please add your key in Settings.")
            return
        }

        val url = "$WS_BASE_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                sendSetup(ws, systemInstruction, voiceName)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleIncomingMessage(bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                val msg = t.localizedMessage ?: "WebSocket error"
                listener.onError(msg)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }
        })
    }

    private fun sendSetup(ws: WebSocket, systemInstruction: String, voiceName: String) {
        val root = JSONObject()
        val setup = JSONObject()
        setup.put("model", MODEL)

        // Generation Config with Audio modality
        val generationConfig = JSONObject()
        val modalities = JSONArray()
        modalities.put("AUDIO")
        generationConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", voiceName)
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        generationConfig.put("speechConfig", speechConfig)
        setup.put("generationConfig", generationConfig)

        // System Instruction
        val sysInst = JSONObject()
        val parts = JSONArray()
        val part = JSONObject()
        part.put("text", systemInstruction)
        parts.put(part)
        sysInst.put("parts", parts)
        setup.put("systemInstruction", sysInst)

        // Transcription configs
        setup.put("inputAudioTranscription", JSONObject())
        setup.put("outputAudioTranscription", JSONObject())

        // Tools
        val toolsArray = JSONArray()
        val toolObj = JSONObject()
        toolObj.put("functionDeclarations", MyraaToolCatalog.declarations())
        toolsArray.put(toolObj)
        setup.put("tools", toolsArray)

        root.put("setup", setup)
        ws.send(root.toString())
    }

    private fun handleIncomingMessage(raw: String) {
        try {
            val json = JSONObject(raw)

            if (json.has("setupComplete")) {
                listener.onSetupComplete()
                return
            }

            if (json.has("sessionResumptionUpdate") || json.has("goAway")) {
                return
            }

            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                if (toolCall.has("functionCalls")) {
                    val calls = toolCall.getJSONArray("functionCalls")
                    for (i in 0 until calls.length()) {
                        val call = calls.getJSONObject(i)
                        val id = call.optString("id", "")
                        val name = call.optString("name", "")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        if (id.isNotEmpty() && !deduplicator.isDuplicate(id)) {
                            listener.onToolCall(id, name, args)
                        }
                    }
                }
            }

            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    listener.onInterrupted()
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    if (modelTurn.has("parts")) {
                        val parts = modelTurn.getJSONArray("parts")
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val dataBase64 = inlineData.optString("data", "")
                                if (dataBase64.isNotEmpty()) {
                                    val audioBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                                    listener.onAudioChunk(audioBytes)
                                }
                            }
                            if (part.has("text")) {
                                val text = part.optString("text", "")
                                if (text.isNotEmpty()) {
                                    listener.onOutputTranscript(text)
                                }
                            }
                        }
                    }
                }

                if (serverContent.has("inputTranscription")) {
                    val text = serverContent.getJSONObject("inputTranscription").optString("text", "")
                    if (text.isNotEmpty()) {
                        listener.onInputTranscript(text)
                    }
                }

                if (serverContent.has("outputTranscription")) {
                    val text = serverContent.getJSONObject("outputTranscription").optString("text", "")
                    if (text.isNotEmpty()) {
                        listener.onOutputTranscript(text)
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    listener.onTurnComplete()
                }
            }
        } catch (_: Exception) {
            // Never log raw server messages or sensitive tokens
        }
    }

    fun sendAudio(pcmBytes: ByteArray) {
        if (pcmBytes.isEmpty()) return
        try {
            val root = JSONObject()
            val realtimeInput = JSONObject()
            val audio = JSONObject()
            audio.put("mimeType", "audio/pcm;rate=16000")
            audio.put("data", Base64.encodeToString(pcmBytes, Base64.NO_WRAP))
            realtimeInput.put("audio", audio)
            root.put("realtimeInput", realtimeInput)
            webSocket?.send(root.toString())
        } catch (_: Exception) {}
    }

    fun sendToolResponse(id: String, name: String, resultJson: JSONObject) {
        try {
            val root = JSONObject()
            val toolResponse = JSONObject()
            val functionResponses = JSONArray()
            val resp = JSONObject()
            resp.put("id", id)
            resp.put("name", name)
            val responseObj = JSONObject()
            responseObj.put("output", resultJson)
            resp.put("response", responseObj)
            functionResponses.put(resp)
            toolResponse.put("functionResponses", functionResponses)
            root.put("toolResponse", toolResponse)
            webSocket?.send(root.toString())
        } catch (_: Exception) {}
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Normal closure")
            webSocket = null
            deduplicator.clear()
        } catch (_: Exception) {}
    }
}
