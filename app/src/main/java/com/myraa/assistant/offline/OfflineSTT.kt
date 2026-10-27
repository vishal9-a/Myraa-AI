package com.myraa.assistant.offline

import android.content.Context

/**
 * OfflineSTT backward-compatible delegate to MyraaOfflineSTT.
 * All offline speech recognition runs through the Vosk/Whisper pipeline.
 */
class OfflineSTT(context: Context) {
    private val delegate = MyraaOfflineSTT(context)

    val state get() = delegate.state
    val engineType get() = delegate.engineType

    fun startListening(
        languageLocale: String = "mr-IN",
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        delegate.startListening(languageLocale, onPartial, onFinal, onError)
    }

    fun stopListening() {
        delegate.stopListening()
    }

    fun release() {
        delegate.release()
    }
}
