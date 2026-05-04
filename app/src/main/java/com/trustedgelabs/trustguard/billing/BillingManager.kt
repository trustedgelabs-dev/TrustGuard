package com.trustedgelabs.trustguard.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stub BillingManager - tum ozellikler ucretsiz.
 * Eski Google Play Billing kodu kaldirildi.
 */
class BillingManager(private val context: Context) {

    companion object {
        private val _isPremium = MutableStateFlow(true)
        val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

        fun getFormattedPrice(): String? = null
        fun getPriceAmountMicros(): Long? = null
        fun getCurrencyCode(): String? = null
    }

    fun initialize() {
        _isPremium.value = true
    }

    fun launchPurchaseFlow(activity: Activity, onResult: (Boolean) -> Unit) {
        onResult(true)
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        onResult(true)
    }

    fun destroy() {}
}
