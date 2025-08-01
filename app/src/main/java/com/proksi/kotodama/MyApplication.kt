package com.proksi.kotodama

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.appsflyer.AppsFlyerLib
import com.facebook.FacebookSdk
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.LanguagePreferences
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppsFlyerLib.getInstance().init(getString(R.string.appsflyer_api_key), null, this)
        AppsFlyerLib.getInstance().start(this)

        FacebookSdk.setClientToken(getString(R.string.client_token_facebook))
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.sdkInitialize(this)

        FirebaseApp.initializeApp(this)

        val firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }

    private fun applyLanguage(context: Context) {
        val languagePreferences = LanguagePreferences(context)

        runBlocking {
            val languageCode = languagePreferences.language.first()

            languageCode?.let {
                setLocale(context, it)
            }
        }
    }



    private fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)

        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
