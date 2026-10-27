package com.myraa.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class LiveAudioIO(
    private val onMicChunk: (ByteArray) -> Unit,
    private val onMicAmplitude: (Float) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var currentVolume = 1.0f

    companion object {
        const val MIC_SAMPLE_RATE = 16000
        const val SPK_SAMPLE_RATE = 24000
        const val MIC_CHUNK_SIZE = 2048

        fun calculateRms(pcmBytes: ByteArray): Float {
            if (pcmBytes.isEmpty()) return 0f
            val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            val sampleCount = pcmBytes.size / 2
            if (sampleCount == 0) return 0f
            var sumSquare = 0.0
            while (buffer.hasRemaining() && buffer.remaining() >= 2) {
                val sample = buffer.short.toDouble()
                sumSquare += sample * sample
            }
            val mean = sumSquare / sampleCount
            val rawRms = sqrt(mean) / 32768.0
            return rawRms.coerceIn(0.0, 1.0).toFloat()
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val minBuf = AudioRecord.getMinBufferSize(
            MIC_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, 4096)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MIC_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize
            )

            val audioSessionId = record.audioSessionId
            if (audioSessionId != 0) {
                try {
                    if (AcousticEchoCanceler.isAvailable()) {
                        aec = AcousticEchoCanceler.create(audioSessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}
                try {
                    if (NoiseSuppressor.isAvailable()) {
                        ns = NoiseSuppressor.create(audioSessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}
                try {
                    if (AutomaticGainControl.isAvailable()) {
                        agc = AutomaticGainControl.create(audioSessionId)?.apply { enabled = true }
                    }
                } catch (_: Exception) {}
            }

            record.startRecording()
            audioRecord = record
            isRecording = true

            recordingThread = Thread({
                val chunk = ByteArray(MIC_CHUNK_SIZE)
                while (isRecording) {
                    val read = record.read(chunk, 0, MIC_CHUNK_SIZE)
                    if (read > 0) {
                        val validBytes = if (read == MIC_CHUNK_SIZE) chunk else chunk.copyOf(read)
                        val amp = calculateRms(validBytes)
                        onMicAmplitude(amp)
                        onMicChunk(validBytes)
                    }
                }
            }, "MyraaLiveAudioCapture").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (_: Exception) {
            isRecording = false
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try { aec?.release() } catch (_: Exception) {}
        try { ns?.release() } catch (_: Exception) {}
        try { agc?.release() } catch (_: Exception) {}
        aec = null
        ns = null
        agc = null
    }

    private fun getOrCreateTrack(): AudioTrack {
        val existing = audioTrack
        if (existing != null) return existing

        val minBuf = AudioTrack.getMinBufferSize(
            SPK_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = minBuf * 4

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(SPK_SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val track = AudioTrack(
            attributes,
            format,
            bufSize,
            AudioTrack.MODE_STREAM,
            0
        )
        track.setVolume(currentVolume)
        track.play()
        audioTrack = track
        return track
    }

    fun playChunk(pcmBytes: ByteArray): Float {
        if (pcmBytes.isEmpty()) return 0f
        val track = getOrCreateTrack()
        track.write(pcmBytes, 0, pcmBytes.size)
        return calculateRms(pcmBytes)
    }

    fun flushPlayback() {
        try {
            audioTrack?.let {
                it.pause()
                it.flush()
                it.play()
            }
        } catch (_: Exception) {}
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(currentVolume)
        } catch (_: Exception) {}
    }

    fun release() {
        stopRecording()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
