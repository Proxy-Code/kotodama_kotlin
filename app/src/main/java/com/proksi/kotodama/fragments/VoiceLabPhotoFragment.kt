package com.proksi.kotodama.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Window
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentVoiceLabPhotoBinding
import com.proksi.kotodama.adapters.ImagesAdapter
import com.proksi.kotodama.models.Image

class VoiceLabPhotoFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabPhotoBinding
    private lateinit var photoUri: Uri
    private lateinit var name:String
    private val TAG = VoiceLabPhotoFragment::class.java.simpleName


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

        design.skipBtn.setOnClickListener{
            Log.d(TAG, "onCreateView: skipteyim")
            showDialog()
        }

        return design.root
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openGallery()
            } else {
                Log.e(TAG, "Permission denied")
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
                design.fotoImg.visibility=View.GONE
            }
        }


    private fun showDialog(){
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_custom_images)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Log.d(TAG, "showDialog: burdayikm")
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.imagesRv)
        recyclerView.layoutManager=
            GridLayoutManager(requireContext(),2, GridLayoutManager.VERTICAL,false)

        val items = mutableListOf<Image>()
        items.add(Image(1, R.drawable.boy))
        items.add(Image(2,R.drawable.boy))
        items.add(Image(3,R.drawable.boy))
        items.add(Image(4,R.drawable.boy))
        items.add(Image(5,R.drawable.boy))
        items.add(Image(6,R.drawable.boy))
        items.add(Image(7,R.drawable.boy))
        items.add(Image(8,R.drawable.boy))

        val adapter = ImagesAdapter(requireContext(), items)
        recyclerView.adapter = adapter

        dialog.show()

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