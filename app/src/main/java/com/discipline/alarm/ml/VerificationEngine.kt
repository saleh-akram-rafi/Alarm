package com.discipline.alarm.ml

import android.graphics.Bitmap
import android.util.Log
import com.discipline.alarm.data.local.converter.Converters
import com.discipline.alarm.domain.model.Mission
import com.discipline.alarm.domain.model.VerificationResult
import com.discipline.alarm.domain.repository.MissionRepository
import com.discipline.alarm.util.FileStorageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationEngine @Inject constructor(
    private val featureExtractor: FeatureExtractor,
    private val fileStorageManager: FileStorageManager,
    private val missionRepository: MissionRepository
) {
    companion object {
        private const val TAG = "VerificationEngine"
        private const val MIN_LUMINANCE_THRESHOLD = 30f
        private const val MAX_LUMINANCE_THRESHOLD = 240f
        private const val MIN_STRUCTURE_CONTRAST = 10f
    }

    suspend fun verify(
        liveBitmap: Bitmap,
        mission: Mission,
        effectiveThreshold: Float = mission.threshold
    ): VerificationResult {
        val startTime = System.currentTimeMillis()

        // 1. Check lighting condition
        val luminance = ImageProcessor.calculateAverageLuminance(liveBitmap)
        Log.d(TAG, "Live image luminance: $luminance")

        if (luminance < MIN_LUMINANCE_THRESHOLD) {
            return VerificationResult(
                isSuccess = false,
                similarityScore = 0f,
                threshold = effectiveThreshold,
                failureReason = "Image too dark",
                guidanceTip = "Turn on the room light or tap the Flash icon to illuminate the object.",
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )
        }

        if (luminance > MAX_LUMINANCE_THRESHOLD) {
            return VerificationResult(
                isSuccess = false,
                similarityScore = 0f,
                threshold = effectiveThreshold,
                failureReason = "Image overexposed",
                guidanceTip = "Avoid pointing directly into blinding lights or glare.",
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // 1b. Check structural contrast (reject covered lens, blank walls, bedsheets)
        val structureContrast = ImageProcessor.calculateStructureContrast(liveBitmap)
        Log.d(TAG, "Live image structure contrast: $structureContrast")

        if (structureContrast < MIN_STRUCTURE_CONTRAST) {
            return VerificationResult(
                isSuccess = false,
                similarityScore = 0.08f,
                threshold = effectiveThreshold,
                failureReason = "Blank surface or covered lens detected",
                guidanceTip = "Point the camera clearly at the registered physical object (${mission.name}), not at a wall or uniform surface.",
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // 2. Extract 640-D discriminative feature vector of live capture
        val liveVector = featureExtractor.extractFeatures(liveBitmap)

        // 3. Obtain reference vectors
        val referenceVectors = mutableListOf<FloatArray>()

        val cachedEmbeddings = listOf(mission.embedding1, mission.embedding2, mission.embedding3)
        var hasValidCachedVectors = true

        for (embBytes in cachedEmbeddings) {
            val floatArray = Converters.byteArrayToFloatArray(embBytes)
            if (floatArray != null && floatArray.size == liveVector.size) {
                referenceVectors.add(floatArray)
            } else {
                hasValidCachedVectors = false
                break
            }
        }

        // If cached vectors are missing or from an older descriptor dimension (e.g. 512 vs 640),
        // reload the reference images from disk, compute new 640-D vectors, and update Room cache.
        if (!hasValidCachedVectors || referenceVectors.isEmpty()) {
            Log.d(TAG, "Recomputing reference embeddings from saved mission images...")
            referenceVectors.clear()
            for (path in mission.imagePaths) {
                val refBitmap = fileStorageManager.loadBitmap(path)
                if (refBitmap != null) {
                    val vector = featureExtractor.extractFeatures(refBitmap)
                    referenceVectors.add(vector)
                }
            }

            if (referenceVectors.size == 3) {
                try {
                    val updatedMission = mission.copy(
                        embedding1 = Converters.floatArrayToByteArray(referenceVectors[0]),
                        embedding2 = Converters.floatArrayToByteArray(referenceVectors[1]),
                        embedding3 = Converters.floatArrayToByteArray(referenceVectors[2])
                    )
                    missionRepository.updateMission(updatedMission)
                    Log.d(TAG, "Upgraded cached mission embeddings in Room database.")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update cached mission embeddings", e)
                }
            }
        }

        if (referenceVectors.isEmpty()) {
            return VerificationResult(
                isSuccess = false,
                similarityScore = 0f,
                threshold = effectiveThreshold,
                failureReason = "No reference images found",
                guidanceTip = "Reference mission images could not be loaded. Please re-register the mission.",
                inferenceTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // 4. Compare live vector against all reference vectors
        val scores = referenceVectors.mapIndexed { index, refVec ->
            Pair(SimilarityEngine.computeNormalizedSimilarity(liveVector, refVec), index)
        }.sortedByDescending { it.first }

        val bestScore = scores.first().first
        val bestIndex = scores.first().second
        val secondBestScore = if (scores.size > 1) scores[1].first else bestScore

        // Multi-angle consensus score: 80% best angle + 20% second-best angle consistency
        val finalScore = (0.80f * bestScore + 0.20f * secondBestScore).coerceIn(0f, 1f)
        val isMatch = finalScore >= effectiveThreshold
        val inferenceDuration = System.currentTimeMillis() - startTime

        Log.d(
            TAG,
            "Verification complete: best=$bestScore, second=$secondBestScore, final=$finalScore, threshold=$effectiveThreshold, pass=$isMatch, time=${inferenceDuration}ms"
        )

        val guidance = if (!isMatch) {
            when {
                finalScore > (effectiveThreshold - 0.12f) ->
                    "Close match (${(finalScore * 100).toInt()}%)! Align angle closer to how you registered ${mission.name} and hold steady."
                else ->
                    "Wrong object or angle (${(finalScore * 100).toInt()}% match). You must scan '${mission.name}' to dismiss the alarm!"
            }
        } else null

        return VerificationResult(
            isSuccess = isMatch,
            similarityScore = finalScore,
            threshold = effectiveThreshold,
            matchedReferenceIndex = bestIndex,
            failureReason = if (!isMatch) "Object does not match '${mission.name}'" else null,
            guidanceTip = guidance,
            inferenceTimeMs = inferenceDuration
        )
    }
}
