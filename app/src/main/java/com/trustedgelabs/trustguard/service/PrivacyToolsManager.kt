package com.trustedgelabs.trustguard.service

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Process

data class PrivacyAlert(
    val type: PrivacyAlertType,
    val appName: String,
    val packageName: String,
    val timestamp: Long,
    val detail: String
)

enum class PrivacyAlertType {
    CAMERA_ACCESS,
    MICROPHONE_ACCESS,
    CLIPBOARD_READ,
    LOCATION_ACCESS
}

data class AppTrackerInfo(
    val packageName: String,
    val appName: String,
    val trackerDomains: List<String>,
    val trackerCount: Int
)

class PrivacyToolsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("privacy_tools_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CLIPBOARD_GUARD = "clipboard_guard_enabled"
        private const val KEY_CAMERA_MONITOR = "camera_monitor_enabled"
        private const val KEY_MIC_MONITOR = "mic_monitor_enabled"
    }

    // Known tracker SDK domains mapped to tracker names
    private val knownTrackers = mapOf(
        "google-analytics.com" to "Google Analytics",
        "googleadservices.com" to "Google Ads",
        "doubleclick.net" to "DoubleClick",
        "facebook.com/tr" to "Facebook Pixel",
        "graph.facebook.com" to "Facebook SDK",
        "app-measurement.com" to "Firebase Analytics",
        "crashlytics.com" to "Crashlytics",
        "adjust.com" to "Adjust",
        "appsflyer.com" to "AppsFlyer",
        "branch.io" to "Branch",
        "amplitude.com" to "Amplitude",
        "mixpanel.com" to "Mixpanel",
        "segment.io" to "Segment",
        "sentry.io" to "Sentry",
        "flurry.com" to "Flurry",
        "unity3d.com" to "Unity Ads",
        "applovin.com" to "AppLovin",
        "mopub.com" to "MoPub",
        "chartboost.com" to "Chartboost",
        "inmobi.com" to "InMobi"
    )

    fun isClipboardGuardEnabled(): Boolean = prefs.getBoolean(KEY_CLIPBOARD_GUARD, false)
    fun setClipboardGuardEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CLIPBOARD_GUARD, enabled).apply()

    fun isCameraMonitorEnabled(): Boolean = prefs.getBoolean(KEY_CAMERA_MONITOR, false)
    fun setCameraMonitorEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CAMERA_MONITOR, enabled).apply()

    fun isMicMonitorEnabled(): Boolean = prefs.getBoolean(KEY_MIC_MONITOR, false)
    fun setMicMonitorEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_MIC_MONITOR, enabled).apply()

    fun getAppsWithCameraPermission(): List<String> {
        return getAppsWithPermission(android.Manifest.permission.CAMERA)
    }

    fun getAppsWithMicPermission(): List<String> {
        return getAppsWithPermission(android.Manifest.permission.RECORD_AUDIO)
    }

    fun getAppsWithLocationPermission(): List<String> {
        return getAppsWithPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun getAppsWithPermission(permission: String): List<String> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { appInfo ->
                try {
                    val pkgInfo = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                    pkgInfo.requestedPermissions?.contains(permission) == true
                } catch (_: Exception) { false }
            }
            .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { pm.getApplicationLabel(it).toString() }
    }

    fun getTrackerMap(): List<AppTrackerInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps
            .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
            .mapNotNull { appInfo ->
                val trackers = detectTrackersForApp(appInfo.packageName)
                if (trackers.isNotEmpty()) {
                    AppTrackerInfo(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        trackerDomains = trackers,
                        trackerCount = trackers.size
                    )
                } else null
            }
            .sortedByDescending { it.trackerCount }
    }

    private fun detectTrackersForApp(packageName: String): List<String> {
        // Heuristic: check if app has INTERNET permission and common tracker permissions
        val pm = context.packageManager
        try {
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val perms = pkgInfo.requestedPermissions ?: return emptyList()
            if (!perms.contains(android.Manifest.permission.INTERNET)) return emptyList()

            // Score-based tracker detection
            val indicators = mutableListOf<String>()
            if (perms.contains(android.Manifest.permission.ACCESS_FINE_LOCATION)) indicators.add("Location Tracker")
            if (perms.contains(android.Manifest.permission.READ_PHONE_STATE)) indicators.add("Device Fingerprinting")
            if (perms.contains("com.google.android.gms.permission.AD_ID")) indicators.add("Google Ad ID")
            if (perms.contains(android.Manifest.permission.GET_ACCOUNTS)) indicators.add("Account Tracker")

            // Check meta-data for known SDK markers
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val metaData = appInfo.metaData
            if (metaData != null) {
                if (metaData.containsKey("com.google.android.gms.ads.APPLICATION_ID")) indicators.add("AdMob SDK")
                if (metaData.containsKey("com.facebook.sdk.ApplicationId")) indicators.add("Facebook SDK")
                if (metaData.containsKey("io.branch.sdk.BranchKey")) indicators.add("Branch SDK")
                if (metaData.containsKey("com.crashlytics.ApiKey")) indicators.add("Crashlytics")
            }

            return indicators
        } catch (_: Exception) {
            return emptyList()
        }
    }
}
