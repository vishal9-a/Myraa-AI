package com.myraa.assistant.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.myraa.assistant.MainActivity
import com.myraa.assistant.R
import com.myraa.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * FloatingBubbleService renders a draggable floating Myraa orb overlay.
 * Users can interact with Myraa from any app without opening the full application.
 */
class FloatingBubbleService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var bubbleContainer: FrameLayout? = null
    private var bubbleCard: LinearLayout? = null
    private var responseTextView: TextView? = null
    private var orbView: View? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        setupForegroundNotification()
        createBubbleOverlay()
        observeMyraaState()
    }

    private fun setupForegroundNotification() {
        val channelId = "myraa_floating_bubble_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Myraa Floating Bubble",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows Myraa floating companion bubble"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Myraa Assistant Active")
            .setContentText("Tap to open full app or drag the floating bubble")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBubbleOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        val density = resources.displayMetrics.density
        val dpToPx = { dp: Int -> (dp * density).toInt() }

        bubbleContainer = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        // Expanded text bubble card
        bubbleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * density
                setColor(Color.parseColor("#E6111827")) // Midnight glass
                setStroke((1.5f * density).toInt(), Color.parseColor("#4B5563"))
            }
            background = bg
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            elevation = 16 * density

            responseTextView = TextView(this@FloatingBubbleService).apply {
                text = "Myraa is listening..."
                setTextColor(Color.parseColor("#F9FAFB"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                maxLines = 5
            }
            addView(responseTextView)
        }

        // Orb Avatar View (Glowing circular badge)
        val orbSize = dpToPx(58)
        orbView = View(this).apply {
            val orbBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(
                    Color.parseColor("#FBBF24"), // Marigold core
                    Color.parseColor("#818CF8"), // Indigo glow
                    Color.parseColor("#0F172A")  // Deep navy
                )
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 35 * density
                setStroke((2 * density).toInt(), Color.parseColor("#FDE68A"))
            }
            background = orbBg
            elevation = 20 * density
        }

        val orbParams = FrameLayout.LayoutParams(orbSize, orbSize).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        val cardParams = FrameLayout.LayoutParams(dpToPx(240), FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            marginStart = dpToPx(64)
        }

        bubbleContainer?.addView(bubbleCard, cardParams)
        bubbleContainer?.addView(orbView, orbParams)

        // Drag and Click handling
        var startClickTime: Long = 0
        var isDragging = false

        orbView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    startClickTime = System.currentTimeMillis()
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(bubbleContainer, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - startClickTime
                    if (!isDragging && duration < 300) {
                        toggleExpanded()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(bubbleContainer, params)
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        bubbleCard?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        if (isExpanded) {
            val status = VoiceSessionManager.statusText.value
            responseTextView?.text = if (status.isNotBlank()) status else "Hi! I'm Myraa. How can I help you right now?"
        }
    }

    private fun observeMyraaState() {
        serviceScope.launch {
            VoiceSessionManager.state.collectLatest { orbState ->
                val status = VoiceSessionManager.statusText.value
                val err = VoiceSessionManager.lastError.value
                responseTextView?.text = when {
                    err != null -> "Notice: $err"
                    status.isNotBlank() -> status
                    orbState == com.myraa.assistant.voice.NovaOrbState.SPEAKING -> "Myraa is speaking..."
                    orbState == com.myraa.assistant.voice.NovaOrbState.LISTENING -> "Myraa is listening..."
                    orbState == com.myraa.assistant.voice.NovaOrbState.CONNECTING -> "Connecting to Myraa..."
                    else -> "Hi, I'm Myraa. Ready whenever you are."
                }
                // Animate orb glow color depending on state
                val orbBg = orbView?.background as? GradientDrawable
                if (orbBg != null) {
                    val colors = when (orbState) {
                        com.myraa.assistant.voice.NovaOrbState.SPEAKING, com.myraa.assistant.voice.NovaOrbState.LISTENING -> {
                            intArrayOf(Color.parseColor("#34D399"), Color.parseColor("#38BDF8"), Color.parseColor("#0F172A"))
                        }
                        com.myraa.assistant.voice.NovaOrbState.ERROR -> {
                            intArrayOf(Color.parseColor("#EF4444"), Color.parseColor("#F87171"), Color.parseColor("#0F172A"))
                        }
                        else -> {
                            intArrayOf(Color.parseColor("#FBBF24"), Color.parseColor("#818CF8"), Color.parseColor("#0F172A"))
                        }
                    }
                    orbBg.colors = colors
                }
            }
        }
    }

    fun showListening(prompt: String = "मी ऐकतेय... बोला! / Listening...") {
        serviceScope.launch {
            isExpanded = true
            bubbleCard?.visibility = View.VISIBLE
            responseTextView?.text = prompt
            val orbBg = orbView?.background as? GradientDrawable
            orbBg?.colors = intArrayOf(Color.parseColor("#34D399"), Color.parseColor("#38BDF8"), Color.parseColor("#0F172A"))
        }
    }

    fun showReplyBubble(text: String) {
        serviceScope.launch {
            isExpanded = true
            bubbleCard?.visibility = View.VISIBLE
            responseTextView?.text = text
            val orbBg = orbView?.background as? GradientDrawable
            orbBg?.colors = intArrayOf(Color.parseColor("#FBBF24"), Color.parseColor("#818CF8"), Color.parseColor("#0F172A"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        serviceScope.cancel()
        if (bubbleContainer != null && windowManager != null) {
            try {
                windowManager?.removeView(bubbleContainer)
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 9021
        var instance: FloatingBubbleService? = null
            private set

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        }
    }
}
