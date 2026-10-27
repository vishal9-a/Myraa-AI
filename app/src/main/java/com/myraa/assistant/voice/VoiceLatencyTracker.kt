package com.myraa.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceLatencyTracker {
    private val _reconnectCount = MutableStateFlow(0)
    val reconnectCount: StateFlow<Int> = _reconnectCount.asStateFlow()

    private val _lastTurnLatencyMs = MutableStateFlow(0L)
    val lastTurnLatencyMs: StateFlow<Long> = _lastTurnLatencyMs.asStateFlow()

    private var userSpeechEndTimestamp: Long = 0L
    private var firstAudioChunkTimestamp: Long = 0L

    @Synchronized
    fun markUserSpeechEnd(timestamp: Long = System.currentTimeMillis()) {
        userSpeechEndTimestamp = timestamp
        firstAudioChunkTimestamp = 0L
    }

    @Synchronized
    fun markFirstAudioChunk(timestamp: Long = System.currentTimeMillis()): Long {
        if (userSpeechEndTimestamp > 0 && timestamp >= userSpeechEndTimestamp) {
            val delta = timestamp - userSpeechEndTimestamp
            userSpeechEndTimestamp = 0L
            firstAudioChunkTimestamp = timestamp
            _lastTurnLatencyMs.value = delta
            return delta
        }
        return -1L
    }

    fun incrementReconnectCount() {
        _reconnectCount.value = _reconnectCount.value + 1
    }

    fun reset() {
        _reconnectCount.value = 0
        _lastTurnLatencyMs.value = 0L
        userSpeechEndTimestamp = 0L
        firstAudioChunkTimestamp = 0L
    }
}
