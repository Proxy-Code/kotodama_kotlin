package com.proksi.kotodama.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.api.LogDescriptor
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentHomeBinding
import com.proksi.kotodama.adapters.CategoryAdapter
import com.proksi.kotodama.adapters.VoicesAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.VoiceModel
import com.proksi.kotodama.retrofit.ApiClient
import com.proksi.kotodama.retrofit.ApiInterface
import com.proksi.kotodama.utils.DialogUtils
import com.proksi.kotodama.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    private val isSubscribed:Boolean = true
    private val TAG = HomeFragment::class.java.simpleName

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentHomeBinding.inflate(inflater, container, false)

        //Adapters
        design.recyclerViewCategories.layoutManager=
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        design.recyclerViewVoices.layoutManager=
            GridLayoutManager(requireContext(),3, GridLayoutManager.VERTICAL,false)

        val categoryList = viewModel.getCategoryList()
        val adapterCategory = CategoryAdapter(this.requireContext(),categoryList)
        design.recyclerViewCategories.adapter=adapterCategory

        adapterCategory.setOnCategoryClickListener(object : CategoryAdapter.OnCategoryClickListener {
            override fun onCategoryClick(category: String) {
                viewModel.fetchVoices(category, requireContext())
            }
        })

        adapterVoice = VoicesAdapter(requireContext(), emptyList(), design.selectedImg,object : VoicesAdapter.OnVoiceSelectedListener{
            override fun onVoiceSelected(voice: VoiceModel) {
                if(voice.id == "create_voice" && isSubscribed){
                    findNavController().navigate(R.id.action_homeFragment_to_voiceLabNameFragment)
                } else if(voice.id == "create_voice" && !isSubscribed){
                    dialogUtils.showPremiumDialogBox(requireContext(), viewLifecycleOwner)
                } else{
                    selectedVoiceId = voice.id
                    imageUrl = voice.imageUrl
                    name = voice.name
                    updateCreateButtonState()
                }

            }
        })

        design.recyclerViewVoices.adapter = adapterVoice

        viewModel.data.observe(viewLifecycleOwner, Observer { voicesList ->
            if (voicesList != null) {
                Log.d("Observer", "Voices List: $voicesList")
                adapterVoice.updateData(voicesList)
            } else {
                Log.d("Observer", "Voices List is null")
            }
        })

        viewModel.fetchVoices("all",requireContext())

        //show dialog
        design.imageCrown.setOnClickListener {
            dialogUtils.showPremiumDialogBox(requireContext(), viewLifecycleOwner)
        }

        design.remaningCounterLayout.setOnClickListener{
            dialogUtils.showAddCharacterDialogBox(requireContext(), viewLifecycleOwner)
        }

        design.editTextLayout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                enteredText = s.toString().trim()
                updateCreateButtonState() // Butonun aktif olup olmadığını kontrol et
            }
        })


        design.buttonCreate.setOnClickListener {
            Log.d(TAG, "onCreateView: called button create")

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val idToken = getFirebaseIdToken()
                    if (idToken != null) {
                        val languageCode = recognizeLanguage(enteredText)
                        val voiceId = selectedVoiceId
                        val text = enteredText
                        val defaultValue = false

                        Log.d(TAG, "onCreateView: send processe gidicek")
                        sendProcessRequest(idToken, enteredText, selectedVoiceId, languageCode,imageUrl,name)
                    } else {
                        Log.e(TAG, "Firebase ID Token is null")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching Firebase ID token", e)
                }
            }
        }


        return design.root
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


    suspend fun recognizeLanguage(text: String): String {
        return suspendCancellableCoroutine { cont ->
            val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    if (languageCode != "und") {
                        val result = if (choices.contains(languageCode)) languageCode else "en"
                        cont.resume(result)  // Result olarak languageCode veya "en" döndürülüyor
                    } else {
                        cont.resume("en") // Default olarak "en" döndürüyoruz
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    cont.resume("en") // Hata durumunda da "en" döndürüyoruz
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
        Log.d(TAG, "sendProcessRequest: called ")


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
                Log.d(TAG, "onResponse is succes?: ${response.isSuccessful}")
                if (response.isSuccessful) {
                    val processResponse = response.body()
                    Log.d(TAG, "onResponse: isscuesss")
                    Log.d(TAG, "Process Response: ${processResponse}")
                    Log.d(TAG, "Process Response Result: ${processResponse?.data?.result}")
                    Log.d(TAG, "Success: ${processResponse?.success}")
                    Log.d(TAG, "Message: ${processResponse?.data?.message}")
                    findNavController().navigate(R.id.libraryFragment)
                } else {
                    val errorMessage = response.errorBody()?.string()
                    Log.d(TAG, "Error: $errorMessage")

                   // Toast.makeText(context, errorMessage ?: "Something went wrong", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiInterface.ProcessResponse>, t: Throwable) {
                Log.d(TAG,"Failure: ${t.message}")
            }
        })
    }






}