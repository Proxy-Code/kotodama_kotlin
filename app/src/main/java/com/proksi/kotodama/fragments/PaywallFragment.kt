package com.proksi.kotodama.fragments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.proksi.kotodama.R
import com.proksi.kotodama.adapters.CategoryAdapter
import com.proksi.kotodama.adapters.FaqAdapter
import com.proksi.kotodama.adapters.VoicesAdapter
import com.proksi.kotodama.databinding.FragmentHomeBinding
import com.proksi.kotodama.databinding.FragmentPaywallBinding
import com.proksi.kotodama.models.Faqs

class PaywallFragment : Fragment() {

    private lateinit var design: FragmentPaywallBinding

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentPaywallBinding.inflate(inflater, container, false)

        design.faqRv.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        val faqList = ArrayList<Faqs>()
        val f1=Faqs(R.string.when_will_multilingual_service_supported,R.string.our_ai_already_supports_multiple_languages,false)
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