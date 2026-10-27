package com.myraa.assistant.offline

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import com.myraa.assistant.action.AppLauncher
import com.myraa.assistant.action.CommIntents
import com.myraa.assistant.action.ContactLookupResult
import com.myraa.assistant.action.ContactResolver
import com.myraa.assistant.action.InstalledAppLabelCache
import com.myraa.assistant.data.NovaPreferences
import com.myraa.assistant.safety.PendingConfirmation
import com.myraa.assistant.safety.SafetyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class IntentResult {
    data class Handled(val responseText: String) : IntentResult()
    data class NeedsBrain(val originalQuery: String) : IntentResult()
}

object IntentRouter {

    suspend fun route(context: Context, rawQuery: String): IntentResult {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) {
            return IntentResult.Handled("मी ऐकतेय, बोला! / Yes, I'm listening.")
        }

        // 0. Active Confirmation check (Voice confirmation for sensitive actions)
        val activeConf = SafetyManager.activeConfirmation
        if (activeConf != null) {
            if (SafetyManager.isAffirmative(q)) {
                SafetyManager.clearPendingConfirmation()
                val resultText = activeConf.onConfirm()
                return IntentResult.Handled(resultText)
            } else if (SafetyManager.isNegative(q) || SafetyManager.isEmergencyStop(q)) {
                SafetyManager.clearPendingConfirmation()
                val resultText = activeConf.onReject()
                return IntentResult.Handled(resultText)
            } else {
                return IntentResult.Handled("कृपया खात्री करण्यासाठी 'हो' किंवा 'नाही' म्हणा. (${activeConf.confirmationPrompt})")
            }
        }

        // 1. Voice Modulation Commands (Cute Girl, Soft Girl, Male, Speed, Tone)
        if (q.contains("cute girl") || q.contains("cute aawaj") || q.contains("cute voice") || q.contains("god aawaj") || q.contains("god bol")) {
            NovaPreferences.putString(context, "voice_profile_id", VoiceProfile.CUTE_GIRL.id)
            NovaPreferences.putString(context, "voice_gender", "female")
            return IntentResult.Handled("माझा आवाज आता क्यूट गर्ल (Cute Girl) केला आहे! मी आता गोड आणि उत्साहात बोलेन.")
        }

        if (q.contains("soft girl") || q.contains("soft aawaj") || q.contains("soft voice") || q.contains("saumya aawaj")) {
            NovaPreferences.putString(context, "voice_profile_id", VoiceProfile.SOFT_GIRL.id)
            NovaPreferences.putString(context, "voice_gender", "female")
            return IntentResult.Handled("माझा आवाज आता सॉफ्ट गर्ल (Soft Girl) केला आहे. मी शांतपणे आणि आपुलकीने बोलेन.")
        }

        if (q.contains("aawaj male") || q.contains("aawaz male") || q.contains("male voice") || q.contains("purush aawaj") || q.contains("male aawaj")) {
            NovaPreferences.putString(context, "voice_profile_id", VoiceProfile.MALE.id)
            NovaPreferences.putString(context, "voice_gender", "male")
            return IntentResult.Handled("माझा आवाज आता पुरुष (male) केला आहे. मी आता या आवाजात बोलेन.")
        }

        if (q.contains("aawaj female") || q.contains("aawaz female") || q.contains("female voice") || q.contains("stri aawaj")) {
            NovaPreferences.putString(context, "voice_profile_id", VoiceProfile.CUTE_GIRL.id)
            NovaPreferences.putString(context, "voice_gender", "female")
            return IntentResult.Handled("माझा आवाज आता स्त्री (female - cute girl) केला आहे.")
        }

        if (q.contains("haluu bol") || q.contains("slow bol") || q.contains("speak slower") || q.contains("dhire bolo")) {
            NovaPreferences.putFloat(context, "speech_speed", 0.8f)
            return IntentResult.Handled("मी आता हळू आणि स्पष्ट बोलेन.")
        }

        if (q.contains("jaast fast bol") || q.contains("fast bol") || q.contains("speak faster") || q.contains("tez bolo")) {
            NovaPreferences.putFloat(context, "speech_speed", 1.25f)
            return IntentResult.Handled("मी आता वेगाने बोलेन.")
        }

        if (q.contains("caring tone") || q.contains("caring tone madhe") || q.contains("premalk bol")) {
            NovaPreferences.putString(context, "tone_mode", "caring")
            return IntentResult.Handled("नक्कीच, मी आपुलकीने आणि काळजीपूर्वक बोलेन.")
        }

