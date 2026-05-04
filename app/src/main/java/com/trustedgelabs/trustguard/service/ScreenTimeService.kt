package com.trustedgelabs.trustguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.trustedgelabs.trustguard.R

/**
 * Separate foreground service ONLY for screen time countdown.
 * Completely independent from AppBlockerService.
 *
 * Shows a live countdown in the notification panel.
 * When time reaches 0 → launches TimeUpActivity with alarm sound.
 */
class ScreenTimeService : Service() {

    companion object {
        private const val CHANNEL_ID = "trustguard_screen_time"
        private const val NOTIFICATION_ID = 6002
        const val EXTRA_MINUTES = "extra_minutes"

        private const val PREFS_NAME = "tg_screen_timer"
        private const val KEY_END_TIME = "st_end_time"
        private const val KEY_TOTAL_MINUTES = "st_total_minutes"
        private const val KEY_ACTIVE = "st_active"

        fun start(context: Context, minutes: Int) {
            if (minutes <= 0) {
                stop(context)
                return
            }
            val intent = Intent(context, ScreenTimeService::class.java).apply {
                putExtra(EXTRA_MINUTES, minutes)
            }
            // Save end time
            val endTime = System.currentTimeMillis() + (minutes * 60_000L)
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putLong(KEY_END_TIME, endTime)
                .putInt(KEY_TOTAL_MINUTES, minutes)
                .putBoolean(KEY_ACTIVE, true)
                .apply()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // Safety: ensure AppBlockerService is also running (belt-and-suspenders)
            try { AppBlockerService.start(context) } catch (_: Exception) { }
        }

        fun stop(context: Context) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean(KEY_ACTIVE, false)
                .apply()
            context.stopService(Intent(context, ScreenTimeService::class.java))
        }

        fun isActive(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ACTIVE, false)
        }

        fun getRemainingMinutes(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ACTIVE, false)) return 0
            val endTime = prefs.getLong(KEY_END_TIME, 0L)
            val remaining = endTime - System.currentTimeMillis()
            return if (remaining > 0) (remaining / 60_000).toInt() else 0
        }

        fun getTotalMinutes(context: Context): Int {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_TOTAL_MINUTES, 0)
        }
    }

    private var countDownTimer: CountDownTimer? = null
    private var notificationManager: NotificationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 0) ?: 0
        if (minutes <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Cancel previous timer
        countDownTimer?.cancel()

        val totalMs = minutes * 60_000L
        startForeground(NOTIFICATION_ID, buildNotification(totalMs))

        // Start countdown
        countDownTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val notif = buildNotification(millisUntilFinished)
                notificationManager?.notify(NOTIFICATION_ID, notif)
            }

            override fun onFinish() {
                // Time's up!
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putBoolean(KEY_ACTIVE, false)
                    .apply()

                // Show alarm notification
                showTimeUpNotification()

                // Launch alarm activity
                val alarmIntent = Intent(this@ScreenTimeService, TimeUpActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(alarmIntent)

                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.fs_screen_time_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.fs_screen_time_channel_desc)
                setShowBadge(false)
            }
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(remainingMs: Long): Notification {
        val hours = (remainingMs / 3600000).toInt()
        val minutes = ((remainingMs % 3600000) / 60000).toInt()
        val seconds = ((remainingMs % 60000) / 1000).toInt()

        val timeText = if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val totalMinutes = getTotalMinutes(this)
        val title = getString(R.string.fs_screen_time_notification_title)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText("$timeText ${getString(R.string.fs_remaining)}")
            .setSubText("${totalMinutes} ${getString(R.string.fs_minutes)}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun showTimeUpNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                "trustguard_time_up",
                getString(R.string.fs_time_up_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.fs_time_up_channel_desc)
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(alarmChannel)
        }

        val notification = NotificationCompat.Builder(this, "trustguard_time_up")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.fs_time_up_title))
            .setContentText(getString(R.string.fs_time_up_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager?.notify(6003, notification)
    }
}
