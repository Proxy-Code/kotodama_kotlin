package com.proksi.kotodama.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentHomeBinding
import com.kotodama.tts.databinding.FragmentReferBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.objects.CloudFunction
import kotlinx.coroutines.launch

class ReferFragment : Fragment() {

    private lateinit var design: FragmentReferBinding
    private lateinit var dataStoreManager: DataStoreManager
    private val cloudFunction = CloudFunction()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentReferBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager

        lifecycleScope.launch {
            dataStoreManager.getReferral(requireContext()).collect { item ->
                design.editText.text = item

                design.inviteBtn.setOnClickListener {
                    val shareText = " Referreal Code:$item\n Check this out this amazing app! https://kotodama.onelink.me/gduU/invite"

                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Choose the app for sharing"))
                }
            }
        }

        Glide.with(this)
            .asGif()
            .load(R.drawable.hands)
            .into(design.playButton)

        design.enterCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    design.confirmButton.setBackgroundResource(R.drawable.create_btn_active)
                    design.confirmButton.isEnabled = true
                } else {
                    design.confirmButton.setBackgroundResource(R.drawable.create_btn_inactive)
                    design.confirmButton.isEnabled = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        design.backButton.setOnClickListener{
            findNavController().navigate(R.id.action_referFragment_to_settingsFragment)
        }


        design.confirmButton.setOnClickListener {
            // Hide the confirm text and show the progress bar
            design.confirmText.visibility = View.GONE
            design.consirmProgressBar.visibility = View.VISIBLE

            val enteredCode = design.enterCode.text.toString()
            val data = mapOf("referralCode" to enteredCode)
            cloudFunction.callEnterReferral(data) { result ->
                // Hide the progress bar and show the confirm text again
                design.consirmProgressBar.visibility = View.GONE
                design.confirmText.visibility = View.VISIBLE

                result.onSuccess {
                    Toast.makeText(requireContext(), "Send successfully!", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), " ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }




        return design.root
    }

}