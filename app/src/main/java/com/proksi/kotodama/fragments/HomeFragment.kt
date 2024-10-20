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
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.api.LogDescriptor
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentHomeBinding
import com.proksi.kotodama.adapters.CategoryAdapter
import com.proksi.kotodama.adapters.VoicesAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.dataStore.DataStoreManager.saveText
import com.proksi.kotodama.models.VoiceModel
import com.proksi.kotodama.retrofit.ApiClient
import com.proksi.kotodama.retrofit.ApiInterface
import com.proksi.kotodama.utils.DialogUtils
import com.proksi.kotodama.viewmodel.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.properties.Delegates

class HomeFragment : Fragment() {

    private lateinit var design: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapterVoice: VoicesAdapter
    private val dialogUtils = DialogUtils()
    private lateinit var dataStoreManager: DataStoreManager
    private var enteredText: String = ""
    private var selectedVoiceId: String? = ""
    private var imageUrl: String? = ""
    private var name: String? = ""
    private val choices = listOf("en", "es", "fr", "de", "it", "pt", "pl", "tr", "ru", "nl", "cs", "ar", "zh", "hu", "ko", "hi")
    private var isSubscribed:Boolean = false
    private val TAG = HomeFragment::class.java.simpleName
    private var remainingCount = 150
    private var remainingCharacters : Number? = null
    private var additionalCount: Number? = null
    private var remainingRights : Number? = null
    private var cloningRights : Number? = null
    private var tokenCounter = 3
    private var initialRemainingCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentHomeBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager

        val currentView = view
        if (currentView != null) {
            val lifecycleOwner = currentView.findViewTreeLifecycleOwner()
            // Continue with your operations
        }

//        lifecycleScope.launchWhenStarted {
//            dataStoreManager.getText(requireContext()).collect { text ->
//
//                Log.d("aaaaaaa", "onCreateView: $text")
//                design.editTextLayout.setText(text)
//
//                // Trigger remaining count update when text is loaded from DataStore
//                if (text != null) {
//                    remainingCount = initialRemainingCount - text.length
//                    updateRemainingCountUI(remainingCount)
//                    design.editTextLayout.setSelection(text.length)
//                }
//            }
//        }


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

        viewModel.fetchVoices("all",requireContext())

        //show dialog
        design.imageCrown.setOnClickListener {
            dialogUtils.showPremiumDialogBox(
                requireContext(),
                viewLifecycleOwner,
                lifecycleScope,
                dataStoreManager )
        }

        design.remaningCounterLayout.setOnClickListener{
            if (isSubscribed){
               dialogUtils.showAddCharacterDialogBox( requireContext(),
                   viewLifecycleOwner,
                   lifecycleScope,
                   dataStoreManager)
            } else {
                dialogUtils.showPremiumDialogBox(
                    requireContext(),
                    viewLifecycleOwner,
                    lifecycleScope,
                    dataStoreManager )

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
            // EditText'in içindeki metni sil
            design.editTextLayout.setText("")

            // Remaining count'u sıfırla
            remainingCount = initialRemainingCount
            updateRemainingCountUI(remainingCount)

            // Diğer UI elementlerini gizle
            design.langCodeLayout.visibility = View.GONE
            design.doneLayout.visibility = View.GONE

            // Eğer başka bir işlem yapılacaksa (örneğin createButton'u disable etmek), buraya ekleyin
            updateCreateButtonState()
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

            if (remainingCount < 0 ){
                if (isSubscribed){
                    dialogUtils.showAddCharacterDialogBox(requireContext(), viewLifecycleOwner,lifecycleScope, dataStoreManager )
                } else{
                    dialogUtils.showPremiumDialogBox(requireContext(),
                                                     viewLifecycleOwner,
                                                     lifecycleScope,
                                                     dataStoreManager )
                }
            }else {

                design.progressBar.visibility = View.VISIBLE
                design.loadingOverlay.visibility = View.VISIBLE

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val idToken = getFirebaseIdToken()
                        if (idToken != null) {
                            val languageCode = recognizeLanguage(enteredText)
                            val voiceId = selectedVoiceId
                            val text = enteredText
                            val defaultValue = false

                            Log.d(TAG, "onCreateView: send processe gidicek")
                            sendProcessRequest(
                                idToken,
                                enteredText,
                                selectedVoiceId,
                                languageCode,
                                imageUrl,
                                name
                            )
                        } else {
                            Log.e(TAG, "Firebase ID Token is null")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching Firebase ID token", e)
                    }
                }
            }
        }

