package com.proksi.kotodama.fragments.Studio

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentStudioCharacterBinding
import com.proksi.kotodama.adapters.CategoryAdapter
import com.proksi.kotodama.adapters.studio.CharacterAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.viewmodel.HomeViewModel
import com.proksi.kotodama.viewmodel.PaywallViewModel
import com.proksi.kotodama.viewmodel.StudioViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class StudioCharacterFragment : Fragment() {

    private lateinit var design:FragmentStudioCharacterBinding
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapterVoice: CharacterAdapter
    private val viewModelStudio: StudioViewModel by activityViewModels()
    private lateinit var dataStoreManager: DataStoreManager
    private var isSubscribed by Delegates.notNull<Boolean>()
    private val viewModelPaywall: PaywallViewModel by activityViewModels()
    private var isShowSlotPaywall : Boolean= false
    private var cloningRights : Number? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentStudioCharacterBinding.inflate(inflater,container,false)

        dataStoreManager = DataStoreManager

        lifecycleScope.launch {
            dataStoreManager.getSubscriptionStatusKey(this@StudioCharacterFragment.requireContext()).collect { isActive ->
                isSubscribed = isActive
            }
        }

        design.recyclerViewCategories.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        design.recyclerViewVoices.layoutManager=
            GridLayoutManager(requireContext(),3, GridLayoutManager.VERTICAL,false)

        design.closeBtn.setOnClickListener {
            findNavController().navigate(R.id.action_studioCharacterFragment_to_studioTextFragment)
        }

        setupVoicesAdapter()

        return design.root
    }

    private fun updateIsShowSlotPaywall(cloneCount: Int) {
        val rights = cloningRights?.toInt()
        if (rights != null) {
            isShowSlotPaywall = (rights <= cloneCount )
        } else {
            isShowSlotPaywall = false
        }
    }


    private fun setupVoicesAdapter() {


        adapterVoice = CharacterAdapter(requireContext(), emptyList()){

            if (it.id == "create_voice" && isSubscribed) {
                if (isShowSlotPaywall){
                    callPaywall("slot")
                }else {
                    findNavController().navigate(R.id.action_studioCharacterFragment_to_voiceLabNameFragment)
                }
            } else if (it.id == "create_voice") {
                callPaywall("subs")
            }else{
                viewModelStudio.setVoice(it)
                findNavController().navigate(R.id.action_studioCharacterFragment_to_studioTextFragment)
            }

        }

        design.recyclerViewVoices.adapter = adapterVoice


//        viewModel.data.observe(viewLifecycleOwner) { voicesList ->
//            if (voicesList != null) {
//                adapterVoice.updateData(voicesList)
//            } else {
//                Log.d("Observer", "Voices List is null")
//            }
//        }

        viewModel.fetchVoices("all", requireContext())
    }

    private fun callPaywall(item: String){
        design.paywallComposeView.disposeComposition()
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error -> Log.e("Paywall", "Error: $error") },
            onSuccess = { offerings ->

                val offeringId = when (item) {
                    "subs" -> "second_paywall_yearly_weekly"
                    "character" -> "extra-characters"
                    "slot" -> "Voice_Clone"
                    else -> null
                }
                val offering = offeringId?.let { offerings[offeringId] }

                design.paywallComposeView.apply {

                    setContent {
                        val paywallState by viewModelPaywall.paywallState.collectAsState()

                        if (paywallState is PaywallViewModel.PaywallState.Visible) {
                            if (offering != null) {
                                RevenueCatPaywall(
                                    offering,
                                    onDismiss = { viewModelPaywall.hidePaywall()
                                        design.paywallComposeView.visibility= View.GONE
                                    }
                                )
                            }
                        }
                    }
                }
                viewModelPaywall.showPaywall()
            }
        )
    }

    @Composable
    private fun RevenueCatPaywall(offering: Offering, onDismiss: () -> Unit) {
        val listener = remember {
            object : PaywallListener {
                override fun onPurchaseCompleted(customerInfo: CustomerInfo, transaction: StoreTransaction) {
                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(requireContext(), true)
                    }
                    onDismiss()
                }
                override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                    if (customerInfo.entitlements["Subscription"]?.isActive == true) {
                        lifecycleScope.launch {
                            dataStoreManager.saveSubscriptionStatus(requireContext(), true)
                        }
                        onDismiss()
                    }
                }
            }
        }

        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setRequiredEntitlementIdentifier("Subscription")
                .setOffering(offering)
                .setListener(listener)
                .build()
        )
    }

    private fun getUidFromDataStore(context: Context) {
        lifecycleScope.launch {
            DataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    val userDocRef = firestore.collection("users").document(uid)
                    Log.d("uid", "getUidFromDataStore: $uid")
                    userDocRef.addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("Home Firestore", "Error listening to user document: ", exception)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            cloningRights = snapshot.getLong("cloningRights")
                            }
                            if (isAdded && view != null) {
                                viewModel.cloneCount.observe(viewLifecycleOwner) { cloneCount ->
                                    if (cloneCount != null) {
                                        updateIsShowSlotPaywall(cloneCount)
                                    }
                                }
                            }
                        }


                    }
                }
            }
    }



}