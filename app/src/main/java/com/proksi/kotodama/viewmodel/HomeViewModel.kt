package com.proksi.kotodama.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.Category
import com.proksi.kotodama.models.VoiceModel
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel : ViewModel() {

    private val _allVoices = MutableLiveData<List<VoiceModel>>(emptyList())
    val allVoices: LiveData<List<VoiceModel>> get() = _allVoices

    private val _loadingState = MutableLiveData<LoadingState>()
    val loadingState: LiveData<LoadingState> get() = _loadingState

    private val _cloneCount = MutableLiveData<Int>(0)
    val cloneCount: LiveData<Int> get() = _cloneCount

    private var hasLoadedOnce = false
    private var _isLoading = false
    val isLoading: Boolean get() = _isLoading

    private var cloneSnapshotListener: ListenerRegistration? = null

    sealed class LoadingState {
        object Loading : LoadingState()
        object Success : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    /** Tüm sesleri tek seferde indirir (sayfalama YOK) */
    fun fetchVoicesOnce(context: Context, forceRefresh: Boolean = false) {
        if (_isLoading) return
        if (hasLoadedOnce && !forceRefresh) return // tekrar çağrıldıysa boşuna indirme

        _isLoading = true
        _loadingState.value = LoadingState.Loading

        val firestore = FirebaseFirestore.getInstance()
            .collection("all-voices")
            .orderBy("allTimeCounter", Query.Direction.DESCENDING)

        // İsteğe bağlı: önce cache, sonra sunucu (ilk algılanan gecikmeyi azaltır)
        firestore.get(Source.CACHE).addOnCompleteListener { _ ->
            firestore.get(Source.SERVER)
                .addOnSuccessListener { snap ->
                    _isLoading = false
                    hasLoadedOnce = true

                    val voiceList = snap.documents.mapNotNull { doc ->
                        doc.toObject(VoiceModel::class.java)?.also { vm ->
                            vm.id = doc.id
                            vm.isClone = false
                        }
                    }

                    // create_voice ve klonlar en tepeye eklenecek (listener sonra doldurur)
                    _allVoices.value = buildList {
                        add(createVoiceItem())
                        addAll(voiceList)
                    }

                    _loadingState.value = LoadingState.Success

                    // Klon dinleyicisini ilk yüklemeden sonra başlat
                    setupCloneListener(context)
                }
                .addOnFailureListener { e ->
                    _isLoading = false
                    _loadingState.value = LoadingState.Error(e.message ?: "Unknown error")
                }
        }
    }

    private fun setupCloneListener(context: Context) {
        cloneSnapshotListener?.remove()

        viewModelScope.launch {
            DataStoreManager.getUid(context).collect { uid ->
                if (uid == null) return@collect

                val clonesRef = FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("clones")

                cloneSnapshotListener = clonesRef.addSnapshotListener { ds, ex ->
                    if (ex != null) {
                        _cloneCount.value = 0
                        mergeClonesIntoList(emptyList())
                        return@addSnapshotListener
                    }

                    val list = ds?.documents?.map { d ->
                        val data = d.data ?: emptyMap<String, Any>()
                        VoiceModel(
                            name = data["name"] as? String ?: "Unnamed Clone",
                            id = d.id,
                            imageUrl = (data["imageUrl"] as? String).orEmpty(),
                            createdAt = (data["createdAt"] as? Timestamp) ?: Timestamp.now(),
                            model_name = (data["model_name"] as? String).orEmpty(),
                            category = emptyList(),
                            isClone = true
                        )
                    }.orEmpty()

                    _cloneCount.value = list.size
                    mergeClonesIntoList(list)
                }
            }
        }
    }

    /** Klonları mevcut listeye üstte ekler; ana listeyi yeniden indirmez */
    private fun mergeClonesIntoList(clones: List<VoiceModel>) {
        val current = _allVoices.value.orEmpty()

        val createVoice = createVoiceItem()
        val nonClone = current.filter { !it.isClone && it.id != "create_voice" }

        _allVoices.value = buildList {
            add(createVoice)
            addAll(clones)
            addAll(nonClone)
        }
    }

    private fun createVoiceItem(): VoiceModel = VoiceModel(
        name = "Create Voice",
        id = "create_voice",
        imageUrl = "R.drawable.sing_ai",
        createdAt = Timestamp.now(),
        model_name = "Create Voice",
        category = listOf("all", "trends"),
        allTimeCounter = 0,
        weeklyCounter = 0,
        charUsedCount = 0,
        isClone = true
    )

    /** İstersen manuel yenileme */
    fun refreshVoices(context: Context) {
        hasLoadedOnce = false
        fetchVoicesOnce(context, forceRefresh = true)
    }

    override fun onCleared() {
        super.onCleared()
        cloneSnapshotListener?.remove()
    }

    fun deleteClone(cloneId: String, context: Context) {
        viewModelScope.launch {
            DataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(uid)
                    val cloneDocRef = userDocRef.collection("clones").document(cloneId)

                    cloneDocRef.delete().addOnSuccessListener {
                        Log.d("DeleteClone", "Clone deleted from Firestore")
                        // Klon silindiğinde listener otomatik olarak güncelleyecek
                    }.addOnFailureListener { exception ->
                        Log.e("DeleteClone", "Error deleting clone: $exception")
                    }
                }
            }
        }
    }

}