package com.myraa.assistant.agent

import org.json.JSONObject

enum class Platform {
    INSTAGRAM,
    FACEBOOK
}

enum class SocialAction {
    CREATE_POST,
    CREATE_REEL,
    CREATE_STORY,
    TEXT_POST
}

enum class TaskMode {
    PUBLISH,
    DRAFT
}

enum class TaskStatus {
    PENDING,
    RUNNING,
    WAITING_FOR_CONFIRMATION,
    WAITING_FOR_USER,
    COMPLETED,
    SUBMITTED_UNVERIFIED,
    FAILED,
    CANCELLED
}

enum class ControlCommand {
    CONFIRM,
    REJECT,
    RESUME,
    CANCEL,
    STATUS
}

enum class TaskState {
    IDLE,
    OPENING_APP,
    OBSERVING,
    RESOLVING_TARGET,
    VALIDATING_TARGET,
    EXECUTING_ACTION,
    WAITING_FOR_UI,
    VERIFYING,
    NEXT_STEP,
    RETRY,
    REOBSERVE,
    WAITING_FOR_CONFIRMATION,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED,
    CANCELLED
}

class TaskStateMachine {
    var currentState: TaskState = TaskState.IDLE
        private set

    val history = mutableListOf<TaskState>()

    fun moveTo(next: TaskState) {
        if (isTerminal(currentState)) {
            throw IllegalStateException("Cannot transition from terminal state $currentState to $next")
        }

        // FAILED and CANCELLED reachable from any non-terminal state
        if (next == TaskState.FAILED || next == TaskState.CANCELLED) {
            history.add(currentState)
            currentState = next
            return
        }

        // Validate state transitions
        val valid = when (currentState) {
            TaskState.IDLE -> next == TaskState.OPENING_APP || next == TaskState.OBSERVING
            TaskState.OPENING_APP -> next == TaskState.OBSERVING || next == TaskState.WAITING_FOR_UI
            TaskState.WAITING_FOR_UI -> next == TaskState.OBSERVING || next == TaskState.REOBSERVE
            TaskState.OBSERVING -> next == TaskState.RESOLVING_TARGET || next == TaskState.WAITING_FOR_USER
            TaskState.RESOLVING_TARGET -> next == TaskState.VALIDATING_TARGET || next == TaskState.RETRY || next == TaskState.REOBSERVE
            TaskState.VALIDATING_TARGET -> next == TaskState.EXECUTING_ACTION || next == TaskState.WAITING_FOR_CONFIRMATION
            TaskState.WAITING_FOR_CONFIRMATION -> next == TaskState.EXECUTING_ACTION || next == TaskState.CANCELLED
            TaskState.WAITING_FOR_USER -> next == TaskState.OBSERVING || next == TaskState.RETRY
            TaskState.EXECUTING_ACTION -> next == TaskState.VERIFYING || next == TaskState.WAITING_FOR_UI
            TaskState.VERIFYING -> next == TaskState.NEXT_STEP || next == TaskState.COMPLETED || next == TaskState.RETRY
            TaskState.NEXT_STEP -> next == TaskState.OBSERVING || next == TaskState.COMPLETED
            TaskState.RETRY -> next == TaskState.OBSERVING || next == TaskState.EXECUTING_ACTION
            TaskState.REOBSERVE -> next == TaskState.OBSERVING
            TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELLED -> false
        }

        if (!valid) {
            throw IllegalStateException("Illegal transition from $currentState to $next")
        }

        history.add(currentState)
        currentState = next
    }

    private fun isTerminal(state: TaskState): Boolean {
        return state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELLED
    }
}

data class SocialTaskRequest(
    val taskId: String,
    val platform: Platform,
    val action: SocialAction,
    val mediaUri: String?,
    val caption: String,
    val mode: TaskMode,
    val targetAccount: String?
) {
    companion object {
        fun parse(json: JSONObject): SocialTaskRequest {
            val platformStr = json.optString("platform", "instagram").uppercase()
            val actionStr = json.optString("action", "create_post").uppercase()
            val modeStr = json.optString("mode", "draft").uppercase()

            val platform = try { Platform.valueOf(platformStr) } catch (_: Exception) { Platform.INSTAGRAM }
            val action = try { SocialAction.valueOf(actionStr) } catch (_: Exception) { SocialAction.CREATE_POST }
            val mode = if (modeStr == "PUBLISH") TaskMode.PUBLISH else TaskMode.DRAFT

            return SocialTaskRequest(
                taskId = "task_${System.currentTimeMillis()}",
                platform = platform,
                action = action,
                mediaUri = json.optString("media_uri", "").ifEmpty { null },
                caption = json.optString("caption", ""),
                mode = mode,
                targetAccount = json.optString("target_account", "").ifEmpty { null }
            )
        }
    }
}
