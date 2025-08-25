package com.proksi.kotodama

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kotodama.tts.R
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.fragments.PaywallFragment
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaywallActivity : AppCompatActivity(), PaywallResultHandler {

    private lateinit var paywallActivityLauncher: PaywallActivityLauncher
    private lateinit var root: View
    private lateinit var dataStoreManager: DataStoreManager
    private var launchCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_paywall)

        dataStoreManager = DataStoreManager

        paywallActivityLauncher = PaywallActivityLauncher(this, this)


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val launchCount = dataStoreManager.getLaunchCount(this@PaywallActivity).first()
                loadAndLaunchOffering(if (launchCount == 1) "final_offer_yearly" else "second_paywall_yearly_weekly")
            }
        }

    }
    private fun loadAndLaunchOffering(offeringId: String) {
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onError(error: PurchasesError) {
                println(":x: Offering yüklenemedi: ${error.message}")
            }
            override fun onReceived(offerings: Offerings) {
                Log.d("Paywall", "onReceived: ${offerings.all.keys}")
                runOnUiThread {
                    val offering = offerings.getOffering(offeringId)
                    if (offering != null) launchPaywall(offering)
                }
            }
        })
    }
    private fun launchPaywall(offering: Offering) {

        findViewById<View>(android.R.id.content).post {
            try {
                paywallActivityLauncher.launchIfNeeded(offering) { true }
            } catch (e: Exception) {
                Log.e("PaywallDebug", "Crash during paywall launch", e)
            }
        }

    }
    override fun onActivityResult(result: PaywallResult) {
        when (result) {
            is PaywallResult.Purchased -> {
                // Kullanıcı satın alma işlemi tamamladı
                Log.d("Paywall", "Satın alma başarılı")
                lifecycleScope.launch {
                    dataStoreManager.saveSubscriptionStatus(this@PaywallActivity, true)
                }
                navigateToMain()
            }
            is PaywallResult.Restored -> {
                // Kullanıcı satın almayı geri yükleme işlemi yaptı
                Log.d("Paywall", "Satın alma geri yüklendi")
                lifecycleScope.launch {
                    dataStoreManager.saveSubscriptionStatus(this@PaywallActivity, true)
                }
                navigateToMain()
            }
            is PaywallResult.Cancelled -> {
                navigateToMain()
            }
            is PaywallResult.Error -> {
                Log.e("Paywall", "Hata: ${result.error}")
                navigateToMain()
            }

        }
    }
    private fun navigateToMain() {
        lifecycleScope.launch {
            if (launchCount < 3) {
                dataStoreManager.incrementLaunchCount(this@PaywallActivity)
            }
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
