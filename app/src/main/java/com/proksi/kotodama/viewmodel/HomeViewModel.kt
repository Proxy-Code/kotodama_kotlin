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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kotodama.app.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.Category
import com.proksi.kotodama.models.VoiceModel
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel : ViewModel() {
    private val _data = MutableLiveData<List<VoiceModel>>() // Filtrelenmiş ses listesini tutar
    private val _allVoices = MutableLiveData<List<VoiceModel>>() // Tüm ses listesini tutar
    val allVoices: LiveData<List<VoiceModel>> get() = _allVoices
    private val _hasClone = MutableLiveData<Boolean>().apply { value = false }  // Clone durumunu tutar
    val hasClone: LiveData<Boolean> get() = _hasClone
    private lateinit var dataStoreManager: DataStoreManager
    private var uid: String = ""

    // Ses listesini gözlemleyen LiveData
    val data: LiveData<List<VoiceModel>> get() = _data

    // Ses listesini Firestore'dan çeken fonksiyon
    fun fetchVoices(category: String, context: Context) {
        Log.d("category", "fetchVoices: $category")

        val firestore = FirebaseFirestore.getInstance().collection("all-voices")
        val date = Date()
        val timestamp = Timestamp(date)
        val createVoiceItem = VoiceModel(
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

        firestore
            .orderBy("allTimeCounter", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val voiceList = mutableListOf<VoiceModel>()
                for (document in querySnapshot) {
                    var voiceItem = document.toObject(VoiceModel::class.java)
                    voiceItem.id = document.id
                    voiceList.add(voiceItem)
                }
                _allVoices.value = voiceList

                Log.d("allvoices", "getVoicesByCategory: ${allVoices.value?.size} ")

                checkForCloneData(context, voiceList, createVoiceItem)
            }
    }

    // Kategoriye göre sesleri döndüren fonksiyon
    fun getVoicesByCategory(category: String, context: Context): List<VoiceModel> {
        Log.d("getbyctg", "getVoicesByCategory: ${allVoices.value?.size} ")
        val voicesList = allVoices.value ?: emptyList()
        val date = Date()
        val timestamp = Timestamp(date)
        val createVoiceItem = VoiceModel(
            name = "Create Voice",
            id = "create_voice",
            imageUrl = "R.drawable.sing_ai",
            createdAt = timestamp,
            model_name = "Create Voice",
            category = listOf("all", "trends"),
            allTimeCounter = 0,
            weeklyCounter = 0,
            charUsedCount = 0,

        )

        val filteredVoices = when (category) {
            "trends" -> voicesList.sortedByDescending { it.weeklyCounter }
            "new" -> voicesList.sortedByDescending { it.createdAt }
            "all" -> voicesList.sortedByDescending { it.allTimeCounter }
            else -> voicesList.filter { it.category.contains(category) }
        }

        checkForCloneData(context, filteredVoices.toMutableList(), createVoiceItem)

        return filteredVoices
    }

    // Clone verisini kontrol eden ve listeye ekleyen fonksiyon
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

                    // "clones" koleksiyonunu dinle
                    val clonesRef = userDocRef.collection("clones")
                    clonesRef.addSnapshotListener { cloneDocumentSnapshot, exception ->
                        if (exception != null) {
                            _hasClone.value = false
                            Log.e("CLONE", "Error listening for clone document changes: $exception")
                            // Clone olmadığı durumlarda "Create Voice" itemini ekleyin
                            voiceList.add(0, createVoiceItem)
                            _data.value = voiceList
                            return@addSnapshotListener
                        }

                        // **Tüm eski klonları temizle**
                        voiceList.removeAll { it.isClone }

                        val cloneCount = cloneDocumentSnapshot?.size() ?: 0
                        voiceList.add(0, createVoiceItem)  // "Create Voice" itemini en üste ekle

                        if (cloneCount > 0) {
                            _hasClone.value = true

                            // Klon dökümanlarını listeye ekleyin
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

                                voiceList.add(1, cloneVoiceModel)
                            }
                        }

                        _data.value = voiceList
                    }
                }
            }
        }
    }


    fun getCategoryList(): List<Category> {
        val categoryData = listOf(
            Triple("all", R.string.all, R.drawable.micro),
            Triple("trends", R.string.trends, R.drawable.trends),
            Triple("new", R.string.newCtg, R.drawable.neww),
            Triple("musicians", R.string.musicians, R.drawable.musicnota),
            Triple("tv-shows", R.string.tvShows, R.drawable.tv),
            Triple("actors", R.string.actors, R.drawable.actor),
            Triple("sports", R.string.sports, R.drawable.sport),
            Triple("fictional", R.string.fictional, R.drawable.fictional),
            Triple("rap", R.string.rap, R.drawable.rap),
            Triple("games", R.string.game, R.drawable.game),
            Triple("anime", R.string.anime, R.drawable.anime),
            Triple("kpop", R.string.kpop, R.drawable.kpop),
            Triple("random", R.string.random, R.drawable.random)
        )
        return categoryData.map { (id, text, image) ->
            Category(id, text, image)
        }
    }

    // Ses listesini arama sorgusuna göre filtreleyen fonksiyon
    fun filterVoices(query: String) {
        val filteredVoices = _allVoices.value?.filter { voiceModel ->
            voiceModel.name.contains(query, ignoreCase = true)
        } ?: emptyList()

        // Filtrelenmiş veriyi _data ile güncelle
        _data.value = filteredVoices
    }
}
