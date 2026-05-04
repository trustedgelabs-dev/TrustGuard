package com.trustedgelabs.trustguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.util.FamilyShieldManager

/**
 * Receives PACKAGE_ADDED broadcasts to detect new app installations.
 * When Family Shield is active, logs the event and sends a local notification.
 */
class AppInstallReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "trustguard_family_shield"
        private const val NOTIFICATION_ID_BASE = 5000
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return // update, not new install

        val packageName = intent.data?.schemeSpecificPart ?: return

        // Skip if Family Shield is not active
        if (!FamilyShieldManager.isEnabled(context)) return

        // Notify FamilyShieldManager
        FamilyShieldManager.onNewAppInstalled(context, packageName)

        // Send local notification
        val config = FamilyShieldManager.getConfig(context)
        if (!config.newInstallAlertEnabled) return

        val pm = context.packageManager
        val appName = try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) { packageName }

        sendNotification(context, appName, packageName)
    }

    private fun sendNotification(context: Context, appName: String, packageName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.fs_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.fs_notification_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.fs_new_app_alert_title))
            .setContentText(context.getString(R.string.fs_new_app_alert_body, appName))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.fs_new_app_alert_detail, appName, packageName)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_BASE + packageName.hashCode() % 1000, notification)
    }
}
