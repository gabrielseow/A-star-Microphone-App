package com.example.microphoneapplication

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest

private const val SUPPORT_AUDIO_SOURCE_UNPROCESSED: String = "android.media.property.SUPPORT_AUDIO_SOURCE_UNPROCESSED"

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    private lateinit var stopButton: Button

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var isRecording: Boolean = false
    private var audioFilePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.recordButton)
        playButton = findViewById(R.id.playButton)
        stopButton = findViewById(R.id.stopButton)
        audioFilePath = applicationContext.getExternalFilesDir(null)?.absolutePath + "/myaudio.3gp"

        checkMic()
        checkMicInput()
        checkPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    private fun checkMic() {
        if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) finish()

        playButton.isEnabled = false
        stopButton.isEnabled = false
    }

    private fun checkMicInput() {
        if (AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED == SUPPORT_AUDIO_SOURCE_UNPROCESSED) {
            Toast.makeText(this, "Supports unprocessed input", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Does not support unprocessed input", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                    Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            }
        }
    }

    fun recordAudio(view: View) {
        isRecording = true
        stopButton.isEnabled = true
        playButton.isEnabled = false
        recordButton.isEnabled = false

        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder?.setOutputFile(audioFilePath)
            mediaRecorder?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaRecorder?.start()
    }

    fun stopAudio(view: View) {
        stopButton.isEnabled = false
        playButton.isEnabled = true

        if (isRecording) {
            recordButton.isEnabled = false
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        } else {
            mediaPlayer?.release()
            mediaPlayer = null
            recordButton.isEnabled = true
        }
    }

    fun playAudio(view: View) {
        playButton.isEnabled = false
        recordButton.isEnabled = false
        stopButton.isEnabled = true

        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(audioFilePath)
        mediaPlayer?.prepare()
        mediaPlayer?.start()
    }
}