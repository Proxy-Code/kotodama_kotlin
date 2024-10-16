package com.proksi.kotodama.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentSettingsBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.utils.DialogUtils
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    private lateinit var design: FragmentSettingsBinding
    private val dialogUtils = DialogUtils()
    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentSettingsBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager

        lifecycleScope.launch {
            dataStoreManager.getSubscriptionStatusKey(this@SettingsFragment.requireContext()).collect { isActive ->
                if (isActive) {
                    design.linearLayout2.visibility = View.GONE
                    design.textView9.visibility = View.GONE
                }
            }
        }

        design.settingsBackBtn.setOnClickListener(){
            findNavController().navigate(R.id.action_settingsFragment_to_homeFragment)
        }

        design.textViewUpgrade.setOnClickListener{
            dialogUtils.showPremiumDialogBox(requireContext(), viewLifecycleOwner,lifecycleScope,
                dataStoreManager)
        }

        design.privacyLayout.setOnClickListener{
                val url = "https://www.kotodama.app/privacy"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
        }

        design.termsLayout.setOnClickListener{
            val url = "https://www.kotodama.app/terms"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        design.mailSupportLayout.setOnClickListener(){
            sendEmail()
        }

        design.languageLayout.setOnClickListener(){
            findNavController().navigate(R.id.action_settingsFragment_to_languageFragment)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        return design.root
    }

    private fun sendEmail() {
        val recipient = "singai@proksiyazilim.com" // Alıcı e-posta adresi
        val subject = "Subject Text" // E-posta konusu
        val message = "Email message text" // E-posta içeriği

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(requireContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show()
        }
    }

}