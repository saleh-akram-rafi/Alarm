package com.discipline.alarm.data

import com.discipline.alarm.data.local.converter.Converters
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    @Test
    fun testFloatArrayToByteArrayAndBack() {
        val original = floatArrayOf(0.123f, -0.456f, 0.789f, 1.0f, 0.0f)
        val bytes = Converters.floatArrayToByteArray(original)
        val restored = Converters.byteArrayToFloatArray(bytes)

        assertArrayEquals(original, restored, 0.00001f)
    }

    @Test
    fun testNullFloatArrayConversion() {
        assertNull(Converters.floatArrayToByteArray(null))
        assertNull(Converters.byteArrayToFloatArray(null))
    }
}
