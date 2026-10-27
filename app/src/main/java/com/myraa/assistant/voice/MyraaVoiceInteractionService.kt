package com.myraa.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.myraa.assistant.MainActivity

/**
 * MyraaVoiceInteractionService allows Myraa to act as the Android Default Digital Assistant
 * triggered by home long-press, power-button assistant gesture, or hardware headset clicks.
 */
class MyraaVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        isActive = true
    }

    override fun onShutdown() {
        super.onShutdown()
        isActive = false
    }

    companion object {
        var isActive: Boolean = false
            private set
    }
}

/**
 * MyraaVoiceSessionService provides the session lifecycle for system assistant triggers.
 */
class MyraaVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return MyraaVoiceInteractionSession(this)
    }
}

class MyraaVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Launch main activity into voice mode or floating bubble
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_VOICE_TRIGGER", true)
        }
        context.startActivity(intent)
        hide()
    }
}
