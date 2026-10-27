package com.myraa.assistant.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

/**
 * MyraaNotificationListener captures incoming notifications for voice queries.
 * Users can ask: "Myraa, read my new notifications", "Any messages from Rahul?", etc.
 */
class MyraaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || shouldIgnore(sbn)) return
        val item = parseNotification(sbn) ?: return
        recentNotifications.removeAll { it.key == item.key }
        recentNotifications.add(0, item)
        while (recentNotifications.size > MAX_ITEMS) {
            recentNotifications.removeAt(recentNotifications.lastIndex)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null) {
            recentNotifications.removeAll { it.key == sbn.key }
        }
    }

    private fun syncActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            for (sbn in active) {
                if (!shouldIgnore(sbn)) {
                    val item = parseNotification(sbn)
                    if (item != null && recentNotifications.none { it.key == item.key }) {
                        recentNotifications.add(item)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun shouldIgnore(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return true
        val notification = sbn.notification ?: return true
        // Ignore ongoing foreground services, persistent downloads, media playback controls
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return true
        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return true
        return false
    }

    private fun parseNotification(sbn: StatusBarNotification): NotificationItem? {
        val extras = sbn.notification?.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val content = bigText?.ifBlank { text } ?: text

        if (title.isNullOrEmpty() && content.isNullOrEmpty()) return null

        val appLabel = try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        return NotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appLabel,
            title = title.orEmpty(),
            content = content.orEmpty(),
            postTime = sbn.postTime
        )
    }

    companion object {
        private const val MAX_ITEMS = 25
        private val recentNotifications = CopyOnWriteArrayList<NotificationItem>()
        var instance: MyraaNotificationListener? = null
            private set

        val isConnected: Boolean
            get() = instance != null

        fun getRecentItems(): List<NotificationItem> = recentNotifications.toList()

        fun getRecentSummary(limit: Int = 5): String {
            val items = recentNotifications.take(limit)
            if (items.isEmpty()) return "You have no new unread notifications."
            val b = StringBuilder()
            b.append("You have ${items.size} recent notifications:\n")
            items.forEachIndexed { i, it ->
                b.append("${i + 1}. [${it.appName}] ${it.title}: ${it.content.take(70)}\n")
            }
            return b.toString().trim()
        }

        fun clearNotifications() {
            recentNotifications.clear()
        }
    }
}

data class NotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val postTime: Long
)
