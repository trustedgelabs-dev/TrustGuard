package com.trustedgelabs.trustguard.domain

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Detects apps that exhibit adware-like behavior patterns.
 *
 * Detection is based on:
 * 1. Suspicious permission combinations (overlay + boot + internet)
 * 2. Known adware SDK signatures (activity/service/receiver class names)
 * 3. Behavioral red flags (aggressive background patterns)
 *
 * IMPORTANT: We don't label apps as "adware" or "malware" to avoid legal issues.
 * Instead we say "suspicious ad behavior detected" / "şüpheli reklam davranışı".
 */
object AdwareDetector {

    data class AdwareReport(
        val packageName: String,
        val appName: String,
        val suspicionScore: Int,        // 0-100
        val reasons: List<AdwareReason>
    ) {
        val isSuspicious: Boolean get() = suspicionScore >= 40
        val isHighlySuspicious: Boolean get() = suspicionScore >= 70
    }

    data class AdwareReason(
        val type: ReasonType,
        val descriptionKey: String   // String resource key
    )

    enum class ReasonType {
        OVERLAY_ADS,           // Can show ads over other apps
        AUTO_START,            // Starts on boot + internet
        KNOWN_AD_SDK,          // Contains known aggressive ad SDK
        AGGRESSIVE_BACKGROUND, // Aggressive background activity
        INSTALL_APPS,          // Can install other apps (PPI)
        SUSPICIOUS_COMBO       // Suspicious permission combination
    }

    // ══════════════════════════════════════════════════
    // Known aggressive ad SDK signatures
    // These are class name patterns found in adware SDKs
    // ══════════════════════════════════════════════════

    private val knownAdwareSdks = listOf(
        // Aggressive ad networks known for popup/overlay ads
        "com.startapp" to "StartApp SDK",
        "com.airpush" to "AirPush SDK",
        "com.leadbolt" to "LeadBolt SDK",
        "com.appnext" to "AppNext SDK",
        "com.mobiroller.ads" to "MobiRoller Ads",
        "com.mobvista" to "Mobvista SDK",
        "com.applovin.adview" to "AppLovin Aggressive",
        "cn.jpush" to "JPush (Popup)",
        "com.igexin" to "iGexin Push",
        "com.pgl.sys" to "PGL Adware",
        "com.wap.browser" to "WAP Browser Injection",
        "com.nativex" to "NativeX Ads",
        "com.apsalar" to "Apsalar Tracker",
        "com.revmob" to "RevMob SDK",
        "com.tapjoy" to "TapJoy SDK",
        "com.supersonic" to "SuperSonic Ads",
        "sdk.adcolony.com" to "AdColony Aggressive",
        "com.inmobi.ads.interstitial" to "InMobi Interstitial",
        // Chinese adware SDKs
        "cn.domob" to "Domob Ad SDK",
        "com.baidu.mobads" to "Baidu Mob Ads",
        "com.qq.e.ads" to "QQ E-Ads",
        "com.bytedance.sdk.openadsdk" to "ByteDance Ads",
    )

    // Known adware/PUP package patterns
    private val knownAdwarePackages = setOf(
        "com.cleanmaster", "com.cm.", "com.ksmobile",
        "com.nqmobile", "com.dianxinos", "com.duapps",
        "com.apus", "com.uc.browser", "com.dolphin",
    )

