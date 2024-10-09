package com.proksi.kotodama

import android.app.Application
import com.google.firebase.FirebaseApp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Purchases.configure(PurchasesConfiguration.Builder(this, "goog_eeDwptXJEvGAUloteeRtTognxZr").build())

    }
}
