package com.proksi.kotodama.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.proksi.kotodama.R
import com.proksi.kotodama.databinding.FragmentSettingsBinding
import com.proksi.kotodama.databinding.FragmentVoiceLabPhotoBinding

class SettingsFragment : Fragment() {

    private lateinit var design: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentSettingsBinding.inflate(inflater, container, false)

        design.settingsBackBtn.setOnClickListener(){
            findNavController().navigate(R.id.action_settingsFragment_to_homeFragment)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        return design.root
    }

}