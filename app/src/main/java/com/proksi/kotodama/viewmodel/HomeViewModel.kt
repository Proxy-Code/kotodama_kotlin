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

    private var lastDocument: DocumentSnapshot? = null
    private val pageSize = 14
    private var _isLoading = false
    private var _isLastPage = false
    val isLoading: Boolean get() = _isLoading
    val isLastPage: Boolean get() = _isLastPage

    private var cloneSnapshotListener: ListenerRegistration? = null

    sealed class LoadingState {
        object Loading : LoadingState()
        object LoadingMore : LoadingState()
        object Success : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    // Ana ses verilerini yükle
    fun fetchVoices(category: String, context: Context, isInitialLoad: Boolean = true) {
        if (_isLoading) return

        _isLoading = true

        if (isInitialLoad) {
            _loadingState.value = LoadingState.Loading
            lastDocument = null
            _isLastPage = false
            // Sadece initial load'da listeyi temizle
            _allVoices.value = emptyList()
        } else {
            _loadingState.value = LoadingState.LoadingMore
        }

        val firestore = FirebaseFirestore.getInstance().collection("all-voices")
        var query = firestore
            .orderBy("allTimeCounter", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        lastDocument?.let { document ->
            query = query.startAfter(document)
        }

        query.get()
            .addOnSuccessListener { querySnapshot ->
                _isLoading = false

                if (querySnapshot.isEmpty) {
                    _isLastPage = true
                    _loadingState.value = LoadingState.Success
                    return@addOnSuccessListener
                }

                lastDocument = querySnapshot.documents[querySnapshot.size() - 1]

                val voiceList = mutableListOf<VoiceModel>()
                for (document in querySnapshot) {
                    val voiceItem = document.toObject(VoiceModel::class.java)
                    voiceItem?.id = document.id
                    voiceItem?.let {
                        it.isClone = false // Firestore'dan gelenler klon değil
                        voiceList.add(it)
                    }
                }

                if (isInitialLoad) {
                    // İlk yükleme: sadece Firestore verilerini ayarla, klonlar sonra eklenecek
                    _allVoices.value = voiceList
                    // İlk yüklemede klonları kontrol et
                    setupCloneListener(context)
                } else {
                    // Daha fazla yükleme: mevcut listeye ekle (klonları koru)
                    val currentList = _allVoices.value?.toMutableList() ?: mutableListOf()
                    // Sadece klon olmayan öğeleri filtrele (create_voice ve user clones hariç)
                    val nonCloneItems = currentList.filter {
                        !it.isClone || it.id == "create_voice"
                    }.toMutableList()
                    nonCloneItems.addAll(voiceList)
                    _allVoices.value = nonCloneItems
                }

                _loadingState.value = LoadingState.Success
                Log.d("Pagination", "Loaded ${voiceList.size} items, Total: ${_allVoices.value?.size ?: 0}")

                if (voiceList.size < pageSize) {
                    _isLastPage = true
                    Log.d("Pagination", "Last page reached")
                }
            }
            .addOnFailureListener { exception ->
                _isLoading = false
                _loadingState.value = LoadingState.Error(exception.message ?: "Unknown error")
                Log.e("Pagination", "Error: ${exception.message}")
            }
    }

    // Klon listener'ını kur
    private fun setupCloneListener(context: Context) {
        // Önceki listener'ı temizle
        cloneSnapshotListener?.remove()

        viewModelScope.launch {
            DataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(uid)
                    val clonesRef = userDocRef.collection("clones")

                    cloneSnapshotListener = clonesRef.addSnapshotListener { cloneDocumentSnapshot, exception ->
                        if (exception != null) {
                            Log.e("CloneListener", "Error: ${exception.message}")
                            _cloneCount.value = 0
                            updateVoicesWithClones(emptyList())
                            return@addSnapshotListener
                        }

                        val cloneCount = cloneDocumentSnapshot?.size() ?: 0
                        _cloneCount.value = cloneCount

                        val cloneList = mutableListOf<VoiceModel>()
                        if (cloneCount > 0) {
                            for (document in cloneDocumentSnapshot!!.documents) {
                                val cloneData = document.data
                                val id = document.id
                                val name = cloneData?.get("name") as? String ?: "Unnamed Clone"
                                val imageUrl = (cloneData?.get("imageUrl") ?: "") as? String ?: ""
                                val createdAt = (cloneData?.get("createdAt") ?: Timestamp.now()) as? Timestamp ?: Timestamp.now()
                                val modelName = (cloneData?.get("model_name") ?: "") as? String ?: ""

                                val cloneVoiceModel = VoiceModel(
                                    name = name,
                                    id = id,
                                    imageUrl = imageUrl,
                                    createdAt = createdAt,
                                    model_name = modelName,
                                    category = emptyList(),
                                    isClone = true
                                )
                                cloneList.add(cloneVoiceModel)
                            }
                        }

                        updateVoicesWithClones(cloneList)
                    }
                }
            }
        }
    }

    // Klonları ana listeye ekle
    private fun updateVoicesWithClones(cloneList: List<VoiceModel>) {
        val currentList = _allVoices.value?.toMutableList() ?: mutableListOf()

        // Create voice item'ını oluştur veya bul
        val createVoiceItem = createVoiceItem()

        // Mevcut listeden tüm klonları ve create_voice'u temizle
        val cleanedList = currentList.filter {
            !it.isClone && it.id != "create_voice"
        }.toMutableList()

        // Yeni listeyi oluştur: create_voice + klonlar + diğer sesler
        val newList = mutableListOf<VoiceModel>().apply {
            add(createVoiceItem) // Her zaman ilk eleman
            addAll(cloneList)    // Kullanıcı klonları
            addAll(cleanedList)  // Firestore'dan gelen sesler
        }

        _allVoices.value = newList
        Log.d("CloneUpdate", "Updated list with ${cloneList.size} clones, total: ${newList.size}")
    }

    private fun createVoiceItem(): VoiceModel {
        return VoiceModel(
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
    }

    fun loadMoreVoices(context: Context) {
        if (!_isLoading && !_isLastPage) {
            Log.d("Pagination", "Loading more voices...")
            fetchVoices("all", context, false)
        }
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

    override fun onCleared() {
        super.onCleared()
        cloneSnapshotListener?.remove()
    }
}