package com.discipline.alarm.domain.model

data class VerificationResult(
    val isSuccess: Boolean,
    val similarityScore: Float,
    val threshold: Float,
    val matchedReferenceIndex: Int = -1,
    val failureReason: String? = null,
    val guidanceTip: String? = null,
    val inferenceTimeMs: Long = 0L
) {
    val scorePercentage: Int
        get() = (similarityScore.coerceIn(0f, 1f) * 100).toInt()

    val thresholdPercentage: Int
        get() = (threshold.coerceIn(0f, 1f) * 100).toInt()
}
