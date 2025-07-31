package com.proksi.kotodama.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentHomeBinding
import com.proksi.kotodama.MainActivity
import com.proksi.kotodama.adapters.CategoryAdapter
import com.proksi.kotodama.adapters.VoicesAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.VoiceModel
import com.proksi.kotodama.objects.EventLogger
import com.proksi.kotodama.retrofit.ApiClient
import com.proksi.kotodama.retrofit.ApiInterface
import com.proksi.kotodama.utils.DialogUtils
import com.proksi.kotodama.viewmodel.HomeViewModel
import com.proksi.kotodama.viewmodel.PaywallViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HomeFragment : Fragment() {

    private lateinit var design: FragmentHomeBinding
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapterVoice: VoicesAdapter
    private val dialogUtils = DialogUtils()
    private lateinit var dataStoreManager: DataStoreManager
    private var enteredText: String = ""
    private var selectedVoiceId: String? = ""
    private var imageUrl: String? = ""
    private var name: String? = ""
    private var isClone: Boolean? = true
    private val choices = listOf("en", "es", "fr", "de", "it", "pt", "pl", "tr", "ru", "nl", "cs", "ar", "zh", "hu", "ko", "hi","fil","sv","bg","ro","el","ms","hr","sk","da","uk","ta")
    private var isSubscribed:Boolean = false
    private val TAG = HomeFragment::class.java.simpleName
    private var remainingCount = 150
    private var remainingCharacters : Number? = null
    private var additionalCount: Number? = null
    private var remainingRights : Number? = null
    private var cloningRights : Number? = null
    private var tokenCounter = 3
    private var referralCode: String? = ""
    private var initialRemainingCount = 0
    private var hasCloneModel :Boolean = false
    private var isShowSlotPaywall : Boolean= false
    private var saveTextJob: Job? = null
    private var isFirstLoad:Boolean = true
    private val viewModelPaywall: PaywallViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentHomeBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager

        setupVoicesAdapter()

        EventLogger.logEvent(requireContext(), "home_screen_shown")

        viewModel.cloneCount.observe(viewLifecycleOwner) { cloneCount ->
            updateIsShowSlotPaywall(cloneCount)
        }

        design.recyclerViewCategories.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        design.recyclerViewVoices.layoutManager=
            GridLayoutManager(requireContext(),3, GridLayoutManager.VERTICAL,false)

        val categoryList = viewModel.getCategoryList()
        val adapterCategory = CategoryAdapter(this.requireContext(),categoryList)
        design.recyclerViewCategories.adapter=adapterCategory

        adapterCategory.setOnCategoryClickListener(object : CategoryAdapter.OnCategoryClickListener {
            override fun onCategoryClick(category: String) {
                viewModel.getVoicesByCategory(category, requireContext())
            }
        })


        if (isFirstLoad) {
            isFirstLoad = false
            design.progressBar.visibility = View.VISIBLE
            design.loadingOverlay.visibility = View.VISIBLE
            (requireActivity() as MainActivity).setBottomNavigationVisibility(false)

            viewModel.data.observe(viewLifecycleOwner) { voicesList ->
                if (voicesList != null && voicesList.isNotEmpty()) {
                    design.progressBar.visibility = View.GONE
                    design.loadingOverlay.visibility = View.GONE
                    (requireActivity() as MainActivity).setBottomNavigationVisibility(true)

                    adapterVoice.updateData(voicesList)
                } else {
                    Log.d(TAG, "Voices List is null or empty")
                }
            }
        } else {

            val voicesList = viewModel.data.value
            if (!voicesList.isNullOrEmpty()) {
                adapterVoice.updateData(voicesList)
            } else {
                Log.d(TAG, "Voices List is null or empty")
            }
        }

        viewModel.fetchVoices("all", requireContext())


        setupUI()


        return design.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getUidFromDataStore(requireContext())

        lifecycleScope.launch {
            dataStoreManager.getSubscriptionStatusKey(this@HomeFragment.requireContext()).collect { isActive ->
                if (isAdded) {
                    getUidFromDataStore(requireContext())
                }
                if (isAdded) {
                    isSubscribed = isActive

                    if (isActive) {
                        design.imageCrown.visibility = View.GONE
                        design.counterLayout.visibility = View.GONE
                        design.referButton.visibility = View.VISIBLE
                    } else{
                        design.imageCrown.visibility = View.VISIBLE
                        design.counterLayout.visibility = View.VISIBLE
                        design.referButton.visibility = View.GONE
                    }
                    // Only set up the voices adapter if the fragment is still attached
                    setupVoicesAdapter()
                } else {
                    Log.d(TAG, "Fragment not added or view is null, skipping UI update")
                }
            }
        }
    }

    private fun setupUI(){
        design.remaningCounterLayout.setOnClickListener{
            if (isSubscribed){
                callPaywall("character")
            } else {
                callPaywall("subs")
            }
        }

        design.doneLayout.setOnClickListener(){
            hideKeyboard()
            design.langCodeLayout.visibility = View.GONE
            design.doneLayout.visibility = View.GONE
            design.deleteLayout.visibility = View.GONE
        }

        design.editTextLayout.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            @SuppressLint("ResourceAsColor")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val enteredText = s.toString().trim()
                remainingCount = initialRemainingCount - enteredText.length
                updateRemainingCountUI(remainingCount)

                viewLifecycleOwner.lifecycleScope.launch {
                    val languageCode = recognizeLanguage(enteredText)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                enteredText = s.toString().trim()
                if (enteredText.isEmpty()) {
                    design.langCodeLayout.visibility = View.GONE
                    design.doneLayout.visibility = View.GONE
                    design.deleteLayout.visibility = View.GONE
                } else {
                    design.langCodeLayout.visibility = View.VISIBLE
                    design.doneLayout.visibility = View.VISIBLE
                    design.deleteLayout.visibility = View.VISIBLE
                }
                updateCreateButtonState()
            }
        })

        design.deleteLayout.setOnClickListener {

            design.editTextLayout.setText("")

            remainingCount = initialRemainingCount
            updateRemainingCountUI(remainingCount)

            design.langCodeLayout.visibility = View.GONE
            design.doneLayout.visibility = View.GONE

            updateCreateButtonState()
        }

        design.counterLayout.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_referFragment)
        }

        design.referButton.setOnClickListener{
            EventLogger.logEvent(requireContext(), "friends_home_screen_shown")
            findNavController().navigate(R.id.action_homeFragment_to_referFragment)
        }


        design.searchVoice.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText ?: ""
                viewModel.filterVoices(query)
                return true
            }
        })

        design.buttonCreate.setOnClickListener {
            EventLogger.logEvent(requireContext(), "home_create_click")

            if (remainingCount <= 0 ){
                if (isSubscribed){
                   callPaywall("character")
                } else{
                    callPaywall("subs")
                }
            }else {
                design.progressBar.visibility = View.VISIBLE
                design.loadingOverlay.visibility = View.VISIBLE
                (requireActivity() as MainActivity).setBottomNavigationVisibility(false)

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val idToken = getFirebaseIdToken()
                        if (idToken != null) {
                            val languageCode = recognizeLanguage(enteredText)

                            isClone?.let { it1 ->
                                sendProcessRequest(
                                    idToken,
                                    enteredText,
                                    selectedVoiceId,
                                    languageCode,
                                    imageUrl,
                                    name,
                                    it1
                                )
                            }
                        } else {
                            Log.e(TAG, "Firebase ID Token is null")
                        }
                    } catch (e: Exception) {
                        design.progressBar.visibility = View.GONE
                        design.loadingOverlay.visibility = View.GONE
                        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
                        Log.e(TAG, "Error fetching Firebase ID token", e)
                    }
                }
            }
        }

        design.imageCrown.setOnClickListener {
            callPaywall("subs")
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



    private fun updateIsShowSlotPaywall(cloneCount: Int) {
        val rights = cloningRights?.toInt()
        if (rights != null) {
            isShowSlotPaywall = (rights <= cloneCount )
        } else {
            isShowSlotPaywall = false
        }
    }


    private fun hideKeyboard() {
        dialogUtils.hideKeyboard(requireActivity())
    }

    private fun updateCreateButtonState() {

        val isButtonActive = enteredText.isNotBlank() && selectedVoiceId != ""

        design.buttonCreate.isEnabled = isButtonActive

        val backgroundResource = if (isButtonActive) R.drawable.create_btn_active else R.drawable.create_btn_inactive
        design.buttonCreate.setBackgroundResource(backgroundResource)
    }

    suspend fun getFirebaseIdToken(): String? = suspendCancellableCoroutine { cont ->
        val firebaseUser = Firebase.auth.currentUser
        firebaseUser?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result?.token)
            } else {
                cont.resumeWithException(task.exception ?: Exception("Unknown error"))
            }
        }
    }


    @SuppressLint("SetTextI18n", "SuspiciousIndentation")
    suspend fun recognizeLanguage(text: String): String {
        return suspendCancellableCoroutine { cont ->
            val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    if (languageCode != "und") {
                        val result = if (choices.contains(languageCode)) languageCode else "en"

                        val languageName = Locale(languageCode).displayLanguage // Dil adını al
                        design.langCode.text = languageName // Dil adını göster
                        design.langCodeImg.setImageResource(R.drawable.circle_green)
                        cont.resume(result)
                    } else {
                        cont.resume("en")
                        design.langCode.text="Unknown"
                        design.langCodeImg.setImageResource(R.drawable.circle_red)
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    cont.resume("en")
                    design.langCode.text="Unknown"
                    design.langCodeImg.setImageResource(R.drawable.circle_red)
                }
        }
    }

    fun sendProcessRequest(
        idToken: String,
        enteredText: String,
        selectedVoiceId: String?,
        languageCode: String,
        imageUrl: String?,
        name: String?,
        isClone: Boolean,
    ) {

        if (selectedVoiceId.isNullOrEmpty() || imageUrl.isNullOrEmpty() || name.isNullOrEmpty()) {
            Log.e(TAG, "Payload values are missing: selectedVoiceId=$selectedVoiceId, imageUrl=$imageUrl, name=$name")
            return
        }

        val payload = ApiInterface.ProcessRequest(
            text = enteredText,
            name = name,
            language_code = languageCode,
            isDefault = !isClone,
            sound_sample_id = selectedVoiceId,
            imageUrl = imageUrl,
            idToken = idToken
        )

        ApiClient.apiService.processRequest(payload).enqueue(object : retrofit2.Callback<ApiInterface.ProcessResponse> {
            override fun onResponse(
                call: Call<ApiInterface.ProcessResponse>,
                response: retrofit2.Response<ApiInterface.ProcessResponse>) {

                if (response.isSuccessful) {
                    design.progressBar.visibility = View.GONE
                    design.loadingOverlay.visibility = View.GONE
                    (requireActivity() as MainActivity).setBottomNavigationVisibility(true)

                    findNavController().navigate(R.id.action_homeFragment_to_libraryFragment)
                } else {
                    val errorMessage = response.errorBody()?.string()
                    design.progressBar.visibility = View.GONE
                    design.loadingOverlay.visibility = View.GONE
                    (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
                    Log.d(TAG, "Error: $errorMessage")
                }
            }

            override fun onFailure(call: Call<ApiInterface.ProcessResponse>, t: Throwable) {
                Log.d(TAG,"Failure: ${t.message}")
                design.progressBar.visibility = View.GONE
                design.loadingOverlay.visibility = View.GONE
                (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
            }
        })
    }

    private fun setupVoicesAdapter() {

        adapterVoice = VoicesAdapter(requireContext(), emptyList(), design.selectedImg, isSubscribed, false,dataStoreManager, viewLifecycleOwner,object : VoicesAdapter.OnVoiceSelectedListener {
            override fun onVoiceSelected(voice: VoiceModel) {
                if (voice.id == "create_voice" && isSubscribed) {
                    if (isShowSlotPaywall){
                        callPaywall("slot")
                    }else {
                        findNavController().navigate(R.id.action_homeFragment_to_voiceLabNameFragment)
                    }
                } else if (voice.id == "create_voice") {
                    callPaywall("subs")
                }
                else {
                    selectedVoiceId = voice.id
                    imageUrl = voice.imageUrl
                    name = voice.name
                    isClone = voice.isClone
                    updateCreateButtonState()
                }
            }
            override fun deleteClone(cloneId: String, context: Context) {
                viewModel.deleteClone(cloneId, context)

            }
        })

        design.recyclerViewVoices.adapter = adapterVoice


        viewModel.data.observe(viewLifecycleOwner) { voicesList ->
            if (voicesList != null) {
                adapterVoice.updateData(voicesList)
            }
        }

        viewModel.fetchVoices("all", requireContext())
    }

    fun generateCode(userId: String): String {
        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()
        val inputString = "$userId$currentTime"

        // Hash the input string using SHA256
        val hashedString = sha256(inputString)

        // Get the first 6 characters and make it uppercase
        val code = hashedString.take(6).uppercase(Locale.getDefault())

        return code
    }

    // SHA256 hash function
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getUidFromDataStore(context: Context) {
        lifecycleScope.launch {
            DataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    val userDocRef = firestore.collection("users").document(uid)
                    Log.d("uid", "getUidFromDataStore: $uid")
                    // Add SnapshotListener for real-time updates
                    userDocRef.addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("Home Firestore", "Error listening to user document: ", exception)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            remainingCharacters = snapshot.getLong("remainingCount")
                            additionalCount = snapshot.getLong("additionalCount") ?: 0L
                            remainingRights = snapshot.getLong("remainingRights")
                            cloningRights = snapshot.getLong("cloningRights")
                            referralCode = snapshot.getString("referralCode")

                            if (remainingRights != null) {
                                if (remainingRights == 0L) {
                                    if (!isSubscribed){
                                        remainingCount = 0
                                        tokenCounter = 0
                                        Log.d(TAG, "getUidFromDataStore: Token sıfır -> remainingCount: $remainingCount")
                                        design.remaningText.text = remainingCount.toString()
                                        design.tokenCounterText.text = tokenCounter.toString()
                                        design.tokenCounterText.visibility = View.GONE
                                        design.imageView3.setImageResource(R.drawable.handshake_2)
                                        design.counterLayout.isEnabled=true
                                    }else{
                                        remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                        design.remaningText.text = remainingCount.toString()
                                        design.tokenCounterText.text = tokenCounter.toString()
                                        design.tokenCounterText.visibility = View.VISIBLE
                                        design.imageView3.setImageResource(R.drawable.icon_counter)
                                        design.counterLayout.isEnabled=false
                                    }

                                } else {
                                    design.counterLayout.isEnabled=false
                                    remainingCount = if (remainingCharacters != null) {
                                        remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                    } else {
                                        150
                                    }
                                    design.remaningText.text = remainingCount.toString()
                                    design.tokenCounterText.text = remainingRights.toString()
                                }
                            } else {
                                val data = hashMapOf(
                                    "remainingRights" to 3,
                                    "remainingCount" to 150,
                                )

                                userDocRef.set(data,SetOptions.merge())

                                design.tokenCounterText.text = "3"
                                design.counterLayout.isEnabled=false
                                if (remainingCharacters != null && additionalCount != null) {
                                    remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                } else {
                                    remainingCount = 150
                                }
                                design.remaningText.text = remainingCount.toString()
                            }
                            if (isAdded && view != null) {
                                viewModel.cloneCount.observe(viewLifecycleOwner) { cloneCount ->
                                    if (cloneCount != null) {
                                        updateIsShowSlotPaywall(cloneCount)
                                    }
                                }
                            }
                            if (referralCode == null) {
                                val refCode = generateCode(uid)

                                val data = hashMapOf(
                                    "referralCode" to refCode
                                 )

                                userDocRef.set(data,SetOptions.merge())
                            } else {
                                val code = referralCode ?: ""
                                if (isAdded) {
                                    lifecycleScope.launch {
                                        context.let { ctx ->
                                            dataStoreManager.saveReferral(ctx, code)
                                        }
                                    }
                                }


                            }

                        } else {
                            val refCode = generateCode(uid)

                            val data = hashMapOf(
                                "referralCode" to refCode,
                                "remainingRights" to 3,
                                "remainingCount" to 150
                            )
                            userDocRef.set(data, SetOptions.merge())

                            remainingCount = 150
                            tokenCounter = 3
                            design.remaningText.text = remainingCount.toString()
                            design.tokenCounterText.text = tokenCounter.toString()
                            design.counterLayout.isEnabled=false
                        }
                        initialRemainingCount = remainingCount
                        lifecycleScope.launchWhenStarted {
                            if (isAdded) {
                                context.let { ctx ->
                                    dataStoreManager.getText(ctx).collect { text ->
                                        Log.d("aaaaaaa", "onCreateView: $text")
                                        design.editTextLayout.setText(text)

                                        if (text != null) {
                                            remainingCount = initialRemainingCount - text.length
                                            updateRemainingCountUI(remainingCount)
                                            design.editTextLayout.setSelection(text.length)
                                        }
                                    }
                                }
                            }
                        }

                    }
                } else {
                    Log.d("HomeFragment", "No UID found in DataStore")
                }
            }
        }
    }




    private fun updateRemainingCountUI(remainingCount: Int) {
        if (remainingCount < 0) {
            design.remaningText.setTextColor(ContextCompat.getColor(this@HomeFragment.requireContext(), R.color.progress_red))
        } else {
            design.remaningText.setTextColor(ContextCompat.getColor(this@HomeFragment.requireContext(), R.color.black))
        }
        design.remaningText.text = remainingCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lifecycleScope.coroutineContext.cancelChildren()  // Cancels any coroutines launched in the scope
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onPause() {
        super.onPause()

        saveTextJob = GlobalScope.launch(Dispatchers.IO) {
            try {
                dataStoreManager.saveText(requireContext(), enteredText)
            } catch (e: Exception) {
                Log.e("SaveText Error", "Error saving text: ${e.message}")
            }
        }
    }



}