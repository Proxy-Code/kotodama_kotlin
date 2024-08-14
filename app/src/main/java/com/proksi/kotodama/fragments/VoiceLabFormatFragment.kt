package com.proksi.kotodama.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.proksi.kotodama.R
import com.proksi.kotodama.databinding.FragmentVoiceLabFormatBinding
import com.proksi.kotodama.databinding.FragmentVoiceLabPhotoBinding
import com.proksi.kotodama.databinding.FragmentVoiceLabRecordBinding

class VoiceLabFormatFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabFormatBinding
    private lateinit var photoUri: Uri
    private lateinit var name:String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentVoiceLabFormatBinding.inflate(inflater, container, false)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        design.backButtonName.setOnClickListener(){
            val action = VoiceLabFormatFragmentDirections.actionVoiceLabFormatFragmentToVoiceLabPhotoFragment(name)
            findNavController().navigate(action)

        }
        val bundle: VoiceLabFormatFragmentArgs by navArgs()
        photoUri = bundle.image
        name = bundle.name

        design.cloneVoiceBtn.setOnClickListener {
            selectButton(design.cloneVoiceBtn)
            deselectButton(design.uploadVoiceBtn)
            design.continueBtn.text = "Start Cloning"
        }

        // İkinci butona tıklanma olayı
        design.uploadVoiceBtn.setOnClickListener {
            selectButton(design.uploadVoiceBtn)
            deselectButton(design.cloneVoiceBtn)
            design.continueBtn.text = "Upload Voice"
        }

        return design.root
    }

    private fun selectButton(button: LinearLayout) {
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_gradient)
        design.continueBtn.setBackgroundResource(R.drawable.radius15_bg_gradient)
    }

    private fun deselectButton(button: LinearLayout) {
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_c8c8)
    }
}