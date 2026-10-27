package com.myraa.assistant.offline

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object AutostartHelper {
    data class OEMGuide(val brand: String, val instructions: String, val intent: Intent?)

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        }
        return true
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    fun getOEMAutostartGuide(context: Context): OEMGuide {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                val intent = Intent().apply {
                    component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                OEMGuide(
                    brand = "Xiaomi / Redmi / POCO",
                    instructions = "Enable 'Autostart' and set Battery Saver to 'No restrictions' in App Info so Myraa can listen when screen is off.",
                    intent = intent
                )
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                val intent = Intent().apply {
                    component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                OEMGuide(
                    brand = "Oppo / Realme",
                    instructions = "Allow 'Auto-launch' and 'Background launch' under App Management so hands-free wake word stays active.",
                    intent = intent
                )
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                val intent = Intent().apply {
                    component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                OEMGuide(
                    brand = "Vivo / iQOO",
                    instructions = "Enable 'Autostart' and allow high background battery usage in Settings -> Battery -> Background power consumption.",
                    intent = intent
                )
            }
            manufacturer.contains("samsung") -> {
                val intent = Intent().apply {
                    component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                OEMGuide(
                    brand = "Samsung",
                    instructions = "Go to Battery -> Background usage limits -> Never sleeping apps -> Add Myraa.",
                    intent = intent
                )
            }
            else -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                OEMGuide(
                    brand = "Standard Android",
                    instructions = "Disable battery optimization and allow background activity so Myraa stays ready to wake on command.",
                    intent = intent
                )
            }
        }
    }

    fun openOEMAutostartSettings(context: Context): Boolean {
        val guide = getOEMAutostartGuide(context)
        return try {
            guide.intent?.let { context.startActivity(it) }
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
