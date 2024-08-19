package com.proksi.kotodama.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.kotodama.app.R
import com.proksi.kotodama.MainActivity


class BoardingFourFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_boarding_four, container, false)
        view.findViewById<TextView>(R.id.continueBtn).setOnClickListener{
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)

            requireActivity().finish()
        }
        return view
    }

}