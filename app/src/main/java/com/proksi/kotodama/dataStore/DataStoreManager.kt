package com.proksi.kotodama.dataStore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore : DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "settings")

object DataStoreManager {
    private val UID = stringPreferencesKey("uid")
    private val SUBSCRIPTION_STATUS_KEY = booleanPreferencesKey("subscription_status")
    private val USER_TEXT_KEY = stringPreferencesKey("user_text")
    suspend fun savedUid(context: Context,uid:String){
        context.dataStore.edit { preferences ->
            preferences[UID] = uid
        }
    }

    suspend fun saveSubscriptionStatus(context: Context,isActive: Boolean) {
        context.dataStore.edit { preferences ->
            Log.d("onsuccessde", "saveSubscriptionStatus: $isActive ")
            preferences[SUBSCRIPTION_STATUS_KEY] = isActive
        }
    }

    suspend fun saveText(context: Context,text: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_TEXT_KEY] = text
        }
    }

    fun getSubscriptionStatusKey(context:Context):Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SUBSCRIPTION_STATUS_KEY]?: false
        }
    }
    fun getUid(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[UID]
        }
    }

    fun getText(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TEXT_KEY]
        }
    }
}