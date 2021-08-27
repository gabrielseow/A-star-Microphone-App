package com.example.microphoneapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*


private const val SUPPORT_AUDIO_SOURCE_UNPROCESSED: String = "android.media.property.SUPPORT_AUDIO_SOURCE_UNPROCESSED"

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var stopRecordButton: Button
    private lateinit var timerSwitch: SwitchCompat
    private lateinit var playButton: Button
    private lateinit var timerButton: Button
    private lateinit var fileNameInput: TextInputEditText
    private lateinit var fileNameSwitch: SwitchCompat

    private lateinit var minutesInput: EditText
    private lateinit var secondsInput: EditText

    private var minutes: Int = 0
    private var seconds: Int = 0

    private var timeLeftInMillis: Long = 0

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var isRecording: Boolean = false
    private var recordingEnabled: Boolean = true
    private var fileName: String = ""

    private lateinit var countDownTimer: CountDownTimer
    private var timerSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.recordButton)
        stopRecordButton = findViewById(R.id.stopRecordButton)
        timerSwitch = findViewById(R.id.timerSwitch)
        playButton = findViewById(R.id.playButton)

        fileNameSwitch = findViewById(R.id.fileNameSwitch)
        fileNameInput = findViewById(R.id.fileNameInput)

        timerButton = findViewById(R.id.timerButton)
        minutesInput = findViewById(R.id.minutesText)
        secondsInput = findViewById(R.id.secondsText)

        checkMic()
        checkPermissions()
        updateButtons()

        recordButton.setOnClickListener{view -> recordAudio(view)}
        stopRecordButton.setOnClickListener{view -> stopRecordAudio(view)}
        timerSwitch.setOnCheckedChangeListener{view, isChecked -> setRecordingType(view, isChecked)}

        playButton.setOnClickListener{view -> playAudio(view)}

        fileNameSwitch.setOnCheckedChangeListener {view, isChecked -> setFileNameAsDate(view, isChecked)}
        fileNameInput.setOnEditorActionListener{view, actionId, event -> confirmEditText(view, actionId, event)}

        minutesInput.setOnEditorActionListener{view, actionId, event -> confirmMinutes(view, actionId, event)}
        secondsInput.setOnEditorActionListener{view, actionId, event -> confirmSeconds(view, actionId, event)}

        //fileName = applicationContext.getExternalFilesDir(null)?.absolutePath + "/myaudio.3gp"
        //audioFilePathInput.setText(SimpleDateFormat("yyyy_MM_dd-HH:mm", Locale.US).format(Date()))

    }

    private fun checkMic() {
        if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            Toast.makeText(this, "Error: Microphone not found", Toast.LENGTH_LONG).show()
            recordingEnabled = false
        }
        if (AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED != SUPPORT_AUDIO_SOURCE_UNPROCESSED) {
            Toast.makeText(this, "Error:Does not support raw input", Toast.LENGTH_LONG).show()
            recordingEnabled = false
        }
    }

    private fun checkPermissions() {
        val hasRecordPermission: Boolean = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasStoragePermission: Boolean = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordPermission) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        if (!hasStoragePermission) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) {
            Toast.makeText(this, "Illegal GrantResults State", Toast.LENGTH_LONG).show()
            recordingEnabled = false
        } else if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone Permission Granted", Toast.LENGTH_LONG).show()
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                recordingEnabled = false
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(this, "Microphone Permission Denied", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Microphone Permission previously Denied", Toast.LENGTH_LONG).show()
                }
            }
        } else if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_LONG).show()
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                recordingEnabled = false
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Storage Permission previously Denied", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Illegal RequestCode", Toast.LENGTH_LONG).show()
            recordingEnabled = false
        }
    }

    private fun updateButtons() {
        stopRecordButton.isEnabled = false
        playButton.isEnabled = false
        timerButton.isEnabled = timerSwitch.isChecked
        minutesInput.isEnabled = timerSwitch.isChecked
        secondsInput.isEnabled = timerSwitch.isChecked
        recordButton.isEnabled = recordingEnabled
    }

    private fun confirmEditText(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (v.text.toString().isEmpty()) {
                Toast.makeText(this, "Empty File Name", Toast.LENGTH_LONG).show()
            } else {
                fileName = applicationContext.getExternalFilesDir(null)?.absolutePath + "/" + v.text.toString() + ".wav"
            }
        }
        return false
    }

    private fun confirmMinutes(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (v.text.toString().isEmpty() || v.text.toString().toInt() < 0 || v.text.toString().toInt() > 59) {
                Toast.makeText(this, "Please input a valid number of minutes", Toast.LENGTH_LONG).show()
            } else {
                minutes = v.text.toString().toInt()
            }
        }
        timerButton.text = getString(R.string.duration_format, minutes, seconds)
        timerSet = true
        timeLeftInMillis = ((minutes * 60 + seconds) * 1000).toLong()
        return false
    }

    private fun confirmSeconds(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (v.text.toString().isEmpty() || v.text.toString().toInt() < 0 || v.text.toString().toInt() > 59) {
                Toast.makeText(this, "Please input a valid number of seconds", Toast.LENGTH_LONG).show()
            } else {
                seconds = v.text.toString().toInt()
            }
        }
        timerButton.text = getString(R.string.duration_format, minutes, seconds)
        timerSet = true
        timeLeftInMillis = ((minutes * 60 + seconds) * 1000).toLong()
        return false
    }

    private fun setFileNameAsDate(view: View, isChecked: Boolean) {
        if (isChecked) {
            fileNameInput.setText(SimpleDateFormat("yyyy_MM_dd-HH:mm", Locale.US).format(Date()))
        } else {
            fileNameInput.setText("")
        }
    }

    private fun setRecordingType(view: View, isChecked: Boolean) {
        if (isChecked) {
            timerButton.isEnabled = true
            minutesInput.isEnabled = true
            secondsInput.isEnabled = true
        } else {
            timerButton.isEnabled = false
            timerSet = false
            timerButton.text = resources.getString(R.string.time)
            minutesInput.text.clear()
            secondsInput.text.clear()
        }
    }

    fun recordAudio(view: View) {
        if (fileName.isEmpty()) {
            Toast.makeText(this, "Please enter a file name", Toast.LENGTH_LONG).show()
            return
        }

        isRecording = true
        stopRecordButton.isEnabled = true
        playButton.isEnabled = false
        recordButton.isEnabled = false
        timerButton.isEnabled = false
        minutesInput.isEnabled = false
        secondsInput.isEnabled = false

        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            mediaRecorder?.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder?.setOutputFile(fileName)
            mediaRecorder?.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder?.start()

        if (timerSet) {
            countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftInMillis = millisUntilFinished
                    updateCountDownText()
                }

                override fun onFinish() {
                    timerSet = false
                    timerButton.text = resources.getString(R.string.time)
                    stopRecordAudio(view)
                }
            }.start()
        }
    }

    private fun updateCountDownText() {
        minutes = ((timeLeftInMillis / 1000) / 60).toInt()
        seconds = ((timeLeftInMillis / 1000) % 60).toInt()
        val timeLeftFormatted =
            java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        timerButton.text = timeLeftFormatted
    }

    fun stopRecordAudio(view: View) {
        stopRecordButton.isEnabled = false
        playButton.isEnabled = true
        recordButton.isEnabled = true
        timerSet = false
        timerButton.isEnabled = timerSwitch.isChecked
        minutesInput.isEnabled = timerSwitch.isChecked
        secondsInput.isEnabled = timerSwitch.isChecked
        timerButton.text = resources.getString(R.string.time)

        if (isRecording) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            countDownTimer.cancel()
        } else {
            mediaPlayer?.release()
            mediaPlayer = null
            recordButton.isEnabled = true
        }
    }

    fun playAudio(view: View) {
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(fileName)
        mediaPlayer?.prepare()
        mediaPlayer?.start()
    }
}