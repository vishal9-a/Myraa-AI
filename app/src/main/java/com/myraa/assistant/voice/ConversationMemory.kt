package com.myraa.assistant.voice

enum class MemoryRole {
    USER,
    ASSISTANT
}

data class MemoryTurn(
    val role: MemoryRole,
    val text: String
)

class ConversationMemory(private val maxTurns: Int = 12) {
    private val turns = ArrayDeque<MemoryTurn>()

    @Synchronized
    fun addTurn(role: MemoryRole, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (turns.size >= maxTurns) {
            turns.removeFirst()
        }
        turns.addLast(MemoryTurn(role, clean))
    }

    @Synchronized
    fun recap(): String? {
        if (turns.isEmpty()) return null
        val sb = StringBuilder()
        for (turn in turns) {
            val prefix = if (turn.role == MemoryRole.USER) "User: " else "You: "
            sb.append(prefix).append(turn.text).append("\n")
        }
        val full = sb.toString().trim()
        if (full.isEmpty()) return null
        return if (full.length > 1600) {
            full.takeLast(1600).substringAfter("\n", full.takeLast(1600))
        } else {
            full
        }
    }

    @Synchronized
    fun clear() {
        turns.clear()
    }

    @Synchronized
    fun size(): Int = turns.size
}
