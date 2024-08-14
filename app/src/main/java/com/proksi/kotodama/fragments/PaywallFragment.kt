package com.proksi.kotodama.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proksi.kotodama.R
import com.proksi.kotodama.adapters.FaqAdapter
import com.proksi.kotodama.databinding.FragmentHomeBinding
import com.proksi.kotodama.databinding.FragmentPaywallBinding
import com.proksi.kotodama.models.Faqs

class PaywallFragment : Fragment() {

    private lateinit var design: FragmentPaywallBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentPaywallBinding.inflate(inflater, container, false)


        val faqList = ArrayList<Faqs>()
        val f1=Faqs(R.string.when_will_multilingual_service_supported,R.string.our_ai_already_supports_multiple_languages,false)
        val f2=Faqs(R.string.how_cancel_my_subscription,R .string.you_can_cancel_your_subscription,false)
        val f3=Faqs(R.string.how_request_refund,R.string.since_all_transactions,false)

        faqList.add(f1)
        faqList.add(f2)
        faqList.add(f3)
        val faqsAdapter = FaqAdapter(requireContext(),faqList)
        return design.root
    }

}