        // 2. Screen Reading: "screen var kay ahe", "screen vach", "screen par kya hai"
        if (q.contains("screen var kay") || q.contains("screen vach") || q.contains("screen par kya") || q.contains("what is on the screen") || q.contains("read screen")) {
            val nodes = try {
                val clazz = Class.forName("com.myraa.assistant.agent.MyraaAccessibilityService")
                val field = clazz.getDeclaredField("instance")
                field.isAccessible = true
                val instance = field.get(null)
                if (instance != null) {
                    val method = clazz.getMethod("readScreen", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    @Suppress("UNCHECKED_CAST")
                    method.invoke(instance, 25, 50) as? List<String> ?: emptyList()
                } else emptyList()
            } catch (_: Throwable) {
                emptyList()
            }

            if (nodes.isNotEmpty()) {
                val topNodes = nodes.take(6).joinToString(", ")
                return IntentResult.Handled("स्क्रीनवर सध्या हे दिसत आहे: $topNodes")
            } else {
                return IntentResult.Handled("स्क्रीन वाचण्यासाठी Accessibility Service चालू असणे आवश्यक आहे. Settings मध्ये जाऊन ती ऑन करा.")
            }
        }

        // 3. Time / Date queries (Marathi + Hindi + English)
        if (q.contains("time") || q.contains("vel") || q.contains("samay") || q.contains("kitne baje") || q.contains("kay zhalay")) {
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            return IntentResult.Handled("आता $time वाजले आहेत.")
        }

        if (q.contains("date") || q.contains("tarikh") || q.contains("din") || q.contains("divas")) {
            val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
            return IntentResult.Handled("आज $date आहे.")
        }

        // 4. Battery percentage
        if (q.contains("battery") || q.contains("charge") || q.contains("charging")) {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 50
            return IntentResult.Handled("फोनची बॅटरी $batteryPct टक्के आहे.")
        }

        // 5. Flashlight / Torch (Marathi + Hindi + English)
        if (q.contains("torch on") || q.contains("torch chalu") || q.contains("torch jalao") || q.contains("flashlight on")) {
            val success = toggleFlashlight(context, true)
            return IntentResult.Handled(if (success) "टॉर्च चालू केली आहे." else "टॉर्च उपलब्ध नाही.")
        }
        if (q.contains("torch off") || q.contains("torch band") || q.contains("torch bujhao") || q.contains("flashlight off")) {
            val success = toggleFlashlight(context, false)
            return IntentResult.Handled(if (success) "टॉर्च बंद केली आहे." else "टॉर्च बंद करण्यात अडचण आली.")
        }

        // 6. Volume controls
        if (q.contains("volume up") || q.contains("volume vadhav") || q.contains("aawaj vadhav") || q.contains("volume badhao")) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            return IntentResult.Handled("आवाज वाढवला आहे.")
        }
        if (q.contains("volume down") || q.contains("volume kami") || q.contains("aawaj kami") || q.contains("volume kam")) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            return IntentResult.Handled("आवाज कमी केला आहे.")
        }

        // 7. Alarms & Timers
        if (q.contains("alarm") || q.contains("uthav") || q.contains("uthana")) {
            val hour = extractHour(q)
            if (hour != null) {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, 0)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "Myraa Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return try {
                    context.startActivity(intent)
                    IntentResult.Handled("$hour वाजताचा अलार्म लावला आहे.")
                } catch (_: Exception) {
                    IntentResult.Handled("अलार्म लावण्यात अडचण आली.")
                }
            }
        }

        if (q.contains("timer")) {
            val minutes = extractDigits(q) ?: 5
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Myraa Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                IntentResult.Handled("$minutes मिनिटांचा टायमर सुरू केला आहे.")
            } catch (_: Exception) {
                IntentResult.Handled("टायमर सुरू करता आला नाही.")
            }
        }

        // 8. Wi-Fi / Bluetooth
        if (q.contains("wifi") || q.contains("wi-fi")) {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            try { context.startActivity(intent) } catch (_: Exception) {}
            return IntentResult.Handled("वायफाय सेटिंग्स उघडत आहे.")
        }
        if (q.contains("bluetooth")) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            try { context.startActivity(intent) } catch (_: Exception) {}
            return IntentResult.Handled("ब्लूटूथ सेटिंग्स उघडत आहे.")
        }

        // 9. Sensitive: Money transfer / Payment guard
        if (q.contains("paise pathav") || q.contains("send money") || q.contains("payment kar") || q.contains("transfer")) {
            val conf = PendingConfirmation(
                actionType = "send_money",
                description = "पैशांचा व्यवहार करणे",
                confirmationPrompt = "पैशांचा व्यवहार सुरू करायचा आहे का?",
                onConfirm = {
                    "सुरक्षिततेसाठी पेमेंट ॲप उघडून स्वतः पिन टाकावा."
                },
                onReject = {
                    "पैशांचा व्यवहार रद्द केला आहे."
                }
            )
            SafetyManager.setPendingConfirmation(conf)
            return IntentResult.Handled("हा संवेदनशील व्यवहार आहे. तुम्हाला खात्री आहे का? 'हो' किंवा 'नाही' म्हणा.")
        }

        // 10. Sensitive: Deletion guard
        if (q.contains("delete kar") || q.contains("delete memory") || q.contains("data delete") || q.contains("remove")) {
            val conf = PendingConfirmation(
                actionType = "delete_data",
                description = "माहिती नष्ट करणे",
                confirmationPrompt = "माहिती हटवायची आहे का?",
                onConfirm = {
                    "माहिती हटवली आहे."
                },
                onReject = {
                    "डिलिट करण्याची कृती रद्द केली आहे."
                }
            )
            SafetyManager.setPendingConfirmation(conf)
            return IntentResult.Handled("तुम्हाला ही माहिती खरंच डिलिट करायची आहे का? खात्री असल्यास 'हो' म्हणा.")
        }

        // 11. Sensitive: Phone Calls with Voice Confirmation
        if (q.startsWith("call ") || q.contains(" la call") || q.contains(" ko call") || q.contains("phone laav") || q.contains("phone lagao")) {
            val contactName = q.removePrefix("call ")
                .replace("la call laav", "")
                .replace("la call kar", "")
                .replace("ko call karo", "")
                .replace("ko call lagao", "")
                .replace("phone laav", "")
                .replace("phone lagao", "")
                .trim()

            if (contactName.isNotEmpty()) {
                val lookup = ContactResolver.lookup(context, contactName)
                return when (lookup) {
                    is ContactLookupResult.Found -> {
                        val conf = PendingConfirmation(
                            actionType = "call_contact",
                            description = "Call ${lookup.displayName}",
                            confirmationPrompt = "${lookup.displayName} ला कॉल लावायचा का?",
                            onConfirm = {
                                CommIntents.callContact(context, lookup.phoneNumber)
                                "${lookup.displayName} यांना कॉल लावत आहे."
                            },
                            onReject = {
                                "कॉल रद्द केला आहे."
                            }
                        )
                        SafetyManager.setPendingConfirmation(conf)
                        IntentResult.Handled("${lookup.displayName} यांना कॉल लावायचा का? खात्री असल्यास 'हो' म्हणा.")
                    }
                    is ContactLookupResult.NoPermission -> {
                        IntentResult.Handled("काँटॅक्ट्स वाचण्यासाठी परवानगी आवश्यक आहे. कृपया सेटिंग्जमध्ये जाऊन परवानगी द्या.")
                    }
                    is ContactLookupResult.NotFound -> {
                        IntentResult.Handled("$contactName चा नंबर सापडला नाही.")
                    }
                }
            }
        }

        // 12. Open Apps: "whatsapp ughad", "youtube open kar", "camera chalu kar", "open instagram"
        if (q.startsWith("open ") || q.endsWith(" ughad") || q.endsWith(" kholo") || q.endsWith(" open kar") || q.endsWith(" open karo") || q.endsWith(" chalu kar")) {
            val appQuery = q.removePrefix("open ")
                .removeSuffix(" ughad")
                .removeSuffix(" kholo")
                .removeSuffix(" open kar")
                .removeSuffix(" open karo")
                .removeSuffix(" chalu kar")
                .trim()

            if (appQuery.isNotEmpty()) {
                val apps = InstalledAppLabelCache.get(context)
                val pkg = AppLauncher.findApp(appQuery, apps)
                if (pkg != null && AppLauncher.launch(context, pkg)) {
                    return IntentResult.Handled("$appQuery उघडत आहे.")
                }
            }
        }

        // If not a fast-path device action, pass to Local LLM / Online brain
        return IntentResult.NeedsBrain(rawQuery)
    }

    private fun toggleFlashlight(context: Context, enable: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enable)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun extractHour(text: String): Int? {
        val regex = Regex("(\\d{1,2})")
        val match = regex.find(text)
        return match?.value?.toIntOrNull()?.coerceIn(0, 23)
    }

    private fun extractDigits(text: String): Int? {
        val regex = Regex("(\\d+)")
        val match = regex.find(text)
        return match?.value?.toIntOrNull()
    }
}
