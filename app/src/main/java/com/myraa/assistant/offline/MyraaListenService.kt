package com.myraa.assistant.offline

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.myraa.assistant.MainActivity
import com.myraa.assistant.overlay.FloatingBubbleService
import com.myraa.assistant.voice.ChimePlayer
import com.myraa.assistant.voice.EchoGuard
import com.myraa.assistant.voice.Personality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyraaListenService : Service() {

    companion object {
        const val ACTION_START = "com.myraa.assistant.offline.START"
        const val ACTION_STOP = "com.myraa.assistant.offline.STOP"
        const val ACTION_TOGGLE_PAUSE = "com.myraa.assistant.offline.TOGGLE_PAUSE"

        private const val CHANNEL_ID = "myraa_offline_listen_channel"
        private const val NOTIFICATION_ID = 3001

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, MyraaListenService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MyraaListenService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var recordThread: Thread? = null
    private var wakeEngine: WakeWordEngine? = null

    private var offlineSTT: MyraaOfflineSTT? = null
    private var offlineTTS: OfflineTTS? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var followUpJob: Job? = null
    private var isUserExplicitStop = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        offlineSTT = MyraaOfflineSTT(applicationContext)
        offlineTTS = OfflineTTS(applicationContext)

        wakeEngine = WakeWordEngine(
            sensitivity = ModeManager.sensitivity.value,
            onWakeDetected = { onWakeWordTriggered() }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isUserExplicitStop = true
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PAUSE -> {
                _isPaused.value = !_isPaused.value
                val notification = buildNotification(_isPaused.value)
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, notification)
                return START_STICKY
            }
        }

        isUserExplicitStop = false
        val notification = buildNotification(isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceActive.value = true
        _isPaused.value = false
        startAudioLoop()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startAudioLoop() {
        if (isRecording) return

        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, 2048)

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize
            )
            record.startRecording()
            audioRecord = record
            isRecording = true
            wakeEngine?.start()

            recordThread = Thread({
                val buffer = ByteArray(1024)
                while (isRecording) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0 && !_isPaused.value && !EchoGuard.isMicMuted()) {
                        wakeEngine?.processAudio(buffer, read)
                    }
                }
            }, "MyraaWakeWordListener").apply {
                priority = Thread.NORM_PRIORITY
                start()
            }
        } catch (_: Exception) {
            isRecording = false
        }
    }

    private fun onWakeWordTriggered() {
        // 1. Haptic feedback pulse
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}

        // 2. Play acoustic chime tone
        ChimePlayer.playWakeChime(this)

        // 3. Pause wake word detection to free up mic for speech command recognition
        pauseWakeDetectionForListening()

        // 4. Show Floating Overlay Orb (Do not open full MainActivity if overlay is allowed)
        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        if (canOverlay) {
            FloatingBubbleService.start(this)
            mainHandler.postDelayed({
                FloatingBubbleService.instance?.showListening("मी ऐकतेय... बोला! / Listening...")
            }, 200)
        } else {
            // Fallback: screen lock / wake trigger activity
            val intent = Intent(this, WakeTriggerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }

        // 5. Start listening to the user's speech command
        startSpeechCommandCapture()
    }

    private fun pauseWakeDetectionForListening() {
        _isPaused.value = true
        wakeEngine?.stop()
    }

    private fun resumeWakeDetection() {
        _isPaused.value = false
        wakeEngine?.start()
    }

    private fun startSpeechCommandCapture() {
        followUpJob?.cancel()

        mainHandler.postDelayed({
            offlineSTT?.startListening(
                languageLocale = "mr-IN",
                onPartial = { partial ->
                    FloatingBubbleService.instance?.showListening(partial)
                },
                onFinal = { finalResult ->
                    if (finalResult.isNotBlank()) {
                        handleRecognizedCommand(finalResult)
                    } else {
                        scheduleFollowUpOrResume()
                    }
                },
                onError = {
                    scheduleFollowUpOrResume()
                }
            )
        }, 350)
    }

    private fun handleRecognizedCommand(query: String) {
        serviceScope.launch {
            FloatingBubbleService.instance?.showListening("प्रक्रिया करत आहे... / Processing...")

            val intentResult = IntentRouter.route(applicationContext, query)
            val replyText = when (intentResult) {
                is IntentResult.Handled -> intentResult.responseText
                is IntentResult.NeedsBrain -> {
                    LocalLLM.generateOfflineReply(
                        context = applicationContext,
                        query = query,
                        personality = Personality.NORMAL,
                        assistantName = "Myraa"
                    )
                }
            }

            FloatingBubbleService.instance?.showReplyBubble(replyText)

            // Speak response using configured gender voice and pitch
            offlineTTS?.speak(
                text = replyText,
                personality = Personality.NORMAL,
                onDone = {
                    // Enter 6-second follow-up window (user can speak again without wake-word)
                    scheduleFollowUpOrResume()
                }
            )
        }
    }

    private fun scheduleFollowUpOrResume() {
        followUpJob?.cancel()
        followUpJob = serviceScope.launch {
            // Keep listening window active for 6 seconds
            delay(6000)
            offlineSTT?.stopListening()
            resumeWakeDetection()
        }
    }

    private fun stopAudioLoop() {
        isRecording = false
        wakeEngine?.stop()
        recordThread?.interrupt()
        recordThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, MyraaListenService::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        val pausePending = PendingIntent.getService(
            this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MyraaListenService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "Hands-free listening is paused" else "Say 'Hey Myraa' or 'Myraa' anytime"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Myraa · Hands-Free Mode")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pausePending
            )
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Myraa Hands-Free Listening"
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-on offline wake word detection for Myraa"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        followUpJob?.cancel()
        offlineSTT?.stopListening()
        offlineTTS?.release()
        stopAudioLoop()
        _isServiceActive.value = false

        // Auto-restart watchdog if not explicitly stopped by the user
        if (!isUserExplicitStop) {
            try {
                val restartIntent = Intent(applicationContext, MyraaListenService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(restartIntent)
                } else {
                    applicationContext.startService(restartIntent)
                }
            } catch (_: Exception) {}
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
