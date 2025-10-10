package com.proksi.kotodama.utils

import ads_mobile_sdk.h6
import android.R
import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleCoroutineScope
import com.proksi.kotodama.dataStore.DataStoreManager
import com.proksi.kotodama.models.RCPlacement
import com.proksi.kotodama.objects.PaywallManager
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.launch


object PaywallUtility {

    // Paywall gösterimi için ana fonksiyon
    fun showPaywall(
        context: Context,
        composeView: ComposeView,
        placement: RCPlacement,
        dataStoreManager: DataStoreManager,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        composeView.disposeComposition()

        Log.d("ali", "paywallda")

        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e("RevenueCat", "Error: $error")
                composeView.visibility = View.GONE
            },
            onSuccess = { offerings ->
                val offering = getOfferingForPlacement(offerings, placement)

                composeView.apply {
                    setContent {
                        val paywallState by PaywallManager.paywallState.collectAsState()

                        if (paywallState is PaywallManager.PaywallState.Visible) {
                            if (offering != null) {
                                RevenueCatPaywall(
                                    offering = offering,
                                    onDismiss = {
                                        PaywallManager.hidePaywall()
                                        composeView.visibility = View.GONE
                                    },
                                    dataStoreManager = dataStoreManager,
                                    lifecycleScope = lifecycleScope,
                                    context = context
                                )
                            } else {
                                FallbackPaywallUI(
                                    onDismiss = {
                                        PaywallManager.hidePaywall()
                                        composeView.visibility = View.GONE
                                    }
                                )
                            }
                        }
                    }
                    visibility = View.VISIBLE
                }
                PaywallManager.showPaywall(placement)
            }
        )
    }

    // Placement'a göre offering getir
    private fun getOfferingForPlacement(offerings: Offerings, placement: RCPlacement): Offering? {
        return when (placement) {
            RCPlacement.CLONE -> offerings["Voice_Clone"]
            RCPlacement.ONBOARDING -> offerings["August Donna Annual Weekly"]
            RCPlacement.HOME -> offerings["August Donna Annual Weekly"]
            RCPlacement.CAMPAIGN -> offerings["final_offer_yearly"]
            RCPlacement.SETTING -> offerings["August Donna Annual Weekly"]
            RCPlacement.CHARACTER -> offerings["extra-characters"]
        } ?: offerings.current
    }

    // RevenueCat Paywall Composable
    @Composable
    fun RevenueCatPaywall(
        offering: Offering,
        onDismiss: () -> Unit,
        dataStoreManager: DataStoreManager,
        lifecycleScope: LifecycleCoroutineScope,
        context: Context
    ) {
        Log.d("RevenueCat", "Showing paywall for: ${offering.identifier}")

        val listener = remember {
            object : PaywallListener {
                override fun onPurchaseCompleted(
                    customerInfo: CustomerInfo,
                    storeTransaction: StoreTransaction
                ) {
                    lifecycleScope.launch {
                        dataStoreManager.saveSubscriptionStatus(context, true)
                    }
                    onDismiss()
                }

                override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                    if (customerInfo.entitlements["subscription"]?.isActive == true) {
                        lifecycleScope.launch {
                            dataStoreManager.saveSubscriptionStatus(context, true)
                        }
                        onDismiss()
                    }
                }
            }
        }

        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setRequiredEntitlementIdentifier("subscription")
                .setOffering(offering)
                .setListener(listener)
                .build()
        )
    }

    // Fallback UI
    @Composable
    fun FallbackPaywallUI(onDismiss: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Premium özellik yüklenemedi",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Text("Kapat", color = MaterialTheme.colors.onPrimary)
            }
        }
    }
}