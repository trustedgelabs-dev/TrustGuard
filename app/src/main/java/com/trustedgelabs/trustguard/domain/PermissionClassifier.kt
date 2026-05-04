package com.trustedgelabs.trustguard.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.trustedgelabs.trustguard.data.model.RiskLevel

object PermissionClassifier {

    /**
     * Trusted publishers whitelist.
     * Apps from these package prefixes are always marked as TRUSTED.
     */
    private val trustedPackagePrefixes = listOf(
        "com.trustedgelabs.",       // TrustEdge Labs apps
        "com.cihan.",               // Cihan's personal apps (TrustEdge Labs developer)
    )

    /**
     * Specific trusted packages (exact match).
     */
    private val trustedPackages = setOf(
        "com.trustedgelabs.trustguard",
        "com.trustedgelabs.ibans",
        "com.trustedgelabs.ghostnotify",
        "com.cihan.ghostnotify",
        "com.cihan.ibans",
    )

    /**
     * Verified major publishers — these apps legitimately need sensitive permissions.
     * Having HIGH permissions is expected for their core functionality.
     * They still show permissions, but risk level is capped at MEDIUM.
     */
    private val verifiedPublisherPrefixes = listOf(
        // ── Android / Google ──
        "com.google.",              // Google apps (Maps, Chrome, Photos, Drive, etc.)
        "com.android.",             // Android system/AOSP apps
        "android.",                 // Android framework

        // ── Samsung ──
        "com.samsung.",             // Samsung stock apps
        "com.sec.android.",         // Samsung system apps (Email, Camera, etc.)
        "com.sec.",                 // Samsung system components
        "com.sem.",                 // Samsung Enterprise
        "com.smlds.",              // Samsung ML
        "com.skt.",                 // SK Telecom (carrier)
        "com.osp.",                 // Samsung One Store
        "com.wssyncmldm",          // Samsung firmware updater
        "com.diotek.",             // Samsung keyboard engine

        // ── Other OEMs ──
        "com.huawei.",              // Huawei stock apps
        "com.xiaomi.",              // Xiaomi stock apps
        "com.miui.",                // MIUI system apps
        "com.oppo.",                // OPPO apps
        "com.coloros.",             // ColorOS (OPPO)
        "com.oneplus.",             // OnePlus apps
        "com.bbk.",                 // BBK (Vivo/OPPO parent)
        "com.vivo.",                // Vivo apps
        "com.realme.",              // Realme apps
        "com.asus.",                // ASUS apps
        "com.lge.",                 // LG apps
        "com.motorola.",            // Motorola apps
        "com.lenovo.",              // Lenovo apps
        "com.sony.",                // Sony apps
        "com.sonymobile.",          // Sony Mobile apps
        "com.htc.",                 // HTC apps
        "com.nokia.",               // Nokia apps
        "com.tcl.",                 // TCL apps
        "jp.co.sharp.",             // Sharp apps

        // ── Microsoft ──
        "com.microsoft.",           // Microsoft apps (Office, Teams, Outlook, etc.)

        // ── Apple ──
        "com.apple.",               // Apple apps (Apple Music, etc.)

        // ── Social / Messaging ──
        "com.whatsapp",             // WhatsApp
        "com.meta.",                // Meta apps
        "com.facebook.",            // Facebook apps
        "com.instagram.",           // Instagram
        "org.telegram.",            // Telegram
        "org.signal.",              // Signal
        "com.twitter.",             // X/Twitter
        "com.snapchat.",            // Snapchat
        "com.discord",              // Discord
        "com.slack",                // Slack
        "us.zoom.",                 // Zoom
        "com.skype.",               // Skype
        "jp.naver.line.",           // LINE
        "com.viber.",               // Viber
        "com.tencent.",             // WeChat/QQ (Tencent)
        "com.kakao.",               // KakaoTalk

        // ── Media / Entertainment ──
        "com.spotify.",             // Spotify
        "com.netflix.",             // Netflix
        "com.amazon.",              // Amazon apps
        "com.pinterest.",           // Pinterest
        "com.tiktok.",              // TikTok
        "com.zhiliaoapp.",          // TikTok (original package)
        "com.ss.android.",          // TikTok/ByteDance
        "tv.twitch.",              // Twitch

        // ── Finance / Shopping ──
        "com.paypal.",              // PayPal
        "com.ebay.",                // eBay
        "com.linkedin.",            // LinkedIn
        "com.shopify.",             // Shopify

        // ── Productivity / Tools ──
        "com.adobe.",               // Adobe apps
        "com.dropbox.",             // Dropbox
        "com.github.",              // GitHub
        "org.mozilla.",             // Firefox
        "com.brave.",               // Brave browser
        "com.opera.",               // Opera browser
        "com.duckduckgo.",          // DuckDuckGo
        "com.openai.",              // OpenAI (ChatGPT)

        // ── Carriers / Telecom ──
        "com.turkcell.",            // Turkcell
        "com.vodafone.",            // Vodafone
        "tr.com.turkcell.",         // Turkcell
        "com.avea.",                // Avea/Türk Telekom
        "com.turktelekom.",         // Türk Telekom
    )

