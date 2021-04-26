package com.example.flatvox

import kotlin.experimental.and

fun bytes2short(bytes: ByteArray): ShortArray {
    val byteArrSize = bytes.size
    val shorts = ArrayList<Short>(byteArrSize / 2)
    for (i in 0 until byteArrSize / 2) {
        val byte1 = bytes[i*2].toInt() and 0x00FF
        val byte2 = (bytes[i*2+1].toInt() shl 8) and 0xFF00
        shorts.add((byte1 or byte2).toShort())
    }
    return shorts.toShortArray()
}

fun short2byte(sData: ShortArray): ByteArray {
    val shortArrsize = sData.size
    val bytes = ByteArray(shortArrsize * 2)
    for (i in 0 until shortArrsize) {
        bytes[i * 2] = (sData[i] and 0x00FF).toByte()
        bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
    }
    return bytes
}