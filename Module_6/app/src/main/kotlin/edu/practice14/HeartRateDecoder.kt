package edu.practice14

fun decodeHeartRate(bytes: ByteArray): Int? {
    if (bytes.size < 2) return null

    val is16Bit = (bytes[0].toInt() and 1) != 0
    val lowByte = bytes[1].toInt() and 0xff
    if (!is16Bit) return lowByte
    if (bytes.size < 3) return null

    val highByte = bytes[2].toInt() and 0xff
    return lowByte or (highByte shl 8)
}
