package com.proksi.kotodama.models

import com.revenuecat.purchases.Offering


enum class RCPlacement(val value: String) {
    SETTING("setting"),
    HOME("home"),
    ONBOARDING("onboarding"),
    CLONE("clone"),
    CAMPAIGN("campaign"),
    CHARACTER("character")
}

data class PurchaseResult(
    val success: Boolean,
    val error: String? = null,
    val offering: Offering? = null
)