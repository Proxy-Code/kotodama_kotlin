package com.proksi.kotodama.fragments

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentVoiceLabLoadingBinding
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.retrofit.ApiClient
import com.proksi.kotodama.retrofit.ApiClient.cloneService
import com.proksi.kotodama.retrofit.ApiInterface
import com.proksi.kotodama.viewmodel.CloneViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VoiceLabLoadingFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabLoadingBinding
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentSong: Int? = null
    private val viewModel: CloneViewModel by activityViewModels()
    private val TAG = VoiceLabLoadingFragment::class.java.simpleName
    private lateinit var dataStoreManager: DataStoreManager
    private var uid: String? = null
    private var taskId: Any? = null
    private var idTokenRequest: String?=""

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabLoadingBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager

        val handler = Handler(Looper.getMainLooper())
        handler.post(object : java.lang.Runnable {
            override fun run() {
                handler.postDelayed(this, 3000)
            }
        })

        lifecycleScope.launch {
            uid = dataStoreManager.getUid(requireContext()).firstOrNull().toString()
            Log.d(TAG, "onCreateView: $uid")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val idToken = getFirebaseIdToken()
                if (idToken != null) {
                    val imageUrl = viewModel.voiceImage.value ?: ""  // URL olarak göndermek
                    val name = viewModel.voiceName.value ?: ""
                    val cleanedIdToken = idToken.trim()
                    val allowedFormats = listOf("wav", "mp3", "m4a")
                    val audioParts = viewModel.audioFilePaths.value?.mapNotNull { audioRecord ->
                        val audioFile = File(audioRecord.path)
                        val fileExtension = audioFile.extension.lowercase()

                        if (allowedFormats.contains(fileExtension)) {
                            val requestFile = RequestBody.create("audio/$fileExtension".toMediaTypeOrNull(), audioFile)
                            MultipartBody.Part.createFormData("files", audioFile.name, requestFile)
                        } else {
                            Log.e("Upload", "Invalid file format: $fileExtension. Allowed formats: $allowedFormats")
                            null
                        }
                    } ?: emptyList()
                    if (audioParts.isNotEmpty()) {
                    uploadData(cleanedIdToken, imageUrl, name, audioParts )
                    } else{
                        Log.d(TAG, "uploadData: No valid audio files to upload")
                    }
                } else {
                    Log.e(TAG, "Firebase ID Token is null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Firebase ID token", e)
            }
        }

        return design.root
    }

    fun uploadData(idToken:String,imageUrl:String,name:String,audioParts: List<MultipartBody.Part>) {
        Log.d(TAG, "uploadData: $idToken ")
        Log.d(TAG, "uploadData: $imageUrl")

        if (idToken.isEmpty() || imageUrl.isEmpty() || name.isEmpty()) {
            Log.e(TAG, "missing: idToken=$idToken, imageUrl=$imageUrl, name=$name")
            return
        }
                        cloneService.cloneRequest(
                            imageUrl = imageUrl,
                            name = name,
                            idToken = idToken,
                            files = audioParts
                        ).enqueue(object : Callback<ApiInterface.CloneResponse> {
                            override fun onResponse(call: Call<ApiInterface.CloneResponse>, response: Response<ApiInterface.CloneResponse>) {
                                Log.d("id token", idToken)
                                if (response.isSuccessful) {
                                    val result = response.body()
                                    val status = result?.status
                                    Log.d("Upload", "Clone created successfully with status: $status")
                                } else {
                                    Log.e("Upload", "Failed: ${response.errorBody()?.string()}")
                                }
                            }

                            override fun onFailure(call: Call<ApiInterface.CloneResponse>, t: Throwable) {
                                Log.e("Upload", "Error: ${t.message}")
                            }
                        })

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

}
