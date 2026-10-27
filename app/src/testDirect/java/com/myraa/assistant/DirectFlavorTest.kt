package com.myraa.assistant

import com.myraa.assistant.agent.TaskState
import com.myraa.assistant.agent.TaskStateMachine
import com.myraa.assistant.license.AccessKeyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectFlavorTest {

    @Test
    fun testTaskStateMachineTransitions() {
        val sm = TaskStateMachine()
        assertEquals(TaskState.IDLE, sm.currentState)

        sm.moveTo(TaskState.OPENING_APP)
        assertEquals(TaskState.OPENING_APP, sm.currentState)

        sm.moveTo(TaskState.WAITING_FOR_UI)
        assertEquals(TaskState.WAITING_FOR_UI, sm.currentState)

        sm.moveTo(TaskState.OBSERVING)
        sm.moveTo(TaskState.RESOLVING_TARGET)
        sm.moveTo(TaskState.VALIDATING_TARGET)
        sm.moveTo(TaskState.WAITING_FOR_CONFIRMATION)
        sm.moveTo(TaskState.EXECUTING_ACTION)
        sm.moveTo(TaskState.VERIFYING)
        sm.moveTo(TaskState.COMPLETED)

        assertEquals(TaskState.COMPLETED, sm.currentState)
    }

    @Test(expected = IllegalStateException::class)
    fun testTerminalStateCannotTransition() {
        val sm = TaskStateMachine()
        sm.moveTo(TaskState.FAILED)
        sm.moveTo(TaskState.IDLE)
    }

    @Test
    fun testAccessKeyRules() {
        val emptyInfo = AccessKeyRules.evaluate("", "", 0L, false)
        assertTrue(emptyInfo.active) // community fallback

        val activeInfo = AccessKeyRules.evaluate("MYRAA-PRO-123", "Pro", System.currentTimeMillis() + 100000L, false)
        assertTrue(activeInfo.active)
        assertFalse(activeInfo.blocked)

        val expiredInfo = AccessKeyRules.evaluate("MYRAA-PRO-123", "Pro", System.currentTimeMillis() - 5000L, false)
        assertFalse(expiredInfo.active)

        val blockedInfo = AccessKeyRules.evaluate("MYRAA-PRO-123", "Pro", System.currentTimeMillis() + 100000L, true)
        assertFalse(blockedInfo.active)
        assertTrue(blockedInfo.blocked)
    }
}
