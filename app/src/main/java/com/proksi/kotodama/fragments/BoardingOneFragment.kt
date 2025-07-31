package com.proksi.kotodama.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentOnboardingOneBinding
import com.proksi.kotodama.objects.EventLogger


class BoardingOneFragment : Fragment() {

    private lateinit var design:FragmentOnboardingOneBinding


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentOnboardingOneBinding.inflate(inflater,container,false)

        design.continueBtn.setOnClickListener{
            findNavController().navigate(R.id.action_boardingOneFragment_to_boardingTwoFragment)
        }

        animateViewsSequentially()
        return design.root
        }

    private fun animateViewsSequentially() {
        val views = listOf(
            design.images,
            design.textBox,
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