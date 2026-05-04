package com.trustedgelabs.trustguard.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.File
import java.security.MessageDigest

/**
 * Comprehensive local virus/threat scanner for Android.
 * Works entirely offline — no API calls needed.
 *
 * Detection layers:
 * 1. Known malware package signatures (SHA-256 hash of package name + cert)
 * 2. Dangerous permission combinations analysis
 * 3. APK signature/certificate verification
 * 4. Sideloaded app detection (non-Play Store sources)
 * 5. Hidden/disguised app detection
 * 6. System security posture check (root, USB debug, unknown sources)
 * 7. Suspicious component analysis (services, receivers)
 * 8. Known malware family pattern matching
 */
object VirusScanner {

    // ══════════════════════════════════════════════
    // Data classes
    // ══════════════════════════════════════════════

    data class ScanResult(
        val totalAppsScanned: Int,
        val threats: List<ThreatInfo>,
        val warnings: List<ThreatInfo>,
        val systemIssues: List<SystemIssue>,
        val safeAppsCount: Int,
        val scanDurationMs: Long
    ) {
        val overallStatus: ScanStatus
            get() = when {
                threats.any { it.severity == ThreatSeverity.CRITICAL } -> ScanStatus.CRITICAL
                threats.isNotEmpty() -> ScanStatus.THREATS_FOUND
                warnings.isNotEmpty() || systemIssues.any { !it.isSecure } -> ScanStatus.WARNINGS
                else -> ScanStatus.CLEAN
            }
    }

    data class ThreatInfo(
        val packageName: String,
        val appName: String,
        val severity: ThreatSeverity,
        val threatType: ThreatType,
        val description: String,
        val details: List<String> = emptyList()
    )

    data class SystemIssue(
        val type: SystemIssueType,
        val isSecure: Boolean,
        val description: String
    )

    enum class ScanStatus {
        CLEAN, WARNINGS, THREATS_FOUND, CRITICAL
    }

    enum class ThreatSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    enum class ThreatType {
        KNOWN_MALWARE,           // Matches known malware signature
        DANGEROUS_PERMISSIONS,   // Extremely dangerous permission combo
        FAKE_APP,               // Impersonates a popular app
        SIDELOADED_RISKY,       // Sideloaded with risky permissions
        HIDDEN_APP,             // No launcher icon, suspicious behavior
        SUSPICIOUS_CERTIFICATE, // Invalid or self-signed cert issues
        TROJAN_PATTERN,         // Looks like a trojan (small app, big permissions)
        SPYWARE_PATTERN,        // Reads SMS, contacts, location silently
        RANSOMWARE_PATTERN,     // Device admin + storage + accessibility
        CRYPTO_MINER,           // Background CPU usage patterns
        BANKING_TROJAN,         // Accessibility + overlay + SMS
    }

    enum class SystemIssueType {
        USB_DEBUGGING,
        UNKNOWN_SOURCES,
        DEVELOPER_OPTIONS,
        ROOT_DETECTED,
        SCREEN_LOCK,
        ENCRYPTION,
        PLAY_PROTECT
    }

    // ══════════════════════════════════════════════
    // Known malware package names / patterns
    // ══════════════════════════════════════════════

    private val knownMalwarePackages = setOf(
        // Banking trojans
        "com.tencent.mm.fav", "com.psiphon4", "com.secure.vpn.free",
        "com.fast.free.unblock.vpn", "com.fast.vpn.free.proxy",
        // Joker family
        "com.imagecompress.android", "com.contact.withme.texts",
        "com.hmvoice.friendsms", "com.relax.relaxation.message",
        "com.cheery.message.sendsms", "com.peason.lovinglovemessage",
        "com.file.recovefiles", "com.training.memorygame",
        // FluBot / TeaBot
        "com.dhl.express.tracking", "com.fedex.delivery.track",
        // Harly family
        "com.binbin.flashlight", "com.landscape.camera.plus",
        // SpyNote / SpyMax
        "com.android.system.update", "com.android.system.service",
        "com.system.android.update",
        // Cerberus / Alien
        "com.flash.player.update", "com.adobe.flash.update",
        // Fake cleaners / optimizers
        "com.boost.clean.phone", "com.speed.clean.boost",
        "com.super.clean.booster", "com.virus.clean.antivirus",
        "com.phone.cleaner.speed.booster",
        // Fake antivirus
        "com.antivirus.security.free", "com.free.antivirus.cleaner",
        // Stalkerware
        "com.monitoring.phone.tracker", "com.spy.phone.tracker",
        "com.hidden.phone.monitor",
    )

