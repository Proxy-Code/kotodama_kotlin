package com.proksi.kotodama.fragments.Studio

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.LeadingMarginSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentStudioTextBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.ConversationModel
import com.proksi.kotodama.models.VoiceModel
import com.proksi.kotodama.utils.DialogUtils
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.properties.Delegates

class StudioTextFragment : Fragment() {

    private lateinit var design:FragmentStudioTextBinding
    private var remainingCount = 150
    private var initialRemainingCount = 5000
    private var enteredText: String = ""
    private val viewModel: StudioViewModel by activityViewModels()
    private var studioId:String = ""
    private var item:VoiceModel? = null
    private var conversation:ConversationModel? = null
    private val dialogUtils = DialogUtils()
    private lateinit var dataStoreManager: DataStoreManager
    private var isSubscribed by Delegates.notNull<Boolean>()
    private var remainingCharacters : Number? = null
    private var additionalCount: Number? = null
    private var remainingRights : Number? = null
    private var cloningRights : Number? = null
    private var userId:String = ""
    private val viewModelPaywall: PaywallViewModel by activityViewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentStudioTextBinding.inflate(inflater,container,false)

        studioId = viewModel.getStudioId().toString()
        dataStoreManager = DataStoreManager

        item = viewModel.getVoice()

        updateCreateButtonState()

        conversation = viewModel.getConversation()

        lifecycleScope.launch {
            dataStoreManager.getSubscriptionStatusKey(this@StudioTextFragment.requireContext()).collect { isActive ->
                isSubscribed = isActive
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })

        setupUI()
        setupInfoList()
        setupEditText()
        getUidFromDataStore(requireContext())

        return design.root
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {

        item?.let {
            design.chooseText.text = item?.name
            design.chooseImgEmpty.visibility = View.GONE
            design.imgChoosen.visibility = View.VISIBLE
            Glide.with(this)
                .load(item?.imageUrl)
                .placeholder(R.drawable.icon_kota)
                .error(R.drawable.icon_kota)
                .into(design.imgChoosen)
        }?: run {
            design.chooseImgEmpty.visibility = View.VISIBLE
            design.imgChoosen.visibility = View.GONE
            design.chooseText.text = "Choose Character"
        }

        conversation?.let {
            design.chooseText.text = ""
            design.chooseImgEmpty.visibility = View.GONE
            design.imgChoosen.visibility = View.VISIBLE
            Glide.with(this)
                .load(it.imageUrl)
                .placeholder(R.drawable.icon_kota)
                .error(R.drawable.icon_kota)
                .transform(RoundedCorners(16))
                .into(design.imgChoosen)
        }

        design.deleteBtn.setOnClickListener{
            deleteItem()
        }


        design.closeBtn.setOnClickListener {
            viewModel.setText("")
            viewModel.setVoice(null)
            viewModel.setConversation(null)
            findNavController().navigate(R.id.action_studioTextFragment_to_studioAddLineFragment)
        }

        design.chooseCharacterBtn.setOnClickListener{
            findNavController().navigate(R.id.action_studioTextFragment_to_studioCharacterFragment)
        }

        val lottieView = design.ltAnimation
        lottieView.addValueCallback(
            KeyPath("**", "Fill 1"),
            LottieProperty.COLOR,
            LottieValueCallback(Color.parseColor("#FFFFFF"))
        )
        lottieView.playAnimation()

        design.createBtn.setOnClickListener {
            design.createBtn.isEnabled = false // butonu devre dışı bırak
            design.textCreate.visibility = View.GONE
            design.ltAnimation.visibility = View.VISIBLE

            viewModel.setText("")
            item?.let { selectedVoice ->
                if(isSubscribed){
                    if(remainingCount>0){
                        createConversationEntry(studioId, enteredText, selectedVoice) {
                            requireActivity().runOnUiThread {
                                design.createBtn.isEnabled = true
                                design.textCreate.visibility = View.VISIBLE
                                design.ltAnimation.visibility = View.GONE
                            }

                        }
                    }else{
                        design.textCreate.visibility = View.VISIBLE
                        design.ltAnimation.visibility = View.GONE
                        callPaywall("character")
                    }
                } else{
                    design.textCreate.visibility = View.VISIBLE
                    design.ltAnimation.visibility = View.GONE
                    callPaywall("subs")
                }

            }
        }
        updateCreateButtonState()

    }

