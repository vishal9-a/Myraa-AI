package com.myraa.assistant.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.myraa.assistant.MainActivity
import com.myraa.assistant.data.NovaPreferences

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val BOOT_CHANNEL_ID = "myraa_boot_activation_channel"
        private const val BOOT_NOTIFICATION_ID = 3002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_USER_PRESENT
        ) {
            val handsFreeEnabled = NovaPreferences.getBoolean(context, "hands_free_wake_word_enabled", false)
            if (!handsFreeEnabled) return

            // On Android 14+, microphone FGS cannot be launched from background without user interaction.
            // Post notification "Tap to activate Myraa" so user tap starts foreground service immediately.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                showActivationNotification(context)
            } else {
                try {
                    MyraaListenService.start(context)
                } catch (_: Exception) {
                    showActivationNotification(context)
                }
            }
        }
    }

    private fun showActivationNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BOOT_CHANNEL_ID,
                "Myraa Assistant Activation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tap to resume Myraa hands-free listening after restart"
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ACTIVATE_HANDS_FREE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BOOT_CHANNEL_ID)
            .setContentTitle("Myraa · Tap to Activate")
            .setContentText("Phone restarted. Tap once to activate hands-free 'Hey Myraa' mode.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(BOOT_NOTIFICATION_ID, notification)
    }
}
