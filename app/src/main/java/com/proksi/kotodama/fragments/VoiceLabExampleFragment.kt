package com.proksi.kotodama.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.view.Window
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentVoiceLabPhotoBinding
import com.proksi.kotodama.objects.EventLogger


class VoiceLabExampleFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabPhotoBinding
    private lateinit var photoUri: Uri
    private lateinit var name:String
    private val TAG = VoiceLabExampleFragment::class.java.simpleName
    private var currentMediaPlayer: MediaPlayer? = null
    private var currentSeekBar: SeekBar? = null
    private val handler = Handler()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabPhotoBinding.inflate(inflater, container, false)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        design.backBtn.setOnClickListener(){
            findNavController().navigate(R.id.action_voiceLabPhotoFragment_to_voiceLabNameFragment)
        }

        EventLogger.logEvent(requireContext(), "cloneExample_screen_shown")


        design.continueButton.setOnClickListener{
            findNavController().navigate(R.id.action_voiceLabPhotoFragment_to_voiceLabFormatFragment)
        }

        setupMusicPlayer(
            design.playBtn1, design.seekbar1, R.raw.good_exp_1

        )
        setupMusicPlayer(
            design.playBtn2, design.seekbar2, R.raw.good_exp_2
        )
        setupMusicPlayer(
            design.playBtn3, design.seekbar3, R.raw.bad_exp_1
        )
        setupMusicPlayer(
            design.playBtn4, design.seekbar4, R.raw.bad_exp_2
        )

        return design.root
    }

    private fun setupMusicPlayer(playButton: ImageView, seekBar: SeekBar,audioFileRes: Int){

        playButton.setOnClickListener{
            Log.d("mediaaa", "setupMusicPlayer: $playButton $audioFileRes")
            if (currentMediaPlayer != null && currentMediaPlayer?.isPlaying == true) {
                Log.d("mediaaa", "setupMusicPlayer: ${currentMediaPlayer?.isPlaying}")
                currentMediaPlayer?.stop()
//                currentMediaPlayer?.release()
                currentMediaPlayer = null
                currentSeekBar?.progress = 0
                resetPlayButtonIcons()
            }else {
                Log.d("mediaaa", "MusicPlayer:${currentMediaPlayer?.isPlaying}")
                currentMediaPlayer = MediaPlayer.create(context, audioFileRes)
                currentMediaPlayer?.start()
                playButton.setImageResource(R.drawable.icon_pause)
//
//                currentMediaPlayer = MediaPlayer.create(requireContext(), audioFileRes)
//                currentSeekBar = seekBar
//
//                currentMediaPlayer?.start()
                seekBar.max = currentMediaPlayer?.duration ?: 0
                updateSeekBar(currentMediaPlayer!!, seekBar, playButton)
            }
        }


    }


    private fun getCorrectlyOrientedBitmap(uri: Uri): Bitmap? {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val exifInterface = ExifInterface(requireContext().contentResolver.openInputStream(uri)!!)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        Log.d("VoiceLabPhotoFragment", "Photo orientation: $orientation")

        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            ExifInterface.ORIENTATION_TRANSPOSE -> transposeImage(bitmap)
            else -> bitmap // Diğer durumlar için döndürme yapmıyoruz
        }

        return rotatedBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun transposeImage(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        matrix.postScale(-1f, 1f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun updateSeekBar(mediaPlayer: MediaPlayer, seekBar: SeekBar, playButton: ImageView){
        seekBar.progress = mediaPlayer.currentPosition
        handler.postDelayed({
            if (mediaPlayer.isPlaying) {
                updateSeekBar(mediaPlayer, seekBar, playButton)
            } else {
                playButton.setImageResource(R.drawable.icon_play) // Reset to play icon when finished
            }
        }, 1000)

    }

    private fun resetPlayButtonIcons() {
        design.playBtn1.setImageResource(R.drawable.icon_play)
        design.playBtn2.setImageResource(R.drawable.icon_play)
        design.playBtn3.setImageResource(R.drawable.icon_play)
        design.playBtn4.setImageResource(R.drawable.icon_play)
    }

    override fun onDestroy() {
        super.onDestroy()
        currentMediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }


}