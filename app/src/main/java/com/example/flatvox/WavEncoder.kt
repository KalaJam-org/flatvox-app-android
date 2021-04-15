package com.example.flatvox

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WaveEncoder(private val sampleRate: Int, private val samples: ShortArray) {

    private fun makeWavHeader(): ByteArray {
        // fill little indians
        val littleBuffer = ByteBuffer.allocate(28)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(36 + samples.size * 2)  // chunk length
            .putInt(16) // chunk length
            .putShort(1) // sample format pcm
            .putShort(1) // channels
            .putInt(sampleRate) //sample Rate
            .putInt(sampleRate * 2) // byte rate (sample rate * block align)
            .putShort(4)  // block align (channel count * bytes per sample)
            .putShort(16) // bits per sample
            .putInt(samples.size * 2) // data length
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

    fun getData(): ByteArray {
        val header = makeWavHeader()
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for(s in samples) {
            data.putShort(s)
        }

        val result = ByteArray(44 + samples.size * 2)
        val dataarray = data.array()
        System.arraycopy(header, 0, result, 0, header.size)
        System.arraycopy(dataarray, 0, result, header.size, dataarray.size)

        return result
    }

    fun saveAudioFile(fileName: String, ctx: Context, data: ByteArray) {
        val values = ContentValues()
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/*")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val audioCollection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val contentResolver: ContentResolver = ctx.contentResolver
        val item = contentResolver.insert(audioCollection, values)

        item?.let {
            contentResolver.openFileDescriptor(it, "w", null).use { pfd ->
                val fos = FileOutputStream(pfd!!.fileDescriptor)
                fos.write(data)
                fos.flush()
                fos.close()
                pfd.close()

            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                values.put(MediaStore.Audio.Media.SIZE, data.size)
                values.put(MediaStore.Audio.Media.DATE_TAKEN, MediaStore.Audio.Media.DATE_MODIFIED)
                contentResolver.update(item, values, null, null)
            }
        }
    }
}