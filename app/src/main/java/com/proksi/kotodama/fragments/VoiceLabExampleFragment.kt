package com.proksi.kotodama.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.view.Window
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentVoiceLabPhotoBinding


class VoiceLabExampleFragment : Fragment() {

    private lateinit var design: FragmentVoiceLabPhotoBinding
    private lateinit var photoUri: Uri
    private lateinit var name:String
    private val TAG = VoiceLabExampleFragment::class.java.simpleName


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        design = FragmentVoiceLabPhotoBinding.inflate(inflater, container, false)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        design.backBtn.setOnClickListener(){
            findNavController().navigate(R.id.action_voiceLabPhotoFragment_to_voiceLabNameFragment)
        }

        design.continueButton.setOnClickListener{
            findNavController().navigate(R.id.action_voiceLabPhotoFragment_to_voiceLabFormatFragment)
        }

        return design.root
    }




//    private fun showDialog(){
//        val dialog = Dialog(requireContext())
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.setCancelable(false)
//        dialog.setContentView(R.layout.dialog_custom_images)
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//
//        Log.d(TAG, "showDialog: burdayikm")
//        val recyclerView = dialog.findViewById<RecyclerView>(R.id.imagesRv)
//        recyclerView.layoutManager=
//            GridLayoutManager(requireContext(),2, GridLayoutManager.VERTICAL,false)
//
//        val items = mutableListOf<Image>()
//        items.add(Image(1, R.drawable.boy))
//        items.add(Image(2,R.drawable.boy))
//        items.add(Image(3,R.drawable.boy))
//        items.add(Image(4,R.drawable.boy))
//        items.add(Image(5,R.drawable.boy))
//        items.add(Image(6,R.drawable.boy))
//        items.add(Image(7,R.drawable.boy))
//        items.add(Image(8,R.drawable.boy))
//
//        val adapter = ImagesAdapter(requireContext(), items)
//        recyclerView.adapter = adapter
//
//        dialog.findViewById<ImageView>(R.id.backButton).setOnClickListener {
//            dialog.dismiss()
//        }
//
//        dialog.show()
//
//    }

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


}