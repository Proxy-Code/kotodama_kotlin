package com.proksi.kotodama.objects

import com.proksi.kotodama.models.RCPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PaywallManager {

    // Paywall gösterim state'ini tutmak için
    private val _paywallState = MutableStateFlow<PaywallState>(PaywallState.Hidden)
    val paywallState: StateFlow<PaywallState> = _paywallState.asStateFlow()

    // Paywall'ı göster
    fun showPaywall(placement: RCPlacement? = null) {
        _paywallState.value = PaywallState.Visible(placement)
    }

    // Paywall'ı gizle
    fun hidePaywall() {
        _paywallState.value = PaywallState.Hidden
    }

    // Paywall state
    sealed class PaywallState {
        object Hidden : PaywallState()
        data class Visible(val placement: RCPlacement? = null) : PaywallState()
    }
}