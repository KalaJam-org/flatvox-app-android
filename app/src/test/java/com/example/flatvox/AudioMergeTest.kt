package com.example.flatvox

import org.junit.Test

import org.junit.Assert.*

class AudioMergeTest {
    @Test
    fun merge_two_regular_different_length() {
        val track1 = arrayListOf<Short>(-10000, -1000, -100, -10, -1, 0, 1, 10, 100, 1000, 10000)
        val track2 = arrayListOf<Short>(10000, 1000, 100, 10, 1)

        val merged = mergeTracks(track1, track2)

        val expected = intArrayOf(0, 0, 0, 0, 0, 0, 1, 10, 100, 1000, 10000)

        assertArrayEquals(expected, merged.toIntArray())
    }

    @Test
    fun merge_track_with_empty() {
        val track1 = arrayListOf<Short>(1, -2, -3, 4)
        val trackEmpty = ArrayList<Short>(0)
        val trackZero = arrayListOf<Short>(0, 0, 0, 0)

        val mergedEmpty = mergeTracks(trackEmpty, track1)
        val track1AsIntArray = track1.map{ sh -> sh.toInt()}.toIntArray()
        assertArrayEquals(track1AsIntArray, mergedEmpty.toIntArray())

        val mergedZero = mergeTracks(track1, trackZero)
        assertArrayEquals(track1AsIntArray, mergedZero.toIntArray())
    }

    @Test
    fun merge_audio_with_empty() {
        val track = ArrayList<Byte>()
        val header = createWavHeader(128, 44100)
        track.addAll(header.asIterable())

        for (i in 0 until 256) {
            track.add(i.toByte())
        }

        val merged = mergeAudio(track.toByteArray(), ByteArray(0), 44100)

        assertEquals(track, merged)
    }
}