    // Permissions that indicate overlay ad capability
    private val overlayPermissions = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.DRAW_OVER_OTHER_APPS"
    )

    // Permissions that indicate auto-start
    private val autoStartPermissions = setOf(
        "android.permission.RECEIVE_BOOT_COMPLETED"
    )

    // Permissions that indicate app installation (PPI - pay per install)
    private val installPermissions = setOf(
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.INSTALL_PACKAGES"
    )

    // Permissions that indicate aggressive background behavior
    private val backgroundPermissions = setOf(
        "android.permission.WAKE_LOCK",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    )

    /**
     * Analyze a single app for adware behavior.
     */
    fun analyzeApp(context: Context, packageName: String): AdwareReport? {
        val pm = context.packageManager

        // Skip trusted apps and verified publishers (Samsung, Google, Microsoft, etc.)
        if (PermissionClassifier.isTrustedApp(packageName)) return null
        if (PermissionClassifier.isVerifiedPublisher(packageName)) return null

        val packageInfo = try {
            val flags = PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
        } catch (_: Exception) {
            return null
        }

        val appName = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
        val permissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()
        val reasons = mutableListOf<AdwareReason>()
        var score = 0

        // ── Check 1: Overlay ad capability ──
        val hasOverlay = permissions.any { it in overlayPermissions }
        val hasInternet = "android.permission.INTERNET" in permissions
        val hasAutoStart = permissions.any { it in autoStartPermissions }

        if (hasOverlay && hasInternet) {
            reasons.add(AdwareReason(ReasonType.OVERLAY_ADS, "adware_overlay_ads"))
            score += 30
        }

        // ── Check 2: Auto-start + internet (can show ads on boot) ──
        if (hasAutoStart && hasInternet && hasOverlay) {
            reasons.add(AdwareReason(ReasonType.AUTO_START, "adware_auto_start"))
            score += 20
        }

        // ── Check 3: Known adware SDK signatures ──
        val detectedSdks = detectAdwareSdks(packageInfo)
        if (detectedSdks.isNotEmpty()) {
            reasons.add(AdwareReason(ReasonType.KNOWN_AD_SDK, "adware_known_sdk"))
            score += 25 * detectedSdks.size.coerceAtMost(2)
        }

        // ── Check 4: Aggressive background pattern ──
        val backgroundCount = permissions.count { it in backgroundPermissions }
        if (backgroundCount >= 2 && hasInternet && hasAutoStart) {
            reasons.add(AdwareReason(ReasonType.AGGRESSIVE_BACKGROUND, "adware_aggressive_bg"))
            score += 15
        }

        // ── Check 5: Can install other apps (PPI behavior) ──
        val canInstall = permissions.any { it in installPermissions }
        if (canInstall && hasInternet) {
            reasons.add(AdwareReason(ReasonType.INSTALL_APPS, "adware_install_apps"))
            score += 20
        }

        // ── Check 6: Known adware package patterns ──
        if (knownAdwarePackages.any { packageName.startsWith(it) }) {
            reasons.add(AdwareReason(ReasonType.SUSPICIOUS_COMBO, "adware_known_package"))
            score += 25
        }

        // ── Check 7: Suspicious combo - too many aggressive permissions ──
        val suspiciousPermsCount = listOf(hasOverlay, hasAutoStart, canInstall, hasInternet,
            backgroundCount >= 2).count { it }
        if (suspiciousPermsCount >= 4) {
            reasons.add(AdwareReason(ReasonType.SUSPICIOUS_COMBO, "adware_suspicious_combo"))
            score += 15
        }

        return AdwareReport(
            packageName = packageName,
            appName = appName,
            suspicionScore = score.coerceAtMost(100),
            reasons = reasons
        )
    }

    /**
     * Scan all installed apps for adware behavior.
     * Returns only suspicious apps sorted by suspicion score.
     */
    fun scanAllApps(context: Context): List<AdwareReport> {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }

        return packages.mapNotNull { info ->
            val appFlags = info.applicationInfo?.flags ?: 0
            // Skip system apps and updated system apps (OEM apps like Samsung Email, etc.)
            val isSystem = appFlags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            val isUpdatedSystem = appFlags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
            if (isSystem || isUpdatedSystem) return@mapNotNull null

            analyzeApp(context, info.packageName)
        }.filter { it.isSuspicious }
            .sortedByDescending { it.suspicionScore }
    }

    /**
     * Check if app contains known adware SDK class names.
     */
    private fun detectAdwareSdks(packageInfo: PackageInfo): List<String> {
        val detected = mutableListOf<String>()
        val allClassNames = mutableSetOf<String>()

        // Collect all declared component class names
        packageInfo.activities?.forEach { allClassNames.add(it.name) }
        packageInfo.services?.forEach { allClassNames.add(it.name) }
        packageInfo.receivers?.forEach { allClassNames.add(it.name) }

        for ((prefix, name) in knownAdwareSdks) {
            if (allClassNames.any { it.startsWith(prefix) }) {
                detected.add(name)
            }
        }
        return detected
    }
}
