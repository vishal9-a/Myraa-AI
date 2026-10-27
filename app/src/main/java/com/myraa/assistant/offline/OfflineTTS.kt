package com.myraa.assistant.offline

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.myraa.assistant.data.NovaPreferences
import com.myraa.assistant.voice.DetectedLanguage
import com.myraa.assistant.voice.EchoGuard
import com.myraa.assistant.voice.LanguageDetector
import com.myraa.assistant.voice.Personality
import java.util.LinkedList
import java.util.Locale
import java.util.Queue

class OfflineTTS(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val streamQueue: Queue<String> = LinkedList()
    @Volatile
    private var isStreaming = false
    private var onFullSpeechDoneCallback: (() -> Unit)? = null

    // Persisted Voice Profile (Default: Cute Girl)
    var currentProfile: VoiceProfile = VoiceProfile.fromId(
        NovaPreferences.getString(context, "voice_profile_id", VoiceProfile.CUTE_GIRL.id)
    )
        private set

    var customSpeed: Float = NovaPreferences.getFloat(context, "speech_speed", 1.08f)
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupInitialVoice()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    EchoGuard.notifyPlaybackStarted()
                }

                override fun onDone(utteranceId: String?) {
                    EchoGuard.notifyPlaybackStopped()
                    // Continue streaming next sentence chunk with micro-pause
                    mainHandler.postDelayed({
                        playNextStreamChunk()
                    }, currentProfile.pauseDurationMs)
                }

                override fun onError(utteranceId: String?) {
                    EchoGuard.notifyPlaybackStopped()
                    mainHandler.post { playNextStreamChunk() }
                }
            })
            isReady = true
        }
    }

    private fun setupInitialVoice() {
        val t = tts ?: return
        val mrLocale = Locale("mr", "IN")
        val hiLocale = Locale("hi", "IN")

        if (t.isLanguageAvailable(mrLocale) >= TextToSpeech.LANG_AVAILABLE) {
            t.language = mrLocale
        } else if (t.isLanguageAvailable(hiLocale) >= TextToSpeech.LANG_AVAILABLE) {
            t.language = hiLocale
        } else {
            t.language = Locale.ENGLISH
        }

        applyProfileSettings(currentProfile)
    }

    fun setVoiceProfile(profile: VoiceProfile) {
        currentProfile = profile
        NovaPreferences.putString(context, "voice_profile_id", profile.id)
        NovaPreferences.putString(context, "voice_gender", if (profile == VoiceProfile.MALE) "male" else "female")
        applyProfileSettings(profile)
    }

    fun setSpeechSpeed(speed: Float) {
        customSpeed = speed.coerceIn(0.6f, 1.8f)
        NovaPreferences.putFloat(context, "speech_speed", customSpeed)
        tts?.setSpeechRate(customSpeed)
    }

    private fun applyProfileSettings(profile: VoiceProfile) {
        val t = tts ?: return
        val isMale = profile == VoiceProfile.MALE

        try {
            val voices = t.voices
            if (!voices.isNullOrEmpty()) {
                val matched = voices.find { v ->
                    val name = v.name.lowercase()
                    if (isMale) {
                        (name.contains("male") && !name.contains("female")) || name.contains("-m-")
                    } else {
                        name.contains("female") || name.contains("-f-") || !name.contains("male")
                    }
                }
                if (matched != null) {
                    t.voice = matched
                }
            }
        } catch (_: Exception) {}

        t.setPitch(profile.pitch)
        t.setSpeechRate(profile.speed)
    }

    /**
     * Streams TTS output chunk-by-chunk for instant playback latency (<200ms).
     * Splits by punctuation marks and plays clauses sequentially with cute micro-pauses.
     */
    fun speak(
        text: String,
        personality: Personality = Personality.NORMAL,
        onDone: (() -> Unit)? = null
    ) {
        stop()

        if (!isReady || tts == null || text.isBlank()) {
            onDone?.invoke()
            return
        }

        onFullSpeechDoneCallback = onDone

        // Detect language for TTS engine
        val lang = LanguageDetector.detect(text)
        configureLanguage(lang)
        applyProfileSettings(currentProfile)

        // Split text into conversational clauses for streaming playback
        val chunks = splitIntoClauses(text)
        synchronized(streamQueue) {
            streamQueue.clear()
            streamQueue.addAll(chunks)
        }

        isStreaming = true
        playNextStreamChunk()
    }

    private fun playNextStreamChunk() {
        if (!isStreaming) return

        val nextChunk: String? = synchronized(streamQueue) {
            if (streamQueue.isNotEmpty()) streamQueue.poll() else null
        }

        if (nextChunk == null) {
            isStreaming = false
            EchoGuard.notifyPlaybackStopped()
            onFullSpeechDoneCallback?.invoke()
            return
        }

        EchoGuard.notifyPlaybackStarted()
        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "myraa_chunk_${System.currentTimeMillis()}"
        tts?.speak(nextChunk, TextToSpeech.QUEUE_FLUSH, params)
    }

    private fun splitIntoClauses(text: String): List<String> {
        val raw = text.split(Regex("(?<=[,.!?।\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (raw.isEmpty()) return listOf(text.trim())

        // Merge very short chunks (< 5 chars) with adjacent chunks for natural cadence
        val result = mutableListOf<String>()
        var buffer = ""
        for (item in raw) {
            if (buffer.isEmpty()) {
                buffer = item
            } else if (buffer.length + item.length < 35) {
                buffer = "$buffer $item"
            } else {
                result.add(buffer)
                buffer = item
            }
        }
        if (buffer.isNotEmpty()) {
            result.add(buffer)
        }
        return result
    }

    fun previewVoice(profile: VoiceProfile, onDone: (() -> Unit)? = null) {
        stop()
        setVoiceProfile(profile)
        val sampleText = profile.previewTextMarathi
        speak(sampleText, Personality.NORMAL, onDone)
    }

    private fun configureLanguage(lang: DetectedLanguage) {
        val t = tts ?: return
        when (lang) {
            DetectedLanguage.MARATHI -> {
                val mr = Locale("mr", "IN")
                if (t.isLanguageAvailable(mr) >= TextToSpeech.LANG_AVAILABLE) {
                    t.language = mr
                } else {
                    t.language = Locale("hi", "IN")
                }
            }
            DetectedLanguage.HINDI, DetectedLanguage.HINGLISH -> {
                t.language = Locale("hi", "IN")
            }
            else -> {
                val en = Locale("en", "IN")
                if (t.isLanguageAvailable(en) >= TextToSpeech.LANG_AVAILABLE) {
                    t.language = en
                } else {
                    t.language = Locale.ENGLISH
                }
            }
        }
    }

    fun stop() {
        isStreaming = false
        synchronized(streamQueue) {
            streamQueue.clear()
        }
        try {
            tts?.stop()
            EchoGuard.notifyPlaybackStopped()
        } catch (_: Exception) {}
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isReady = false
    }
}
