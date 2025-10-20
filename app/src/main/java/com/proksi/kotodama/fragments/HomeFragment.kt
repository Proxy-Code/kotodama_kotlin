package com.proksi.kotodama.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.database.Cursor
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentHomeBinding
import com.proksi.kotodama.BaseFragment
import com.proksi.kotodama.MainActivity
import com.proksi.kotodama.adapters.VoicesAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.RCPlacement
import com.proksi.kotodama.models.VoiceModel
import com.proksi.kotodama.objects.EventLogger
import com.proksi.kotodama.retrofit.ApiClient
import com.proksi.kotodama.retrofit.ApiInterface
import com.proksi.kotodama.utils.DialogUtils
import com.proksi.kotodama.utils.KeyboardUtils
import com.proksi.kotodama.viewmodel.HomeViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HomeFragment : BaseFragment() {

    private lateinit var design: FragmentHomeBinding
    private var audioHash: String? = null

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapterVoice: VoicesAdapter
    private val dialogUtils = DialogUtils()
    private var listenerRegistration: ListenerRegistration? = null
    private var snapshotListener: ListenerRegistration? = null

    private lateinit var dataStoreManager: DataStoreManager
    private var enteredText: String = ""
    private var selectedVoiceId: String? = ""
    private var imageUrl: String? = ""
    private var name: String? = ""
    private var isClone: Boolean? = true
    private var isStsMode: Boolean = false
    private val choices = listOf("en", "es", "fr", "de", "it", "pt", "pl", "tr", "ru", "nl", "cs", "ar", "zh", "hu", "ko", "hi","fil","sv","bg","ro","el","ms","hr","sk","da","uk","ta")
    private var isSubscribed:Boolean = false
    private val TAG = HomeFragment::class.java.simpleName

    override val paywallComposeView: ComposeView
        get() = design.paywallComposeView

    private var serverRemaining: Int = 150
    private var remainingCharacters : Int? = null
    private var additionalCount: Number? = null
    private var remainingRights : Number? = null
    private var cloningRights : Number? = null
    private var tokenCounter = 3
    private var referralCode: String? = ""
    private var initialRemainingCount = 150
    private var isShowSlotPaywall : Boolean= false
    private var saveTextJob: Job? = null

    private var isRecording = false
    private var recorder: MediaRecorder? = null
    private var recordedFile: File? = null

    private val requestMicPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecording() else {
                Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentHomeBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager

        EventLogger.logEvent(requireContext(), "home_screen_shown")

        return design.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KeyboardUtils.hideKeyboard(requireActivity())

        setupRecycler()
        setupObservers()
        setupUI() // senin fonksiyonun
        setupSts()
        setupRecordButton()

        viewModel.fetchVoices("all", requireContext(), true)

        viewModel.cloneCount.observe(viewLifecycleOwner) { cloneCount ->
            updateIsShowSlotPaywall(cloneCount)
        }

        design.recyclerViewVoices.layoutManager=
            GridLayoutManager(requireContext(),3, GridLayoutManager.VERTICAL,false)


        if (viewModel.allVoices.value.isNullOrEmpty()) {
            design.progressBar.visibility = View.VISIBLE
            design.loadingOverlay.visibility = View.VISIBLE
            (requireActivity() as MainActivity).setBottomNavigationVisibility(false)
            viewModel.fetchVoices("all", requireContext(), true)
        } else {

            viewModel.allVoices.value?.let { voices ->
                adapterVoice.updateData(voices)
            }
        }


        setupToggle()

        isStsMode = false   // TTS
        updateAuxLayoutsVisibility()

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

                } else {
                    Log.d(TAG, "Fragment not added or view is null, skipping UI update")
                }
            }
        }
    }

    private fun setupRecycler() {
        adapterVoice = VoicesAdapter(
            requireContext(),
            emptyList(),
            design.selectedImg,
            isSubscribed,
            false,
            DataStoreManager,
            viewLifecycleOwner,
            object : VoicesAdapter.OnVoiceSelectedListener {
                override fun onVoiceSelected(voice: VoiceModel) {
                    Log.d("adapterr", "ses: ${voice.id}")
                    if (voice.id == "create_voice" && isSubscribed) {
                        Log.d("adapterr", "onVoiceSelected: burda1" )
                        if (isShowSlotPaywall) {
                            showPaywall(RCPlacement.CLONE)
                            Log.d("adapterr", "onVoiceSelected: burda12" )

                        } else {
                            findNavController().navigate(R.id.action_homeFragment_to_voiceLabNameFragment)
                            Log.d("adapterr", "onVoiceSelected: burda13" )

                        }
                    } else if (voice.id == "create_voice") {
                        Log.d("adapterr", "bureda 3 home ")
                        showPaywall(RCPlacement.HOME)
                    } else {
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
            }
        )

        design.recyclerViewVoices.apply {
            layoutManager = GridLayoutManager(requireContext(), 3, GridLayoutManager.VERTICAL, false)
            adapter = adapterVoice
            setHasFixedSize(true)
        }

        design.homeScroll.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) { // Aşağı kaydırma
                val view = v.getChildAt(v.childCount - 1)
                val diff = (view.bottom - (v.height + v.scrollY))

                if (diff < 100 && !viewModel.isLoading && !viewModel.isLastPage) {
                    viewModel.loadMoreVoices(requireContext())
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.allVoices.observe(viewLifecycleOwner) { voicesList ->

            if (!voicesList.isNullOrEmpty()) {
                showLoading(false)
                adapterVoice.updateData(voicesList)
                (requireActivity() as? MainActivity)?.setBottomNavigationVisibility(true)

                design.recyclerViewVoices.post {
                    Log.d("Pagination", "RecyclerView item count: ${adapterVoice.itemCount}")
                }
            } else {
                showLoading(false)
            }
        }

        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeViewModel.LoadingState.Loading -> {
                    if (viewModel.allVoices.value.isNullOrEmpty()) {
                        showLoading(true)
                    }
                }
                is HomeViewModel.LoadingState.LoadingMore -> {

                }
                is HomeViewModel.LoadingState.Success -> {
                    showLoading(false)
                }
                is HomeViewModel.LoadingState.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(visible: Boolean) {
        design.progressBar.visibility = if (visible) View.VISIBLE else View.GONE
        design.loadingOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        (requireActivity() as? MainActivity)?.setBottomNavigationVisibility(!visible)
    }

    private fun updateAuxLayoutsVisibility() {
        if (isStsMode) {
            design.langCodeLayout.visibility = View.GONE
            design.doneLayout.visibility = View.GONE
            design.deleteLayout.visibility = View.GONE
            return
        }

        val hasText = !design.editTextLayout.text?.toString()?.trim().isNullOrEmpty()
        val v = if (hasText) View.VISIBLE else View.GONE
        design.langCodeLayout.visibility = v
        design.doneLayout.visibility = v
        design.deleteLayout.visibility = v
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupSts(){
        design.uploadBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                openFilePicker()
            } else {
                requestAudioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
    }

    private fun setupUI(){
        design.remaningCounterLayout.setOnClickListener{
            if (isSubscribed){
                showPaywall(RCPlacement.CHARACTER)
            } else {
                showPaywall(RCPlacement.HOME)
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

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                val enteredText = s.toString().trim()
//                remainingCount = remainingCount?.minus(enteredText.length)
//                updateRemainingCountUI(remainingCount)
//
                viewLifecycleOwner.lifecycleScope.launch {
                    val languageCode = recognizeLanguage(enteredText)
                    // languageCode’u UI’da gösteriyorsan burada set edebilirsin
                }
//
//                // metin her değiştiğinde kurala göre güncelle
                updateAuxLayoutsVisibility()
                applyRemainingUI()

            }

            override fun afterTextChanged(s: Editable?) {
                enteredText = s.toString().trim()
                updateCreateButtonState()
                // Gerek yok: görünürlük zaten updateAuxLayoutsVisibility() ile yönetiliyor
            }
        })


        design.deleteLayout.setOnClickListener {

            design.editTextLayout.setText("")
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

        design.buttonCreate.setOnClickListener {
            EventLogger.logEvent(requireContext(), "home_create_click")

            remainingCharacters.let { it1 ->
                it1?.let { it2 ->
                    if (it2 <= 0 ){
                        if (isSubscribed){
                            showPaywall(RCPlacement.CHARACTER)
                        } else{
                            showPaywall(RCPlacement.HOME)
                        }
                    } else if (tokenCounter == 0){
                        showPaywall(RCPlacement.HOME)
                    }
                    else {
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
                                design.loadingOverlay.visibility = View.GONE
                                (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
                                Log.e(TAG, "Error fetching Firebase ID token", e)
                            }
                        }
                    }
                }
            }
            design.progressBar.visibility = View.VISIBLE
            design.loadingOverlay.visibility = View.VISIBLE
            (requireActivity() as MainActivity).setBottomNavigationVisibility(false)

        }

        design.imageCrown.setOnClickListener {
            showPaywall(RCPlacement.HOME)
        }

    }

    private fun applyRemainingUI() {
        val len = design.editTextLayout.text?.length ?: 0
        val remaining = (serverRemaining - len).coerceAtLeast(0)
        remainingCharacters = remaining
        updateRemainingCountUI(remaining)
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
                    snapshotListener?.remove()

                    val firestore = FirebaseFirestore.getInstance()
                    val userDocRef = firestore.collection("users").document(uid)

                    snapshotListener = userDocRef.addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.e("Home Firestore", "Error listening to user document: ", exception)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            handleSnapshotData(snapshot, uid, userDocRef)
                        } else {
                            createInitialUserData(uid, userDocRef)
                        }
                    }
                } else {
                    Log.d("HomeFragment", "No UID found in DataStore")
                }
            }
        }
    }

    private fun handleSnapshotData(snapshot: DocumentSnapshot, uid: String, userDocRef: DocumentReference) {
        additionalCount = snapshot.getLong("additionalCount") ?: 0L
        remainingRights = snapshot.getLong("remainingRights")
        cloningRights = snapshot.getLong("cloningRights")
        referralCode = snapshot.getString("referralCode")
        serverRemaining = snapshot.getLong("remainingCount")?.toInt() ?: 150
        applyRemainingUI()

        design.tokenCounterText.text = remainingRights.toString()
        tokenCounter = remainingRights?.toInt() ?: 3
        // Referral code kontrolü - sadece bir kere yap
        if (referralCode == null) {
            val refCode = generateCode(uid)
            userDocRef.set(mapOf("referralCode" to refCode), SetOptions.merge())
        } else {
            saveReferralCodeToDataStore(referralCode!!)
        }

        // Clone count observe'ını sadece bir kere başlat
        setupCloneCountObserver()
    }


    private fun createInitialUserData(uid: String, userDocRef: DocumentReference) {
        serverRemaining = 150
        applyRemainingUI()
        tokenCounter = 3
        design.tokenCounterText.text = tokenCounter.toString()
        design.counterLayout.isEnabled = false
    }

    private fun saveReferralCodeToDataStore(code: String) {
        if (isAdded) {
            lifecycleScope.launch {
                context?.let { ctx ->
                    dataStoreManager.saveReferral(ctx, code)
                }
            }
        }
    }

    private fun setupCloneCountObserver() {
        if (isAdded && view != null) {
            viewModel.cloneCount.observe(viewLifecycleOwner) { cloneCount ->
                if (cloneCount != null) {
                    updateIsShowSlotPaywall(cloneCount)
                }
            }
        }
    }

    private fun updateRemainingCountUI(remainingCount: Int?) {
        if (remainingCount != null) {
            if (remainingCount <= 0) {
                design.remaningText.setTextColor(ContextCompat.getColor(this@HomeFragment.requireContext(), R.color.progress_red))

            } else {
                design.remaningText.setTextColor(ContextCompat.getColor(this@HomeFragment.requireContext(), R.color.black))
            }
        }
        design.remaningText.text = remainingCount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
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

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openFilePicker()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun openFilePicker() {

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
        }
        audioPickerLauncher.launch(intent)
    }

    private val audioPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val audioUri = result.data?.data ?: return@registerForActivityResult
                design.uploadImg.visibility = View.GONE
                design.uploadProgressBar.visibility = View.VISIBLE
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // 1) Firebase idToken
                        val token = getFirebaseIdToken()
                        if (token.isNullOrEmpty()) {
                            Log.e("lelelele", "Firebase ID Token is null/empty")
                            return@launch
                        }

                        // 3) Multipart parçaları oluştur (IO thread)
                        val tokenPart = idTokenPart(token)
                        val filePart = withContext(Dispatchers.IO) {
                            uriToAudioPart(requireContext(), audioUri)
                        }

                        // 4) API çağrısı (IO thread'de)
                        val startTime = System.currentTimeMillis()
                        val resp = withContext(Dispatchers.IO) {
                            ApiClient.apiService.transcribePreview(tokenPart, filePart)
                        }
                        val endTime = System.currentTimeMillis()
                        val duration = endTime - startTime

                        // 5) UI güncelleme - MAIN THREAD'DE!
                        withContext(Dispatchers.Main) {
                            // Null ve boyut kontrolü
                            val displayText = resp.text?.takeIf { it.isNotEmpty() } ?: "No transcription available"

                            // Büyük metinleri kısalt
                            val finalText = if (displayText.length > 5000) {
                                displayText.substring(0, 5000) + "\n\n...[text truncated]..."
                            } else {
                                displayText
                            }

                            design.editTextSpeechLayout.setText(finalText)
                            design.editTextLayout.setText(finalText)
                            this@HomeFragment.audioHash = resp.audioHash
                        }

                    } catch (t: Throwable) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Upload failed: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            design.uploadImg.visibility = View.VISIBLE
                            design.uploadProgressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

    fun idTokenPart(token: String): RequestBody =
        token.toRequestBody("text/plain".toMediaTypeOrNull())

    fun uriToAudioPart(
        context: Context,
        uri: Uri,
        partName: String = "files" // backend bu ismi bekliyor
    ): MultipartBody.Part {
        val cr = context.contentResolver

        // Dosya adı
        var fileName = "upload.mp3"
        cr.query(uri, null, null, null, null)?.use { c: Cursor ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && c.moveToFirst()) fileName = c.getString(idx) ?: fileName
        }

        val mime = cr.getType(uri) ?: "audio/mpeg"

        val input = cr.openInputStream(uri)!!
        val temp = File.createTempFile("upload_", "_audio", context.cacheDir)
        FileOutputStream(temp).use { out -> input.copyTo(out) }
        input.close()

        val body = temp.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, fileName, body)
    }

    private fun setupToggle(){
        val toggleGroup = design.typeButtons

        toggleGroup.check( design.btnTts.id)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            when (checkedId) {
                R.id.btn_tts -> {
                    isStsMode = false
                    design.editTextLayout.visibility = View.VISIBLE
                    design.editTextSpeechLayout.visibility = View.GONE
                    design.stsButtonsLayout.visibility = View.GONE
                    updateAuxLayoutsVisibility()
                    design.editTextSpeechLayout.setText("")
                }
                R.id.btn_sts -> {
                    isStsMode = true
                    design.langCodeLayout.visibility = View.GONE
                    design.deleteLayout.visibility = View.GONE
                    design.doneLayout.visibility = View.GONE
                    design.editTextLayout.setText("")
                    design.stsButtonsLayout.visibility = View.VISIBLE
                    design.editTextLayout.visibility = View.GONE
                    design.editTextSpeechLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupRecordButton() {
        design.recordBtn.setOnClickListener {
            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    startRecording()
                } else {
                    requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else {
                stopRecordingAndUpload()
            }
        }
    }

    private fun startRecording() {
        try {
            toggleRecordUi(isRecording = true)

            recordedFile = File.createTempFile("rec_", ".m4a", requireContext().cacheDir)

            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(recordedFile!!.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (t: Throwable) {
            toggleRecordUi(isRecording = false)
            isRecording = false
            Toast.makeText(requireContext(), "Recording failed: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecordingAndUpload() {
        try {
            recorder?.run {
                try { stop() } catch (_: Throwable) {}
                reset()
                release()
            }
            recorder = null
            isRecording = false
            toggleRecordUi(isRecording = false)

            val file = recordedFile
            if (file == null || !file.exists() || file.length() == 0L) {
                Toast.makeText(requireContext(), "Empty recording", Toast.LENGTH_SHORT).show()
                return
            }

            design.recordImg.visibility = View.GONE
            design.recordPauseImg.visibility = View.GONE
            design.recordProgressBar.visibility = View.VISIBLE

            // Aynı transcribePreview hattı
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val token = getFirebaseIdToken()
                    if (token.isNullOrEmpty()) {
                        return@launch
                    }

                    val tokenPart = idTokenPart(token)
                    val filePart = withContext(Dispatchers.IO) {
                        fileToAudioPart(file, mime = "audio/m4a", partName = "files")
                    }

                    val start = System.currentTimeMillis()

                    val resp = withContext(Dispatchers.IO) {
                        ApiClient.apiService.transcribePreview(tokenPart, filePart)
                    }

                    val dur = System.currentTimeMillis() - start

                    withContext(Dispatchers.Main) {
                        val displayText = resp.text?.takeIf { it.isNotEmpty() } ?: "No transcription available"
                        val finalText = if (displayText.length > 5000)
                            displayText.substring(0, 5000) + "\n\n...[text truncated]..."
                        else displayText

                        design.recordImg.visibility = View.VISIBLE
                        design.recordPauseImg.visibility = View.GONE
                        design.recordProgressBar.visibility = View.GONE

                        design.editTextSpeechLayout.setText(finalText)
                        design.editTextLayout.setText(finalText)
                        this@HomeFragment.audioHash = resp.audioHash
                    }
                } catch (t: Throwable) {
                    Log.e("REC", "Upload failed", t)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Upload failed: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                    design.recordImg.visibility = View.VISIBLE
                    design.recordPauseImg.visibility = View.GONE
                    design.recordProgressBar.visibility = View.GONE
                } finally {
                    withContext(Dispatchers.IO) {
                        try { recordedFile?.delete() } catch (_: Throwable) {}
                        recordedFile = null
                    }
                    design.recordImg.visibility = View.VISIBLE
                    design.recordPauseImg.visibility = View.GONE
                    design.recordProgressBar.visibility = View.GONE
                }
            }

        } catch (t: Throwable) {
            Toast.makeText(requireContext(), "Stop failed: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleRecordUi(isRecording: Boolean) {
        if (isRecording) {
            design.recordImg.visibility = View.GONE
            design.recordPauseImg.visibility = View.VISIBLE
            design.recordProgressBar.visibility = View.GONE
        } else {
            design.recordPauseImg.visibility = View.GONE
            design.recordImg.visibility = View.VISIBLE
            design.recordProgressBar.visibility = View.GONE
        }
    }

    fun fileToAudioPart(
        file: File,
        mime: String = "audio/m4a",
        partName: String = "files"
    ): MultipartBody.Part {
        val body = file.asRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, body)
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            try {
                recorder?.stop()
            } catch (_: Throwable) {}
            recorder?.release()
            recorder = null
            isRecording = false
            toggleRecordUi(isRecording = false)
        }
    }








}