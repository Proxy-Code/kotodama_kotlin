package com.proksi.kotodama.fragments

import android.content.Intent
import android.graphics.Color
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentPaywallBinding
import com.kotodama.tts.databinding.FragmentPaywallChristmasBinding
import com.proksi.kotodama.MainActivity
import com.proksi.kotodama.adapters.FaqAdapter
import com.proksi.kotodama.adapters.ReviewAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.Faqs
import com.proksi.kotodama.models.Review
import com.proksi.kotodama.objects.EventLogger
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.launch

class PaywallFragment : Fragment() {

    private lateinit var design: FragmentPaywallBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var mostPackage: Package
    private lateinit var normalPackage: Package
    private lateinit var finalPackage: Package
    private val TAG = PaywallFragment::class.java.simpleName
    private lateinit var mostPopularButton:TextView
    private lateinit var normalPlanButton:TextView
    private lateinit var dataStoreManager: DataStoreManager
    private var packageType: String = ""
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private var planType=false
    private lateinit var firebaseAnalytics: FirebaseAnalytics



    private val reviews = listOf(
        Review(R.drawable.boy, "sdfsf","fsfsfs","Great app, I love it!"),
        Review(R.drawable.boy, "sdfsf","fsfsfs", "Amazing features!"),
        Review(R.drawable.boy, "sdfsf","fsfsfs", "Very useful and easy to use."),
        Review(R.drawable.boy, "sdfsf","fsfsfs", "Fantastic user experience.")
    )

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentPaywallBinding.inflate(inflater, container, false)
        recyclerView = design.viewPagerReview
        dataStoreManager = DataStoreManager

        EventLogger.logEvent(requireContext(), "paywall_first_screen_shown")
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val eventBundle = Bundle().apply {
            putString("paywall_first_screen_shown", "paywall_first_screen_shown")
        }
        firebaseAnalytics.logEvent("paywall_first_screen_shown", eventBundle)


        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        mostPopularButton=design.textView15
        normalPlanButton=design.textView152

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 saat
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val adapter = ReviewAdapter(reviews)
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        design.faqRv.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val faqList = ArrayList<Faqs>()
        val f1=Faqs(R.string.whenWillMulti,
            R.string.support_multiple,false)
        val f2=Faqs(R.string.howDoI,R .string.youCanCancelYour,false)
        val f3=Faqs(R.string.howDoIRequest,R.string.sinceAllTransactions,false)

        faqList.add(f1)
        faqList.add(f2)
        faqList.add(f3)
        val faqsAdapter = FaqAdapter(this.requireContext(),faqList)
        design.faqRv.adapter=faqsAdapter


        val normalLayout: ConstraintLayout = design.normalLayout
        val mostLayout: ConstraintLayout = design.mostLayout

        val layouts = listOf( normalLayout, mostLayout)

        design.restoreButtonLayout.setOnClickListener{
            restorePurchases()
            showLoadingState(true)
        }

        fun resetBackgrounds() {
            layouts.forEach { layout ->
                layout.setBackgroundResource(R.drawable.radius14_bg_white)
            }
        }

        fun setOnClickListener(layout: ConstraintLayout, type: String) {
            layout.setOnClickListener {
                resetBackgrounds() // Tüm arka planları sıfırla
                layout.setBackgroundResource(R.drawable.radius14_bg_white_purple) // Seçili arka planı ayarla
            //    layout.setBackgroundResource(R.drawable.radius14_bg_christmas)

                packageType = type // Package type'ı ayarla
            }
        }

        setOnClickListener(normalLayout, "normal")
        setOnClickListener(mostLayout, "most")

        design.getPremiumButton.isEnabled = packageType != ""

        design.getPremiumButton.setOnClickListener(){
           // EventLogger.logEvent(requireContext(), "rc_initial_purchase_event")
            EventLogger.logEvent(requireContext(), "paywallFirst_getPremium_click")
            firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
            val eventBundle = Bundle().apply {
                putString("paywallFirst_getPremium_click", "paywallFirst_getPremium_click")
            }
            firebaseAnalytics.logEvent("paywallFirst_getPremium_click", eventBundle)
            selectPackage(packageType)
        }

        design.closeButton.setOnClickListener{
            navigateToHomeFragment()
        }
//
       fetchAndDisplayOfferings()

 //       fetchRemoteConfig()

