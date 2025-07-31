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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.fragments.RecordVoiceFragment
import com.revenuecat.purchases.*
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback

import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var auth: FirebaseAuth
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private var isSubscribed: Boolean = false
    private val TAG = SplashScreen::class.java.simpleName


    companion object {
        private const val SPLASH_DELAY = 2000L
        private const val RC_FETCH_INTERVAL = 3600L
        private const val REVENUE_CAT_KEY = "goog_ZFxhttuGLirLABneJJhRTbkNOst"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        dataStoreManager = DataStoreManager
        auth = Firebase.auth
        Log.d(TAG, "onCreate: 1")

        checkUserAndProceed()
    }

    private fun checkUserAndProceed() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            proceedWithUser(currentUser)
        } else {
            signInAnonymously { user ->
                user?.let {
                    proceedWithUser(it)
                } ?: run {
                    Toast.makeText(this, "Failed to authenticate.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signInAnonymously(callback: (FirebaseUser?) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    callback(auth.currentUser)
                } else {
                    callback(null)
                }
            }
    }

    private fun proceedWithUser(user: FirebaseUser) {

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {

                    val referralCode = document.getString("referralCode")
                    if (!referralCode.isNullOrEmpty()) {
                        Log.d(TAG, "User has a referral code: $referralCode")
                    } else {
                        Log.d(TAG, "No referral code for this user")
                        // referralCode yoksa yapılacak işlemler
                    }
                } else {
                    Log.d(TAG, "No such document for user")
                }
            }

        lifecycleScope.launch {
            DataStoreManager.savedUid(this@SplashScreen, user.uid)
        }
        configureRevenueCat(user.uid)
    }

    private fun configureRevenueCat(userId: String) {
        Purchases.logLevel = LogLevel.DEBUG

        // Sadece bir kere configure et
        if (!Purchases.isConfigured) {
            Purchases.configure(
                PurchasesConfiguration.Builder(this, REVENUE_CAT_KEY)
                    .appUserID(userId)
                    .build()
            )
            Purchases.sharedInstance.collectDeviceIdentifiers()
        }

        // opsiyonel ama iyi: .logIn ile tekrar eşle
        Purchases.sharedInstance.logIn(userId, object : LogInCallback {
            override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                Log.d(TAG, "RevenueCat logIn success")
                handleCustomerInfo(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                Log.e(TAG, "RevenueCat logIn failed: ${error.message}")
            }
        })
    }



    private fun handleCustomerInfo(customerInfo: CustomerInfo) {
        val isActive = customerInfo.entitlements["Subscription"]?.isActive ?: false
        Log.d(TAG, "handleCustomerInfo:isactive $isActive")

        lifecycleScope.launch {
          dataStoreManager.saveSubscriptionStatus(this@SplashScreen, isActive)
        }

        isSubscribed = isActive

        lifecycleScope.launch {
            if (dataStoreManager.isOnboardingCompleted(this@SplashScreen)) {
                if (isActive) {
                    startMainActivity()
                } else {
                    startPaywallActivity()
                }
            } else {
                startOnboardingActivity()
            }
        }

    }

    private fun startMainActivity() {
        Intent(this, MainActivity::class.java).also {
            startActivity(it)
        }
        finish()
    }

    private fun startPaywallActivity() {
        Intent(this, PaywallActivity::class.java).also {
            startActivity(it)
        }
        finish()
    }

    private fun startOnboardingActivity() {

        Intent(this, OnboardingActivity::class.java).also {
            startActivity(it)
        }
        finish()
    }
}

