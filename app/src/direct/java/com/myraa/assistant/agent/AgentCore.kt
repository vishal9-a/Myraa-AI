package com.myraa.assistant.agent

import android.graphics.Rect

data class UiElement(
    val id: String,
    val text: String,
    val contentDescription: String,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean
)

data class ScreenObservation(
    val observationId: String,
    val packageName: String,
    val windowClass: String,
    val elements: List<UiElement>,
    val screenWidth: Int,
    val screenHeight: Int,
    val windowStamp: Long = System.currentTimeMillis()
)

enum class ScreenDriverResult {
    OK,
    FAILED,
    STALE,
    NOT_FOUND,
    UNSUPPORTED
}

interface ScreenDriver {
    fun capture(): ScreenObservation?
    fun clickElement(obs: ScreenObservation, element: UiElement): ScreenDriverResult
    fun gestureTap(x: Float, y: Float): ScreenDriverResult
    fun setText(text: String): ScreenDriverResult
    fun scroll(direction: String): ScreenDriverResult
    fun back(): ScreenDriverResult
    fun home(): ScreenDriverResult
}

enum class ExecPhase {
    PREPARE,
    TARGETING,
    EXECUTION,
    VERIFICATION
}

enum class ActionMethod {
    ACCESSIBILITY_CLICK,
    GESTURE_ON_ELEMENT_BOUNDS,
    VISION_TAP,
    SET_TEXT
}

enum class Blocker {
    LOGIN,
    CAPTCHA,
    TWO_FACTOR,
    SECURITY_CHECK,
    PERMISSION_PROMPT
}

enum class FailureReason {
    ELEMENT_NOT_FOUND,
    ACTION_TIMEOUT,
    APP_CRASH,
    USER_CANCELLED,
    UNSUPPORTED_OPERATION
}

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failed(val reason: FailureReason) : ActionResult()
    data class Blocked(val blocker: Blocker) : ActionResult()
}

class AccessibilityScreenDriver : ScreenDriver {
    override fun capture(): ScreenObservation? {
        val service = MyraaAccessibilityService.instance ?: return null
        val elements = mutableListOf<UiElement>()
        val root = service.rootInActiveWindow ?: return null

        val queue = ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0

        while (queue.isNotEmpty() && count < 60) {
            val node = queue.removeFirst()
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            elements.add(
                UiElement(
                    id = node.viewIdResourceName ?: "elem_$count",
                    text = node.text?.toString() ?: "",
                    contentDescription = node.contentDescription?.toString() ?: "",
                    className = node.className?.toString() ?: "",
                    bounds = bounds,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable
                )
            )
            count++
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return ScreenObservation(
            observationId = "obs_${System.currentTimeMillis()}",
            packageName = root.packageName?.toString() ?: "",
            windowClass = root.className?.toString() ?: "",
            elements = elements,
            screenWidth = 1080,
            screenHeight = 2400
        )
    }

    override fun clickElement(obs: ScreenObservation, element: UiElement): ScreenDriverResult {
        val service = MyraaAccessibilityService.instance ?: return ScreenDriverResult.FAILED
        val success = service.tapText(element.text.ifEmpty { element.contentDescription })
        return if (success) ScreenDriverResult.OK else ScreenDriverResult.FAILED
    }

    override fun gestureTap(x: Float, y: Float): ScreenDriverResult {
        val service = MyraaAccessibilityService.instance ?: return ScreenDriverResult.FAILED
        return if (service.gestureTap(x, y)) ScreenDriverResult.OK else ScreenDriverResult.FAILED
    }

    override fun setText(text: String): ScreenDriverResult {
        val service = MyraaAccessibilityService.instance ?: return ScreenDriverResult.FAILED
        return if (service.typeText(text)) ScreenDriverResult.OK else ScreenDriverResult.FAILED
    }

    override fun scroll(direction: String): ScreenDriverResult {
        val service = MyraaAccessibilityService.instance ?: return ScreenDriverResult.FAILED
        return if (service.scrollScreen(direction)) ScreenDriverResult.OK else ScreenDriverResult.FAILED
    }

    override fun back(): ScreenDriverResult {
        val service = MyraaAccessibilityService.instance ?: return ScreenDriverResult.FAILED
        return if (service.goBack()) ScreenDriverResult.OK else ScreenDriverResult.FAILED
    }

    override fun home(): ScreenDriverResult {
        val service = MyraaAccessibilityService.instance ?: return ScreenDriverResult.FAILED
        return if (service.goHome()) ScreenDriverResult.OK else ScreenDriverResult.FAILED
    }
}
