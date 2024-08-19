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
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentVoiceLabFormatBinding



class VoiceLabFormatFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabFormatBinding
    private lateinit var imageUri: Uri
    private lateinit var name:String
    private lateinit var typeClone:String

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
        imageUri = bundle.image
        name = bundle.name

        design.cloneVoiceBtn.setOnClickListener {
            selectButton(design.cloneVoiceBtn)
            deselectButton(design.uploadVoiceBtn)
            design.continueBtn.text = "Start Cloning"
            typeClone="clone"

        }


        design.uploadVoiceBtn.setOnClickListener {
            selectButton(design.uploadVoiceBtn)
            deselectButton(design.cloneVoiceBtn)
            design.continueBtn.text = "Upload Voice"
            typeClone="upload"
        }

        design.continueBtn.setOnClickListener{
            if(typeClone =="clone"){
                val action = VoiceLabFormatFragmentDirections.actionVoiceLabFormatFragmentToVoiceLabRecordFragment(name,imageUri)
                findNavController().navigate(action)
            }else{
                //
            }
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