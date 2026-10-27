package com.myraa.assistant.safety

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

data class PendingConfirmation(
    val actionType: String,
    val description: String,
    val confirmationPrompt: String,
    val onConfirm: () -> String,
    val onReject: () -> String
)

/**
 * SafetyManager handles:
 * 1. Voice emergency kill-switch ("Myraa stop", "Myraa cancel", "shant ho", "ruk ja", "thamb", "thamba")
 * 2. Sensitive action verification guards (calls, money, deletions, texts)
 * 3. Audit trail of assistant actions for transparency.
 */
object SafetyManager {

    private val STOP_PHRASES = setOf(
        "stop", "myraa stop", "cancel", "myraa cancel", "shant ho", "ruk ja",
        "thamb", "thamba", "myraa thamb", "myraa thamba", "band kar", "exit", "shut up", "abort"
    )

    private val AFFIRMATIVE_PHRASES = setOf(
        "ho", "ha", "haan", "yes", "kar", "kara", "chalu kar", "proceed", "sure", "thik ahe", "theek hai"
    )

    private val NEGATIVE_PHRASES = setOf(
        "nahi", "nako", "no", "cancel", "rahude", "mat karo", "abort", "don't"
    )

    private val _actionAuditLog = CopyOnWriteArrayList<ActionAuditEntry>()
    private val _auditLogFlow = MutableStateFlow<List<ActionAuditEntry>>(emptyList())
    val auditLogFlow: StateFlow<List<ActionAuditEntry>> = _auditLogFlow.asStateFlow()

    @Volatile
    var activeConfirmation: PendingConfirmation? = null
        private set

    fun isEmergencyStop(phrase: String): Boolean {
        val clean = phrase.trim().lowercase()
        return STOP_PHRASES.any { clean == it || clean.startsWith("$it ") || clean.endsWith(" $it") }
    }

    fun isAffirmative(phrase: String): Boolean {
        val clean = phrase.trim().lowercase()
        return AFFIRMATIVE_PHRASES.any { clean == it || clean.startsWith("$it ") || clean.endsWith(" $it") }
    }

    fun isNegative(phrase: String): Boolean {
        val clean = phrase.trim().lowercase()
        return NEGATIVE_PHRASES.any { clean == it || clean.startsWith("$it ") || clean.endsWith(" $it") }
    }

    fun isSensitiveAction(actionName: String): Boolean {
        val lower = actionName.lowercase()
        return when {
            lower.contains("send_money") || lower.contains("payment") || lower.contains("paise") -> true
            lower.contains("delete") || lower.contains("remove_all") || lower.contains("erase") -> true
            lower.contains("call") || lower.contains("phone") -> true
            lower.contains("sms") || lower.contains("message") -> true
            lower == "modify_system_settings" -> true
            else -> false
        }
    }

    fun setPendingConfirmation(confirmation: PendingConfirmation) {
        activeConfirmation = confirmation
    }

    fun clearPendingConfirmation() {
        activeConfirmation = null
    }

    fun logAction(toolName: String, description: String, success: Boolean) {
        val entry = ActionAuditEntry(
            id = System.currentTimeMillis(),
            toolName = toolName,
            description = description,
            success = success,
            timestamp = System.currentTimeMillis()
        )
        _actionAuditLog.add(0, entry)
        while (_actionAuditLog.size > 50) {
            _actionAuditLog.removeAt(_actionAuditLog.lastIndex)
        }
        _auditLogFlow.value = _actionAuditLog.toList()
    }
}

data class ActionAuditEntry(
    val id: Long,
    val toolName: String,
    val description: String,
    val success: Boolean,
    val timestamp: Long
)
