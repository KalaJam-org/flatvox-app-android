package com.example.flatvox.recorder

import android.widget.ImageButton

class RecordButton(
        private val button: ImageButton,
        private val duringRecordImage: Int,
        private val duringStoppedImage: Int,
        private val startRecord: () -> Unit,
        private val stopRecord: () -> Unit) {

    private var recordInProgress = false;

    init {
        button.setOnClickListener {
            if(recordInProgress) {
                recordInProgress = false
                button.setImageResource(duringRecordImage)
                stopRecord.invoke()
            } else {
                recordInProgress = true
                button.setImageResource(duringStoppedImage)
                startRecord.invoke()
            }
        }
    }
}