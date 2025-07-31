package com.proksi.kotodama.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offerings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class PaywallViewModel : ViewModel() {


    private val _paywallState = MutableStateFlow<PaywallState>(PaywallState.Hidden)
    val paywallState = _paywallState.asStateFlow()

    sealed class PaywallState {
        object Hidden : PaywallState()
        data class Visible(val instanceId: Int = Random.nextInt()) : PaywallState()
    }

    fun showPaywall() {
        viewModelScope.launch {
            _paywallState.emit(PaywallState.Visible())
        }
    }

    fun hidePaywall() {
        viewModelScope.launch {
            _paywallState.emit(PaywallState.Hidden)
        }
    }
}