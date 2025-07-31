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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.fragment.findNavController
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentOneboardingTwoBinding
import com.proksi.kotodama.objects.EventLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class BoardingTwoFragment : Fragment() {

    private lateinit var design:FragmentOneboardingTwoBinding
    private var currentIndex = 0
    private var job: Job? = null
    private val flagList = listOf(
        R.drawable.flags_1,
        R.drawable.flags_2,
        R.drawable.flags_3,
        R.drawable.flags_4,
        R.drawable.flags_5,
        R.drawable.flags_6,
        R.drawable.flags_7,
        R.drawable.flags_8,
        R.drawable.flags_9,
        R.drawable.flags_10,
        R.drawable.flags_11,
        R.drawable.flags_12,
        R.drawable.flags_13,
        R.drawable.flags_14,
        R.drawable.flags_15,
        R.drawable.flags_16,
        R.drawable.flags_17,
        R.drawable.flags_18,
        R.drawable.flags_19
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        design = FragmentOneboardingTwoBinding.inflate(inflater,container,false)

        EventLogger.logEvent(requireContext(), "onboardingv2_screen_shown")

        design.continueBtn.setOnClickListener{
            findNavController().navigate(R.id.action_boardingTwoFragment_to_boardingThreeFragment)
        }

        animateViewsSequentially()
        startFlagRotation()
        return design.root
    }


    private fun animateViewsSequentially() {
        val views = listOf(
            design.imgGirl

        )

        for ((index, view) in views.withIndex()) {
            view.alpha = 0f
            view.translationY = 400f
            view.visibility = View.VISIBLE

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 400).toLong()) // sırayla gecikme
                .setDuration(400)
                .start()
        }
    }

    private fun startFlagRotation() {
        job = lifecycleScope.launch {
            while (isActive) {
                design.flagsImg.setImageResource(flagList[currentIndex])
                currentIndex = (currentIndex + 1) % flagList.size
                delay(300L)

                // En son resme geldiğinde, sadece doğru fragment'taysan geçiş yap
                if (currentIndex == flagList.size - 1) {
                    val navController = findNavController()
                    if (isAdded && navController.currentDestination?.id == R.id.boardingTwoFragment) {
                        navController.navigate(R.id.action_boardingTwoFragment_to_boardingThreeFragment)
                    }
                    break // bu satır olmazsa coroutine sonsuza kadar çalışır
                }
            }
        }
    }





}