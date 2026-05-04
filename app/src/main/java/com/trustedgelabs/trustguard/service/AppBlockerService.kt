package com.trustedgelabs.trustguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.model.ActivityLogEntry
import com.trustedgelabs.trustguard.data.model.ActivityType
import com.trustedgelabs.trustguard.util.FamilyShieldManager
import java.util.Timer
import java.util.TimerTask

/**
 * Foreground service that monitors running apps and blocks ones
 * that are in the Family Shield block list.
 *
 * Also tracks screen time via ScreenTimeTracker and shows
 * a live countdown in the notification panel.
 */
class AppBlockerService : Service() {

    companion object {
        private const val CHANNEL_ID = "trustguard_app_blocker"
        private const val NOTIFICATION_ID = 6001
        private const val CHECK_INTERVAL_MS = 2000L

        fun start(context: Context) {
            val intent = Intent(context, AppBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppBlockerService::class.java))
        }
    }

    private var timer: Timer? = null
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0L
    private var notificationManager: NotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(NOTIFICATION_ID, buildNotification())
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If timer died for some reason, restart monitoring
        if (timer == null) {
            startMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fs_blocker_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fs_blocker_service_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.fs_blocker_notification_title))
            .setContentText(getString(R.string.fs_blocker_notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startMonitoring() {
        timer = Timer("AppBlocker", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    try {
                        checkForegroundApp()
                    } catch (_: Exception) {
                        // Never let the timer die
                    }
                }
            }, 0, CHECK_INTERVAL_MS)
        }
    }

    // ═══════════════════════════════════════════
    // ORIGINAL WORKING APP BLOCKER (restored)
    // ═══════════════════════════════════════════

    private fun checkForegroundApp() {
        if (!FamilyShieldManager.isEnabled(this)) return

        // Check overlay permission - required on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return

        val foregroundPkg = getForegroundPackage() ?: return
        if (foregroundPkg == packageName) return // Don't block ourselves

        // ── App blocking (original logic) ──
        if (FamilyShieldManager.isAppBlocked(this, foregroundPkg)) {
            val now = System.currentTimeMillis()
            // Debounce: don't re-block same app within 3 seconds
            if (foregroundPkg == lastBlockedPackage && now - lastBlockTime < 3000) return

            lastBlockedPackage = foregroundPkg
            lastBlockTime = now

            // Log the block attempt
            val appName = try {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(foregroundPkg, android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(foregroundPkg, 0)
                }
                packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) { foregroundPkg }

            FamilyShieldManager.addLogEntry(this, ActivityLogEntry(
                type = ActivityType.APP_BLOCKED_ATTEMPT,
                packageName = foregroundPkg,
                appName = appName,
                details = "Blocked app launch: $appName"
            ))

            // Launch blocking overlay
            val blockIntent = Intent(this, BlockedAppActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("blocked_app_name", appName)
                putExtra("blocked_package", foregroundPkg)
                putExtra("is_screen_time_limit", false)
            }
            startActivity(blockIntent)
        }
    }

    // ═══════════════════════════════════════════
    // FOREGROUND APP DETECTION (original)
    // ═══════════════════════════════════════════

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 5000 // Last 5 seconds

        val events = try {
            usm.queryEvents(startTime, endTime)
        } catch (_: Exception) { return null }

        var lastForegroundPkg: String? = null
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPkg = event.packageName
            }
        }
        return lastForegroundPkg
    }
}