    private fun updateCreateButtonState() {

        val currentText = design.editTextLayout.text.toString().trim()
        val savedText = viewModel.getText()
        val currentVoice = viewModel.getVoice()

        val isButtonActive = (currentText.isNotBlank() || savedText!="" ) && currentVoice != null
        design.createBtn.isEnabled = isButtonActive

        val backgroundResource = if (isButtonActive) {
            R.drawable.create_btn_active
        } else {
            R.drawable.create_btn_inactive
        }
        design.createBtn.setBackgroundResource(backgroundResource)
    }


    private fun setupInfoList(){
        val infoList = listOf(
            "Studio would spend 2 time of your characters due to its complexity.",
            "Pressing “Delete” button would delete this text-to-speech if it is already generated.",
            "Pressing “Listen” button would not regenerate but plays the text-to-speech that was generated before.",
            "If you already generated text-to-speech before, pressing “Create” would replace your previous output."
        )

        val bulletGap = 30
        val spannableText = SpannableStringBuilder()

        infoList.forEach { item ->
            val bullet = "• $item\n\n"
            val start = spannableText.length
            spannableText.append(bullet)
            spannableText.setSpan(
                LeadingMarginSpan.Standard(bulletGap),
                start,
                spannableText.length,
                0
            )
        }

        design.infoTextView.text = spannableText

    }

