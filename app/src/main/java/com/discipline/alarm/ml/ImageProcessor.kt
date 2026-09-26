package com.discipline.alarm.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.min

object ImageProcessor {

    const val TARGET_WIDTH = 224
    const val TARGET_HEIGHT = 224

    /**
     * Checks average luminance of the image.
     * Returns a float in [0.0, 255.0].
     */
    fun calculateAverageLuminance(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        var totalLuminance = 0.0

        for (x in 0 until 32) {
            for (y in 0 until 32) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuminance += lum
            }
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return (totalLuminance / (32 * 32)).toFloat()
    }

    /**
     * Computes structural contrast (standard deviation of luminance) to detect
     * if the camera is covered or pointing at a blank/uniform surface (wall, floor, sheet).
     */
    fun calculateStructureContrast(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val lums = FloatArray(32 * 32)
        var sum = 0.0
        var idx = 0

        for (x in 0 until 32) {
            for (y in 0 until 32) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toFloat()
                lums[idx++] = lum
                sum += lum
            }
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        val mean = (sum / lums.size).toFloat()
        var sqDiff = 0.0
        for (l in lums) {
            val d = l - mean
            sqDiff += d * d
        }
        return kotlin.math.sqrt(sqDiff / lums.size).toFloat()
    }

    /**
     * Crops image to center square and resizes to targetWidth x targetHeight.
     */
    fun cropCenterAndResize(
        bitmap: Bitmap,
        targetWidth: Int = TARGET_WIDTH,
        targetHeight: Int = TARGET_HEIGHT
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val minDim = min(width, height)

        val cropX = (width - minDim) / 2
        val cropY = (height - minDim) / 2

        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, minDim, minDim)
        val resized = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)

        if (cropped != bitmap && cropped != resized) {
            cropped.recycle()
        }

        return resized
    }

    /**
     * Converts Bitmap to a normalized 1D FloatArray for MobileNet input.
     * Shape: [TARGET_HEIGHT * TARGET_WIDTH * 3]
     * Normalized to range [-1.0f, 1.0f] (MobileNet convention: (pixel - 127.5) / 127.5)
     */
    fun bitmapToNormalizedFloatArray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val floatValues = FloatArray(width * height * 3)
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var idx = 0
        for (i in 0 until width * height) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)

            // MobileNet normalization: [-1, 1]
            floatValues[idx++] = (r - 127.5f) / 127.5f
            floatValues[idx++] = (g - 127.5f) / 127.5f
            floatValues[idx++] = (b - 127.5f) / 127.5f
        }

        return floatValues
    }
}
