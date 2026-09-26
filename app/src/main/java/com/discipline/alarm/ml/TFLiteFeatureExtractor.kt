package com.discipline.alarm.ml

import android.content.Context
import android.graphics.Bitmap
import com.discipline.alarm.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.sqrt

@Singleton
class TFLiteFeatureExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FeatureExtractor {

    companion object {
        /**
         * 640-Dimensional Multi-Modal Discriminative Descriptor (100% 16KB-Page Compatible):
         * - [0..255]   (256 dims): 8x8 Grid x 4-Bin Oriented Gradients (HOG Shape & Structure)
         * - [256..447] (192 dims): 8x8 Grid x 3 Zero-Mean Spatial Luminance & Color Opponent (Z-Lum, R-G, Y-B)
         * - [448..575] (128 dims): 8x8 Grid x 2 Local Texture Variance & Edge Density
         * - [576..639] (64 dims) : 64-Bin Chromaticity & Intensity Distribution Histogram
         */
        const val EMBEDDING_DIM_FALLBACK = 640
    }

    override val featureDimension: Int
        get() = EMBEDDING_DIM_FALLBACK

    override suspend fun extractFeatures(bitmap: Bitmap): FloatArray = withContext(ioDispatcher) {
        val processedBitmap = ImageProcessor.cropCenterAndResize(bitmap)
        extractDiscriminativeDescriptor(processedBitmap)
    }

    /**
     * Extracts a 640-dimensional zero-mean normalized descriptor combining:
     * 1. HOG (Histogram of Oriented Gradients) for strict object shape/geometry verification.
     * 2. Spatial Z-Scored Luminance & Opponent Color (R-G, Y-B) for layout and material matching.
     * 3. Local Texture Variance & Edge Density for surface structure.
     * 4. 64-Bin Chromaticity Histogram for global color signature.
     */
    fun extractDiscriminativeDescriptor(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val gridCount = 8
        val cellW = width / gridCount
        val cellH = height / gridCount
        val descriptor = FloatArray(EMBEDDING_DIM_FALLBACK)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Precompute luminance map [0..255]
        val lumMap = FloatArray(width * height)
        var globalLumSum = 0.0
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val l = (0.299f * r + 0.587f * g + 0.114f * b)
            lumMap[i] = l
            globalLumSum += l
        }
        val globalLumMean = (globalLumSum / pixels.size).toFloat()
        var globalLumVarSum = 0.0
        for (l in lumMap) {
            val d = l - globalLumMean
            globalLumVarSum += d * d
        }
        val globalLumStd = sqrt(globalLumVarSum / pixels.size).toFloat().coerceAtLeast(1f)

        var hogIdx = 0           // [0..255]
        var spatialIdx = 256     // [256..447]
        var textureIdx = 448     // [448..575]
        val histOffset = 576     // [576..639]