    /**
     * Check if package is from a verified major publisher.
     */
    fun isVerifiedPublisher(packageName: String): Boolean {
        return verifiedPublisherPrefixes.any { packageName.startsWith(it) }
    }

    /**
     * Check if app was installed from Play Store.
     */
    fun isPlayStoreInstalled(context: Context, packageName: String): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
            installer == "com.android.vending" || installer == "com.google.android.feedback"
        } catch (_: Exception) {
            false
        }
    }

    fun isTrustedApp(packageName: String): Boolean {
        if (packageName in trustedPackages) return true
        return trustedPackagePrefixes.any { packageName.startsWith(it) }
    }

    private val highRiskPermissions = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_MMS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.BODY_SENSORS",
        "android.permission.BODY_SENSORS_BACKGROUND",
        "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_SIP",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
    )

    private val mediumRiskPermissions = setOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.CHANGE_NETWORK_STATE",
        "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_ADMIN",
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.NFC",
        "android.permission.VIBRATE",
        "android.permission.WAKE_LOCK",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.REQUEST_DELETE_PACKAGES",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.USE_EXACT_ALARM",
        "android.permission.USE_BIOMETRIC",
        "android.permission.USE_FINGERPRINT",
        "android.permission.GET_ACCOUNTS",
        "android.permission.AUTHENTICATE_ACCOUNTS",
        "android.permission.MANAGE_ACCOUNTS",
        "android.permission.READ_SYNC_SETTINGS",
        "android.permission.WRITE_SYNC_SETTINGS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        "android.permission.BIND_VPN_SERVICE",
        "android.permission.PACKAGE_USAGE_STATS"
    )

    fun classify(permissionName: String): RiskLevel {
        return when {
            permissionName in highRiskPermissions -> RiskLevel.HIGH
            permissionName in mediumRiskPermissions -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    /**
     * Classify app risk considering:
     * 1. Trusted apps → always TRUSTED
     * 2. Verified publishers (Google, Microsoft, etc.) → cap at MEDIUM even with HIGH permissions
     * 3. Play Store installed apps → cap at MEDIUM if HIGH permission count is low (<=3)
     * 4. Others → standard classification
     */
    fun classifyApp(
        permissions: List<String>,
        packageName: String = "",
        context: Context? = null
    ): RiskLevel {
        if (isTrustedApp(packageName)) return RiskLevel.TRUSTED
        if (permissions.isEmpty()) return RiskLevel.LOW

        val hasHigh = permissions.any { it in highRiskPermissions }
        val hasMedium = permissions.any { it in mediumRiskPermissions }

        val rawLevel = when {
            hasHigh -> RiskLevel.HIGH
            hasMedium -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Verified major publishers: cap HIGH → MEDIUM
        if (rawLevel == RiskLevel.HIGH && isVerifiedPublisher(packageName)) {
            return RiskLevel.MEDIUM
        }

        // Play Store installed apps with few HIGH permissions: cap HIGH → MEDIUM
        if (rawLevel == RiskLevel.HIGH && context != null) {
            val highCount = permissions.count { it in highRiskPermissions }
            if (highCount <= 3 && isPlayStoreInstalled(context, packageName)) {
                return RiskLevel.MEDIUM
            }
        }

        return rawLevel
    }

    /**
     * Calculate risk score with verified publisher normalization.
     * Verified publishers get 50% score reduction.
     * Play Store apps with few HIGH permissions get 30% reduction.
     */
    fun calculateScore(
        permissions: List<String>,
        packageName: String = "",
        context: Context? = null
    ): Int {
        if (isTrustedApp(packageName)) return 0

        val rawScore = permissions.sumOf { classify(it).score }

        if (isVerifiedPublisher(packageName)) {
            return (rawScore * 0.5f).toInt()
        }

        if (context != null) {
            val highCount = permissions.count { it in highRiskPermissions }
            if (highCount <= 3 && isPlayStoreInstalled(context, packageName)) {
                return (rawScore * 0.7f).toInt()
            }
        }

        return rawScore
    }
}
