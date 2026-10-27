package com.myraa.assistant.safety

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.myraa.assistant.notifications.MyraaNotificationListener

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isSpecial: Boolean = false,
    val actionType: SpecialActionType? = null
)

enum class SpecialActionType {
    ACCESSIBILITY,
    NOTIFICATION_LISTENER,
    OVERLAY,
    BATTERY_OPTIMIZATION,
    USAGE_ACCESS,
    DEFAULT_ASSISTANT
}

object CompanionPermissions {

    fun getPermissionsState(context: Context): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        list.add(
            PermissionItem(
                id = Manifest.permission.RECORD_AUDIO,
                title = "Microphone Access",
                description = "Required for always-on 'Hey Myraa' wake word and live voice conversation.",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            )
        )

        // 2. Camera
        list.add(
            PermissionItem(
                id = Manifest.permission.CAMERA,
                title = "Camera Sight",
                description = "Allows Myraa to see you or objects in front of you on demand ('Myraa, mala bagh').",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            )
        )

        // 3. Contacts
        list.add(
            PermissionItem(
                id = Manifest.permission.READ_CONTACTS,
                title = "Contacts",
                description = "Allows calling and messaging your saved contacts by name.",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            )
        )

        // 4. Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    id = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Post Notifications",
                    description = "Displays the persistent 'Myraa is listening' foreground companion status.",
                    isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        // 5. Phone Call & Call Log
        list.add(
            PermissionItem(
                id = Manifest.permission.CALL_PHONE,
                title = "Phone Calls",
                description = "Places hands-free phone calls when you instruct Myraa.",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            )
        )

        // 6. Accessibility Service (Special)
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val accessibilityGranted = enabledServices.contains(context.packageName)
        list.add(
            PermissionItem(
                id = "special_accessibility",
                title = "Accessibility Automation",
                description = "Enables screen vision, tapping buttons, typing messages, and navigating apps.",
                isGranted = accessibilityGranted,
                isSpecial = true,
                actionType = SpecialActionType.ACCESSIBILITY
            )
        )

        // 7. Floating Bubble Overlay (Special)
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        list.add(
            PermissionItem(
                id = "special_overlay",
                title = "Floating Bubble (Display Over Apps)",
                description = "Allows Myraa to float over other apps and show answers without leaving what you are doing.",
                isGranted = overlayGranted,
                isSpecial = true,
                actionType = SpecialActionType.OVERLAY
            )
        )

        // 8. Notification Listener (Special)
        val notifListenerGranted = MyraaNotificationListener.isConnected
        list.add(
            PermissionItem(
                id = "special_notif_listener",
                title = "Notification Reader",
                description = "Allows Myraa to read incoming messages and summarize alerts by voice.",
                isGranted = notifListenerGranted,
                isSpecial = true,
                actionType = SpecialActionType.NOTIFICATION_LISTENER
            )
        )

        // 9. Battery Optimization Exemption (Special)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true
        list.add(
            PermissionItem(
                id = "special_battery",
                title = "Ignore Battery Optimization",
                description = "Prevents Android from killing hands-free background listening when screen is locked.",
                isGranted = batteryIgnored,
                isSpecial = true,
                actionType = SpecialActionType.BATTERY_OPTIMIZATION
            )
        )

        return list
    }

    fun openSpecialPermissionSettings(context: Context, type: SpecialActionType) {
        try {
            when (type) {
                SpecialActionType.ACCESSIBILITY -> {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                SpecialActionType.NOTIFICATION_LISTENER -> {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                SpecialActionType.OVERLAY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }
                }
                SpecialActionType.BATTERY_OPTIMIZATION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }
                }
                SpecialActionType.USAGE_ACCESS -> {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                SpecialActionType.DEFAULT_ASSISTANT -> {
                    context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        } catch (_: Exception) {
            // Fallback to app details settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
