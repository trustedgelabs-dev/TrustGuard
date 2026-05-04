package com.trustedgelabs.trustguard.service

import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Detects and reports ad-like notifications from installed apps.
 * Requires notification access permission from the user.
 */
class NotificationAdDetector : NotificationListenerService() {

    companion object {
        private val _adNotifications = MutableStateFlow<List<AdNotificationInfo>>(emptyList())
        val adNotifications: StateFlow<List<AdNotificationInfo>> = _adNotifications.asStateFlow()

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        // Keywords commonly found in ad notifications
        private val adKeywords = listOf(
            "deal", "offer", "discount", "sale", "coupon",
            "cashback", "reward", "win", "free", "prize",
            "limited time", "hurry", "buy now", "shop now",
            "install", "download", "try now", "claim",
            "special offer", "exclusive", "bonus", "earn",
            "play now", "spin", "jackpot", "coins"
        )

        // Known ad-heavy notification senders
        private val knownAdPackages = setOf(
            "com.cleanmaster", "com.apus.launcher", "com.gau.go.launcherex",
            "com.uc.browser", "com.ksmobile.launcher", "com.nqmobile.antivirus20",
            "com.lionmobi.powerclean", "com.apusapps.launcher", "com.jb.emoji.gokeyboard",
            "com.battery.saver", "com.battery.cooler", "com.ram.booster"
        )

        fun clearAdNotification(packageName: String) {
            _adNotifications.value = _adNotifications.value.filter { it.packageName != packageName }
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isServiceActive.value = true
    }

    override fun onDestroy() {
        _isServiceActive.value = false
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification ?: return

        // Skip system apps and our own app
        if (packageName == applicationContext.packageName) return
        if (isSystemApp(packageName)) return

        val isAd = analyzeNotification(packageName, notification)
        if (isAd) {
            val pm = applicationContext.packageManager
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }

            val extras = notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            val info = AdNotificationInfo(
                packageName = packageName,
                appName = appName,
                title = title,
                text = text,
                timestamp = sbn.postTime,
                notificationKey = sbn.key
            )

            val current = _adNotifications.value.toMutableList()
            // Replace existing from same package or add new
            current.removeAll { it.packageName == packageName }
            current.add(0, info)
            // Keep max 50
            _adNotifications.value = current.take(50)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed
    }

    private fun analyzeNotification(packageName: String, notification: Notification): Boolean {
        // Check known ad packages
        if (packageName in knownAdPackages) return true

        val extras = notification.extras ?: return false
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase() ?: ""
        val combined = "$title $text"

        // Count how many ad keywords match
        var matchCount = 0
        for (keyword in adKeywords) {
            if (keyword in combined) matchCount++
        }

        // If 2+ keywords match, it's likely an ad notification
        if (matchCount >= 2) return true

        // Check for action buttons with ad-like labels
        if (notification.actions != null) {
            for (action in notification.actions) {
                val actionTitle = action.title?.toString()?.lowercase() ?: ""
                if (actionTitle in listOf("install", "download", "buy now", "shop now", "play now", "claim")) {
                    return true
                }
            }
        }

        // Check notification priority and category
        if (notification.category == Notification.CATEGORY_PROMO) return true

        return false
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = applicationContext.packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }
}

data class AdNotificationInfo(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val notificationKey: String
)
