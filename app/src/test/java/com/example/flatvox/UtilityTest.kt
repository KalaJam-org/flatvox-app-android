package com.example.flatvox

import org.junit.Assert
import org.junit.Test

class UtilityTest {
    @Test
    fun short2byte_test() {
        val shortArray = shortArrayOf(0xABCD.toShort(), 0x01EF)
        val actual = short2byte(shortArray)

        val expected = byteArrayOf(0xCD.toByte(), 0xAB.toByte(), 0xEF.toByte(), 0x01)

        Assert.assertArrayEquals(expected, actual)
    }

    @Test
    fun byte2short_test() {
        val byteArray = byteArrayOf(0xCD.toByte(), 0xAB.toByte(), 0xEF.toByte(), 0x01)
        val actual = bytes2short(byteArray)

        val expected = shortArrayOf(0xABCD.toShort(), 0x01EF)

        Assert.assertArrayEquals(expected, actual)
    }
}