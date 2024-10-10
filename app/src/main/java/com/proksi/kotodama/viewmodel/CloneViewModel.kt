package com.proksi.kotodama.viewmodel


import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.proksi.kotodama.models.AudioRecord

class CloneViewModel : ViewModel() {
    val audioFilePaths = MutableLiveData<MutableList<AudioRecord>>()
    private val _voiceName = MutableLiveData<String?>(null)
    val voiceName: LiveData<String?> get() = _voiceName
    private val _voiceImage = MutableLiveData<String>()
    val voiceImage: LiveData<String> get() = _voiceImage

    private val _voiceCustomImage = MutableLiveData<View>()
    val voiceCustomImage: LiveData<View> get() =  _voiceCustomImage

    val idToken: LiveData<String?> get() = _idToken
    private val _idToken = MutableLiveData<String>()


    init {
        audioFilePaths.value = mutableListOf()
    }

    fun addAudioFilePath(newAudioRecord: AudioRecord): Boolean {

        val currentTotalDuration = audioFilePaths.value?.sumOf { it.duration } ?: 0L
        Log.d("AudioRecord", "New audio duration: ${newAudioRecord.duration}")
        Log.d("AudioRecord", "Current total duration: $currentTotalDuration")


        if (currentTotalDuration + newAudioRecord.duration > 420000) {
            return false
        }

        audioFilePaths.value?.add(newAudioRecord)
        audioFilePaths.value = audioFilePaths.value // Trigger observers
        return true
    }

    fun removeAudioFilePath(audioRecord: AudioRecord) {
        audioFilePaths.value?.let {
            it.remove(audioRecord)
            audioFilePaths.value = it
        }
    }

    val isButtonEnabled = MediatorLiveData<Boolean>().apply {

        addSource(_voiceName) { checkButtonEnabled() }
        addSource(_voiceImage) { checkButtonEnabled() }

    }

    fun setIdToken(idToken: String) {
        _idToken.value = idToken
        Log.d("idTokenn", "setIdToken: $idToken ")
    }

    private fun checkButtonEnabled() {
        isButtonEnabled.value = !(_voiceName.value.isNullOrEmpty() || _voiceImage.value == null)
    }

    fun setVoiceName(item: String) {
        _voiceName.value=item
    }

    fun setVoiceImage(item: String) {
        Log.d("setvoiceimage", "setVoiceImage: $item")
        _voiceImage.value=item
    }

    fun setCustomVoiceImage(item: View) {
        _voiceCustomImage.value=item
    }

}