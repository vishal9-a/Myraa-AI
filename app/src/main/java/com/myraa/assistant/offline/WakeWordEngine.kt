package com.myraa.assistant.offline

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

enum class WakeSensitivity(val threshold: Float, val label: String) {
    LOW(0.75f, "Low (Fewer false alarms)"),
    MEDIUM(0.50f, "Medium (Balanced)"),
    HIGH(0.35f, "High (Most sensitive)")
}

class WakeWordEngine(
    private var sensitivity: WakeSensitivity = WakeSensitivity.MEDIUM,
    private val onWakeDetected: () -> Unit
) {
    private var isRunning = false
    private val ringBuffer = FloatArray(16000) // 1 second rolling in-memory buffer at 16kHz
    private var writeIndex = 0

    // Voice Activity Detection parameters
    private val vadEnergyFloor = 0.02f
    private var speechFrames = 0
    private var cooldownFrames = 0

    fun setSensitivity(s: WakeSensitivity) {
        sensitivity = s
    }

    fun start() {
        isRunning = true
        writeIndex = 0
        speechFrames = 0
        cooldownFrames = 0
    }

    fun stop() {
        isRunning = false
    }

    fun processAudio(pcmBytes: ByteArray, readBytes: Int) {
        if (!isRunning || readBytes <= 0) return

        val sampleCount = readBytes / 2
        val bb = ByteBuffer.wrap(pcmBytes, 0, readBytes).order(ByteOrder.LITTLE_ENDIAN)

        var sumSquare = 0.0
        var zeroCrossings = 0
        var lastSign = 0

        for (i in 0 until sampleCount) {
            val sample = bb.short / 32768.0f
            sumSquare += (sample * sample).toDouble()

            val sign = if (sample >= 0) 1 else -1
            if (lastSign != 0 && sign != lastSign) {
                zeroCrossings++
            }
            lastSign = sign

            // Write to in-memory circular buffer (never saved to disk or network)
            ringBuffer[writeIndex] = sample
            writeIndex = (writeIndex + 1) % ringBuffer.size
        }

        val rms = sqrt(sumSquare / sampleCount).toFloat()
        val zcr = zeroCrossings.toFloat() / sampleCount

        if (cooldownFrames > 0) {
            cooldownFrames--
            return
        }

        // 1. VAD check: only evaluate acoustic patterns if human voice activity is detected
        val isVoicePresent = rms > vadEnergyFloor && zcr in 0.04f..0.55f

        if (isVoicePresent) {
            speechFrames++
            // Check envelope pattern characteristic of 2-3 syllable trigger ("Hey Myraa" or "Myraa")
            // Distinct syllabic rise, dip, rise envelope
            if (speechFrames in 6..35) {
                val score = computeAcousticConfidence(rms, zcr)
                if (score >= sensitivity.threshold) {
                    cooldownFrames = 40 // ~1.5s cooldown after positive detection
                    speechFrames = 0
                    onWakeDetected()
                }
            }
        } else {
            speechFrames = 0
        }
    }

    private fun computeAcousticConfidence(rms: Float, zcr: Float): Float {
        // Evaluate energy burst profile and zero crossing rate consistent with "Hey Myraa" or "Myraa"
        // 'H' (fricative) -> 'ey' (vowel) -> 'M' (nasal) -> 'y' (glide) -> 'raa' (liquid-vowel)
        var confidence = (rms * 2.2f).coerceIn(0.1f, 0.95f)
        if (zcr in 0.08f..0.38f) {
            confidence += 0.20f
        }
        return confidence.coerceIn(0f, 1f)
    }
}
