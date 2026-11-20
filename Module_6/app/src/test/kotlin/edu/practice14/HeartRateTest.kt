package edu.practice14

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateTest {
    @Test
    fun decodesUnsigned8and16() {
        assertEquals(72, decodeHeartRate(byteArrayOf(0, 0x48)))
        assertEquals(300, decodeHeartRate(byteArrayOf(1, 0x2c, 1)))
        assertEquals(255, decodeHeartRate(byteArrayOf(0, -1)))
    }

    @Test
    fun rejectsTruncatedMeasurements() {
        assertNull(decodeHeartRate(byteArrayOf()))
        assertNull(decodeHeartRate(byteArrayOf(0)))
        assertNull(decodeHeartRate(byteArrayOf(1, 0x2c)))
    }
}
