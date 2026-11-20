package edu.practice7

internal fun normalizedAzimuth(degrees: Float): Float = (degrees % 360f + 360f) % 360f

internal fun nearestNorthRotation(azimuth: Float, currentRotation: Float): Float {
    val north = -azimuth
    val delta = normalizedAzimuth(north - currentRotation + 180f) - 180f
    return currentRotation + delta
}