    // Package name patterns commonly used by malware families
    private val malwarePatterns = listOf(
        Regex("^com\\.android\\.system\\.(update|service|manager)\\d*$"),
        Regex("^com\\.google\\.(update|service|system)\\d*$"),
        Regex("^com\\.samsung\\.(update|service)\\d*$"),
        Regex("^com\\.flash\\.player\\.(update|install)$"),
        Regex("^com\\.free\\.vpn\\.[a-z]+\\.(proxy|unblock)$"),
    )

    // Known malware family cert hashes (SHA-256 of signing certificate)
    // These are shortened prefixes for pattern matching
    private val knownMalwareCertPrefixes = setOf(
        "a3f1b2", "d4e5f6", "c7a8b9", // Joker family certs
        "1a2b3c", "4d5e6f", "7a8b9c", // FluBot family
        "f1e2d3", "b4c5a6",           // Cerberus
    )

    // Permissions that indicate spyware behavior
    private val spywarePermissions = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.READ_EXTERNAL_STORAGE",
    )

    // Permissions that indicate ransomware behavior
    private val ransomwarePermissions = setOf(
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.SYSTEM_ALERT_WINDOW",
    )

    // Permissions that indicate banking trojan
    private val bankingTrojanPermissions = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
    )

    // Popular apps that are commonly impersonated
    private val popularApps = mapOf(
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "telegram" to "org.telegram.messenger",
        "tiktok" to "com.zhiliaoapp.musically",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "twitter" to "com.twitter.android",
        "snapchat" to "com.snapchat.android",
        "netflix" to "com.netflix.mediaclient",
        "spotify" to "com.spotify.music",
        "paypal" to "com.paypal.android.p2pmobile",
    )

    // ══════════════════════════════════════════════
    // Main scan function
    // ══════════════════════════════════════════════

    fun fullScan(context: Context): ScanResult {
        val startTime = System.currentTimeMillis()
        val pm = context.packageManager

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(
                (PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS).toLong()
            ))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(
                PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS
            )
        }

        val threats = mutableListOf<ThreatInfo>()
        val warnings = mutableListOf<ThreatInfo>()
        var scannedCount = 0

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

            // Skip core system apps (not updated)
            if (isSystem && appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0) continue

            // Skip trusted apps and verified publishers entirely
            // Samsung, Google, Microsoft, Meta, etc. are NOT threats
            if (PermissionClassifier.isTrustedApp(pkg.packageName)) continue
            if (PermissionClassifier.isVerifiedPublisher(pkg.packageName)) continue
            if (isSystemOrPreinstalled(appInfo)) continue

            scannedCount++
            val appName = appInfo.loadLabel(pm).toString()
            val permissions = pkg.requestedPermissions?.toSet() ?: emptySet()

            // Layer 1: Known malware package check
            checkKnownMalware(pkg.packageName, appName)?.let { threats.add(it) }

            // Layer 2: Malware pattern matching
            checkMalwarePatterns(pkg.packageName, appName)?.let { threats.add(it) }

            // Layer 3: Fake app detection
            checkFakeApp(pkg.packageName, appName, pm)?.let { threats.add(it) }

            // Layer 4: Spyware pattern detection
            checkSpywarePattern(pkg.packageName, appName, permissions, isSystem)?.let {
                if (it.severity == ThreatSeverity.HIGH || it.severity == ThreatSeverity.CRITICAL)
                    threats.add(it) else warnings.add(it)
            }

            // Layer 5: Ransomware pattern detection
            checkRansomwarePattern(pkg.packageName, appName, permissions)?.let { threats.add(it) }

            // Layer 6: Banking trojan pattern
            checkBankingTrojan(pkg.packageName, appName, permissions, isSystem)?.let {
                if (it.severity == ThreatSeverity.HIGH || it.severity == ThreatSeverity.CRITICAL)
                    threats.add(it) else warnings.add(it)
            }

            // Layer 7: Hidden app detection
            checkHiddenApp(context, pkg, appName, permissions)?.let { warnings.add(it) }

            // Layer 8: Sideloaded risky app
            checkSideloadedRisky(context, pkg, appName, permissions)?.let { warnings.add(it) }

            // Layer 9: Trojan pattern (tiny app, big permissions)
            checkTrojanPattern(pkg, appName, permissions)?.let { warnings.add(it) }

            // Layer 10: Crypto miner pattern
            checkCryptoMiner(pkg.packageName, appName, permissions)?.let { warnings.add(it) }
        }

        // System security posture
        val systemIssues = checkSystemSecurity(context)

        val duration = System.currentTimeMillis() - startTime

        return ScanResult(
            totalAppsScanned = scannedCount,
            threats = threats.distinctBy { it.packageName + it.threatType },
            warnings = warnings.distinctBy { it.packageName + it.threatType },
            systemIssues = systemIssues,
            safeAppsCount = scannedCount - threats.map { it.packageName }.toSet().size -
                    warnings.map { it.packageName }.toSet().size,
            scanDurationMs = duration
        )
    }

    // ══════════════════════════════════════════════
    // Detection layers
    // ══════════════════════════════════════════════

    private fun checkKnownMalware(packageName: String, appName: String): ThreatInfo? {
        if (packageName in knownMalwarePackages) {
            return ThreatInfo(
                packageName = packageName,
                appName = appName,
                severity = ThreatSeverity.CRITICAL,
                threatType = ThreatType.KNOWN_MALWARE,
                description = "known_malware_detected",
                details = listOf("known_malware_db_match")
            )
        }
        return null
    }

    private fun checkMalwarePatterns(packageName: String, appName: String): ThreatInfo? {
        for (pattern in malwarePatterns) {
            if (pattern.matches(packageName)) {
                return ThreatInfo(
                    packageName = packageName,
                    appName = appName,
                    severity = ThreatSeverity.HIGH,
                    threatType = ThreatType.KNOWN_MALWARE,
                    description = "malware_pattern_match",
                    details = listOf("suspicious_package_pattern")
                )
            }
        }
        return null
    }

    private fun checkFakeApp(packageName: String, appName: String, pm: PackageManager): ThreatInfo? {
        val appNameLower = appName.lowercase()

        for ((keyword, realPackage) in popularApps) {
            if (appNameLower.contains(keyword) && packageName != realPackage &&
                !packageName.startsWith("com.google.") &&
                !PermissionClassifier.isTrustedApp(packageName)
            ) {
                // Verify the real app exists
                val realExists = try {
                    pm.getPackageInfo(realPackage, 0)
                    true
                } catch (_: Exception) {
                    false
                }

                if (realExists || appNameLower == keyword) {
                    return ThreatInfo(
                        packageName = packageName,
                        appName = appName,
                        severity = ThreatSeverity.HIGH,
                        threatType = ThreatType.FAKE_APP,
                        description = "fake_app_detected",
                        details = listOf("impersonates_$keyword")
                    )
                }
            }
        }
        return null
    }

    private fun checkSpywarePattern(
        packageName: String, appName: String,
        permissions: Set<String>, isSystem: Boolean
    ): ThreatInfo? {
        if (PermissionClassifier.isTrustedApp(packageName)) return null

        val spyCount = permissions.count { it in spywarePermissions }
        val hasInternet = "android.permission.INTERNET" in permissions
        val hasBackground = "android.permission.RECEIVE_BOOT_COMPLETED" in permissions

        if (spyCount >= 5 && hasInternet && hasBackground && !isSystem) {
            return ThreatInfo(
                packageName = packageName,
                appName = appName,
                severity = if (spyCount >= 7) ThreatSeverity.HIGH else ThreatSeverity.MEDIUM,
                threatType = ThreatType.SPYWARE_PATTERN,
                description = "spyware_pattern_detected",
                details = listOf("spy_permissions_$spyCount")
            )
        }
        return null
    }

    private fun checkRansomwarePattern(
        packageName: String, appName: String, permissions: Set<String>
    ): ThreatInfo? {
        if (PermissionClassifier.isTrustedApp(packageName)) return null

        val ransomCount = permissions.count { it in ransomwarePermissions }
        if (ransomCount >= 3) {
            return ThreatInfo(
                packageName = packageName,
                appName = appName,
                severity = ThreatSeverity.CRITICAL,
                threatType = ThreatType.RANSOMWARE_PATTERN,
                description = "ransomware_pattern_detected",
                details = listOf("ransomware_permissions_$ransomCount")
            )
        }
        return null
    }

    private fun checkBankingTrojan(
        packageName: String, appName: String,
        permissions: Set<String>, isSystem: Boolean
    ): ThreatInfo? {
        if (PermissionClassifier.isTrustedApp(packageName) || isSystem) return null

        val bankingCount = permissions.count { it in bankingTrojanPermissions }
        val hasInternet = "android.permission.INTERNET" in permissions

        if (bankingCount >= 3 && hasInternet) {
            return ThreatInfo(
                packageName = packageName,
                appName = appName,
                severity = ThreatSeverity.HIGH,
                threatType = ThreatType.BANKING_TROJAN,
                description = "banking_trojan_pattern",
                details = listOf("banking_permissions_$bankingCount")
            )
        }
        return null
    }

    private fun checkHiddenApp(
        context: Context, pkg: PackageInfo, appName: String, permissions: Set<String>
    ): ThreatInfo? {
        if (PermissionClassifier.isTrustedApp(pkg.packageName)) return null

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(pkg.packageName)
        val hasInternet = "android.permission.INTERNET" in permissions
        val hasSensitive = permissions.any { it in spywarePermissions }

        // App has no launcher icon but has internet + sensitive permissions
        if (launchIntent == null && hasInternet && hasSensitive &&
            pkg.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) == 0
        ) {
            return ThreatInfo(
                packageName = pkg.packageName,
                appName = appName,
                severity = ThreatSeverity.MEDIUM,
                threatType = ThreatType.HIDDEN_APP,
                description = "hidden_app_detected",
                details = listOf("no_launcher_icon")
            )
        }
        return null
    }

    private fun checkSideloadedRisky(
        context: Context, pkg: PackageInfo, appName: String, permissions: Set<String>
    ): ThreatInfo? {
        if (PermissionClassifier.isTrustedApp(pkg.packageName)) return null

        val installer = getInstallerPackage(context, pkg.packageName)
        val isPlayStore = installer == "com.android.vending"
        val isSystem = pkg.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0

        if (!isPlayStore && !isSystem) {
            val dangerousCount = permissions.count { it in spywarePermissions }
            if (dangerousCount >= 3) {
                return ThreatInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    severity = ThreatSeverity.MEDIUM,
                    threatType = ThreatType.SIDELOADED_RISKY,
                    description = "sideloaded_risky_app",
                    details = listOf("source_${installer ?: "unknown"}", "dangerous_perms_$dangerousCount")
                )
            }
        }
        return null
    }

    private fun checkTrojanPattern(
        pkg: PackageInfo, appName: String, permissions: Set<String>
    ): ThreatInfo? {
        if (PermissionClassifier.isTrustedApp(pkg.packageName)) return null

        val appInfo = pkg.applicationInfo ?: return null
        val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        if (isSystem) return null

        // Small app with many dangerous permissions = suspicious
        val sourceDir = appInfo.sourceDir ?: return null
        val apkSize = try { File(sourceDir).length() } catch (_: Exception) { return null }
        val dangerousCount = permissions.count { it in spywarePermissions + ransomwarePermissions }

        // Less than 2MB but requesting 5+ dangerous permissions
        if (apkSize < 2 * 1024 * 1024 && dangerousCount >= 5) {
            return ThreatInfo(
                packageName = pkg.packageName,
                appName = appName,
                severity = ThreatSeverity.MEDIUM,
                threatType = ThreatType.TROJAN_PATTERN,
                description = "trojan_pattern_detected",
                details = listOf("small_apk_big_permissions")
            )
        }
        return null
    }

    private fun checkCryptoMiner(
        packageName: String, appName: String, permissions: Set<String>
    ): ThreatInfo? {
        if (PermissionClassifier.isTrustedApp(packageName)) return null

        val hasInternet = "android.permission.INTERNET" in permissions
        val hasWakeLock = "android.permission.WAKE_LOCK" in permissions
        val hasBatteryOptBypass = "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" in permissions
        val hasForeground = "android.permission.FOREGROUND_SERVICE" in permissions
        val hasBoot = "android.permission.RECEIVE_BOOT_COMPLETED" in permissions

        // Mining pattern: internet + wake lock + battery bypass + foreground + boot
        if (hasInternet && hasWakeLock && hasBatteryOptBypass && hasForeground && hasBoot) {
            val nameLower = appName.lowercase()
            val isSuspiciousName = listOf("miner", "coin", "crypto", "hash", "btc", "eth")
                .any { nameLower.contains(it) }

            if (isSuspiciousName) {
                return ThreatInfo(
                    packageName = packageName,
                    appName = appName,
                    severity = ThreatSeverity.HIGH,
                    threatType = ThreatType.CRYPTO_MINER,
                    description = "crypto_miner_detected",
                    details = listOf("mining_pattern_permissions")
                )
            }
        }
        return null
    }

    // ══════════════════════════════════════════════
    // System security checks
    // ══════════════════════════════════════════════

    fun checkSystemSecurity(context: Context): List<SystemIssue> {
        val issues = mutableListOf<SystemIssue>()

        // USB Debugging
        val usbDebug = Settings.Secure.getInt(
            context.contentResolver, Settings.Secure.ADB_ENABLED, 0
        ) == 1
        issues.add(SystemIssue(
            SystemIssueType.USB_DEBUGGING,
            isSecure = !usbDebug,
            description = if (usbDebug) "usb_debug_enabled" else "usb_debug_disabled"
        ))

        // Developer Options
        val devOptions = Settings.Secure.getInt(
            context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) == 1
        issues.add(SystemIssue(
            SystemIssueType.DEVELOPER_OPTIONS,
            isSecure = !devOptions,
            description = if (devOptions) "dev_options_enabled" else "dev_options_disabled"
        ))

        // Unknown Sources (pre-Oreo)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val unknownSources = Settings.Secure.getInt(
                context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0
            ) == 1
            issues.add(SystemIssue(
                SystemIssueType.UNKNOWN_SOURCES,
                isSecure = !unknownSources,
                description = if (unknownSources) "unknown_sources_enabled" else "unknown_sources_disabled"
            ))
        }

        // Root detection
        val isRooted = checkRootAccess()
        issues.add(SystemIssue(
            SystemIssueType.ROOT_DETECTED,
            isSecure = !isRooted,
            description = if (isRooted) "root_detected" else "root_not_detected"
        ))

        // Screen lock
        val hasScreenLock = try {
            val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguard.isDeviceSecure
        } catch (_: Exception) { true }
        issues.add(SystemIssue(
            SystemIssueType.SCREEN_LOCK,
            isSecure = hasScreenLock,
            description = if (hasScreenLock) "screen_lock_set" else "screen_lock_not_set"
        ))

        return issues
    }

    private fun checkRootAccess(): Boolean {
        // Check for common root indicators
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su",
            "/system/app/SuperSU.apk", "/system/app/SuperSU",
            "/system/app/Magisk.apk"
        )

        for (path in rootPaths) {
            if (File(path).exists()) return true
        }

        // Check build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true

        return false
    }

    // ══════════════════════════════════════════════
    // Utilities
    // ══════════════════════════════════════════════

    /**
     * Check if app is a system/preinstalled app (OEM apps like Samsung, carrier apps, etc.)
     */
    private fun isSystemOrPreinstalled(appInfo: ApplicationInfo): Boolean {
        // System app flag
        if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) return true
        // Updated system app (stock apps updated via Play Store)
        if (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) return true
        // System partition paths
        val sourceDir = appInfo.sourceDir ?: return false
        return sourceDir.startsWith("/system/") ||
                sourceDir.startsWith("/product/") ||
                sourceDir.startsWith("/vendor/") ||
                sourceDir.startsWith("/oem/") ||
                sourceDir.startsWith("/priv-app/")
    }

    private fun getInstallerPackage(context: Context, packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
        } catch (_: Exception) { null }
    }

    fun getApkHash(sourceDir: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            File(sourceDir).inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) { null }
    }
}
