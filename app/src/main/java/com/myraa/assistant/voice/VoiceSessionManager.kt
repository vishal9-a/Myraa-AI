package com.myraa.assistant.voice

import android.content.Context
import com.myraa.assistant.action.ActionExecutor
import com.myraa.assistant.action.InstalledAppLabelCache
import com.myraa.assistant.data.ApiKeyStore
import com.myraa.assistant.data.LanguagePreference
import com.myraa.assistant.data.PersonalityRepository
import com.myraa.assistant.forge.ForgeController
import com.myraa.assistant.license.AccessKeyManager
import com.myraa.assistant.ui.fx.OverlayEdgeGlowController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

object VoiceSessionManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectMutex = Mutex()

    private val _state = MutableStateFlow(NovaOrbState.IDLE)
    val state: StateFlow<NovaOrbState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _statusText = MutableStateFlow("Ready to talk")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _currentPersonality = MutableStateFlow(Personality.NORMAL)
    val currentPersonality: StateFlow<Personality> = _currentPersonality.asStateFlow()

    private val _currentLanguage = MutableStateFlow(LanguagePreference.AUTO)
    val currentLanguage: StateFlow<LanguagePreference> = _currentLanguage.asStateFlow()

    private val _detectedLanguage = MutableStateFlow(DetectedLanguage.UNKNOWN)
    val detectedLanguage: StateFlow<DetectedLanguage> = _detectedLanguage.asStateFlow()

    private val _vadActive = MutableStateFlow(false)
    val vadActive: StateFlow<Boolean> = _vadActive.asStateFlow()

    val latencyTracker = VoiceLatencyTracker()
    val memory = ConversationMemory()

    private var liveClient: GeminiLiveClient? = null
    private var audioIO: LiveAudioIO? = null
    private val sessionEpoch = AtomicInteger(0)

    private var reconnectJob: Job? = null
    private val reconnectMutex = Mutex()
    private var playbackSilenceWatchdog: Job? = null
    private var isFirstAudioChunkOfTurn = true

    fun start(context: Context) {
        if (_isActive.value) return

        if (!AccessKeyManager.isAccessAllowed()) {
            _lastError.value = "Access key inactive or expired. Please check licensing."
            _statusText.value = "Licensing inactive"
            _state.value = NovaOrbState.ERROR
            return
        }

        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isBlank()) {
            _lastError.value = "Gemini API key is required. Please set it in Settings."
            _statusText.value = "Missing API Key"
            _state.value = NovaOrbState.ERROR
            return
        }

        _isActive.value = true
        _lastError.value = null
        _state.value = NovaOrbState.CONNECTING
        _statusText.value = "Connecting to Myraa..."
        memory.clear()
        latencyTracker.reset()
        EchoGuard.reset()

        InstalledAppLabelCache.prewarm(context)
        OverlayEdgeGlowController.start(context)

        audioIO = LiveAudioIO(
            onMicChunk = { chunk ->
                if (!_muted.value && !EchoGuard.isMicMuted()) {
                    liveClient?.sendAudio(chunk)
                }
            },
            onMicAmplitude = { amp ->
                if (_state.value == NovaOrbState.LISTENING) {
                    _amplitude.value = amp
                    _vadActive.value = amp > 0.05f
                }
            }
        )

        scope.launch {
            connectSession(context, isResuming = false)
        }
    }

    private suspend fun connectSession(context: Context, isResuming: Boolean) {
        connectMutex.withLock {
            val epoch = sessionEpoch.incrementAndGet()
            liveClient?.disconnect()
            liveClient = null

            val personality = PersonalityRepository.getPersonality(context)
            val language = PersonalityRepository.getLanguage(context)
            val assistantName = PersonalityRepository.getAssistantName(context)
            val voiceName = PersonalityRepository.getVoiceName(context)

            _currentPersonality.value = personality
            _currentLanguage.value = language

            val recap = if (isResuming) memory.recap() else null
            val memoryContext = try {
                com.myraa.assistant.memory.MemoryStore.get(context).retrieveMemoryContext(assistantName)
            } catch (_: Exception) { "" }

            val systemPrompt = PersonalityPromptBuilder.build(
                personality = personality,
                language = language,
                detectedLanguage = _detectedLanguage.value,
                recap = recap,
                assistantName = assistantName,
                memoryContext = memoryContext
            )

            val apiKey = ApiKeyStore.getKey(context)
            val client = GeminiLiveClient(apiKey, object : GeminiLiveClient.Listener {
                override fun onSetupComplete() {
                    if (sessionEpoch.get() != epoch) return
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "$assistantName is listening..."
                    audioIO?.startRecording()
                }

                override fun onAudioChunk(bytes: ByteArray) {
                    if (sessionEpoch.get() != epoch) return
                    if (isFirstAudioChunkOfTurn) {
                        isFirstAudioChunkOfTurn = false
                        EchoGuard.notifyPlaybackStarted()
                        latencyTracker.markFirstAudioChunk()
                    }
                    _state.value = NovaOrbState.SPEAKING
                    _statusText.value = "$assistantName is speaking..."
                    val amp = audioIO?.playChunk(bytes) ?: 0f
                    _amplitude.value = amp

                    // Re-arm playback silence watchdog (300 ms)
                    playbackSilenceWatchdog?.cancel()
                    playbackSilenceWatchdog = scope.launch {
                        delay(300)
                        if (sessionEpoch.get() == epoch && _state.value == NovaOrbState.SPEAKING) {
                            endSpeakingTurn()
                        }
                    }
                }

                override fun onInputTranscript(text: String) {
                    if (sessionEpoch.get() != epoch) return

                    // Emergency kill-switch
                    if (com.myraa.assistant.safety.SafetyManager.isEmergencyStop(text)) {
                        stop()
                        return
                    }

                    memory.addTurn(MemoryRole.USER, text)
                    val detected = LanguageDetector.detect(text)
                    if (detected != DetectedLanguage.UNKNOWN) {
                        _detectedLanguage.value = detected
                    }

                    // Auto-extract personal facts into permanent memory
                    scope.launch {
                        try {
                            com.myraa.assistant.memory.MemoryStore.get(context).autoExtractAndStore(text)
                        } catch (_: Exception) {}
                    }

                    latencyTracker.markUserSpeechEnd()
                    _state.value = NovaOrbState.THINKING
                    _statusText.value = "$assistantName is thinking..."
                }

                override fun onOutputTranscript(text: String) {
                    if (sessionEpoch.get() != epoch) return
                    memory.addTurn(MemoryRole.ASSISTANT, text)
                    try {
                        com.myraa.assistant.overlay.FloatingBubbleService.instance?.showReplyBubble(text)
                    } catch (_: Exception) {}
                }

                override fun onToolCall(id: String, name: String, args: JSONObject) {
                    if (sessionEpoch.get() != epoch) return
                    _state.value = NovaOrbState.THINKING
                    _statusText.value = "Executing $name..."

                    if (name == "build_website") {
                        val prompt = args.optString("prompt", "")
                        ForgeController.start(context, prompt)
                        val immediateResp = JSONObject().apply { put("result", "forge_started") }
                        liveClient?.sendToolResponse(id, name, immediateResp)
                        _state.value = NovaOrbState.LISTENING
                        return
                    }

                    scope.launch {
                        val response = ActionExecutor.execute(context, name, args)
                        if (sessionEpoch.get() == epoch) {
                            liveClient?.sendToolResponse(id, name, response)
                            _state.value = NovaOrbState.LISTENING
                        }
                    }
                }

                override fun onTurnComplete() {
                    if (sessionEpoch.get() != epoch) return
                    endSpeakingTurn()
                }

                override fun onInterrupted() {
                    if (sessionEpoch.get() != epoch) return
                    audioIO?.flushPlayback()
                    endSpeakingTurn()
                }

                override fun onError(message: String) {
                    if (sessionEpoch.get() != epoch) return
                    if (_isActive.value) {
                        scheduleReconnect(context)
                    } else {
                        _state.value = NovaOrbState.ERROR
                        _lastError.value = message
                        _statusText.value = message
                    }
                }

                override fun onClosed() {
                    if (sessionEpoch.get() != epoch) return
                    if (_isActive.value) {
                        scheduleReconnect(context)
                    }
                }
            })

            liveClient = client
            client.connect(systemPrompt, voiceName)
        }
    }

    private fun endSpeakingTurn() {
        playbackSilenceWatchdog?.cancel()
        playbackSilenceWatchdog = null
        isFirstAudioChunkOfTurn = true
        EchoGuard.notifyPlaybackStopped()
        _amplitude.value = 0f
        _state.value = NovaOrbState.LISTENING
        _statusText.value = "${currentPersonality.value.displayName} mode · Listening..."
    }

    private fun scheduleReconnect(context: Context) {
        scope.launch {
            reconnectMutex.withLock {
                if (reconnectJob?.isActive == true) return@withLock
                latencyTracker.incrementReconnectCount()
                val attempts = latencyTracker.reconnectCount.value

                if (attempts > 5) {
                    _state.value = NovaOrbState.ERROR
                    _lastError.value = "Connection lost - tap Stop and reopen Voice Mode"
                    _statusText.value = "Connection lost"
                    return@withLock
                }

                _state.value = NovaOrbState.CONNECTING
                _statusText.value = "Reconnecting (attempt $attempts)..."

                reconnectJob = scope.launch {
                    delay(1200)
                    if (_isActive.value) {
                        connectSession(context, isResuming = true)
                    }
                }
            }
        }
    }

    fun refreshInstructions(context: Context) {
        if (_isActive.value) {
            scope.launch {
                connectSession(context, isResuming = true)
            }
        }
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    fun stop() {
        sessionEpoch.incrementAndGet()
        reconnectJob?.cancel()
        reconnectJob = null
        playbackSilenceWatchdog?.cancel()
        playbackSilenceWatchdog = null

        liveClient?.disconnect()
        liveClient = null

        audioIO?.release()
        audioIO = null

        EchoGuard.reset()
        OverlayEdgeGlowController.stop()
        memory.clear()

        _isActive.value = false
        _amplitude.value = 0f
        _state.value = NovaOrbState.IDLE
        _statusText.value = "Session stopped"
    }
}
