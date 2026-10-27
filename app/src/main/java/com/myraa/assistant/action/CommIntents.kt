package com.myraa.assistant.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.net.URLEncoder

object CommIntents {
    fun callContact(context: Context, phoneNumber: String): String {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            if (hasCallPermission) "calling" else "dialer_opened_no_permission"
        } catch (_: Exception) {
            "failed_to_place_call"
        }
    }

    fun openMessageComposer(context: Context, recipient: String, message: String, platform: String): String {
        val isWhatsApp = platform.equals("whatsapp", ignoreCase = true)
        val cleanPhone = recipient.filter { it.isDigit() || it == '+' }

        if (isWhatsApp) {
            val encodedMsg = try {
                URLEncoder.encode(message, "UTF-8")
            } catch (_: Exception) {
                message
            }
            val waUri = if (cleanPhone.isNotEmpty()) {
                val digitsOnly = cleanPhone.filter { it.isDigit() }
                Uri.parse("https://wa.me/$digitsOnly?text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }

            val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if standard WhatsApp or WhatsApp Business is installed
            val pm = context.packageManager
            val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            for (pkg in waPackages) {
                try {
                    pm.getPackageInfo(pkg, 0)
                    waIntent.setPackage(pkg)
                    break
                } catch (_: Exception) {}
            }

            return try {
                context.startActivity(waIntent)
                "composer_opened"
            } catch (_: Exception) {
                // Fallback to SMS if WhatsApp failed
                openSmsComposer(context, cleanPhone, message)
            }
        } else {
            return openSmsComposer(context, cleanPhone, message)
        }
    }

    private fun openSmsComposer(context: Context, phoneNumber: String, message: String): String {
        val uri = if (phoneNumber.isNotEmpty()) {
            Uri.parse("smsto:$phoneNumber")
        } else {
            Uri.parse("smsto:")
        }
        val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(smsIntent)
            "composer_opened"
        } catch (_: Exception) {
            "failed_to_open_composer"
        }
    }
}
