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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentOnboardingThreeBinding
import com.proksi.kotodama.objects.EventLogger


class BoardingThreeFragment : Fragment() {

    private lateinit var design:FragmentOnboardingThreeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentOnboardingThreeBinding.inflate(inflater, container, false)

        EventLogger.logEvent(requireContext(), "onboardingv3_screen_shown")

        design.continueBtn.setOnClickListener{
            findNavController().navigate(R.id.action_boardingThreeFragment_to_boardingFourFragment)
        }

        animateViewsSequentially()

        return design.root
    }

    private fun animateViewsSequentially() {
        val views = listOf(
            design.box1,
            design.box2,
            design.box3,
            design.box4,
            design.box5,
            design.box6,
            design.continueBtn,
            design.textLast
        )

        for ((index, view) in views.withIndex()) {
            view.alpha = 0f
            view.translationY = 200f
            view.visibility = View.VISIBLE

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 300).toLong()) // sırayla gecikme
                .setDuration(400)
                .start()
        }
    }

}