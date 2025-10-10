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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentSettingsBinding
import com.proksi.kotodama.BaseFragment
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.RCPlacement
import com.proksi.kotodama.objects.EventLogger
import com.proksi.kotodama.utils.DialogUtils
import com.proksi.kotodama.viewmodel.PaywallViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


class SettingsFragment : BaseFragment() {

    private lateinit var design: FragmentSettingsBinding
    private lateinit var dataStoreManager: DataStoreManager
    private var uid: String? = null
    private val viewModelPaywall: PaywallViewModel by activityViewModels()

    override val paywallComposeView: ComposeView
        get() = design.paywallComposeView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentSettingsBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager

        setupUi()

        lifecycleScope.launch {
            uid = dataStoreManager.getUid(requireContext()).firstOrNull()
        }

        lifecycleScope.launch {
            dataStoreManager.getSubscriptionStatusKey(this@SettingsFragment.requireContext()).collect { isActive ->
                if (isActive) {
                    design.linearLayout2.visibility = View.GONE
                    design.textView9.visibility = View.GONE
                }
            }
        }

        EventLogger.logEvent(requireContext(), "settings_screen_shown")

        design.textViewUpgrade.setOnClickListener{
            showPaywall(RCPlacement.SETTING)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        return design.root
    }
    private fun showFeedbackDialog(){


        val manager = ReviewManagerFactory.create(requireContext())
        EventLogger.logEvent(requireContext(), "settings_screen_shown")


        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(requireActivity(),reviewInfo)
                Log.d("showFeedbackDialog settings", "success $reviewInfo ")

            } else {
                @ReviewErrorCode val reviewErrorCode = (task.getException() as ReviewException).errorCode
                Log.d("showFeedbackDialog settings", "success $reviewErrorCode")

            }
        }
    }
    private fun sendEmail() {
        val recipient = "support@kotodama.app" // Alıcı e-posta adresi
        val subject = "Support Request - $uid" // E-posta konusu
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



    private fun setupUi(){
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

        design.languageLayout.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_languageFragment)
        }

        design.rateLayout.setOnClickListener{
            showFeedbackDialog()
        }

        design.shareSongLayout.setOnClickListener{


            val shareLink = "https://play.google.com/store/apps/details?id=com.kotodama.tts"

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareLink)
                type = "text/plain"
            }

            val chooser = Intent.createChooser(shareIntent, "Uygulama ile paylaş")
            startActivity(chooser)
        }


        design.settingsBackBtn.setOnClickListener(){
            findNavController().navigate(R.id.action_settingsFragment_to_homeFragment)
        }

        design.referLayout.setOnClickListener{
            EventLogger.logEvent(requireContext(), "friends_settings_screen_shown")

            findNavController().navigate(R.id.action_settingsFragment_to_referFragment)
        }
    }

}