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
    private val TAG = SplashScreen::class.java.simpleName
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

        Log.d("rvcat", "ilk  $reveneuCatKey ")


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

        lifecycleScope.launch {
            DataStoreManager.savedUid(this@SplashScreen, user.uid)
        }
        configureRevenueCat(user.uid)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun configureRevenueCat(userId: String) {
        Purchases.logLevel = LogLevel.DEBUG

        Log.d("rvcat", "configureRevenueCat: $reveneuCatKey ")

        if (!Purchases.isConfigured) {
            Purchases.configure(
                PurchasesConfiguration.Builder(this, reveneuCatKey)
                    .appUserID(userId)
                    .build()
            )
            Purchases.sharedInstance.collectDeviceIdentifiers()
        }

        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                Toast.makeText(this, "There is an issue with your configuration.", Toast.LENGTH_SHORT).show()
            },
            onSuccess = { customerInfo ->
                customerInfo.entitlements["Subscription"]?.let { entitlement ->
                val isActive = entitlement.isActive
                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(this@SplashScreen, isActive)
                    }
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update(mapOf("subscriptionActive" to isActive))
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Abonelik durumu başarıyla güncellendi")
                            handleCustomerInfo(isActive)
                        } else {
                            Log.e(TAG, "Abonelik durumu güncellenemedi: ${task.exception}")
                        }
                    }
            } },
        )

    }



    private fun handleCustomerInfo(isActive: Boolean) {

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

