package com.proksi.kotodama.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.VideoView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.kotodama.app.R


class BoardingOneFragment : Fragment() {

    private var player: ExoPlayer? = null
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

            val view = inflater.inflate(R.layout.fragment_onboarding_one, container, false)
            val playerView = view.findViewById<PlayerView>(R.id.videoView)

            // Initialize ExoPlayer
            player = ExoPlayer.Builder(requireContext()).build()
            playerView.player = player

            // Prepare the media item
            val videoUri = "android.resource://" + requireContext().packageName + "/" + R.raw.video1
            val mediaItem = MediaItem.fromUri(videoUri)
            player?.setMediaItem(mediaItem)

            // Prepare and play the video
            player?.prepare()
            player?.play()


        val textView = view.findViewById<TextView>(R.id.voiceChanger)

        // Gradient colors
        val colors = intArrayOf(
            Color.parseColor("#D192D7"),
            Color.parseColor("#D29996"),
            Color.parseColor("#A475E1")
        )

        val positions = floatArrayOf(0.0f, 0.5f, 1.0f)

        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())

        val textShader = LinearGradient(
            0f, 0f, width, textView.textSize,
            colors, positions, Shader.TileMode.CLAMP
        )

        textView.paint.shader = textShader

        view.findViewById<TextView>(R.id.continueBtn).setOnClickListener{
            findNavController().navigate(R.id.action_boardingOneFragment_to_boardingTwoFragment)
        }
        return view
        }

        override fun onDestroy() {
            super.onDestroy()
            player?.release()
            player = null
        }




}