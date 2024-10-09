package com.proksi.kotodama.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentHomeBinding
import com.kotodama.app.databinding.FragmentNullFilesBinding
import com.proksi.kotodama.BaseFragment
import com.proksi.kotodama.viewmodel.UserLibraryViewModel


class NullFilesFragment : BaseFragment() {

    private lateinit var design: FragmentNullFilesBinding
    private val viewModel: UserLibraryViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentNullFilesBinding.inflate(inflater, container, false)

        viewModel.libraryItems.observe(viewLifecycleOwner, Observer { libraryItems ->
            Log.d("fetch items", "onCreateView: ")
            if (libraryItems.isEmpty()) {

            } else {


            }
        })

        viewModel.fetchUserLibrary(this.requireContext())

        return design.root

    }

}