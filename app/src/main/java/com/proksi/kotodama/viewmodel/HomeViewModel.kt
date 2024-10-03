package com.proksi.kotodama.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.proksi.kotodama.models.VoiceModel

class HomeViewModel : ViewModel() {
    private val _data = MutableLiveData<List<VoiceModel>>() // Örnek veri tipi
    val data: LiveData<List<VoiceModel>> get() = _data

    private val db = FirebaseFirestore.getInstance()

    fun fetchVoices() {
        Log.d("HomeViewModel", "fetchVoices: ")
        db.collection("all-voices")
            .get()
            .addOnSuccessListener { result ->
                val dataList = result.map { document ->
                    VoiceModel(
                        name = document.getString("name") ?: "",
                        id = document.id,
                        imageUrl = document.getString("imageUrl") ?: "",
                        createdAt = document.getTimestamp("createdAt") ?: Timestamp.now(),
                        model_name = document.getString("model_name") ?: "",
                        category = document.get("category") as? List<String> ?: listOf(),
                        allTimeCounter = document.getLong("allTimeCounter") ?: 0,
                        weeklyCounter = document.getLong("weeklyCounter") ?: 0,
                        charUsedCount = document.getLong("charUsedCount") ?: 0
                    )
                }

                Log.d("Firebase", "Data List: $dataList") // Verilerin geldiğini kontrol et
                _data.value = dataList
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Veri çekme hatası: ${e.message}") // Hata varsa göster
            }
    }

}