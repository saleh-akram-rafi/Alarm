package com.discipline.alarm.data.local.converter

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Converters {

    companion object {
        /**
         * Converts a FloatArray (e.g. 1280-d MobileNet feature vector) to a ByteArray for Room BLOB storage.
         */
        fun floatArrayToByteArray(floats: FloatArray?): ByteArray? {
            if (floats == null) return null
            val byteBuffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            floats.forEach { byteBuffer.putFloat(it) }
            return byteBuffer.array()
        }

        /**
         * Converts a ByteArray BLOB back to a FloatArray.
         */
        fun byteArrayToFloatArray(bytes: ByteArray?): FloatArray? {
            if (bytes == null || bytes.isEmpty()) return null
            val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val floatArray = FloatArray(bytes.size / 4)
            byteBuffer.asFloatBuffer().get(floatArray)
            return floatArray
        }
    }

    @TypeConverter
    fun fromByteArray(bytes: ByteArray?): String? {
        return bytes?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
    }

    @TypeConverter
    fun toByteArray(encoded: String?): ByteArray? {
        return encoded?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
    }
}
