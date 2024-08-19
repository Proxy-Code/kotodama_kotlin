package com.proksi.kotodama.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import android.media.MediaRecorder
import android.widget.TextView
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentVoiceLabRecordBinding
import java.io.IOException

class VoiceLabRecordFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabRecordBinding
    private lateinit var imageUri: Uri
    private lateinit var name:String
    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String = ""
    private var isRecording = false
    private var isPaused = false
    private var mediaPlayer: MediaPlayer? = null
    private val TAG = VoiceLabRecordFragment::class.java.simpleName
    private lateinit var continueBtn: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabRecordBinding.inflate(inflater, container, false)
        continueBtn = design.continueBtn

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}


        val bundle:  VoiceLabRecordFragmentArgs by navArgs()
        name= bundle.name
        imageUri=bundle.image

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        design.backButtonName.setOnClickListener(){
            val action = VoiceLabRecordFragmentDirections.actionVoiceLabRecordFragmentToVoiceLabFormatFragment(name,imageUri)
            findNavController().navigate(action)
        }

        if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }

        design.recordButton.setOnClickListener{
            if (isRecording) {
                stopRecording()
                design.recordButton.setImageResource(R.drawable.micro)
                continueBtn.isEnabled = true
                design.continueBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_gradient)

            } else {
                startRecording()
                design.recordButton.setImageResource(R.drawable.pause_record)
                design.continueBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_c8c8)
                continueBtn.isEnabled = false
            }
            isRecording = !isRecording
        }

        design.playRecordButton.setOnClickListener{
            if (isPaused){
                design.playRecordButton.setImageResource(R.drawable.icon_play)
            }else {
                design.playRecordButton.setImageResource(R.drawable.icon_pause)
            }
            playRecording()
        }

        design.restartButton.setOnClickListener{
            restartPlaying()
        }

        design.continueBtn.setOnClickListener{
            val action = VoiceLabRecordFragmentDirections
                .actionVoiceLabRecordFragmentToVoiceLabCompletedFragment(name,imageUri)
            findNavController().navigate(action)
        }


        return design.root
    }

    private fun startRecording() {
        outputFilePath = "${requireContext().externalCacheDir?.absolutePath}/audiorecordtest.3gp"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFilePath)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

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
            }
        } else if (isPaused) {
            mediaPlayer?.start()
            isPaused = false
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
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
        isPaused = false
    }

    private fun restartPlaying() {
        stopPlaying()
        playRecording()
    }


}