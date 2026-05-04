package com.trustedgelabs.trustguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.trustedgelabs.trustguard.billing.BillingManager
import com.trustedgelabs.trustguard.service.AppBlockerService
import com.trustedgelabs.trustguard.util.FamilyShieldManager
import com.trustedgelabs.trustguard.util.LocaleManager

class TrustGuardApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleManager.applyLocale(base))
    }

    lateinit var billingManager: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initBilling()
        ensureFamilyShieldService()
    }

    /**
     * If Family Shield is enabled, make sure AppBlockerService is running.
     * Covers cases where the OS killed the service (Samsung battery optimization etc.)
     */
    private fun ensureFamilyShieldService() {
        try {
            if (FamilyShieldManager.isEnabled(this)) {
                AppBlockerService.start(this)
            }
        } catch (_: Exception) { /* non-critical */ }
    }

    private fun initBilling() {
        billingManager = BillingManager(this)
        billingManager.initialize()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vpnChannel = NotificationChannel(
                "trustguard_vpn",
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.vpn_notification_title)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(vpnChannel)
        }
    }
}
