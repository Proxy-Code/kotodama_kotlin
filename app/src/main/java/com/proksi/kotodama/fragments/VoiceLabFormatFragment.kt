package com.proksi.kotodama.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.LayerDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.kotodama.app.R
import com.kotodama.app.databinding.FragmentVoiceLabFormatBinding
import com.proksi.kotodama.adapters.RecordUploadVoices
import com.proksi.kotodama.models.AudioRecord
import com.proksi.kotodama.viewmodel.CloneViewModel


class VoiceLabFormatFragment : Fragment(),RecordUploadVoices.OnItemClickListener {

    private lateinit var design: FragmentVoiceLabFormatBinding
    private lateinit var typeClone:String
    private lateinit var adapter: RecordUploadVoices
    private val viewModel: CloneViewModel by activityViewModels()
    private val READ_EXTERNAL_STORAGE_REQUEST = 100
    private val PICK_AUDIO_REQUEST = 101
    private val TAG = VoiceLabFormatFragment::class.java.simpleName


    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openFilePicker()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        design = FragmentVoiceLabFormatBinding.inflate(inflater, container, false)

        design.recordBtn.setOnClickListener {
            findNavController().navigate(R.id.action_voiceLabFormatFragment_to_recordVoiceFragment)
        }

        design.backBtn.setOnClickListener{
            findNavController().navigate(R.id.action_voiceLabFormatFragment_to_voiceLabPhotoFragment)
        }

        design.uploadBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                openFilePicker()
            } else {
                requestAudioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }

        adapter = RecordUploadVoices(requireContext(), mutableListOf(), this, viewModel,object : RecordUploadVoices.OnDataUpdatedListener {
            override fun onDataUpdated(newItems: List<AudioRecord>) {
                updateSeekBarAndButton(newItems)
            }
        })

        viewModel.audioFilePaths.observe(viewLifecycleOwner) { records ->
            adapter.updateData(records)
            Log.d("CloneVoice", "onCreateView: ${records.size}")
            updateSeekBarAndButton(records)
        }

        design.continueButton.setOnClickListener {
            findNavController().navigate(R.id.action_voiceLabFormatFragment_to_customizeFragment)
        }
        design.rvVoices.adapter = adapter
        design.rvVoices.layoutManager = LinearLayoutManager(requireContext())

        return design.root
    }

    private fun selectButton(button: LinearLayout) {
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_gradient)
        design.continueButton.setBackgroundResource(R.drawable.radius15_bg_gradient)
    }

    private fun updateSeekBarAndButton(records: List<AudioRecord>) {
        Log.d(TAG, "updateSeekBarAndButton: called")
        val totalDuration = records.sumOf { it.duration } / 1000

        val progressValue = totalDuration.coerceAtMost(420).toInt()

        design.seekbar.apply {
            max = 420
            progress = progressValue

            val progressDrawable = progressDrawable as LayerDrawable
            val progressLayer = progressDrawable.findDrawableByLayerId(android.R.id.progress)
            val totalDurationInSeconds = records.sumOf { it.duration } / 1000

            if (progressLayer != null) {
                if (totalDurationInSeconds > 420) {
                    progressLayer.setTint(ContextCompat.getColor(requireContext(), R.color.progress_red))
                    design.continueButton.isEnabled = false
                    design.continueButton.setBackgroundResource(R.drawable.create_btn_inactive)

                    Toast.makeText(requireContext(), "Total duration exceeds 7 minutes. Please remove some audio files.", Toast.LENGTH_SHORT).show()

                } else if (totalDurationInSeconds < 30) {
                    progressLayer.setTint(ContextCompat.getColor(requireContext(), R.color.progress_red))
                    design.continueButton.isEnabled = false
                    design.continueButton.setBackgroundResource(R.drawable.create_btn_inactive)

                } else {
                    progressLayer.setTint(ContextCompat.getColor(requireContext(), R.color.progress_green))
                    design.continueButton.isEnabled = true
                    design.continueButton.setBackgroundResource(R.drawable.create_btn_active)
                }
            } else {
                Log.e("SeekBar", "Progress layer is null")
            }
        }
    }



    private fun deselectButton(button: LinearLayout) {
        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.radius15_bg_c8c8)
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFilePicker()
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getRealPathFromURI(uri: Uri): String? {
        var realPath: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                if (index != -1) {
                    realPath = it.getString(index)
                }
            }
        }
        return realPath
    }

    private fun getFileSizeFromUri(uri: Uri): Long {
        var fileSize: Long = 0
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = it.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    override fun onStop() {
        super.onStop()
        adapter.stopPlayingAudio()
    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
        }
        audioPickerLauncher.launch(intent)
        Log.d(TAG, "openFilePicker: called")
    }

    private val audioPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "audiopickerlauncherda : ")
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "audiopicker da log un icinde: ")
                val data: Intent? = result.data
                Log.d(TAG, "data => ${data?.data}")
                data?.data?.let { audioUri ->
                    val path = getRealPathFromURI(audioUri)

                    // Retrieve duration using MediaMetadataRetriever
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(requireContext(), audioUri)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val duration = durationStr?.toLongOrNull() ?: 0L // Duration in milliseconds

                    // Get file size using ContentResolver
                    val fileSize = getFileSizeFromUri(audioUri)

                    // Create AudioRecord object
                    val audioRecord = AudioRecord(
                        path = path ?: audioUri.toString(),
                        duration = duration,
                        fileSize = fileSize
                    )

                    // Add to ViewModel list
                    //viewModel.addAudioFilePath(audioRecord)

                    val isAdded = viewModel.addAudioFilePath(audioRecord)

                    if (isAdded) {
                        //  findNavController().navigate(R.id.action_recordVoiceFragment_to_cloneVoiceSelectionFragment)
                    } else {
                        Toast.makeText(requireContext(), "Total duration exceeds 7 minutes", Toast.LENGTH_SHORT).show()
                    }

                    retriever.release() // Release retriever after use
                }
            }
        }

    override fun onItemClick(item: AudioRecord) {
        Log.d("buradayim", "onItemClick: ${item.duration}")
    }

}