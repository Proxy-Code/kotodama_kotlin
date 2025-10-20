package com.proksi.kotodama.objects

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class StsPicker(
    fragment: Fragment,
    private val onPicked: (Uri) -> Unit
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestPermission = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pickAudioInternal() else Log.d("stspicker", "permission denied ")
    }

    private val pickAudio = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.data?.let(onPicked)
        }
    }

    private var lastFragment: Fragment? = fragment

    fun pickAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val ctx = lastFragment?.requireContext() ?: return
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) pickAudioInternal() else requestPermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            pickAudioInternal()
        }
    }

    private fun pickAudioInternal() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "audio/*" }
        pickAudio.launch(intent)
    }
}
