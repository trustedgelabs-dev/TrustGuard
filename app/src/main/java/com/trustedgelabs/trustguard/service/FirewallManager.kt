package com.trustedgelabs.trustguard.service

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages per-app firewall rules.
 * Apps can be blocked from accessing WiFi, mobile data, or both.
 */
class FirewallManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("firewall_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BLOCKED_WIFI = "blocked_wifi_apps"
        private const val KEY_BLOCKED_MOBILE = "blocked_mobile_apps"
        private const val KEY_FIREWALL_ENABLED = "firewall_enabled"

        private val _rulesChanged = MutableStateFlow(0L)
        val rulesChanged: StateFlow<Long> = _rulesChanged.asStateFlow()

        fun notifyRulesChanged() {
            _rulesChanged.value = System.currentTimeMillis()
        }
    }

    fun isFirewallEnabled(): Boolean {
        return prefs.getBoolean(KEY_FIREWALL_ENABLED, false)
    }

    fun setFirewallEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FIREWALL_ENABLED, enabled).apply()
        notifyRulesChanged()
    }

    fun getWifiBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_WIFI, emptySet()) ?: emptySet()
    }

    fun getMobileBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_MOBILE, emptySet()) ?: emptySet()
    }

    fun isAppBlockedWifi(packageName: String): Boolean {
        return packageName in getWifiBlockedApps()
    }

    fun isAppBlockedMobile(packageName: String): Boolean {
        return packageName in getMobileBlockedApps()
    }

    fun toggleWifiBlock(packageName: String) {
        val current = getWifiBlockedApps().toMutableSet()
        if (packageName in current) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet(KEY_BLOCKED_WIFI, current).apply()
        notifyRulesChanged()
    }

    fun toggleMobileBlock(packageName: String) {
        val current = getMobileBlockedApps().toMutableSet()
        if (packageName in current) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet(KEY_BLOCKED_MOBILE, current).apply()
        notifyRulesChanged()
    }

    fun blockAppCompletely(packageName: String) {
        val wifi = getWifiBlockedApps().toMutableSet()
        val mobile = getMobileBlockedApps().toMutableSet()
        wifi.add(packageName)
        mobile.add(packageName)
        prefs.edit()
            .putStringSet(KEY_BLOCKED_WIFI, wifi)
            .putStringSet(KEY_BLOCKED_MOBILE, mobile)
            .apply()
        notifyRulesChanged()
    }

    fun unblockAppCompletely(packageName: String) {
        val wifi = getWifiBlockedApps().toMutableSet()
        val mobile = getMobileBlockedApps().toMutableSet()
        wifi.remove(packageName)
        mobile.remove(packageName)
        prefs.edit()
            .putStringSet(KEY_BLOCKED_WIFI, wifi)
            .putStringSet(KEY_BLOCKED_MOBILE, mobile)
            .apply()
        notifyRulesChanged()
    }

    fun hasAnyRules(): Boolean {
        return getWifiBlockedApps().isNotEmpty() || getMobileBlockedApps().isNotEmpty()
    }

    fun getAllBlockedApps(): Set<String> {
        return getWifiBlockedApps() + getMobileBlockedApps()
    }

    data class AppFirewallRule(
        val packageName: String,
        val appName: String,
        val blockedWifi: Boolean,
        val blockedMobile: Boolean
    )

    fun getInstalledAppsWithRules(): List<AppFirewallRule> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        val wifiBlocked = getWifiBlockedApps()
        val mobileBlocked = getMobileBlockedApps()

        return apps
            .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { it.packageName != context.packageName }
            .map { appInfo ->
                AppFirewallRule(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    blockedWifi = appInfo.packageName in wifiBlocked,
                    blockedMobile = appInfo.packageName in mobileBlocked
                )
            }
            .sortedWith(compareByDescending<AppFirewallRule> { it.blockedWifi || it.blockedMobile }.thenBy { it.appName.lowercase() })
    }
}
