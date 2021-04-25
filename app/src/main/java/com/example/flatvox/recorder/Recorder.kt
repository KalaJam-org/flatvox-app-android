package com.example.flatvox.recorder

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.flatvox.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and


class Recorder : Activity() {
    companion object {
        private const val SAMPLING_RATE = 44100
        val minBufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    }

    private var recordingThread: Thread? = null
    private var output: String? = null
    private var recorder: AudioRecord? = null
    private var state: Boolean = false
    private var requestQueue: RequestQueue? = null
    private var formerPlayer: MediaPlayer? = null

    private var recordButton: RecordButton? = null
    private var submitButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recorder_new)

        val startWithPermissionCheck = {
            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions, 0)
            } else {
                startRecording()
            }
        }

        requestQueue = Volley.newRequestQueue(baseContext.applicationContext)

        recordButton = RecordButton(
                findViewById(R.id.btn_record),
                R.drawable.rec_btn,
                R.drawable.stop_rec_btn,
                startWithPermissionCheck,
                { stopRecording() })

        submitButton = findViewById(R.id.btn_submit);

        output = Environment.getExternalStorageDirectory().absolutePath + "/recording.wav"
    }

    private fun startRecording() {
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                    SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                2 * minBufSize)

            playAudio()
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
            formerPlayer!!.stop()

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

    private fun playAudio() {
        val path: String = Environment.getExternalStorageDirectory().toString() + "/former.wav";
        formerPlayer = MediaPlayer()
        formerPlayer!!.setDataSource(path)
        formerPlayer!!.prepare()
        formerPlayer!!.start()
    }

    inner class RecordingRunnable : Runnable {
        override fun run() {
            val file = File(Environment.getExternalStorageDirectory(), "recording.pcm")
            val buffer = ShortArray(minBufSize)
            try {
                FileOutputStream(file).use { outStream ->
                    while (state) {
                        val result: Int = recorder!!.read(buffer, 0, minBufSize)
                        if (result < 0) {
                            throw RuntimeException(
                                "Reading of audio buffer failed: " +
                                        getBufferReadFailureReason(result)
                            )
                        }
                        val bytes = short2byte(buffer)
                        outStream.write(bytes, 0, 2 * minBufSize)
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Writing of recorded audio failed", e)
            }
        }

        //Conversion of short to byte
        private fun short2byte(sData: ShortArray): ByteArray {
            val shortArrsize = sData.size
            val bytes = ByteArray(shortArrsize * 2)
            for (i in 0 until shortArrsize) {
                bytes[i * 2] = (sData[i] and 0x00FF).toByte()
                bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
                sData[i] = 0
            }
            return bytes
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