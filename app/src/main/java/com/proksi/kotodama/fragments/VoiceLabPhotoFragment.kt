package com.proksi.kotodama.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.proksi.kotodama.R
import com.proksi.kotodama.databinding.FragmentVoiceLabNameeBinding
import com.proksi.kotodama.databinding.FragmentVoiceLabPhotoBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController

class VoiceLabPhotoFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabPhotoBinding
    private lateinit var photoUri: Uri
    private lateinit var name:String


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabPhotoBinding.inflate(inflater, container, false)

        val bundle: VoiceLabPhotoFragmentArgs by navArgs()
        name = bundle.name

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        design.backButtonName.setOnClickListener(){
            findNavController().navigate(R.id.action_voiceLabPhotoFragment_to_voiceLabNameFragment)
        }

        design.continueBtn.setOnClickListener{
            val action= VoiceLabPhotoFragmentDirections.actionVoiceLabPhotoFragmentToVoiceLabFormatFragment(name,photoUri)
            findNavController().navigate(action)
        }

        design.uploadPhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        return design.root
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openGallery()
            } else {
                Log.e("VoiceLabPhotoFragment", "Permission denied")
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                photoUri = it
                val bitmap = getCorrectlyOrientedBitmap(photoUri)
                design.uploadPhoto.setImageBitmap(bitmap)
                design.continueBtn.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_gradient)
                design.continueBtn.isEnabled = true
                design.uploadText.text = name
            }
        }

    private fun getCorrectlyOrientedBitmap(uri: Uri): Bitmap? {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val exifInterface = ExifInterface(requireContext().contentResolver.openInputStream(uri)!!)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        Log.d("VoiceLabPhotoFragment", "Photo orientation: $orientation")

        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            ExifInterface.ORIENTATION_TRANSPOSE -> transposeImage(bitmap)
            else -> bitmap // Diğer durumlar için döndürme yapmıyoruz
        }

        return rotatedBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun transposeImage(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        matrix.postScale(-1f, 1f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

}