package com.discipline.alarm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileStorageManager(private val context: Context) {

    private val missionsDir: File
        get() = File(context.filesDir, "missions").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Saves a captured Bitmap into internal storage and returns the absolute file path.
     */
    suspend fun saveMissionImage(bitmap: Bitmap, missionName: String, slotIndex: Int): String = withContext(Dispatchers.IO) {
        val sanitizedName = missionName.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
        val filename = "mission_${sanitizedName}_slot${slotIndex}_${System.currentTimeMillis()}.jpg"
        val file = File(missionsDir, filename)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Loads a Bitmap from the given file path.
     */
    suspend fun loadBitmap(path: String): Bitmap? = withContext(Dispatchers.IO) {
        if (path.isBlank()) return@withContext null
        val file = File(path)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    /**
     * Deletes images associated with a mission.
     */
    suspend fun deleteImages(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { path ->
            if (path.isNotBlank()) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
}
