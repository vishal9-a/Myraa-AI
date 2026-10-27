package com.myraa.assistant.action

import android.content.Context
import com.myraa.assistant.agent.MyraaAccessibilityService
import com.myraa.assistant.agent.Platform
import com.myraa.assistant.agent.SocialAction
import com.myraa.assistant.agent.SocialTaskRequest
import com.myraa.assistant.agent.TaskMode
import com.myraa.assistant.agent.WhatsAppAgent
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

object ActionExecutor {
    private var pendingSocialConfirmation: SocialTaskRequest? = null

    suspend fun execute(context: Context, name: String, args: JSONObject): JSONObject {
        val result = JSONObject()
        val service = MyraaAccessibilityService.instance

        when (name) {
            "open_app" -> {
                val appName = args.optString("app_name", "").trim()
                if (appName.isEmpty()) {
                    result.put("error", "No app name provided")
                    return result
                }
                val apps = InstalledAppLabelCache.get(context)
                val pkg = AppLauncher.findApp(appName, apps)
                if (pkg != null && AppLauncher.launch(context, pkg)) {
                    result.put("status", "app_opened")
                    result.put("app_name", appName)
                    result.put("package", pkg)
                } else {
                    result.put("error", "App not found or could not be launched: $appName")
                }
            }

            "call_contact" -> {
                val query = args.optString("contact_name_or_number", "").trim()
                if (query.isEmpty()) {
                    result.put("error", "No contact name or number provided")
                    return result
                }
                when (val lookup = ContactResolver.lookup(context, query)) {
                    is ContactLookupResult.Found -> {
                        val callStatus = CommIntents.callContact(context, lookup.phoneNumber)
                        result.put("status", callStatus)
                        result.put("contact_name", lookup.displayName)
                        result.put("phone_number", lookup.phoneNumber)
                    }
                    is ContactLookupResult.NoPermission -> {
                        result.put("error", "Permission READ_CONTACTS not granted")
                    }
                    is ContactLookupResult.NotFound -> {
                        result.put("error", "Contact not found: $query")
                    }
                }
            }

            "message_contact" -> {
                val recipientQuery = args.optString("contact_name_or_number", "").trim()
                val message = args.optString("message", "").trim()
                val platform = args.optString("platform", "whatsapp").trim()

                if (message.isEmpty()) {
                    result.put("error", "No message content provided")
                    return result
                }

                val recipientNumber = if (recipientQuery.isNotEmpty()) {
                    when (val lookup = ContactResolver.lookup(context, recipientQuery)) {
                        is ContactLookupResult.Found -> lookup.phoneNumber
                        else -> recipientQuery
                    }
                } else ""

                val status = CommIntents.openMessageComposer(context, recipientNumber, message, platform)
                result.put("status", status)
                result.put("platform", platform)
                result.put("recipient", recipientQuery)

                // Try auto-tapping Send if accessibility service is active
                if (service != null && platform.equals("whatsapp", ignoreCase = true)) {
                    var sent = false
                    for (i in 0 until 12) {
                        delay(400)
                        if (service.tapText("Continue to chat")) {
                            delay(400)
                        }
                        if (service.tapText("Send")) {
                            sent = true
                            break
                        }
                    }
                    if (sent) {
                        result.put("status", "sent")
                    } else {
                        result.put("status", "composer_opened_send_not_found")
                    }
                }
            }

            "read_screen" -> {
                if (service == null) {
                    result.put("error", "accessibility_not_enabled")
                } else {
                    val elements = service.readScreen(maxElements = 40, maxLabelLength = 40)
                    val arr = JSONArray()
                    elements.forEach { arr.put(it) }
                    result.put("status", "success")
                    result.put("screen_elements", arr)
                }
            }

            "tap_text" -> {
                val targetText = args.optString("target_text", "").trim()
                if (service == null) {
                    result.put("error", "accessibility_not_enabled")
                } else if (targetText.isEmpty()) {
                    result.put("error", "No target_text provided")
                } else {
                    val tapped = service.tapText(targetText)
                    if (tapped) {
                        result.put("status", "tapped")
                        result.put("target", targetText)
                    } else {
                        result.put("error", "element_not_found")
                    }
                }
            }

            "type_text" -> {
                val text = args.optString("text", "")
                if (service == null) {
                    result.put("error", "accessibility_not_enabled")
                } else {
                    val typed = service.typeText(text)
                    if (typed) {
                        result.put("status", "typed")
                    } else {
                        result.put("error", "no_focused_field")
                    }
                }
            }

            "scroll_screen" -> {
                val direction = args.optString("direction", "down")
                if (service == null) {
                    result.put("error", "accessibility_not_enabled")
                } else {
                    val scrolled = service.scrollScreen(direction)
                    if (scrolled) {
                        result.put("status", "scrolled")
                    } else {
                        result.put("error", "nothing_scrollable")
                    }
                }
            }

            "go_back" -> {
                if (service == null) {
                    result.put("error", "accessibility_not_enabled")
                } else {
                    val done = service.goBack()
                    result.put("status", if (done) "done" else "failed")
                }
            }

            "go_home" -> {
                if (service == null) {
                    result.put("error", "accessibility_not_enabled")
                } else {
                    val done = service.goHome()
                    result.put("status", if (done) "done" else "failed")
                }
            }

            "execute_social_media_task" -> {
                val req = SocialTaskRequest.parse(args)
                if (req.mode == TaskMode.PUBLISH) {
                    pendingSocialConfirmation = req
                    result.put("status", "waiting_for_confirmation")
                    result.put("task_id", req.taskId)
                    result.put("prompt_to_user", "You asked to publish a ${req.action} on ${req.platform}. Should I go ahead and post it?")
                } else {
                    result.put("status", "draft_created")
                    result.put("task_id", req.taskId)
                    result.put("details", "Draft created for ${req.platform}: ${req.caption}")
                }
            }

            "social_media_task_control" -> {
                val cmd = args.optString("command", "").lowercase()
                val taskId = args.optString("task_id", "")
                val pending = pendingSocialConfirmation
                if (cmd == "confirm" && pending != null && pending.taskId == taskId) {
                    pendingSocialConfirmation = null
                    result.put("status", "submitted_unverified")
                    result.put("message", "Post submitted to ${pending.platform}. Please verify on the app.")
                } else if (cmd == "reject" || cmd == "cancel") {
                    pendingSocialConfirmation = null
                    result.put("status", "cancelled")
                } else {
                    result.put("status", "idle")
                }
            }

            "execute_whatsapp_task" -> {
                return WhatsAppAgent.execute(context, args)
            }

            "whatsapp_task_control" -> {
                result.put("status", "task_controlled")
            }

            "look_at_screen" -> {
                val question = args.optString("question", "")
                val hierarchy = com.myraa.assistant.vision.ScreenVisionManager.extractScreenSemanticHierarchy()
                result.put("status", "screen_analyzed")
                result.put("app_package", hierarchy.packageName)
                result.put("has_secure_fields", hierarchy.hasSecureField)
                result.put("screen_summary", hierarchy.toSummary())
                if (question.isNotEmpty()) {
                    result.put("user_question", question)
                }
                com.myraa.assistant.safety.SafetyManager.logAction("look_at_screen", "Inspected active screen (${hierarchy.packageName})", true)
            }

            "look_at_camera" -> {
                val cameraPref = args.optString("camera", "front").lowercase()
                val useFront = cameraPref != "back"
                val frameBytes = com.myraa.assistant.vision.CameraVisionManager.captureEphemeralFrame(context, useFront)
                if (frameBytes != null) {
                    result.put("status", "camera_active")
                    result.put("camera_used", if (useFront) "front" else "back")
                    result.put("frame_bytes_size", frameBytes.size)
                    result.put("message", "Camera frame inspected ephemerally in RAM. User is in front of the camera.")
                    com.myraa.assistant.safety.SafetyManager.logAction("look_at_camera", "Ephemeral camera look (${if (useFront) "front" else "back"})", true)
                } else {
                    result.put("status", "camera_permission_required")
                    result.put("message", "Camera permission or hardware was not available.")
                    com.myraa.assistant.safety.SafetyManager.logAction("look_at_camera", "Camera access failed", false)
                }
            }

            "remember_fact" -> {
                val key = args.optString("key", "").trim()
                val value = args.optString("value", "").trim()
                val catStr = args.optString("category", "PERSONAL").uppercase()
                val cat = try {
                    com.myraa.assistant.memory.MemoryCategory.valueOf(catStr)
                } catch (_: Exception) {
                    com.myraa.assistant.memory.MemoryCategory.PERSONAL
                }
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    val store = com.myraa.assistant.memory.MemoryStore.get(context)
                    store.saveFact(key, value, cat, 8)
                    result.put("status", "fact_remembered")
                    result.put("key", key)
                    result.put("value", value)
                    com.myraa.assistant.safety.SafetyManager.logAction("remember_fact", "Remembered $key: $value", true)
                } else {
                    result.put("error", "Key or value cannot be blank")
                }
            }

            "recall_memory" -> {
                val query = args.optString("query", "").trim()
                val store = com.myraa.assistant.memory.MemoryStore.get(context)
                val memoryContext = store.retrieveMemoryContext(query)
                result.put("status", "memory_recalled")
                result.put("query", query)
                result.put("memory_context", memoryContext.ifBlank { "No specific memories found matching '$query'." })
            }

            "forget_memory" -> {
                val topic = args.optString("topic", "").trim().lowercase()
                val store = com.myraa.assistant.memory.MemoryStore.get(context)
                if (topic == "all" || topic == "everything") {
                    store.clearAllMemories()
                    result.put("status", "all_memories_wiped")
                    com.myraa.assistant.safety.SafetyManager.logAction("forget_memory", "Permanently wiped all memories", true)
                } else {
                    // Match and delete facts containing topic in key
                    val facts = store.factsFlow.value
                    val toDelete = facts.filter { it.key.contains(topic, ignoreCase = true) }
                    toDelete.forEach { store.deleteFact(it.id) }
                    result.put("status", "facts_forgotten")
                    result.put("deleted_count", toDelete.size)
                    result.put("topic", topic)
                    com.myraa.assistant.safety.SafetyManager.logAction("forget_memory", "Forgot $topic (${toDelete.size} items)", true)
                }
            }

            "read_notifications" -> {
                val limit = args.optInt("limit", 5).coerceIn(1, 15)
                val summary = com.myraa.assistant.notifications.MyraaNotificationListener.getRecentSummary(limit)
                result.put("status", "notifications_read")
                result.put("summary", summary)
                com.myraa.assistant.safety.SafetyManager.logAction("read_notifications", "Read $limit recent notifications", true)
            }

            else -> {
                result.put("error", "Unknown tool: $name")
            }
        }

        return result
    }
}
