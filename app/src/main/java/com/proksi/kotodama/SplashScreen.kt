package com.proksi.kotodama

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.kotodama.app.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback

import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var auth: FirebaseAuth
    private lateinit var remoteConfig: FirebaseRemoteConfig

    companion object {
        private const val SPLASH_DELAY = 2000L
        private const val RC_FETCH_INTERVAL = 3600L
        private const val REVENUE_CAT_KEY = "goog_eeDwptXJEvGAUloteeRtTognxZr"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        dataStoreManager = DataStoreManager
        auth = Firebase.auth

        Handler(Looper.getMainLooper()).postDelayed({
            continueWithAppFlow()
        }, SPLASH_DELAY)
    }

    private fun continueWithAppFlow() {
        val currentUser = auth.currentUser
        currentUser?.let {
            proceedWithUser(it)
        } ?: signInAnonymously { user ->
            user?.let { proceedWithUser(it) }
        }
        //setupRemoteConfig()
        startMainActivity()
    }

    private fun signInAnonymously(callback: (FirebaseUser?) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SplashScreen", "signInAnonymously:success")
                    callback(auth.currentUser)
                } else {
                    Log.w("SplashScreen", "signInAnonymously:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }
    }

    private fun proceedWithUser(user: FirebaseUser) {
        Log.d("CURRENT USER", "Sign in: ${user.uid}")
        lifecycleScope.launch {
            DataStoreManager.savedUid(this@SplashScreen, user.uid)
        }
        setupPurchases(user.uid)
    }

    private fun setupRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = RC_FETCH_INTERVAL
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                val showOnboarding = if (task.isSuccessful) {
                    remoteConfig.getBoolean("show_onboarding")
                } else false
                decideNextActivity(showOnboarding)
            }
    }

    private fun decideNextActivity(showOnboarding: Boolean) {
        startMainActivity() // Currently always starting main activity
    }

    private fun startMainActivity() {
        Intent(this, MainActivity::class.java).also {
            startActivity(it)
        }
        finish()
    }

    private fun setupPurchases(userId: String) {
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(this, REVENUE_CAT_KEY)
                .appUserID(userId)
                .build()
        )
        Purchases.sharedInstance.collectDeviceIdentifiers()

        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                handleCustomerInfo(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                Log.e("CustomerInfoError", "Error fetching customer info: ${error.message}")
            }
        })
    }

    private fun handleCustomerInfo(customerInfo: CustomerInfo) {
        val isActive = customerInfo.entitlements["subscription"]?.isActive ?: false
        Log.d("isSubscribed SPLASH", "$isActive")
        lifecycleScope.launch {
            dataStoreManager.saveSubscriptionStatus(this@SplashScreen, isActive)
           // dataStoreManager.saveSubscriptionStatus(this@SplashScreen, true)
        }
        fetchAndDisplayOfferings()
    }

    private fun fetchAndDisplayOfferings() {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val currentOfferings = offerings.current
                currentOfferings?.availablePackages?.forEach { pkg ->
                    Log.d("RevenueCat", "Offering: ${pkg.offering}")
                    // Save package details if necessary
                } ?: Log.d("Offerings", "No offerings available")
            }

            override fun onError(error: PurchasesError) {
                Log.e("OfferingsError", "Error fetching offerings: ${error.message}")
            }
        })
    }
}
