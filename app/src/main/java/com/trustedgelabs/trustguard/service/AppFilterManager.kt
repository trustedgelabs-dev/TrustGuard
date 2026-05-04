package com.trustedgelabs.trustguard.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * Manages per-app VPN filtering preferences.
 * Users can choose which apps should have their DNS filtered through the VPN.
 * By default, all apps are filtered (disallowed list is empty).
 */
class AppFilterManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_filter_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXCLUDED_APPS = "excluded_apps"
        private const val KEY_FILTER_MODE = "filter_mode"
    }

    enum class FilterMode {
        ALL_APPS,       // Filter all apps (default)
        SELECTED_APPS   // Only filter selected apps
    }

    fun getFilterMode(): FilterMode {
        val mode = prefs.getString(KEY_FILTER_MODE, FilterMode.ALL_APPS.name)
        return try {
            FilterMode.valueOf(mode ?: FilterMode.ALL_APPS.name)
        } catch (_: Exception) {
            FilterMode.ALL_APPS
        }
    }

    fun setFilterMode(mode: FilterMode) {
        prefs.edit().putString(KEY_FILTER_MODE, mode.name).apply()
    }

    fun getExcludedApps(): Set<String> {
        return prefs.getStringSet(KEY_EXCLUDED_APPS, emptySet()) ?: emptySet()
    }

    fun setExcludedApps(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_EXCLUDED_APPS, packages).apply()
    }

    fun toggleAppExclusion(packageName: String) {
        val current = getExcludedApps().toMutableSet()
        if (packageName in current) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        setExcludedApps(current)
    }

    fun isAppExcluded(packageName: String): Boolean {
        return packageName in getExcludedApps()
    }

    /**
     * Returns list of user-installed apps for the per-app filter UI.
     */
    fun getInstalledUserApps(): List<AppFilterInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val excludedApps = getExcludedApps()

        return apps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { it.packageName != context.packageName } // Exclude TrustGuard itself
            .map { appInfo ->
                AppFilterInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    isExcluded = appInfo.packageName in excludedApps
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}

data class AppFilterInfo(
    val packageName: String,
    val appName: String,
    val isExcluded: Boolean
)