    private fun createConversationEntry(
        studioId: String,
        text: String,
        selectedVoice: VoiceModel,
        onComplete: () -> Unit) {

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("Conversation", "Missing user or voice")
            onComplete()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val studioRef = db.collection("users").document(user.uid).collection("studio").document(studioId)
        val conversationRef = studioRef.collection("conversation")

        conversationRef.orderBy("order", Query.Direction.DESCENDING).limit(1).get()
            .addOnSuccessListener { snapshot ->
                val maxOrder = snapshot.documents.firstOrNull()?.getLong("order")?.toInt() ?: -1
                val newOrder = maxOrder + 1
                val newDocRef = conversationRef.document()

                val conversationData = mapOf(
                    "createdAt" to Timestamp.now(),
                    "imageUrl" to selectedVoice.imageUrl,
                    "isGenerating" to false,
                    "order" to newOrder,
                    "soundUrl" to "",
                    "sound_sample_id" to selectedVoice.id,
                    "text" to text
                )

                newDocRef.set(conversationData)
                    .addOnSuccessListener {
                        Log.d("Conversation", "Created conversation: ${newDocRef.id}")

                        // 4. Token al
                        user.getIdToken(true).addOnSuccessListener { result ->
                            val idToken = result.token
                            if (idToken == null) {
                                Log.e("Conversation", "ID token is null")
                                onComplete()
                                return@addOnSuccessListener
                            }

                            // 5. Sunucuya POST isteği
                            val payload = JSONObject().apply {
                                put("studio_id", studioId)
                                put("conversation_id", newDocRef.id)
                                put("idToken", idToken)
                            }

                            val client = OkHttpClient()
                            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
                            val request = Request.Builder()
                                .url("https://api.kotodama.app/studio/process")
                                .post(requestBody)
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    Log.e("Conversation", "Failed to trigger processing: ${e.localizedMessage}")
                                    onComplete()
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    Log.d("Conversation", "Process API response: ${response.code}")
                                    if (response.isSuccessful) {
                                        Log.d("Conversation", "Processing triggered successfully")
                                        onComplete()
                                        requireActivity().runOnUiThread {
                                            findNavController().navigate(R.id.action_studioTextFragment_to_studioAddLineFragment)
                                        }                                    }else{
                                        Log.d("Conversation", "${response.message}")
                                        onComplete()
                                    }
                                }
                            })
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Conversation", "Failed to create conversation: ${e.localizedMessage}")
                        onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Conversation", "Failed to fetch max order: ${e.localizedMessage}")
                onComplete()
            }
    }

    private fun hideKeyboard() {
        dialogUtils.hideKeyboard(requireActivity())
    }

    private fun setupEditText(){
        design.langCodeLayout.visibility = View.GONE

        conversation?.let {
            design.editTextLayout.setText(it.text)
            enteredText= it.text.toString()
        }?: run {
            enteredText  = viewModel.getText().toString()
            updateCreateButtonState()
            if(enteredText == "null" ){
                design.editTextLayout.setText("")
            }else{
                design.editTextLayout.setText(enteredText)
            }
        }

        design.editTextLayout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                enteredText = s.toString().trim()
                remainingCount = initialRemainingCount - (enteredText.length * 2)
                updateRemainingCountUI(remainingCount)
            }
            @SuppressLint("ResourceAsColor")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                enteredText = s.toString().trim()
                remainingCount = initialRemainingCount - (enteredText.length * 2)
                updateRemainingCountUI(remainingCount)
                updateCreateButtonState()
            }

            override fun afterTextChanged(s: Editable?) {
                enteredText = s.toString().trim()
                if (enteredText.isEmpty()) {
                    design.doneLayout.visibility = View.GONE
                    design.deleteLayout.visibility = View.GONE
                } else {
                    design.doneLayout.visibility = View.VISIBLE
                    design.deleteLayout.visibility = View.VISIBLE
                }

                viewModel.setText(enteredText)
                updateCreateButtonState()
            }
        })

        design.remaningCounterLayout.setOnClickListener{
            if (isSubscribed){
                callPaywall("character")
            } else {
                callPaywall("subs")
            }
        }

        design.doneLayout.setOnClickListener(){
            hideKeyboard()
            viewModel.setText(enteredText)
            design.langCodeLayout.visibility = View.GONE
            design.doneLayout.visibility = View.GONE
            design.deleteLayout.visibility = View.GONE
        }

        design.deleteLayout.setOnClickListener {

            design.editTextLayout.setText("")

            remainingCount = initialRemainingCount
            updateRemainingCountUI(remainingCount)

            design.langCodeLayout.visibility = View.GONE
            design.doneLayout.visibility = View.GONE

            viewModel.setVoice(null)

            updateCreateButtonState()
        }
    }

    private fun updateRemainingCountUI(remainingCount: Int) {

        if (remainingCount < 0) {
            design.remaningCount.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.progress_red))
        } else {
            design.remaningCount.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.black))
        }

        design.remaningCount.text = remainingCount.toString()

        updateCreateButtonState()
    }

    private fun getUidFromDataStore(context: Context) {
        lifecycleScope.launch {
            DataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    userId = uid
                    val firestore = FirebaseFirestore.getInstance()
                    val userDocRef = firestore.collection("users").document(uid)

                    userDocRef.addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            remainingCharacters = snapshot.getLong("remainingCount")
                            additionalCount = snapshot.getLong("additionalCount") ?: 0L
                            remainingRights = snapshot.getLong("remainingRights")
                            cloningRights = snapshot.getLong("cloningRights")


                            if (remainingRights != null) {
                                if (remainingRights == 0L) {
                                    if (!isSubscribed){
                                        remainingCount = 0
                                        design.remaningCount.text = remainingCount.toString()
                                    }else{
                                        remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                        design.remaningCount.text = remainingCount.toString()
                                    }

                                } else {
                                    if (remainingCharacters != null) {
                                        remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                    } else {
                                        remainingCount = 150
                                    }
                                    design.remaningCount.text = remainingCount.toString()
                                }
                            } else {
                                val data = hashMapOf(
                                    "remainingRights" to 3,
                                    "remainingCount" to 150,
                                )

                                userDocRef.set(data,SetOptions.merge())

                                if (remainingCharacters != null && additionalCount != null) {
                                    remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                } else {
                                    remainingCount = 150
                                }
                                design.remaningText.text = remainingCount.toString()
                            }

                        }

                        initialRemainingCount = remainingCount
                    }
                } else {
                    Log.d("HomeFragment", "No UID found in DataStore")
                }
            }
        }
    }

    fun deleteItem() {
        val db = FirebaseFirestore.getInstance()
        conversation?.id?.let {
            db.collection("users")
                .document(userId)
                .collection("studio")
                .document(studioId)
                .collection("conversation")
                .document(it)
                .delete()
                .addOnSuccessListener {
                    Log.d("DeleteItem", "DocumentSnapshot successfully deleted!")
                    findNavController().navigate(R.id.action_studioTextFragment_to_studioAddLineFragment)
                }
                .addOnFailureListener { e ->
                    Log.w("DeleteItem", "Error deleting document", e)
                }
        } ?: run {
            viewModel.setText("")

            findNavController().navigate(R.id.action_studioTextFragment_to_studioAddLineFragment)
        }

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

}