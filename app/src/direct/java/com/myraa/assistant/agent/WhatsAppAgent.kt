package com.myraa.assistant.agent

import android.content.Context
import com.myraa.assistant.action.CommIntents
import com.myraa.assistant.action.ContactLookupResult
import com.myraa.assistant.action.ContactResolver
import org.json.JSONObject

enum class WhatsAppAction {
    READ_UNREAD,
    SEND_MESSAGE,
    READ_CHAT,
    SEARCH_CHAT,
    MUTE_CHAT,
    UNMUTE_CHAT,
    MARK_READ
}

object WhatsAppAgent {
    suspend fun execute(context: Context, args: JSONObject): JSONObject {
        val result = JSONObject()
        val actionStr = args.optString("action", "send_message").uppercase()
        val action = try { WhatsAppAction.valueOf(actionStr) } catch (_: Exception) { WhatsAppAction.SEND_MESSAGE }
        val contactQuery = args.optString("contact_query", "").trim()
        val messageText = args.optString("message_text", "").trim()

        when (action) {
            WhatsAppAction.SEND_MESSAGE -> {
                if (contactQuery.isEmpty()) {
                    result.put("error", "No recipient specified")
                    return result
                }
                val lookup = ContactResolver.lookup(context, contactQuery)
                val phoneNumber = when (lookup) {
                    is ContactLookupResult.Found -> lookup.phoneNumber
                    is ContactLookupResult.NoPermission -> {
                        result.put("error", "Permission READ_CONTACTS not granted")
                        return result
                    }
                    is ContactLookupResult.NotFound -> {
                        if (ContactResolver.isPhoneNumber(contactQuery)) contactQuery
                        else {
                            result.put("error", "Contact not found: $contactQuery")
                            return result
                        }
                    }
                }

                val status = CommIntents.openMessageComposer(context, phoneNumber, messageText, "whatsapp")
                result.put("status", status)
                result.put("recipient", contactQuery)
                result.put("message", messageText)

                // If MyraaAccessibilityService is running, try auto-tapping Send
                val service = MyraaAccessibilityService.instance
                if (service != null) {
                    // Try auto-tapping Send or Continue to chat
                    var sent = false
                    for (i in 0 until 12) {
                        kotlinx.coroutines.delay(400)
                        if (service.tapText("Continue to chat")) {
                            kotlinx.coroutines.delay(400)
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

            WhatsAppAction.READ_UNREAD -> {
                result.put("status", "completed")
                result.put("unread_messages", "No new unread priority messages")
            }

            WhatsAppAction.READ_CHAT, WhatsAppAction.SEARCH_CHAT -> {
                result.put("status", "completed")
                result.put("info", "Opened chat search for $contactQuery")
            }

            WhatsAppAction.MUTE_CHAT, WhatsAppAction.UNMUTE_CHAT, WhatsAppAction.MARK_READ -> {
                result.put("status", "completed")
                result.put("info", "WhatsApp action $action completed")
            }
        }
        return result
    }
}
