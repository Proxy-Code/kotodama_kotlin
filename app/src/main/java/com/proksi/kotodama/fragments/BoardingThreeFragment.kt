package com.proksi.kotodama.fragments

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.navigation.fragment.findNavController
import com.proksi.kotodama.R

class BoardingThreeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_onboarding_three, container, false)

        val videoView = view.findViewById<VideoView>(R.id.videoView)

        val textView = view.findViewById<TextView>(R.id.voiceChanger)

        // Gradient colors
        val colors = intArrayOf(
            Color.parseColor("#D192D7"), // 80%
            Color.parseColor("#D29996"), // 100%
            Color.parseColor("#A475E1")  // 100%
        )

        // Start and end positions of the gradient
        val positions = floatArrayOf(0.0f, 0.5f, 1.0f)

        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())

        val textShader = LinearGradient(
            0f, 0f, width, textView.textSize,
            colors, positions, Shader.TileMode.CLAMP
        )

        textView.paint.shader = textShader

        if (videoView != null) {
            val videoUri = Uri.parse("android.resource://" + requireContext().packageName + "/" + R.raw.video3)
            videoView.setVideoURI(videoUri)
            videoView.start()
        }

        view.findViewById<TextView>(R.id.continueBtn).setOnClickListener{
            findNavController().navigate(R.id.action_boardingThreeFragment_to_boardingFourFragment)
        }


        return view
    }

}