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
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.fragments.RecordVoiceFragment
import com.revenuecat.purchases.*
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
        Log.d(TAG, "checkUserAndProceed: 2")
        val currentUser = auth.currentUser
        Log.d(TAG, "checkUserAndProceed: 3 $currentUser")
        if (currentUser != null) {
            Log.d(TAG, "checkUserAndProceed: oturum aciksa 4")
            proceedWithUser(currentUser)
        } else {
            
            Log.d(TAG, "checkUserAndProceed: oturumn acik degilse  4")
            signInAnonymously { user ->
                user?.let {
                    Log.d(TAG, "checkUserAndProceed: 5 $it")
                    proceedWithUser(it)
                } ?: run {
                    Log.d(TAG, "checkUserAndProceed: 5 anonim giris basairiiz")
                    Toast.makeText(this, "Failed to authenticate.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signInAnonymously(callback: (FirebaseUser?) -> Unit) {
        Log.d(TAG, "signInAnonymously: 6")
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SplashScreen", "signInAnonymously:success")
                    callback(auth.currentUser)
                } else {
                    Log.w("SplashScreen", "signInAnonymously:failure", task.exception)
                    callback(null)
                }
            }
    }

    private fun proceedWithUser(user: FirebaseUser) {
        Log.d(TAG, "Sign in 7 : ${user.uid}")
        lifecycleScope.launch {
            DataStoreManager.savedUid(this@SplashScreen, user.uid)
        }
            setupPurchases(user.uid)
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
        val isActive = customerInfo.entitlements["Subscription"]?.isActive ?: false
        Log.d("isSubscribed SPLASH", "$isActive")
        lifecycleScope.launch {
            dataStoreManager.saveSubscriptionStatus(this@SplashScreen, isActive)
           // dataStoreManager.saveSubscriptionStatus(this@SplashScreen, true)

        }
        isSubscribed = isActive
        if (isActive) {
            startMainActivity()
        } else {
            startPaywallActivity()
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
}

