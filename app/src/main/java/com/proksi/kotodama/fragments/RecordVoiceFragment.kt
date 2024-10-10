package com.proksi.kotodama.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentRecordVoiceBinding
import com.proksi.kotodama.models.AudioRecord
import com.proksi.kotodama.viewmodel.CloneViewModel
import java.io.File
import java.io.IOException

class RecordVoiceFragment : Fragment() {



        private lateinit var design: FragmentRecordVoiceBinding
        private val TAG = RecordVoiceFragment::class.java.simpleName
        private var mediaRecorder: MediaRecorder? = null
        private var outputFilePath: String = ""
        private var isRecording = false
        private var isPaused = false
        private var mediaPlayer: MediaPlayer? = null
        private var handler: Handler? = null
        private var startTime = 0L
        private var runnable: Runnable? = null
        private val viewModel: CloneViewModel by activityViewModels()
        private var isPlaying:String = "record mode"


        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {

            design = FragmentRecordVoiceBinding.inflate(inflater,container,false)


            if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
            }

            design.submitButton.setOnClickListener {
                val duration = getAudioDuration(outputFilePath)
                val fileSize = File(outputFilePath).length() // dosya boyutu byte cinsinden

                val audioRecord = AudioRecord(
                    path = outputFilePath,
                    duration = duration,
                    fileSize = fileSize
                )

                val isAdded = viewModel.addAudioFilePath(audioRecord)

                if (isAdded) {
                    findNavController().navigate(R.id.action_recordVoiceFragment_to_voiceLabFormatFragment)
                } else {
                    Toast.makeText(requireContext(), "Total duration exceeds 7 minutes", Toast.LENGTH_SHORT).show()
                }

            }

            design.microBtn.setOnClickListener{

                if (isPlaying==="playing") {
                    if (isPaused){
                        design.microImg.setImageResource(R.drawable.icon_play)
                        playRecording()
                    }else {
                        design.microImg.setImageResource(R.drawable.icon_pause)
                        stopPlaying()
                    }
                } else {
                    if (isRecording) {
                        stopRecording()
                    }
                    else {
                        startRecording()
                    }
                    isRecording = !isRecording
                    updateButtonStates()
                }
            }

            design.cancelRecord.setOnClickListener {
                val file = File(outputFilePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        Log.d(TAG, "Dosya silindi: $outputFilePath")
                        design.recordingDuration.text = "00:00"
                        isRecording = false
                        updateButtonStates()
                    } else {
                        Log.e(TAG, "Dosya silinemedi: $outputFilePath")
                    }
                } else {
                    Log.e(TAG, "Dosya bulunamadı: $outputFilePath")
                }
            }

            updateButtonStates()
            return design.root
        }

        private fun startRecording() {
            outputFilePath = "${requireContext().externalCacheDir?.absolutePath}/audiorecordtest.m4a"

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFilePath)

                try {
                    prepare()
                    start()
                    startTimer()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        private fun stopRecording() {
            mediaRecorder?.apply {
                stop()
                release()
                stopTimer()
            }
            mediaRecorder = null
            updateButtonStates()
        }

        private fun startTimer() {
            handler = Handler()
            startTime = System.currentTimeMillis()
            runnable = object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - startTime
                    val minutes = (elapsed / 1000) / 60
                    val seconds = (elapsed / 1000) % 60
                    design.recordingDuration.text = String.format("%02d:%02d", minutes, seconds)
                    handler?.postDelayed(this, 1000)
                }
            }
            handler?.post(runnable!!)
        }

        private fun stopTimer() {
            handler?.removeCallbacks(runnable!!)
            // design.recordingDuration.text = "00:00" // Reset duration text
        }

        fun getAudioDuration(filePath: String): Long {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong() // milisaniye cinsinden süre
            mediaPlayer.release()
            return duration
        }

        private fun playRecording() {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    isPaused = true
                    try {
                        setDataSource(outputFilePath)
                        prepare()
                        start()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    setOnCompletionListener {
                        design.microImg.setImageResource(R.drawable.icon_play)
                        isPaused = true
                    }
                }
            } else if (isPaused) {
                mediaPlayer?.start()
                isPaused = false
            }
            design.microImg.setImageResource(R.drawable.icon_pause) // Kayıt bittikten sonra "play" göster

        }

        private fun updateButtonStates() {
            val fileExists = File(outputFilePath).exists()

            when {
                isRecording -> {
                    design.microImg.setImageResource(R.drawable.icon_pause) // Kaydedilirken "pause" göster
                    design.microBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_pause)
                    design.submitButton.visibility = View.GONE
                    design.cancelRecord.visibility = View.GONE
                    isPlaying="record mode"
                }
                fileExists -> {
                    design.microImg.setImageResource(R.drawable.icon_play) // Kayıt bittikten sonra "play" göster
                    design.microBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_micro)
                    design.submitButton.visibility = View.VISIBLE
                    design.cancelRecord.visibility = View.VISIBLE
                    isPlaying="playing"
                }
                else -> {
                    design.microImg.setImageResource(R.drawable.micro) // Kayıt yoksa "micro" göster
                    design.microBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_micro)
                    design.submitButton.visibility = View.GONE
                    design.cancelRecord.visibility = View.GONE
                    isPlaying="record mode"
                }
            }
        }

        override fun onStop() {
            super.onStop()
            mediaRecorder?.release()
            mediaRecorder = null
        }

        private fun stopPlaying() {
            mediaPlayer?.apply {
                stop()
                reset()
                release()
            }
            mediaPlayer = null
            isPaused = true

            design.microImg.setImageResource(R.drawable.icon_play)

        }





}