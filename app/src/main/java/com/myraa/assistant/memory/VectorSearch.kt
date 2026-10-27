package com.myraa.assistant.memory

import kotlin.math.sqrt

/**
 * Pure Kotlin on-device semantic vector similarity engine.
 * Uses subword n-grams and word tokens to build TF vector representations,
 * computing cosine similarity with zero external binary dependencies.
 * Fast, lightweight, multilingual (English, Hindi, Marathi, Hinglish).
 */
object VectorSearch {

    /**
     * Finds top-K most relevant facts for a user utterance or context query.
     */
    fun findTopRelevantFacts(
        query: String,
        facts: List<FactMemory>,
        topK: Int = 4,
        minScoreThreshold: Double = 0.08
    ): List<FactMemory> {
        if (query.isBlank() || facts.isEmpty()) return emptyList()

        val queryVec = extractVector(query)
        if (queryVec.isEmpty()) return emptyList()

        val scored = facts.map { fact ->
            val factText = "${fact.key} ${fact.value} ${fact.category.name}"
            val factVec = extractVector(factText)
            val cosine = cosineSimilarity(queryVec, factVec)
            // Weight slightly by importance score (1..10 -> 0.8..1.2x)
            val importanceWeight = 0.8 + (fact.importance * 0.04)
            val finalScore = cosine * importanceWeight
            fact to finalScore
        }

        return scored
            .filter { it.second >= minScoreThreshold }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    /**
     * Computes term frequency vector with word tokens and character 3-grams.
     */
    fun extractVector(text: String): Map<String, Double> {
        val normalized = text.lowercase().trim()
        val tokens = normalized.split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.length > 1 }

        val termCounts = mutableMapOf<String, Double>()

        // 1. Whole word tokens
        for (token in tokens) {
            termCounts[token] = (termCounts[token] ?: 0.0) + 1.5
        }

        // 2. Character 3-grams for fuzzy subword matching across Hindi/Marathi transliterations
        for (token in tokens) {
            if (token.length >= 3) {
                for (i in 0..token.length - 3) {
                    val gram = token.substring(i, i + 3)
                    termCounts[gram] = (termCounts[gram] ?: 0.0) + 0.5
                }
            }
        }

        // Normalize vector to unit length
        val sumSquares = termCounts.values.sumOf { it * it }
        val norm = sqrt(sumSquares)
        if (norm == 0.0) return emptyMap()

        return termCounts.mapValues { it.value / norm }
    }

    fun cosineSimilarity(vec1: Map<String, Double>, vec2: Map<String, Double>): Double {
        if (vec1.isEmpty() || vec2.isEmpty()) return 0.0

        var dotProduct = 0.0
        // Iterate over smaller map for speed
        val (small, large) = if (vec1.size <= vec2.size) vec1 to vec2 else vec2 to vec1
        for ((term, weight1) in small) {
            val weight2 = large[term] ?: continue
            dotProduct += weight1 * weight2
        }
        return dotProduct.coerceIn(0.0, 1.0)
    }
}
