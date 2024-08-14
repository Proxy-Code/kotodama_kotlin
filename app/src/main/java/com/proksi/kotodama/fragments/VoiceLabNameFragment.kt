package com.proksi.kotodama.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.proksi.kotodama.R
import com.proksi.kotodama.databinding.FragmentHomeBinding
import com.proksi.kotodama.databinding.FragmentVoiceLabFormatBinding
import com.proksi.kotodama.databinding.FragmentVoiceLabNameeBinding

class VoiceLabNameFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabNameeBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabNameeBinding.inflate(inflater, container, false)

        design.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            true
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        design.backButtonName.setOnClickListener(){
            findNavController().navigate(R.id.action_voiceLabNameFragment_to_homeFragment)
        }

        design.nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s.isNullOrEmpty()) {
                    design.continueBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_c8c8)
                    design.continueBtn.isEnabled=false
                } else {
                    design.continueBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_gradient)
                    design.continueBtn.isEnabled=true
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        design.continueBtn.setOnClickListener {
            val voiceName = design.nameInput.text.toString()
            saveVoiceName(voiceName)
        }

        return design.root
    }

    @SuppressLint("ServiceCast")
    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
    private fun saveVoiceName(name: String) {
        val action= VoiceLabNameFragmentDirections.actionVoiceLabNameFragmentToVoiceLabPhotoFragment(name)
        findNavController().navigate(action)
    }

}