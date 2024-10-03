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
import com.proksi.kotodama.fragments.DataStoreManager
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var auth: FirebaseAuth
    private lateinit var remoteConfig: FirebaseRemoteConfig


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        dataStoreManager = DataStoreManager
        auth = Firebase.auth

        Handler(Looper.getMainLooper()).postDelayed({
            continueWithAppFlow()
        }, 2000) // 2000 milliseconds = 2 seconds
    }

    private fun continueWithAppFlow() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            signInAnonymously { user ->
                user?.let {
                    Log.d("CURRENT USER", "Sign in: ${user.uid}")
                    lifecycleScope.launch {
                        DataStoreManager.savedUid(this@SplashScreen, user.uid)
                    }
                    startMainActivity()
                }
            }
        } else {
            lifecycleScope.launch {
                DataStoreManager.savedUid(this@SplashScreen, currentUser.uid)
            }
         ///   setupPurchases(currentUser.uid)
        }
        setupRemoteConfig()
    }

    private fun signInAnonymously(callback: (FirebaseUser?) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("splashscreen", "signInAnonymously:success")
                    val user = auth.currentUser
                    callback(user) // Callback ile kullanıcıyı döndür
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("splashscreen", "signInAnonymously:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    callback(null) // İşlem başarısız olduğunda null döndür
                }
            }
    }

    // a/b testing icin remoteconfig yapilir
    private fun setupRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    val showOnboarding = remoteConfig.getBoolean("show_onboarding")
                    decideNextActivity(showOnboarding)
                } else {

                    decideNextActivity(false) // Default to false if fetch fails
                }
            }
    }

    private fun decideNextActivity(showOnboarding: Boolean) {
//        if (showOnboarding) {
//            closeSplash()
//        } else {
//            startMainActivity()
//        }
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
