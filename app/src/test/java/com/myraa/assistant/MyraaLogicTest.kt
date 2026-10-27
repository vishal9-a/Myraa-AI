package com.myraa.assistant

import com.myraa.assistant.data.LanguagePreference
import com.myraa.assistant.forge.ForgeHtml
import com.myraa.assistant.forge.ForgeIntent
import com.myraa.assistant.forge.ForgeStage
import com.myraa.assistant.forge.ForgeStageDetector
import com.myraa.assistant.forge.SseParser
import com.myraa.assistant.voice.ConversationMemory
import com.myraa.assistant.voice.DetectedLanguage
import com.myraa.assistant.voice.EchoGuard
import com.myraa.assistant.voice.LanguageDetector
import com.myraa.assistant.voice.MemoryRole
import com.myraa.assistant.voice.Personality
import com.myraa.assistant.voice.PersonalityPromptBuilder
import com.myraa.assistant.voice.ToolCallDeduplicator
import com.myraa.assistant.voice.VoiceLatencyTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyraaLogicTest {

    @Test
    fun testLanguageDetector() {
        assertEquals(DetectedLanguage.MARATHI, LanguageDetector.detect("कशी आहेस? काय चाललंय?"))
        assertEquals(DetectedLanguage.MARATHI, LanguageDetector.detect("tula kay pahije sang"))
        assertEquals(DetectedLanguage.HINDI, LanguageDetector.detect("नमस्ते, आप कैसे हैं?"))
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("kya haal hai yaar"))
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("bhai sun mujhe ek baat batao"))
        assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detect("what is the weather today in Delhi"))
        assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect(""))
        assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect("   "))
    }

    @Test
    fun testToolCallDeduplicator() {
        val dedup = ToolCallDeduplicator(capacity = 5)
        assertFalse(dedup.isDuplicate("call_1"))
        assertTrue(dedup.isDuplicate("call_1"))
        assertFalse(dedup.isDuplicate("call_2"))
        assertEquals(2, dedup.size())

        dedup.clear()
        assertEquals(0, dedup.size())
        assertFalse(dedup.isDuplicate("call_1"))
    }

    @Test
    fun testConversationMemory() {
        val memory = ConversationMemory(maxTurns = 3)
        assertNull(memory.recap())

        memory.addTurn(MemoryRole.USER, "Hello Myraa")
        memory.addTurn(MemoryRole.ASSISTANT, "Namaste!")
        memory.addTurn(MemoryRole.USER, "How are you?")
        memory.addTurn(MemoryRole.ASSISTANT, "I am great!")

        // Exceeds max turns 3, oldest turn removed
        assertEquals(3, memory.size())
        val recap = memory.recap()
        assertNotNull(recap)
        assertTrue(recap!!.contains("Namaste!"))
        assertTrue(recap.contains("I am great!"))

        memory.clear()
        assertEquals(0, memory.size())
    }

    @Test
    fun testVoiceLatencyTracker() {
        val tracker = VoiceLatencyTracker()
        tracker.markUserSpeechEnd(1000L)
        val delta = tracker.markFirstAudioChunk(1450L)
        assertEquals(450L, delta)
        assertEquals(450L, tracker.lastTurnLatencyMs.value)

        tracker.incrementReconnectCount()
        assertEquals(1, tracker.reconnectCount.value)

        tracker.reset()
        assertEquals(0, tracker.reconnectCount.value)
        assertEquals(0L, tracker.lastTurnLatencyMs.value)
    }

    @Test
    fun testPersonalityPromptBuilder() {
        val prompt = PersonalityPromptBuilder.build(
            personality = Personality.DEVELOPER,
            language = LanguagePreference.HINGLISH,
            detectedLanguage = DetectedLanguage.HINGLISH,
            recap = "User: Hi\nYou: Hello",
            assistantName = "Myraa"
        )
        assertTrue(prompt.contains("Developer"))
        assertTrue(prompt.contains("Hinglish"))
        assertTrue(prompt.contains("Recap:"))
        assertTrue(prompt.contains("Myraa"))
    }

    @Test
    fun testEchoGuard() {
        EchoGuard.reset()
        assertFalse(EchoGuard.isMicMuted())

        EchoGuard.notifyPlaybackStarted()
        assertTrue(EchoGuard.isMicMuted())
        assertEquals(1, EchoGuard.getActivePlaybackCount())

        EchoGuard.reset()
        assertFalse(EchoGuard.isMicMuted())
    }

    @Test
    fun testForgeIntent() {
        assertEquals("Myraa, ek cafe ki website banao", ForgeIntent.extract("Myraa, ek cafe ki website banao"))
        assertEquals("build a modern portfolio landing page", ForgeIntent.extract("build a modern portfolio landing page"))
        assertNull(ForgeIntent.extract("what is a website?"))
        assertNull(ForgeIntent.extract("hello how are you"))
    }

    @Test
    fun testForgeHtmlProcessing() {
        val raw = "```html\n<!DOCTYPE html><html><body><h1>Hello</h1></body></html>\n```"
        val stripped = ForgeHtml.stripFences(raw)
        assertTrue(stripped.startsWith("<!DOCTYPE html>"))
        assertTrue(stripped.endsWith("</html>"))
        assertTrue(ForgeHtml.looksLikeHtml(stripped))

        val truncated = "<html><head><title>Test</title></head><body><h1>Content</h1>"
        val repaired = ForgeHtml.repairTruncated(truncated)
        assertTrue(repaired.contains("</body>"))
        assertTrue(repaired.contains("</html>"))

        val withSafety = ForgeHtml.withSafetyNet(repaired)
        assertTrue(withSafety.contains("myraa-safety-net"))
    }

    @Test
    fun testForgeStageDetector() {
        assertEquals(ForgeStage.PLANNING, ForgeStageDetector.detect("<!DOCTYPE html>"))
        assertEquals(ForgeStage.STRUCTURE, ForgeStageDetector.detect("<!DOCTYPE html><html><head></head><body>"))
        assertEquals(ForgeStage.STYLING, ForgeStageDetector.detect("<body class=\"bg-slate-900 text-white\">"))
        assertEquals(ForgeStage.SECTIONS, ForgeStageDetector.detect("<section id=\"hero\"><h1>Welcome</h1></section>"))
        assertEquals(ForgeStage.MOTION, ForgeStageDetector.detect("<script src=\"three.min.js\"></script>"))
        assertEquals(ForgeStage.COMPLETE, ForgeStageDetector.detect("<html><body>Done</body></html>"))
    }

    @Test
    fun testSseParser() {
        val chunkLine = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello world\"}]}}]}"
        assertEquals("Hello world", SseParser.extractText(chunkLine))

        val finishLine = "data: {\"candidates\":[{\"finishReason\":\"STOP\"}]}"
        assertEquals("STOP", SseParser.extractFinishReason(finishLine))

        assertNull(SseParser.extractText("data: [DONE]"))
    }

    @Test
    fun testVectorSearchSimilarity() {
        val facts = listOf(
            com.myraa.assistant.memory.FactMemory(
                id = 1,
                key = "favorite_coffee",
                value = "Latte with oat milk and brown sugar",
                category = com.myraa.assistant.memory.MemoryCategory.PREFERENCE,
                importance = 9
            ),
            com.myraa.assistant.memory.FactMemory(
                id = 2,
                key = "brother_name",
                value = "Rohan",
                category = com.myraa.assistant.memory.MemoryCategory.RELATIONSHIP,
                importance = 8
            ),
            com.myraa.assistant.memory.FactMemory(
                id = 3,
                key = "work_role",
                value = "Android Software Engineer at Pune",
                category = com.myraa.assistant.memory.MemoryCategory.WORK,
                importance = 7
            )
        )

        val coffeeQuery = "What coffee do I like?"
        val topForCoffee = com.myraa.assistant.memory.VectorSearch.findTopRelevantFacts(coffeeQuery, facts, topK = 1)
        assertEquals(1, topForCoffee.size)
        assertEquals("favorite_coffee", topForCoffee[0].key)

        val workQuery = "software engineer pune"
        val topForWork = com.myraa.assistant.memory.VectorSearch.findTopRelevantFacts(workQuery, facts, topK = 1)
        assertEquals(1, topForWork.size)
        assertEquals("work_role", topForWork[0].key)
    }

    @Test
    fun testSafetyEmergencyStop() {
        assertTrue(com.myraa.assistant.safety.SafetyManager.isEmergencyStop("stop"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isEmergencyStop("myraa stop"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isEmergencyStop("shant ho"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isEmergencyStop("ruk ja"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isEmergencyStop("myraa thamb"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isAffirmative("ho"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isAffirmative("yes"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isNegative("nako"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isNegative("nahi"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isSensitiveAction("send_money"))
        assertTrue(com.myraa.assistant.safety.SafetyManager.isSensitiveAction("call_contact"))
        assertFalse(com.myraa.assistant.safety.SafetyManager.isEmergencyStop("tell me a story"))
        assertFalse(com.myraa.assistant.safety.SafetyManager.isEmergencyStop("stopwatch lagao"))
    }

    @Test
    fun testScreenHierarchySummaryMasking() {
        val hierarchy = com.myraa.assistant.vision.ScreenHierarchy(
            packageName = "com.banking.app",
            elements = listOf(
                com.myraa.assistant.vision.ScreenNode(
                    text = "Welcome Back",
                    className = "android.widget.TextView",
                    bounds = android.graphics.Rect(0, 0, 100, 50),
                    isClickable = false,
                    isEditable = false
                ),
                com.myraa.assistant.vision.ScreenNode(
                    text = "[PROTECTED_PASSWORD_FIELD]",
                    className = "android.widget.EditText",
                    bounds = android.graphics.Rect(0, 60, 100, 110),
                    isClickable = true,
                    isEditable = true
                )
            ),
            hasSecureField = true
        )
        val summary = hierarchy.toSummary()
        assertTrue(summary.contains("Active App: com.banking.app"))
        assertTrue(summary.contains("contains secure or password fields"))
        assertTrue(summary.contains("[PROTECTED_PASSWORD_FIELD]"))
    }

    @Test
    fun testVoiceProfile() {
        val defaultProfile = com.myraa.assistant.offline.VoiceProfile.fromId(null)
        assertEquals(com.myraa.assistant.offline.VoiceProfile.CUTE_GIRL, defaultProfile)
        assertEquals(1.08f, defaultProfile.speed, 0.001f)
        assertEquals(1.18f, defaultProfile.pitch, 0.001f)
        assertEquals(70L, defaultProfile.pauseDurationMs)

        val softGirl = com.myraa.assistant.offline.VoiceProfile.fromId("soft_girl")
        assertEquals(com.myraa.assistant.offline.VoiceProfile.SOFT_GIRL, softGirl)
        assertEquals(0.96f, softGirl.speed, 0.001f)
        assertEquals(1.06f, softGirl.pitch, 0.001f)

        val male = com.myraa.assistant.offline.VoiceProfile.fromId("male")
        assertEquals(com.myraa.assistant.offline.VoiceProfile.MALE, male)
        assertEquals(0.82f, male.pitch, 0.001f)

        val fallback = com.myraa.assistant.offline.VoiceProfile.fromId("unknown_voice")
        assertEquals(com.myraa.assistant.offline.VoiceProfile.CUTE_GIRL, fallback)
    }

    @Test
    fun testVoskResultParser() {
        val partialJson = """{"partial" : "myraa kashi"}"""
        assertEquals("myraa kashi", com.myraa.assistant.offline.VoskResultParser.parsePartial(partialJson))

        val finalJson = """{"text" : "myraa torch chalu kara"}"""
        assertEquals("myraa torch chalu kara", com.myraa.assistant.offline.VoskResultParser.parseFinal(finalJson))

        val plainText = "plain unformatted text"
        assertEquals("plain unformatted text", com.myraa.assistant.offline.VoskResultParser.parseFinal(plainText))

        val formatted = com.myraa.assistant.offline.VoskResultParser.formatVoskJson("namaste", isFinal = true)
        assertTrue(formatted.contains("\"text\": \"namaste\""))
    }
}