        return design.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            dataStoreManager.getSubscriptionStatusKey(this@HomeFragment.requireContext()).collect { isActive ->
                isSubscribed = isActive
                if (isActive) {
                    design.imageCrown.visibility=View.GONE
                    design.counterLayout.visibility=View.GONE
                }
                setupVoicesAdapter()
            }
        }
        getUidFromDataStore(requireContext())
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
        name: String?
    ) {

        if (selectedVoiceId.isNullOrEmpty() || imageUrl.isNullOrEmpty() || name.isNullOrEmpty()) {
            Log.e(TAG, "Payload values are missing: selectedVoiceId=$selectedVoiceId, imageUrl=$imageUrl, name=$name")
            return
        }

        val payload = ApiInterface.ProcessRequest(
            text = enteredText,
            name = name,
            language_code = languageCode,
            isDefault = true,
            sound_sample_id = selectedVoiceId,
            imageUrl = imageUrl,
            idToken = idToken
        )

        Log.d(TAG, "Payload: $payload")

        ApiClient.apiService.processRequest(payload).enqueue(object : retrofit2.Callback<ApiInterface.ProcessResponse> {
            override fun onResponse(
                call: Call<ApiInterface.ProcessResponse>,
                response: retrofit2.Response<ApiInterface.ProcessResponse>) {

                if (response.isSuccessful) {
                    val processResponse = response.body()
                    Log.d(TAG, "onResponse: isscuesss")
                    Log.d(TAG, "Process Response: ${processResponse}")
                    Log.d(TAG, "Process Response Result: ${processResponse?.data?.result}")
                    Log.d(TAG, "Success: ${processResponse?.success}")
                    Log.d(TAG, "Message: ${processResponse?.data?.message}")
                    design.progressBar.visibility = View.GONE
                    design.loadingOverlay.visibility = View.GONE
                    findNavController().navigate(R.id.action_homeFragment_to_libraryFragment)
                } else {
                    val errorMessage = response.errorBody()?.string()
                    design.progressBar.visibility = View.GONE
                    design.loadingOverlay.visibility = View.GONE
                    Log.d(TAG, "Error: $errorMessage")
                    Toast.makeText(this@HomeFragment.requireContext(), "An error occurred: $errorMessage", Toast.LENGTH_LONG).show()


                }
            }

            override fun onFailure(call: Call<ApiInterface.ProcessResponse>, t: Throwable) {
                Log.d(TAG,"Failure: ${t.message}")
            }
        })
    }

    private fun setupVoicesAdapter() {
        val currentView = view ?: return // Exit if view is null

        adapterVoice = VoicesAdapter(requireContext(), emptyList(), design.selectedImg, isSubscribed, false,dataStoreManager, viewLifecycleOwner,object : VoicesAdapter.OnVoiceSelectedListener {
            override fun onVoiceSelected(voice: VoiceModel) {
                if (voice.id == "create_voice" && isSubscribed) {
                    findNavController().navigate(R.id.action_homeFragment_to_voiceLabNameFragment)
                } else if (voice.id == "create_voice" && !isSubscribed) {
                    dialogUtils.showPremiumDialogBox(
                        requireContext(),
                        viewLifecycleOwner,
                        lifecycleScope,
                        dataStoreManager )
                } else {
                    selectedVoiceId = voice.id
                    imageUrl = voice.imageUrl
                    name = voice.name
                    updateCreateButtonState()
                }
            }
            override fun deleteClone(cloneId: String, context: Context) {
                viewModel.deleteClone(cloneId, context)
            }
        })

        design.recyclerViewVoices.adapter = adapterVoice


        viewModel.data.observe(viewLifecycleOwner) { voicesList ->
            Log.d(TAG, "Data changed, updating adapter with size: ${voicesList.size}")

            if (voicesList != null) {
                Log.d("Observer", "Voices List: $voicesList")
                adapterVoice.updateData(voicesList)
            } else {
                Log.d("Observer", "Voices List is null")
            }
        }

        viewModel.fetchVoices("all", requireContext())
    }

    private fun getUidFromDataStore(context: Context) {
        lifecycleScope.launch {
            DataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    Log.d("HomeFragment", "UID from DataStore: $uid")
                    val firestore = FirebaseFirestore.getInstance()
                    val userDocRef = firestore.collection("users").document(uid)

                    // Add SnapshotListener for real-time updates
                    userDocRef.addSnapshotListener { documentSnapshot, exception ->
                        if (exception != null) {
                            Log.e("Home Firestore", "Error listening to user document: ", exception)
                            return@addSnapshotListener
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            remainingCharacters = documentSnapshot.getLong("remainingCount")
                            additionalCount = documentSnapshot.getLong("additionalCount") ?: 0L
                            remainingRights = documentSnapshot.getLong("remainingRights")
                            cloningRights = documentSnapshot.getLong("cloningRights")

                            if (remainingRights != null) {
                                if (remainingRights == 0L) {
                                    if (!isSubscribed){
                                        remainingCount = 0
                                        tokenCounter = 0
                                        Log.d(TAG, "getUidFromDataStore: Token sıfır -> remainingCount: $remainingCount")
                                        design.remaningText.text = remainingCount.toString()
                                        design.tokenCounterText.text = tokenCounter.toString()
                                    }else{
                                        remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                        design.remaningText.text = remainingCount.toString()
                                        design.tokenCounterText.text = tokenCounter.toString()
                                    }

                                } else {
                                    if (remainingCharacters != null) {
                                        remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                    } else {
                                        remainingCount = 150
                                    }
                                    design.remaningText.text = remainingCount.toString()
                                    design.tokenCounterText.text = remainingRights.toString()
                                }
                            } else {
                                // Eğer remainingRights null ise varsayılan değerler
                                design.tokenCounterText.text = "3"
                                if (remainingCharacters != null && additionalCount != null) {
                                    remainingCount = remainingCharacters!!.toInt() + additionalCount!!.toInt()
                                } else {
                                    remainingCount = 150
                                }
                                design.remaningText.text = remainingCount.toString()
                            }
                        } else {
                            // Eğer documentSnapshot yoksa varsayılan değerler
                            remainingCount = 150
                            tokenCounter = 3
                            design.remaningText.text = remainingCount.toString()
                            design.tokenCounterText.text = tokenCounter.toString()
                        }
                        initialRemainingCount = remainingCount
                        lifecycleScope.launchWhenStarted {
                            dataStoreManager.getText(requireContext()).collect { text ->

                                Log.d("aaaaaaa", "onCreateView: $text")
                                design.editTextLayout.setText(text)

                                // Trigger remaining count update when text is loaded from DataStore
                                if (text != null) {
                                    remainingCount = initialRemainingCount - text.length
                                    updateRemainingCountUI(remainingCount)
                                    design.editTextLayout.setSelection(text.length)
                                }
                            }
                        }
                        Log.d(TAG, "getUidFromDataStore: $initialRemainingCount")
                    }
                } else {
                    Log.d("HomeFragment", "No UID found in DataStore")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            dataStoreManager.saveText(requireContext(),enteredText)
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




}