package com.proksi.kotodama.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentVoiceLabNameeBinding
import com.proksi.kotodama.viewmodel.CloneViewModel


class VoiceLabFirstFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabNameeBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabNameeBinding.inflate(inflater, container, false)

        design.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_voiceLabNameFragment_to_homeFragment)
        }
        design.continueButton.setOnClickListener {
           findNavController().navigate(R.id.action_voiceLabNameFragment_to_voiceLabPhotoFragment)
        }

        return design.root
    }

}