package com.discipline.alarm.ml

import android.graphics.Bitmap

interface FeatureExtractor {
    /**
     * Extracts a high-dimensional feature embedding vector from the input image.
     */
    suspend fun extractFeatures(bitmap: Bitmap): FloatArray

    /**
     * Expected dimension of the feature vector (e.g. 1280 for MobileNetV2 or 512 for descriptor).
     */
    val featureDimension: Int
}
