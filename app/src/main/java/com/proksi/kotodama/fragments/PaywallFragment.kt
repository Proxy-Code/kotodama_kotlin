package com.proksi.kotodama.fragments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.RequiresApi
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

class PaywallFragment : Fragment() {

    private lateinit var design: FragmentPaywallBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton

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

        return design.root
    }

}