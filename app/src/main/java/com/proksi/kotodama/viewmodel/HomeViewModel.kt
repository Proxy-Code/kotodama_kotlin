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

        when (category) {

            "trends" -> {
                firestore
                    .orderBy("weeklyCounter", Query.Direction.DESCENDING)
                    .limit(11)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val voiceList = mutableListOf<VoiceModel>()
                        for (document in querySnapshot) {
                            var voiceItem = document.toObject(VoiceModel::class.java)
                            voiceItem.id = document.id
                            voiceList.add(voiceItem)

                        }
                        checkForCloneData(context,voiceList, createVoiceItem)

                    }
            }
            "new" -> {
                firestore
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(11)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val voiceList = mutableListOf<VoiceModel>()
                        for (document in querySnapshot) {
                            var voiceItem = document.toObject(VoiceModel::class.java)
                            voiceItem.id = document.id
                            voiceList.add(voiceItem)
                        }
                        checkForCloneData(context,voiceList, createVoiceItem)
                    }
            }
            "all" -> {
                firestore
                    .orderBy("allTimeCounter", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val voiceList = mutableListOf<VoiceModel>()
                        for (document in querySnapshot) {
                            var voiceItem = document.toObject(VoiceModel::class.java)
                            voiceItem.id = document.id
                            voiceList.add(voiceItem)
//
//                            if (voiceList.size <= 5) {
//                                imagesFive.add(document.id)
//                            }
                        }
                        checkForCloneData(context,voiceList, createVoiceItem)

                       // _data.value=voiceList
                    }
            }
            else -> {
                FirebaseFirestore.getInstance().collection("all-voices")
                    .whereArrayContains("category", category)
                    //.orderBy("allTimeCounter", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val voiceList = mutableListOf<VoiceModel>()
                        for (document in querySnapshot) {
                            var voiceItem = document.toObject(VoiceModel::class.java)
                            voiceItem.id = document.id
                            voiceList.add(voiceItem)
                        }
                        checkForCloneData(context,voiceList, createVoiceItem)

                        Log.d("Firestore", "Fetched ${voiceList.size} items for category: $category")
                    }
                    .addOnFailureListener { e ->
                        Log.d("Firestore", "Error fetching voices: ${e.message}")
                    }
            }

        }
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
                            Log.e("CLONE", "Error listening for clone document changes: $exception")
                            voiceList.add(0, createVoiceItem)
                            return@addSnapshotListener
                        }

                        voiceList.removeAll { it.id == "create_voice" }  // Remove any previous clone items

                        val cloneCount = cloneDocumentSnapshot?.size() ?: 0
                        when (cloneCount) {
                            0 -> {
                                voiceList.add(0, createVoiceItem)
                                _data.value = voiceList
                            }
                            1 -> {
                                val cloneData = cloneDocumentSnapshot?.documents?.first()?.data
                                val name = (cloneData?.get("name")) as String
                                val imageUrl = (cloneData["imageUrl"] ?: "") as String
                                val createdAt = (cloneData["createdAt"] ?: Timestamp.now()) as Timestamp
                                val modelName = (cloneData["model_name"] ?: "") as String

                                val cloneVoiceModel = VoiceModel(
                                    name = name,
                                    id = "create_voice",  // Use appropriate ID if available
                                    imageUrl = imageUrl,
                                    createdAt = createdAt,
                                    model_name = modelName,
                                    category = emptyList()  // Set if available
                                )

                                voiceList.add(0, cloneVoiceModel)
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

}