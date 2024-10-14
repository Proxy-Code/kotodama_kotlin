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
    private val _data = MutableLiveData<List<VoiceModel>>() // Örnek veri tipi
    private val _allVoices = MutableLiveData<List<VoiceModel>>() // Örnek veri tipi
    val allVoices: LiveData<List<VoiceModel>> get() = _allVoices
    private val _hasClone = MutableLiveData<Boolean>().apply { value = false }  // LiveData olarak tanımladık
    val hasClone: LiveData<Boolean> get() = _hasClone
    private lateinit var dataStoreManager: DataStoreManager
    private var uid : String= ""

    val data: LiveData<List<VoiceModel>> get() = _data

    fun fetchVoices(category : String, context: Context) {
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
            category = listOf("all,trends"),
            allTimeCounter = 0,
            weeklyCounter = 0,
            charUsedCount = 0
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
                    _allVoices.value = voiceList

                }
                checkForCloneData(context,voiceList, createVoiceItem)

            }
    }

    fun getVoicesByCategory(category: String, context: Context): List<VoiceModel> {
        // Get the current value of allVoices safely using the value property
        val voicesList = allVoices.value ?: emptyList()
        val date = Date()
        val timestamp = Timestamp(date)
        val createVoiceItem = VoiceModel(
            name = "Create Voice",
            id = "create_voice",
            imageUrl = "R.drawable.sing_ai",
            createdAt = timestamp,
            model_name = "Create Voice",
            category = listOf("all,trends"),
            allTimeCounter = 0,
            weeklyCounter = 0,
            charUsedCount = 0
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

    private fun checkForCloneData(
        context: Context,
        voiceList: MutableList<VoiceModel>,
        createVoiceItem: VoiceModel) {
        dataStoreManager = DataStoreManager
        viewModelScope.launch {
            dataStoreManager.getUid(context).collect { uid ->
                if (uid != null) {
                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(uid)

                    // Listen to the "clones" collection for real-time updates
                    val clonesRef = userDocRef.collection("clones")
                    clonesRef.addSnapshotListener { cloneDocumentSnapshot, exception ->
                        if (exception != null) {
                            _hasClone.value = false
                            Log.e("CLONE", "Error listening for clone document changes: $exception")
                            voiceList.add(0, createVoiceItem)
                            return@addSnapshotListener
                        }

                        voiceList.removeAll { it.id == "create_voice" }  // Remove any previous clone items

                        val cloneCount = cloneDocumentSnapshot?.size() ?: 0
                        voiceList.add(0, createVoiceItem)
                        when (cloneCount) {
                            1 -> {
                                val cloneData = cloneDocumentSnapshot?.documents?.first()?.data
                                val id=cloneDocumentSnapshot?.documents?.first()?.id
                                val name = (cloneData?.get("name")) as String
                                val imageUrl = (cloneData["imageUrl"] ?: "") as String
                                val createdAt = (cloneData["createdAt"] ?: Timestamp.now()) as Timestamp
                                val modelName = (cloneData["model_name"] ?: "") as String

                                val cloneVoiceModel = id?.let {
                                    VoiceModel(
                                        name = name,
                                        id = it,  // Use appropriate ID if available
                                        imageUrl = imageUrl,
                                        createdAt = createdAt,
                                        model_name = modelName,
                                        category = emptyList()  // Set if available
                                    )
                                }
                                Log.d("adapterraa", "checkForCloneData: 1 da ")
                                _hasClone.value = true
                                if (cloneVoiceModel != null) {
                                    voiceList.add(1, cloneVoiceModel)
                                }
                                _data.value = voiceList
                            }
                            else -> {
                                Log.d("CLONE", "Clones count: $cloneCount")
                            }
                        }
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

    fun filterVoices(query: String) {
        val filteredVoices = _allVoices.value?.filter { voiceModel ->
            voiceModel.name.contains(query, ignoreCase = true) // Burada isme göre filtreleme yapıyoruz
        } ?: emptyList()
        _data.value = filteredVoices // Filtrelenmiş veriyi güncelle
    }

}