        for (gy in 0 until gridCount) {
            for (gx in 0 until gridCount) {
                val startX = gx * cellW
                val startY = gy * cellH

                val hogBins = FloatArray(4) // 0: Horizontal, 1: Diagonal-1, 2: Vertical, 3: Diagonal-2
                var sumZLum = 0f
                var sumRG = 0f
                var sumYB = 0f
                var sumLumCell = 0f
                var sumLumSqCell = 0f
                var edgeCount = 0

                val cellPixels = cellW * cellH

                for (y in startY until (startY + cellH)) {
                    for (x in startX until (startX + cellW)) {
                        val idx = y * width + x
                        val p = pixels[idx]
                        val r = ((p shr 16) and 0xFF) / 255f
                        val g = ((p shr 8) and 0xFF) / 255f
                        val b = (p and 0xFF) / 255f
                        val lum = lumMap[idx]

                        // Z-scored luminance invariant to overall brightness shift
                        sumZLum += (lum - globalLumMean) / globalLumStd
                        // Opponent color channels
                        sumRG += (r - g)
                        sumYB += (0.5f * (r + g) - b)

                        sumLumCell += lum
                        sumLumSqCell += lum * lum

                        // Compute 2D spatial gradients inside valid border
                        if (x in 1 until (width - 1) && y in 1 until (height - 1)) {
                            val gxVal = lumMap[y * width + (x + 1)] - lumMap[y * width + (x - 1)]
                            val gyVal = lumMap[(y + 1) * width + x] - lumMap[(y - 1) * width + x]
                            val mag = sqrt(gxVal * gxVal + gyVal * gyVal)

                            if (mag > 15f) {
                                edgeCount++
                            }

                            // Unsigned orientation in [0, PI)
                            var angle = atan2(gyVal, gxVal)
                            if (angle < 0f) angle += Math.PI.toFloat()
                            val bin = ((angle / Math.PI.toFloat()) * 4f).toInt().coerceIn(0, 3)
                            hogBins[bin] += mag
                        }
                    }
                }

                // 1. Store 4-bin HOG for this cell
                for (b in 0 until 4) {
                    descriptor[hogIdx++] = hogBins[b] / cellPixels
                }

                // 2. Store Spatial Z-Lum and Color Opponent means
                descriptor[spatialIdx++] = sumZLum / cellPixels
                descriptor[spatialIdx++] = sumRG / cellPixels
                descriptor[spatialIdx++] = sumYB / cellPixels

                // 3. Store Local Texture StdDev & Edge Density
                val meanCell = sumLumCell / cellPixels
                val varianceCell = ((sumLumSqCell / cellPixels) - (meanCell * meanCell)).coerceAtLeast(0f)
                descriptor[textureIdx++] = sqrt(varianceCell) / 255f
                descriptor[textureIdx++] = edgeCount.toFloat() / cellPixels
            }
        }

        // 4. Global 64-bin Chromaticity & Opponent Histogram (4 R-G bins x 4 Y-B bins x 4 Z-Lum bins)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            val rg = ((r - g + 0.5f)).coerceIn(0f, 0.999f)
            val yb = ((0.5f * (r + g) - b + 0.5f)).coerceIn(0f, 0.999f)
            val zLum = (((lumMap[i] - globalLumMean) / (globalLumStd * 4f)) + 0.5f).coerceIn(0f, 0.999f)

            val binRG = (rg * 4f).toInt().coerceIn(0, 3)
            val binYB = (yb * 4f).toInt().coerceIn(0, 3)
            val binL = (zLum * 4f).toInt().coerceIn(0, 3)

            val histIdx = histOffset + (binRG * 16 + binYB * 4 + binL)
            descriptor[histIdx] += 1f
        }

        // Zero-mean and L2-normalize each sub-block independently so each modality has balanced weight
        zeroMeanAndNormalizeL2(descriptor, 0, 256, weight = 0.45f)     // Shape & Oriented Gradients (45%)
        zeroMeanAndNormalizeL2(descriptor, 256, 448, weight = 0.25f)   // Spatial Layout & Opponent Color (25%)
        zeroMeanAndNormalizeL2(descriptor, 448, 576, weight = 0.15f)   // Texture & Edge Structure (15%)
        zeroMeanAndNormalizeL2(descriptor, 576, 640, weight = 0.15f)   // Global Color Histogram (15%)

        // Final unit normalization
        return normalizeL2(descriptor)
    }

    private fun zeroMeanAndNormalizeL2(
        array: FloatArray,
        start: Int,
        end: Int,
        weight: Float = 1.0f
    ): FloatArray {
        val len = end - start
        if (len <= 0) return array

        var sum = 0.0
        for (i in start until end) {
            sum += array[i]
        }
        val mean = (sum / len).toFloat()

        var sumSq = 0.0
        for (i in start until end) {
            val centered = array[i] - mean
            array[i] = centered
            sumSq += centered * centered
        }

        val norm = sqrt(sumSq).toFloat()
        val scale = sqrt(weight.toDouble()).toFloat()
        if (norm > 1e-6f) {
            for (i in start until end) {
                array[i] = (array[i] / norm) * scale
            }
        } else {
            for (i in start until end) {
                array[i] = 0f
            }
        }
        return array
    }

    private fun normalizeL2(vector: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v).toDouble()
        }
        val norm = sqrt(sumSquares).toFloat()
        if (norm > 1e-6f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
    }
}
