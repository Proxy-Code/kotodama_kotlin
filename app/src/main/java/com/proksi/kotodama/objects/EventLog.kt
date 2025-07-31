package com.proksi.kotodama.objects

import android.content.Context
import android.os.Bundle
import com.appsflyer.AppsFlyerLib
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics

object EventLogger {

    fun logEvent(context: Context, eventName: String) {
        // Firebase Analytics
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val eventBundle = Bundle().apply {
            putString(eventName, eventName)
        }
        firebaseAnalytics.logEvent(eventName, eventBundle)

        // AppsFlyer
        val eventValue = HashMap<String, Any>()
        eventValue[eventName] = eventName
        AppsFlyerLib.getInstance().logEvent(context, eventName, eventValue)

    }
}
