package com.proksi.kotodama

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
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
    private val TAG = "Splash Screen"
    private lateinit var reveneuCatKey:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        
      reveneuCatKey = this.packageManager
             .getApplicationInfo(this.packageName, PackageManager.GET_META_DATA)
             .metaData.getString("REVENEUCAT_KEY").orEmpty()
        
             dataStoreManager = DataStoreManager
             auth = Firebase.auth

        checkUserAndProceed() 
      
    }

    private fun checkUserAndProceed() {
        Log.d(TAG, "checkUserAndProceed: burda 1 ")
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "checkUserAndProceed: burda 2 ")
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
        Log.d(TAG, "checkUserAndProceed: burda 3 ")

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
        Log.d(TAG, "checkUserAndProceed: burda 4 ")


        lifecycleScope.launch {
            DataStoreManager.savedUid(this@SplashScreen, user.uid)
        }
        configureRevenueCat(user.uid)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun configureRevenueCat(userId: String) {
        Log.d(TAG, "checkUserAndProceed: burda 5 ")

        Purchases.logLevel = LogLevel.DEBUG

        Log.d("rvcat", "configureRevenueCat: $reveneuCatKey ")

        if (!Purchases.isConfigured) {
            Log.d(TAG, "checkUserAndProceed: burda 6 ")

            Purchases.configure(
                PurchasesConfiguration.Builder(this, reveneuCatKey)
                    .appUserID(userId)
                    .build()
            )
            Log.d(TAG, "checkUserAndProceed: burda 7 ")
            Purchases.sharedInstance.collectDeviceIdentifiers()
        }



        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {

            override fun onReceived(customerInfo: CustomerInfo) {
                Log.d("paywall 2", "custome info $customerInfo")
                Log.d(TAG, "onError: burda 9 $customerInfo")


                lifecycleScope.launch {
                    val isActive = customerInfo.entitlements["subscription"]?.isActive ?: false

                    dataStoreManager.saveSubscriptionStatus(this@SplashScreen, isActive)

                    handleCustomerInfo(isActive)

                }
            }

            override fun onError(error: PurchasesError) {
                Log.d(TAG, "onError: burda 10")

                Log.e("Subscription", "Error: ${error.message}")
            }
        })

    }



    private fun handleCustomerInfo(isActive: Boolean) {
        Log.d(TAG, "handleCustomerInfo da")

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

