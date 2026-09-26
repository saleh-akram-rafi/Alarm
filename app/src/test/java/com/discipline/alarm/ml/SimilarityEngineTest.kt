package com.discipline.alarm.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityEngineTest {

    @Test
    fun testIdenticalVectors_ProduceMaximumSimilarity() {
        val vectorA = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
        val vectorB = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)

        val cosine = SimilarityEngine.computeCosineSimilarity(vectorA, vectorB)
        val normalized = SimilarityEngine.computeNormalizedSimilarity(vectorA, vectorB)

        assertEquals(1.0f, cosine, 0.001f)
        assertEquals(1.0f, normalized, 0.001f)
    }

    @Test
    fun testOrthogonalVectors_ProduceZeroSimilarity() {
        val vectorA = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vectorB = floatArrayOf(0.0f, 1.0f, 0.0f)

        val cosine = SimilarityEngine.computeCosineSimilarity(vectorA, vectorB)
        val normalized = SimilarityEngine.computeNormalizedSimilarity(vectorA, vectorB)

        assertEquals(0.0f, cosine, 0.001f)
        assertEquals(0.0f, normalized, 0.001f)
    }

    @Test
    fun testOppositeVectors_ProduceZeroNormalizedSimilarity() {
        val vectorA = floatArrayOf(1.0f, 2.0f, 3.0f)
        val vectorB = floatArrayOf(-1.0f, -2.0f, -3.0f)

        val cosine = SimilarityEngine.computeCosineSimilarity(vectorA, vectorB)
        val normalized = SimilarityEngine.computeNormalizedSimilarity(vectorA, vectorB)

        assertEquals(-1.0f, cosine, 0.001f)
        assertEquals(0.0f, normalized, 0.001f)
    }

    @Test
    fun testZeroVector_SafeHandling() {
        val vectorA = floatArrayOf(0.0f, 0.0f, 0.0f)
        val vectorB = floatArrayOf(1.0f, 2.0f, 3.0f)

        val cosine = SimilarityEngine.computeCosineSimilarity(vectorA, vectorB)
        assertEquals(0.0f, cosine, 0.0001f)
    }

    @Test
    fun testFindBestMatch() {
        val target = floatArrayOf(1.0f, 0.0f, 0.0f)
        val ref1 = floatArrayOf(0.0f, 1.0f, 0.0f) // Orthogonal (norm = 0.0)
        val ref2 = floatArrayOf(0.9f, 0.1f, 0.0f) // Very close (norm > 0.99)
        val ref3 = floatArrayOf(0.0f, 0.0f, 1.0f) // Orthogonal (norm = 0.0)

        val (bestScore, bestIndex) = SimilarityEngine.findBestMatch(target, listOf(ref1, ref2, ref3))

        assertEquals(1, bestIndex)
        assertTrue(bestScore > 0.9f)
    }
}
