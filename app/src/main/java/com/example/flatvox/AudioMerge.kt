package com.example.flatvox

import java.nio.ByteBuffer
import java.nio.ByteOrder

fun mergeTracks(track1: List<Short>, track2: List<Short>): List<Int> {
    val length = track1.size.coerceAtLeast(track2.size)
    val merged = ArrayList<Int>(length)

    for(i in 0 until length) {
        var formerSample: Short = 0
        var newSample: Short = 0

        if(track1.size > i) {
            formerSample = track1[i]
        }

        if(track2.size > i) {
            newSample = track2[i]
        }
        val sample = (formerSample + newSample)
        merged.add(sample)
    }
    return merged
}

fun mergeAudio(wav: ByteArray, pcm: ByteArray, sampleRate: Int): List<Byte> {
    val wavTrack = bytes2short(withoutWavHeader(wav)).asList()
    val pcmTrack = bytes2short(pcm).asList()

    val merged = short2byte(mergeTracks(wavTrack, pcmTrack).map { int -> int.toShort() }.toShortArray())
    val wavHeader = createWavHeader(merged.size / 2, sampleRate)

    val mergedwav = ArrayList<Byte>()
    mergedwav.addAll(wavHeader.asIterable())
    mergedwav.addAll(merged.asIterable())
    return mergedwav
}

fun createWavHeader(sampleNum: Int, sampleRate: Int): ByteArray {
    // fill little indians
    val littleBuffer = ByteBuffer.allocate(28)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(36 + sampleNum* 2)  // chunk length
        .putInt(16) // chunk length
        .putShort(1) // sample format pcm
        .putShort(1) // channels
        .putInt(sampleRate) //sample Rate
        .putInt(sampleRate * 2) // byte rate (sample rate * block align)
        .putShort(4)  // block align (channel count * bytes per sample)
        .putShort(16) // bits per sample
        .putInt(sampleNum * 2) // data length
        .array()

    return ByteBuffer.allocate(44)
        .put("RIFF".toByteArray())
        .put(littleBuffer.copyOfRange(0, 4))
        .put("WAVE".toByteArray())
        .put("fmt ".toByteArray())
        .put(littleBuffer.copyOfRange(4, 24))
        .put("data".toByteArray())
        .put(littleBuffer.copyOfRange(24, 28))
        .array()
}

fun withoutWavHeader(audio: ByteArray): ByteArray {
    return audio.copyOfRange(44, audio.size)
}
