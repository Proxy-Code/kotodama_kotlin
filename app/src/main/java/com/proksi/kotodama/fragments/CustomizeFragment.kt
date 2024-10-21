package com.proksi.kotodama.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kotodama.tts.R
import com.kotodama.tts.databinding.FragmentCustomizeBinding
import com.kotodama.tts.databinding.FragmentVoiceLabFormatBinding
import com.proksi.kotodama.adapters.ImagesAdapter
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.retrofit.ApiClient
import com.proksi.kotodama.retrofit.ApiClient.imagesService
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class CustomizeFragment : Fragment() {

    private lateinit var design:FragmentCustomizeBinding
    private val TAG = CustomizeFragment::class.java.simpleName
    private lateinit var photoUri: Uri
    private lateinit var name:String
    private val viewModel: CloneViewModel by activityViewModels()
    private lateinit var imagesButtons:List<ImageView>
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

            design.progressBar.visibility = View.VISIBLE
            design.loadingOverlay.visibility = View.VISIBLE

            //findNavController().navigate(R.id.action_customizeFragment_to_voiceLabLoadingFragment)
        }

        design.backBtn.setOnClickListener{
            findNavController().navigate(R.id.action_customizeFragment_to_voiceLabFormatFragment)
        }

        lifecycleScope.launch {
            uid = dataStoreManager.getUid(requireContext()).firstOrNull().toString()
            Log.d(TAG, "onCreateView: $uid")
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

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }


    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                photoUri = it
                viewModel.setVoiceImage(photoUri.toString())
                val bitmap = getCorrectlyOrientedBitmap(photoUri)
                design.uploadPhoto.setImageBitmap(bitmap)
                design.fotoImg.visibility=View.GONE
            }
        }

    private fun getCorrectlyOrientedBitmap(uri: Uri): Bitmap? {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val exifInterface = ExifInterface(requireContext().contentResolver.openInputStream(uri)!!)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> TransformationUtils.rotateImage(bitmap, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> TransformationUtils.rotateImage(bitmap, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> TransformationUtils.rotateImage(bitmap, 270)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            ExifInterface.ORIENTATION_TRANSPOSE -> transposeImage(bitmap)
            else -> bitmap
        }

        return rotatedBitmap
    }

    private fun transposeImage(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        matrix.postScale(-1f, 1f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
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
            // Optionally show a message to the user or handle it appropriately
        }
    }

    fun uploadData(idToken:String,imageUrl:String,name:String,audioParts: List<MultipartBody.Part>) {
        Log.d(TAG, "uploadData: $idToken ")
        Log.d(TAG, "uploadData: $imageUrl")

        if (idToken.isEmpty() || imageUrl.isEmpty() || name.isEmpty()) {
            Log.e(TAG, "missing: idToken=$idToken, imageUrl=$imageUrl, name=$name")
            return
        }
        ApiClient.cloneService.cloneRequest(
            imageUrl = RequestBody.create("text/plain".toMediaTypeOrNull(), imageUrl ?: ""),
            name = RequestBody.create("text/plain".toMediaTypeOrNull(), name ?: ""),
            idToken = RequestBody.create("text/plain".toMediaTypeOrNull(), idToken ?: ""),
            files = audioParts
        ).enqueue(object : Callback<ApiInterface.CloneResponse> {
            override fun onResponse(call: Call<ApiInterface.CloneResponse>, response: Response<ApiInterface.CloneResponse>) {
                Log.d("id token", idToken)
                if (response.isSuccessful) {
                    val result = response.body()
                    val status = result?.status
                    Log.d("Upload", "Clone created successfully with status: $status")
                    findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)
                } else {
                    Log.e("Upload", "Failed: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)
                }


            }

            override fun onFailure(call: Call<ApiInterface.CloneResponse>, t: Throwable) {
                Log.e("Upload", "onFailure: ${t.message}")
                //Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_customizeFragment_to_homeFragment)

            }
        })

    }

    suspend fun getFirebaseIdToken(): String? = suspendCancellableCoroutine { cont ->
        val firebaseUser = Firebase.auth.currentUser
        firebaseUser?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result?.token)
                Log.d(TAG, "getFirebaseIdToken: success ")
            } else {
                Log.d(TAG, "getFirebaseIdToken: basarisiz")
                cont.resumeWithException(task.exception ?: Exception("Unknown error"))
            }
        }
    }




}




