package com.proksi.kotodama.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore : DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "settings")

object DataStoreManager {
    private val UID = stringPreferencesKey("uid")

    suspend fun savedUid(context: Context,uid:String){
        context.dataStore.edit { preferences ->
            preferences[UID] = uid
        }
    }

    fun getUid(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[UID]
        }
    }
}