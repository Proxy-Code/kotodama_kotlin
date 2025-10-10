package com.proksi.kotodama

import android.content.Context
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
import com.proksi.kotodama.models.RCPlacement
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
    private var TAG = "PaywallActivity"
    private var placement: RCPlacement? = null
    private var launchCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_paywall)

        placement = intent.getSerializableExtra("PLACEMENT") as? RCPlacement
            ?: RCPlacement.ONBOARDING // Default

        dataStoreManager = DataStoreManager

        paywallActivityLauncher = PaywallActivityLauncher(this, this)


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val launchCount = dataStoreManager.getLaunchCount(this@PaywallActivity).first()
                loadAndLaunchOfferingForPlacement(if (launchCount == 1) "campaign" else "onboarding")
            }
        }

    }

    private fun loadAndLaunchOfferingForPlacement(placementName: String) {
        // String'i RCPlacement'e çevir
        val placement = RCPlacement.entries.find { it.value == placementName }
            ?: RCPlacement.ONBOARDING // Fallback

        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onError(error: PurchasesError) {
                Log.e(TAG, "Offering yüklenemedi: ${error.message}")
                navigateToMain()
            }

            override fun onReceived(offerings: Offerings) {
                runOnUiThread {
                    val offering = getOfferingForPlacement(offerings, placement)

                    if (offering != null) {
                        Log.d(TAG, "Kullanılacak offering: ${offering.identifier}")
                        launchPaywall(offering)
                    } else {
                        Log.e(TAG, "Hiç offering bulunamadı, current: ${offerings.current?.identifier}")
                        // Current offering'i dene
                        offerings.current?.let {
                            Log.d(TAG, "Current offering kullanılıyor: ${it.identifier}")
                            launchPaywall(it)
                        } ?: run {
                            Log.e(TAG, "Current offering de yok, ana sayfaya dönülüyor")
                            navigateToMain()
                        }
                    }
                }
            }
        })
    }

    private fun getOfferingForPlacement(offerings: Offerings, placement: RCPlacement?): Offering? {
        return placement?.let { rcPlacement ->
            Log.d(TAG, "Aranan placement: ${rcPlacement.value}")

            offerings.all.values.find {
                Log.d(TAG, "Offering placement: ${it.identifier}, aranan: ${rcPlacement.value}")
                it.identifier.contains(rcPlacement.value, ignoreCase = true)
            }?.also {
                Log.d(TAG, "Placement-specific offering bulundu: ${it.identifier}")
            } ?: run {
                // 2. Bulunamazsa, offering ID'sinde placement adı geçenleri ara
                offerings.all.values.find { offering ->
                    offering.identifier.contains(rcPlacement.value, ignoreCase = true)
                }?.also {
                    Log.d(TAG, "Offering ID'sinde placement bulundu: ${it.identifier}")
                } ?: run {
                    // 3. Hala bulunamazsa, default offering'leri dene
                    when (rcPlacement) {
                        RCPlacement.ONBOARDING -> {
                            offerings.getOffering("default") ?:
                            offerings.getOffering("default") ?:
                            offerings.current
                        }

                        else -> offerings.current
                    }?.also {
                        Log.d(TAG, "Fallback offering kullanılıyor: ${it.identifier}")
                    }
                }
            }
        } ?: offerings.current
    }
    private fun launchPaywall(offering: Offering) {
        findViewById<View>(android.R.id.content).post {
            try {
                paywallActivityLauncher.launchIfNeeded(offering) { true }
            } catch (e: Exception) {
                navigateToMain()
            }
        }
    }



    private fun navigateToMain() {
        Log.d(TAG, "navigateToMain: HERE")
        lifecycleScope.launch {
            if (launchCount < 3) {
                dataStoreManager.incrementLaunchCount(this@PaywallActivity)
            }
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onActivityResult(result: PaywallResult) {
        when (result) {
            is PaywallResult.Purchased, is PaywallResult.Restored -> {
                lifecycleScope.launch {
                    dataStoreManager.saveSubscriptionStatus(this@PaywallActivity, true)
                }
                navigateToMain()
            }
            is PaywallResult.Cancelled, is PaywallResult.Error -> {
                navigateToMain()
            }
        }
    }

    companion object {
        fun start(context: Context, placement: RCPlacement) {
            val intent = Intent(context, PaywallActivity::class.java).apply {
                putExtra("PLACEMENT", placement)
            }
            context.startActivity(intent)
        }
    }

}
