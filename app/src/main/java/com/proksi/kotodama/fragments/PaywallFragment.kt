package com.proksi.kotodama.fragments

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
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentPaywallBinding
import com.proksi.kotodama.adapters.FaqAdapter
import com.proksi.kotodama.adapters.ReviewAdapter
import com.proksi.kotodama.models.Faqs
import com.proksi.kotodama.models.Review
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import kotlinx.coroutines.launch

class PaywallFragment : Fragment() {

    private lateinit var design: FragmentPaywallBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var mostPackage: Package
    private lateinit var normalPackage: Package
    private lateinit var lifetimePackage: Package
    private lateinit var finalPackage: Package
    private val TAG = PaywallFragment::class.java.simpleName
    private lateinit var mostPopularButton:TextView
    private lateinit var normalPlanButton:TextView
    private lateinit var lifetimeButton:TextView


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

        mostPopularButton=design.textView15
        normalPlanButton=design.textView152
        lifetimeButton=design.textView151

        val adapter = ReviewAdapter(reviews)
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        design.faqRv.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val faqList = ArrayList<Faqs>()
        val f1=Faqs(R.string.when_will_multilingual_service_supported,
            R.string.our_ai_already_supports_multiple_languages,false)
        val f2=Faqs(R.string.how_cancel_my_subscription,R .string.you_can_cancel_your_subscription,false)
        val f3=Faqs(R.string.how_request_refund,R.string.since_all_transactions,false)

        faqList.add(f1)
        faqList.add(f2)
        faqList.add(f3)
        val faqsAdapter = FaqAdapter(this.requireContext(),faqList)
        design.faqRv.adapter=faqsAdapter

        design.closeButton.setOnClickListener{
            findNavController().navigate(R.id.action_paywallFragment_to_homeFragment)
        }


        val lifetimeLayout: ConstraintLayout = design.lifetimeLayout
        val normalLayout: ConstraintLayout = design.normalLayout
        val mostLayout: ConstraintLayout = design.mostLayout

        val layouts = listOf(lifetimeLayout, normalLayout, mostLayout)
        fun resetBackgrounds() {
            layouts.forEach { layout ->
                layout.setBackgroundResource(R.drawable.radius14_bg_white)
            }
        }

        layouts.forEach { layout ->
            layout.setOnClickListener {
                resetBackgrounds() // Tüm arka planları sıfırla
                layout.setBackgroundResource(R.drawable.radius14_bg_white_purple) // Tıklanan layout'un arka planını değiştir
            }
        }




        fetchAndDisplayOfferings()


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

                            Log.d(TAG, "onReceived: ${pkg} ")
                            lifetimeButton.text=pkg.product.title

                            when (pkg.product.id) {
                                "subscription_annual:subs" -> {
                                    normalPackage = pkg
                                    normalPlanButton.text = pkg.product.title
                                    design.normalPrice.text = pkg.product.price.formatted
                                }
                                "life_time_offer" -> {
                                    lifetimePackage = pkg
                                    lifetimeButton.text = pkg.product.title
                                    design.lifetimeCampPrice.text = pkg.product.price.formatted
                                }
                                "subscription_weekly:subs" -> {
                                    mostPackage = pkg
                                    mostPopularButton.text = pkg.product.title
                                    design.mostPrice.text = pkg.product.price.formatted
                                }
                                "life_time_final" -> {
                                    finalPackage = pkg
                                }

                            }
                        }
                        design.getPremiumButton.isEnabled = true
                        Log.d(TAG, "getPremium clicked ")

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

}