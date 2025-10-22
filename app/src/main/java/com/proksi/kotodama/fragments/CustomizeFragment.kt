package com.proksi.kotodama.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentCustomizeBinding
import com.proksi.kotodama.adapters.ImagesAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.AudioRecord
import com.proksi.kotodama.objects.EventLogger
import com.proksi.kotodama.retrofit.ApiClient
import com.proksi.kotodama.retrofit.ApiClient.imagesService
import com.proksi.kotodama.retrofit.ApiInterface
import com.proksi.kotodama.viewmodel.CloneViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CustomizeFragment : Fragment() {
    private lateinit var design:FragmentCustomizeBinding
    private val TAG = CustomizeFragment::class.java.simpleName
    private lateinit var name:String
    private val viewModel: CloneViewModel by activityViewModels()
    private lateinit var imageUrls:List<String>
    private lateinit var dataStoreManager: DataStoreManager
    private var uid: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentCustomizeBinding.inflate(inflater,container,false)
        name = design.nameInput.text.toString()

        dataStoreManager = DataStoreManager
        EventLogger.logEvent(requireContext(), "clonePicture_screen_shown")


        design.continueButton.setOnClickListener{
            lifecycleScope.launch {
                try {
                    val idToken = getFirebaseIdToken()
                    idToken?.let {
                        viewModel.setIdToken(it)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseAuth", "Error getting ID token", e)
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val idToken = getFirebaseIdToken()
                    if (idToken != null) {
                        val imageUrl = viewModel.voiceImage.value ?: ""
                        val name = viewModel.voiceName.value ?: ""
                        val cleanedIdToken = idToken.trim()

                        val records = viewModel.audioFilePaths.value ?: emptyList()
                        val audioParts = prepareAudioPartsFromRecords(records)  // <-- değişen kısım

                        if (audioParts.isNotEmpty()) {
                            uploadData(cleanedIdToken, imageUrl, name, audioParts)
                        } else {
                            Log.d(TAG, "uploadData: No valid audio files to upload")
                            Toast.makeText(requireContext(), "Something went wrong, please try again", Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)

                        }
                    } else {
                        Log.e(TAG, "Firebase ID Token is null")
                        Toast.makeText(requireContext(), "Something went wrong, please try again", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)

                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching Firebase ID token", e)
                    Toast.makeText(requireContext(), "Something went wrong, please try again", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)

                }
            }


            design.progressBar.visibility = View.VISIBLE
            design.loadingOverlay.visibility = View.VISIBLE
        }

        design.backBtn.setOnClickListener{
            findNavController().navigate(R.id.action_customizeFragment_to_voiceLabFormatFragment)
        }

        lifecycleScope.launch {
            uid = dataStoreManager.getUid(requireContext()).firstOrNull().toString()
        }

        viewModel.isButtonEnabled.observe(viewLifecycleOwner, Observer { isEnabled ->
            design.continueButton.isEnabled = isEnabled
            if (isEnabled) {
                design.continueButton.setBackgroundResource(R.drawable.create_btn_active)
            } else {
                design.continueButton.setBackgroundResource(R.drawable.create_btn_inactive)
            }
        })

        design.nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                name = design.nameInput.text.toString()
                viewModel.setVoiceName(name)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        design.uploadPhoto.setOnClickListener{
            showCustomDialog()
        }

        imagesService.getImages().enqueue(object : Callback<List<String>> {
            @SuppressLint("SuspiciousIndentation")
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful) {
                    imageUrls = response.body()!!
                    design.uploadPhoto.isEnabled = true

                } else {
                    Log.d(TAG, "onResponse: unsuccessful response")
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Log.d(TAG, "onFailure: image service failed", t)
            }
        })


        return design.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showCustomDialog(){
        if (::imageUrls.isInitialized) {
            val dialog = Dialog(requireContext())

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.images_dialog)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.window!!.setGravity(Gravity.BOTTOM)

            val recyclerView = dialog.findViewById<RecyclerView>(R.id.rc_images)
            recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

            val imagesAdapter = ImagesAdapter(requireContext(), imageUrls) { selectedUrl ->
                design.fotoImg.visibility = View.GONE
                viewModel.setVoiceImage(selectedUrl)

                Glide.with(this)
                    .load(selectedUrl)
                    .placeholder(R.drawable.icon_kotodama)
                    .error(R.drawable.icon_kotodama)
                    .into(design.uploadPhoto)
                dialog.dismiss()
            }

            recyclerView.adapter = imagesAdapter

            dialog.show()
        } else {
            Log.e(TAG, "imageUrls not initialized yet.")
        }
    }

    fun uploadData(idToken:String,imageUrl:String,name:String,audioParts: List<MultipartBody.Part>) {

        if (idToken.isEmpty() || imageUrl.isEmpty() || name.isEmpty()) {
            return
        }
        ApiClient.cloneService.cloneRequest(
            imageUrl = RequestBody.create("text/plain".toMediaTypeOrNull(), imageUrl ?: ""),
            name = RequestBody.create("text/plain".toMediaTypeOrNull(), name ?: ""),
            idToken = RequestBody.create("text/plain".toMediaTypeOrNull(), idToken ?: ""),
            files = audioParts
        ).enqueue(object : Callback<ApiInterface.CloneResponse> {
            override fun onResponse(call: Call<ApiInterface.CloneResponse>, response: Response<ApiInterface.CloneResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)
                } else {
                    Toast.makeText(requireContext(), "Failed ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)
                }
            }

            override fun onFailure(call: Call<ApiInterface.CloneResponse>, t: Throwable) {
                findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)
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

    private fun Fragment.prepareAudioPartsFromRecords(
        records: List<AudioRecord>,
        allowed: Set<String> = setOf("wav", "mp3", "m4a")
    ): List<MultipartBody.Part> {

        fun mimeToExt(mime: String?): String? = when (mime?.lowercase()) {
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/aac", "audio/m4a", "audio/x-m4a" -> "m4a"
            else -> null
        }

        val parts = mutableListOf<MultipartBody.Part>()
        val cr = requireContext().contentResolver

        for (rec in records) {
            val path = rec.path
            try {
                if (path.startsWith("content://")) {
                    val uri = Uri.parse(path)

                    // 1) MIME ve görünen ad
                    val mime = cr.getType(uri)
                    var ext = mimeToExt(mime)
                    var displayName: String? = null

                    cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && c.moveToFirst()) displayName = c.getString(idx)
                    }
                    if (ext == null && !displayName.isNullOrBlank()) {
                        val dot = displayName!!.lastIndexOf('.')
                        if (dot != -1 && dot < displayName!!.length - 1) {
                            ext = displayName!!.substring(dot + 1).lowercase()
                        }
                    }

                    if (ext == null || ext !in allowed) {
                        Log.e("Upload", "Unsupported audio from content:// (mime=$mime, name=$displayName, ext=$ext)")
                        continue
                    }

                    // 2) İçeriği cache'e kopyala
                    val temp = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.$ext")
                    cr.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }

                    // 3) Multipart part oluştur
                    val mediaType = ("audio/$ext").toMediaTypeOrNull()
                    val body = temp.asRequestBody(mediaType)
                    val fileName = displayName ?: temp.name
                    val part = MultipartBody.Part.createFormData("files", fileName, body)
                    parts += part

                } else {
                    // Gerçek dosya yolu kullanımı
                    val file = File(path)
                    val ext = file.extension.lowercase()
                    if (ext in allowed && file.exists()) {
                        val body = file.asRequestBody(("audio/$ext").toMediaTypeOrNull())
                        parts += MultipartBody.Part.createFormData("files", file.name, body)
                    } else {
                        Log.e("Upload", "Invalid file path or ext: ${file.absolutePath} (ext=$ext)")
                    }
                }
            } catch (e: Exception) {
                Log.e("Upload", "Failed to prepare part for $path", e)
            }
        }
        return parts
    }



}




