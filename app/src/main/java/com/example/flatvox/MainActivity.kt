package com.example.flatvox

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {
    companion object {
        private const val SAMPLING_RATE = 22050
        val minBufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    }

    private var recordingThread: Thread? = null
    private var output: String? = null
    private var recorder: AudioRecord? = null
    private var state: Boolean = false

    private var btnStartRecording: Button? = null
    private var btnStopRecording: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartRecording = findViewById(R.id.button_start_recording)
        btnStopRecording = findViewById(R.id.button_stop_recording)

        btnStartRecording?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions,0)
            } else {
                startRecording()
            }
        }

        btnStopRecording?.setOnClickListener{
            stopRecording()
        }

        output = Environment.getExternalStorageDirectory().absolutePath + "/recording.wav"

    }

    private fun startRecording() {
        try {

            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                2*minBufSize)

            recorder!!.startRecording()

            state = true

            recordingThread = Thread(RecordingRunnable(), "Recording Thread")
            recordingThread!!.start()

            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }



    private fun stopRecording() {
        if(state){
            if (null == recorder) {
                return
            }
            state = false
            recorder!!.stop()
            recorder!!.release()
            recorder = null
            recordingThread = null

            val inFile = File(Environment.getExternalStorageDirectory(), "recording.pcm")
            val numberOfSamples = inFile.length().toInt() / 2

            val inStream = FileInputStream(inFile)
            val os = FileOutputStream(output)
            os.write(createWavHeader(numberOfSamples, SAMPLING_RATE))

            val bytes = inStream.readBytes()
            os.write(bytes)

            inStream.close()
            os.close()

            inFile.delete()
        }else{
            Toast.makeText(this, "You are not recording right now!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createWavHeader(sampleNum: Int, sampleRate: Int): ByteArray {
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

    inner class RecordingRunnable : Runnable {
        override fun run() {
            val file = File(Environment.getExternalStorageDirectory(), "recording.pcm")
            val buffer = ByteBuffer.allocateDirect(minBufSize)
            try {
                FileOutputStream(file).use { outStream ->
                    while (state) {
                        val result: Int = recorder!!.read(buffer, minBufSize)
                        if (result < 0) {
                            throw RuntimeException(
                                "Reading of audio buffer failed: " +
                                        getBufferReadFailureReason(result)
                            )
                        }
                        outStream.write(buffer.array(), 0, minBufSize)
                        buffer.clear()
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Writing of recorded audio failed", e)
            }
        }

        private fun getBufferReadFailureReason(errorCode: Int): String {
            return when (errorCode) {
                AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
                AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
                AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
                AudioRecord.ERROR -> "ERROR"
                else -> "Unknown ($errorCode)"
            }
        }
    }
}