        normalLayout.performClick()


        return design.root
    }

    private fun fetchAndDisplayOfferings() {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val currentOfferings = offerings.current
                if (currentOfferings != null) {
                    val availablePackages = currentOfferings.availablePackages

                    lifecycleScope.launch {
                        for (pkg in availablePackages) {
                            when (pkg.product.id) {

                                "subscription_annual:subs" -> {
                                    normalPackage = pkg
                                     //   mostPopularButton.text = pkg.product.title.split("(")[0].trim()
                                        design.mostPrice.text = pkg.product.price.formatted

                                }
                                "subscription_weekly:subs" -> {
                                    mostPackage = pkg
                                  //  normalPlanButton.text = pkg.product.title.split("(")[0].trim()
                                    design.normalPrice.text = pkg.product.price.formatted

                                }
                                "subscription_final_offer:subs" -> {
                                    finalPackage = pkg
                                }

                            }
                        }
                        design.getPremiumButton.isEnabled = true

                    }
                } else {
                    Log.d("Offerings", "No offerings available")
                }
            }

            override fun onError(error: PurchasesError) {
                Log.e("Offerings", "Error fetching offerings: ${error.message}")
            }
        })
    }

    private fun selectPackage(packageType: String) {
        Log.d("PaywallFragment", "Selecting package: $packageType $planType" )

        when (packageType) {

            "normal" -> {
                    handleSelectedPackage(mostPackage) //yearly
            }
            "most" -> {
                handleSelectedPackage(normalPackage)  //weekly
            }
        }
    }

    private fun handleSelectedPackage(selectedPackage: Package) {
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(requireActivity(), selectedPackage).build(),
            onError = { error, userCancelled ->
                Log.e("PurchaseError", "Purchase failed: $error, userCancelled: $userCancelled")
            },
            onSuccess = { storeTransaction, customerInfo ->
                Log.d(TAG, "handleSelectedPackage: $storeTransaction ")
                Log.d(TAG, "handleSelectedPackage: $customerInfo ")
                if (customerInfo.entitlements["Subscription"]?.isActive == true) {
                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(requireContext(), true)
                        navigateToHomeFragment()
                    }
                }
            }
        )
    }


    private fun navigateToHomeFragment() {
        Intent(requireContext(), MainActivity::class.java).also {
            // Start MainActivity
            startActivity(it)
            // Optional: If you want to clear the back stack so the user can't return to PaywallActivity
            requireActivity().finishAffinity()
        }

    }

    private fun restorePurchases() {
        Purchases.sharedInstance.restorePurchasesWith(
            onSuccess = { customerInfo ->
                val entitlement = customerInfo.entitlements["Subscription"]
                if (entitlement?.isActive == true) {

                    Log.d("Restore", "Subscription restored successfully")

                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(requireContext(), true)
                        navigateToHomeFragment() // Geri yüklendikten sonra kullanıcıyı yönlendir
                    }
                    showLoadingState(false)

                } else {
                    Log.d("Restore", "No active subscription found")
                    showNoSubscriptionDialog()
                    showLoadingState(false)
                }
            },
            onError = { error ->
                Log.e("Restore", "Error restoring purchases: ${error.message}")
                Toast.makeText(requireContext(), "Error restoring purchases: ${error.message}", Toast.LENGTH_LONG).show()
                showLoadingState(false)
            }
        )
    }

    private fun showLoadingState(isLoading: Boolean) {
        if (isLoading) {
            design.restoreBtn.visibility = View.GONE
            design.restoreProgressBar.visibility = View.VISIBLE
        } else {
            design.restoreBtn.visibility = View.VISIBLE
            design.restoreProgressBar.visibility = View.GONE
        }
    }

    private fun showNoSubscriptionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Subscription Not Found")
            .setMessage("We couldn't find active subscription. Contact us if problem continues.")
            .setPositiveButton("Okay") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun fetchRemoteConfig() {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val planVariation = firebaseRemoteConfig.getBoolean("show_yearly")
                    planType = planVariation
                    lifecycleScope.launch {
                        dataStoreManager.savePlanType(requireContext(),planVariation)
                    }
                    Log.d(TAG, "fetchRemoteConfig: $planVariation")
                //    fetchAndDisplayOfferings(planVariation)
                } else {
                    Log.e("RemoteConfig", "Failed to fetch Remote Config")
                }
            }
    }


}