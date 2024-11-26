package com.proksi.kotodama

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.appsflyer.AppsFlyerLib
import com.google.firebase.FirebaseApp
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

        FirebaseApp.initializeApp(this)
      //  Purchases.configure(PurchasesConfiguration.Builder(this, "goog_eeDwptXJEvGAUloteeRtTognxZr").build())

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
