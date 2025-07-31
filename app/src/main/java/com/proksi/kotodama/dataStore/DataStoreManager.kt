package com.proksi.kotodama.dataStore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore : DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "settings")

object DataStoreManager {
    private val UID = stringPreferencesKey("uid")
    private val SUBSCRIPTION_STATUS_KEY = booleanPreferencesKey("subscription_status")
    private val USER_TEXT_KEY = stringPreferencesKey("user_text")
    private val REFERRAL_CODE = stringPreferencesKey("refferal_code")
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    private const val FEEDBACK_SHOWN_KEY = "feedback_shown_key"
    private val PLAN_TYPE = booleanPreferencesKey("plan_type")
    private val LAUNCH_COUNT_KEY = intPreferencesKey("launch_count")


    suspend fun incrementLaunchCount(context: Context) {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[LAUNCH_COUNT_KEY] ?: 0
            preferences[LAUNCH_COUNT_KEY] = currentCount + 1
        }
    }

    suspend fun getLaunchCount(context: Context): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[LAUNCH_COUNT_KEY] ?: 0 // Provide default value
        }

    suspend fun saveOnboardingCompleted(context: Context, isCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(ONBOARDING_COMPLETED_KEY)] = isCompleted
        }
    }

    suspend fun isOnboardingCompleted(context: Context): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[booleanPreferencesKey(ONBOARDING_COMPLETED_KEY)] ?: false
    }

    suspend fun saveFeedbackShown(context: Context, isShown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(FEEDBACK_SHOWN_KEY)] = isShown
        }
    }

    suspend fun savePlanType(context: Context, planType: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUBSCRIPTION_STATUS_KEY] = planType
        }
    }

    fun getPlanType(context:Context):Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PLAN_TYPE]?: false
        }
    }


    suspend fun isFeedbackShown(context: Context): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[booleanPreferencesKey(FEEDBACK_SHOWN_KEY)] ?: false
    }



    suspend fun savedUid(context: Context,uid:String){
        context.dataStore.edit { preferences ->
            preferences[UID] = uid
        }
    }

    suspend fun saveReferral(context: Context,referral: String) {
        context.dataStore.edit { preferences ->
            preferences[REFERRAL_CODE] = referral
        }
    }

    fun getReferral(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[REFERRAL_CODE]
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
        Log.d("aaaaaaa", "saveText: $text")
    }

    fun getSubscriptionStatusKey(context:Context):Flow<Boolean> {
        Log.d("onsuccessde", "getSubscriptionStatusKey: ")
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

class LanguagePreferences(private val context: Context) {
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    val language: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY]
        }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { settings ->
            settings[LANGUAGE_KEY] = language
        }
    }
}