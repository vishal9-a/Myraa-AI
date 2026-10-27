package com.myraa.assistant.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object ChimePlayer {

    fun playWakeChime(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Synthesize smooth pleasant dual-tone chime: 880Hz (A5) -> 1320Hz (E6)
                val sampleRate = 22050
                val duration1 = (sampleRate * 0.08).toInt()
                val duration2 = (sampleRate * 0.14).toInt()
                val totalSamples = duration1 + duration2
                val buffer = ShortArray(totalSamples)

                val freq1 = 880.0
                val freq2 = 1320.0

                for (i in 0 until duration1) {
                    val angle = 2.0 * Math.PI * i * freq1 / sampleRate
                    val envelope = (i.toDouble() / duration1).coerceIn(0.0, 1.0)
                    buffer[i] = (sin(angle) * 18000 * envelope).toInt().toShort()
                }

                for (i in 0 until duration2) {
                    val angle = 2.0 * Math.PI * i * freq2 / sampleRate
                    val envelope = (1.0 - (i.toDouble() / duration2)).coerceIn(0.0, 1.0)
                    buffer[duration1 + i] = (sin(angle) * 22000 * envelope).toInt().toShort()
                }

                val minBufSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufSize, totalSamples * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(250)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Fallback to system tone generator
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                } catch (_: Exception) {}
            }
        }
    }
}
