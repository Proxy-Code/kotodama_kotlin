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
import com.google.firebase.firestore.Query
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.Category
import com.proksi.kotodama.models.VoiceModel
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel : ViewModel() {
    val data: LiveData<List<VoiceModel>> get() = _allVoices
    private val _allVoices = MutableLiveData<List<VoiceModel>>() // Tüm ses listesini tutar
    val allVoices: LiveData<List<VoiceModel>> get() = _allVoices
    private val _hasClone = MutableLiveData<Boolean>().apply { value = false }  // Clone durumunu tutar
    private val _loadingState = MutableLiveData<LoadingState>()
    val loadingState: LiveData<LoadingState> get() = _loadingState
    private var lastDocument: DocumentSnapshot? = null
    private val pageSize = 15 // Sayfa başına item sayısı
    private var _isLoading = false
    private var _isLastPage = false
    val isLoading: Boolean get() = _isLoading
    val isLastPage: Boolean get() = _isLastPage

    private lateinit var dataStoreManager: DataStoreManager
    private val _text = MutableLiveData<String>()
    val text: LiveData<String> get() = _text
    private val _enteredText = MutableLiveData<String>()
    val enteredText: LiveData<String> get() = _enteredText
    private val _cloneCount = MutableLiveData<Int>()
    val cloneCount: LiveData<Int> get() = _cloneCount

    sealed class LoadingState {
        object Loading : LoadingState()
        object LoadingMore : LoadingState()
        object Success : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    fun fetchVoices(category: String, context: Context, isInitialLoad: Boolean = true) {
        if (_isLoading) return

        _isLoading = true

        if (isInitialLoad) {
            _loadingState.value = LoadingState.Loading
            lastDocument = null
            _isLastPage = false
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
                    voiceItem?.let { voiceList.add(it) }
                }

                // İlk eleman olarak createVoiceItem ekle
                val createVoiceItem = createVoiceItem()
                val voicesWithCreateItem = mutableListOf<VoiceModel>().apply {
                    add(createVoiceItem) // İlk eleman olarak ekle
                    addAll(voiceList)
                }

                val currentList = if (isInitialLoad) {
                    voicesWithCreateItem
                } else {
                    // Daha fazla yükleme yaparken, createVoiceItem'ı koru
                    val existingList = _allVoices.value ?: emptyList()
                    val existingWithoutCreate = existingList.filter { it.id != "create_voice" }
                    val newList = mutableListOf<VoiceModel>().apply {
                        add(createVoiceItem)
                        addAll(existingWithoutCreate)
                        addAll(voiceList)
                    }
                    newList
                }

                _allVoices.value = currentList
                _loadingState.value = LoadingState.Success

                Log.d("Pagination", "Loaded ${voiceList.size} items, Total: ${currentList.size}")

                // İlk yüklemede clone kontrolü yap
                if (isInitialLoad) {
                    checkForCloneData(context, currentList.toMutableList(), createVoiceItem)
                }

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

    fun loadMoreVoices(context: Context) {
        if (!_isLoading && !_isLastPage) {
            Log.d("Pagination", "Loading more voices...")
            fetchVoices("all", context, false)
        }
    }

    private fun createVoiceItem(): VoiceModel {
        val date = Date()
        val timestamp = Timestamp(date)
        return VoiceModel(
            name = "Create Voice",
            id = "create_voice",
            imageUrl = "R.drawable.sing_ai",
            createdAt = timestamp,
            model_name = "Create Voice",
            category = listOf("all", "trends"),
            allTimeCounter = 0,
            weeklyCounter = 0,
            charUsedCount = 0,
            isClone = true
        )
    }

    private fun checkForCloneData(
        context: Context,
        voiceList: MutableList<VoiceModel>,
        createVoiceItem: VoiceModel
    ) {
        dataStoreManager = DataStoreManager
        viewModelScope.launch {
            dataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(uid)

                    val clonesRef = userDocRef.collection("clones")
                    clonesRef.addSnapshotListener { cloneDocumentSnapshot, exception ->
                        if (exception != null) {
                            _hasClone.value = false
                            // Create voice item zaten eklendi, sadece cloneCount'u güncelle
                            _cloneCount.value = 0
                            return@addSnapshotListener
                        }

                        // Mevcut listeden klonları temizle (create_voice hariç)
                        voiceList.removeAll { it.isClone && it.id != "create_voice" }

                        val cloneCount = cloneDocumentSnapshot?.size() ?: 0
                        _cloneCount.value = cloneCount

                        if (cloneCount > 0) {
                            _hasClone.value = true

                            // Klon dökümanlarını listeye ekleyin (create_voice'tan sonra)
                            for (document in cloneDocumentSnapshot!!.documents) {
                                val cloneData = document.data
                                val id = document.id
                                val name = cloneData?.get("name") as String
                                val imageUrl = (cloneData["imageUrl"] ?: "") as String
                                val createdAt = (cloneData["createdAt"] ?: Timestamp.now()) as Timestamp
                                val modelName = (cloneData["model_name"] ?: "") as String

                                // Klon verilerini içeren VoiceModel oluştur
                                val cloneVoiceModel = VoiceModel(
                                    name = name,
                                    id = id,
                                    imageUrl = imageUrl,
                                    createdAt = createdAt,
                                    model_name = modelName,
                                    category = emptyList(),
                                    isClone = true  // Klon olarak işaretle
                                )

                                // Create voice item'ından sonra ekle
                                val createVoiceIndex = voiceList.indexOfFirst { it.id == "create_voice" }
                                if (createVoiceIndex != -1) {
                                    voiceList.add(createVoiceIndex + 1, cloneVoiceModel)
                                } else {
                                    voiceList.add(0, cloneVoiceModel)
                                }
                            }
                        } else {
                            _hasClone.value = false
                        }
                        _allVoices.value = voiceList
                    }
                }
            }
        }
    }

    fun filterVoices(query: String) {
        val filteredVoices = _allVoices.value?.filter { voiceModel ->
            voiceModel.name.contains(query, ignoreCase = true)
        } ?: emptyList()
        // Filtrelenmiş verileri göstermek için ayrı bir LiveData kullanabilirsiniz
        // _filteredVoices.value = filteredVoices
    }

    fun deleteClone(cloneId: String, context: Context) {
        viewModelScope.launch {
            dataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(uid)
                    val cloneDocRef = userDocRef.collection("clones").document(cloneId)

                    cloneDocRef.delete().addOnSuccessListener {
                        Log.d("DeleteClone", "Clone deleted from Firestore")

                        // Mevcut listeyi al
                        val currentList = _allVoices.value?.toMutableList() ?: mutableListOf()

                        // Silinecek klonu bul
                        val cloneToRemove = currentList.find { it.id == cloneId }

                        if (cloneToRemove != null) {
                            // Klonu listeden kaldır
                            currentList.remove(cloneToRemove)
                            _allVoices.value = currentList

                            // Klon sayısını güncelle
                            val cloneCount = currentList.count { it.isClone && it.id != "create_voice" }
                            _cloneCount.value = cloneCount

                            Log.d("DeleteClone", "Clone removed. Total items: ${currentList.size}, Clones: $cloneCount")
                        } else {
                            Log.d("DeleteClone", "Clone not found in current list")
                        }
                    }.addOnFailureListener { exception ->
                        Log.e("DeleteClone", "Error deleting clone: $exception")
                    }
                }
            }
        }
    }

    fun updateEnteredText(text: String) {
        Log.d("Observed entered text", "updateEnteredText: $text ")
        _enteredText.value = text
    }
}
