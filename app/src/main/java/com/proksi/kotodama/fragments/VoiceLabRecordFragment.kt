package com.proksi.kotodama.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.proksi.kotodama.R
import com.proksi.kotodama.databinding.FragmentVoiceLabPhotoBinding
import com.proksi.kotodama.databinding.FragmentVoiceLabRecordBinding


class VoiceLabRecordFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabRecordBinding
    private lateinit var imageUri: Uri
    private lateinit var name:String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabRecordBinding.inflate(inflater, container, false)





        return design.root
    }

}