package com.example.flatvox

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import com.example.flatvox.recorder.Recorder

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record_mode_chooser)
        val newBtn = findViewById<Button>(R.id.new_song_button)
//        val contribute_btn = findViewById<Button>(R.id.contribute_button)

        newBtn.setOnClickListener {
            val intent = Intent(this, Recorder::class.java);
            startActivity(intent)
        }
    }
}