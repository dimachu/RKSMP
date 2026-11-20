package edu.practice7

import org.junit.Assert.assertEquals
import org.junit.Test

class CompassAnglesTest {
    @Test fun normalizesSignedSensorAngles() {
        assertEquals(359f, normalizedAzimuth(-1f), 0.001f)
        assertEquals(90f, normalizedAzimuth(450f), 0.001f)
    }

    @Test fun crossingNorthDoesNotSpinTheArrowAround() {
        assertEquals(-1f, nearestNorthRotation(1f, 1f), 0.001f)
        assertEquals(1f, nearestNorthRotation(359f, -1f), 0.001f)
        assertEquals(-90f, nearestNorthRotation(90f, 0f), 0.001f)
    }
}
