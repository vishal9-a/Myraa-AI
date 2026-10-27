package com.myraa.assistant.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object EchoGuard {
    private const val ECHO_TAIL_MS = 450L
    private const val WATCHDOG_FORCE_UNMUTE_MS = 60_000L

    private val playbackCounter = AtomicInteger(0)
    private val isMutedFlag = AtomicBoolean(false)
    private var tailJob: Job? = null
    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Synchronized
    fun notifyPlaybackStarted() {
        val count = playbackCounter.incrementAndGet()
        isMutedFlag.set(true)
        tailJob?.cancel()
        tailJob = null

        if (count == 1) {
            watchdogJob?.cancel()
            watchdogJob = scope.launch {
                delay(WATCHDOG_FORCE_UNMUTE_MS)
                synchronized(this@EchoGuard) {
                    playbackCounter.set(0)
                    isMutedFlag.set(false)
                }
            }
        }
    }

    @Synchronized
    fun notifyPlaybackStopped() {
        val remaining = playbackCounter.decrementAndGet().coerceAtLeast(0)
        if (remaining == 0) {
            watchdogJob?.cancel()
            watchdogJob = null
            tailJob?.cancel()
            tailJob = scope.launch {
                delay(ECHO_TAIL_MS)
                synchronized(this@EchoGuard) {
                    if (playbackCounter.get() == 0) {
                        isMutedFlag.set(false)
                    }
                }
            }
        }
    }

    fun isMicMuted(): Boolean {
        return isMutedFlag.get()
    }

    @Synchronized
    fun reset() {
        playbackCounter.set(0)
        isMutedFlag.set(false)
        tailJob?.cancel()
        tailJob = null
        watchdogJob?.cancel()
        watchdogJob = null
    }

    fun getActivePlaybackCount(): Int = playbackCounter.get()
}
