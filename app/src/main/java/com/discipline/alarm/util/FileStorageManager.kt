package com.discipline.alarm.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
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

    private val customTonesDir: File
        get() = File(context.filesDir, "custom_tones").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Copies a user-selected audio file (MP3, WAV, M4A, OGG, etc.) from device storage
     * into internal app storage so it plays 100% reliably even after reboot or in Doze mode.
     */
    suspend fun saveCustomAudioTone(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Ignore if the provider does not support persistable URI permissions
        }

        val originalName = resolveUriFileName(context, sourceUri) ?: "custom_alarm.mp3"
        val sanitizedName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(customTonesDir, "custom_${System.currentTimeMillis()}__$sanitizedName")

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            if (destFile.exists() && destFile.length() > 0L) {
                return@withContext Uri.fromFile(destFile).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to original URI string (e.g. for system ringtones)
        sourceUri.toString()
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

    companion object {
        fun resolveUriFileName(context: Context, uri: Uri): String? {
            if (uri.scheme == "content") {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(uri, null, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    cursor?.close()
                }
            }
            return uri.lastPathSegment?.substringAfterLast('/')
        }

        fun getSoundDisplayName(context: Context, soundUri: String): String {
            if (soundUri.isBlank() ||
                soundUri.equals("Kafir.mp3", ignoreCase = true) ||
                soundUri.equals("kafir", ignoreCase = true) ||
                soundUri.contains("raw/kafir")
            ) {
                return "Kafir.mp3 (Discipline)"
            }
            if (soundUri.equals("system", ignoreCase = true)) {
                return "System Alarm"
            }
            if (soundUri.startsWith("file://") || soundUri.startsWith("/")) {
                val fileName = Uri.parse(soundUri).lastPathSegment ?: File(soundUri).name
                return fileName.substringAfter("__", fileName).replace('_', ' ')
            }
            return try {
                val uri = Uri.parse(soundUri)
                val fileName = resolveUriFileName(context, uri)
                if (!fileName.isNullOrBlank() && !fileName.all { it.isDigit() }) {
                    fileName
                } else {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.getTitle(context) ?: "Custom Device Tune"
                }
            } catch (_: Exception) {
                "Custom Device Tune"
            }
        }
    }
}
