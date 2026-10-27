package com.myraa.assistant.voice

class ToolCallDeduplicator(private val capacity: Int = 100) {
    private val seenIds = LinkedHashSet<String>()

    @Synchronized
    fun isDuplicate(callId: String): Boolean {
        if (callId.isBlank()) return false
        if (seenIds.contains(callId)) {
            return true
        }
        if (seenIds.size >= capacity) {
            val first = seenIds.iterator().next()
            seenIds.remove(first)
        }
        seenIds.add(callId)
        return false
    }

    @Synchronized
    fun clear() {
        seenIds.clear()
    }

    @Synchronized
    fun size(): Int = seenIds.size
}
