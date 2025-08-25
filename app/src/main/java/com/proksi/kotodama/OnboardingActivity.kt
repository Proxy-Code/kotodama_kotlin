package com.proksi.kotodama

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var dataStoreManager: DataStoreManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        enableEdgeToEdge()
        dataStoreManager = DataStoreManager
       // completeOnboarding()
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            dataStoreManager.saveOnboardingCompleted(this@OnboardingActivity, true)
        }
    }
}