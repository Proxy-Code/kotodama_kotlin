package com.proksi.kotodama.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.UserLibrary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserLibraryViewModel : ViewModel() {

        private val _libraryItems = MutableLiveData<List<UserLibrary>>()
        val libraryItems: LiveData<List<UserLibrary>> = _libraryItems
        private var registration: ListenerRegistration? = null
        fun fetchUserLibrary(context: Context) {
            viewModelScope.launch {
                val uid = DataStoreManager.getUid(context).first()
                if (uid != null) {
                    fetchItems(uid)
                } else {
                    _libraryItems.value = emptyList()
                }
            }
        }

    fun fetchItems(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(uid).collection("library")

        registration = userDocRef.addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                _libraryItems.value = emptyList()
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                val items = querySnapshot.documents.mapNotNull { document ->
                    val itemId = document.id
                    val isGeneratingFirestore = document.getBoolean("isGenerating")
                    document.toObject(UserLibrary::class.java)?.apply {
                        this.id = itemId
                        if (isGeneratingFirestore != null) {
                            this.isGenerating = isGeneratingFirestore
                        }
                    }
                }

                // Sort the items by createdAt in descending order
                val sortedItems = items.sortedByDescending { it.createdAt }

                _libraryItems.value = sortedItems
            } else {
                _libraryItems.value = emptyList()
            }
        }
    }


    fun removeFirestoreListener() {
            registration?.remove()
            registration = null // Set to null after removing the listener
        }



}