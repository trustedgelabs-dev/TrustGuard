package com.trustedgelabs.trustguard.util

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.trustedgelabs.trustguard.data.model.AppUsageEntry
import com.trustedgelabs.trustguard.data.model.DailyUsageSummary
import org.json.JSONObject
import java.util.Calendar

/**
 * Custom screen time tracker that works independently of UsageStatsManager.
 * AppBlockerService calls [recordTick] every 2 seconds with the current foreground package.
 * We accumulate time ourselves — this is 100% reliable on all devices.
 *
 * Data resets automatically at midnight.
 */
object ScreenTimeTracker {

    private const val PREFS_NAME = "tg_screen_time"
    private const val KEY_DATE = "st_date"
    private const val KEY_USAGE_DATA = "st_data"
    private const val TICK_SECONDS = 2L

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getTodayString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    /**
     * Called by AppBlockerService every 2 seconds with the current foreground package.
     * Adds TICK_SECONDS to that package's daily usage.
     */
    fun recordTick(context: Context, foregroundPackage: String) {
        try {
            if (foregroundPackage == context.packageName) return
            if (isSystemLauncher(context, foregroundPackage)) return

            val prefs = getPrefs(context)
            val today = getTodayString()
            val storedDate = prefs.getString(KEY_DATE, "") ?: ""

            val usageData = if (storedDate != today) {
                JSONObject()
            } else {
                try {
                    JSONObject(prefs.getString(KEY_USAGE_DATA, "{}") ?: "{}")
                } catch (_: Exception) {
                    JSONObject()
                }
            }

            val currentSeconds = usageData.optLong(foregroundPackage, 0L)
            usageData.put(foregroundPackage, currentSeconds + TICK_SECONDS)

            prefs.edit()
                .putString(KEY_DATE, today)
                .putString(KEY_USAGE_DATA, usageData.toString())
                .apply()
        } catch (_: Exception) {
            // Never crash - this runs in the blocker service timer
        }
    }

    /**
     * Get today's usage summary using our own tracked data.
     */
    fun getTodayUsageSummary(context: Context, dailyLimitMinutes: Int): DailyUsageSummary {
        val prefs = getPrefs(context)
        val today = getTodayString()
        val storedDate = prefs.getString(KEY_DATE, "") ?: ""

        if (storedDate != today) {
            return DailyUsageSummary(
                date = today,
                totalMinutes = 0,
                appUsages = emptyList(),
                limitMinutes = dailyLimitMinutes,
                isLimitExceeded = false
            )
        }

        val usageData = try {
            JSONObject(prefs.getString(KEY_USAGE_DATA, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }

        val pm = context.packageManager
        val entries = mutableListOf<AppUsageEntry>()

        val keys = usageData.keys()
        while (keys.hasNext()) {
            val pkg = keys.next()
            val seconds = usageData.getLong(pkg)
            val minutes = seconds / 60

            if (minutes < 1) continue // Skip <1 min

            // Skip system apps
            val isSystem = try {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
                info.flags and ApplicationInfo.FLAG_SYSTEM != 0
            } catch (_: Exception) { true }

            if (isSystem) continue

            val appName = try {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) { pkg }

            entries.add(AppUsageEntry(
                packageName = pkg,
                appName = appName,
                usageMinutes = minutes
            ))
        }

        val totalMinutes = entries.sumOf { it.usageMinutes }

        return DailyUsageSummary(
            date = today,
            totalMinutes = totalMinutes,
            appUsages = entries.sortedByDescending { it.usageMinutes },
            limitMinutes = dailyLimitMinutes,
            isLimitExceeded = dailyLimitMinutes > 0 && totalMinutes >= dailyLimitMinutes
        )
    }

    /**
     * Get total screen time in minutes for today (fast, no app name resolution).
     */
    fun getTotalMinutesToday(context: Context): Long {
        val prefs = getPrefs(context)
        val today = getTodayString()
        val storedDate = prefs.getString(KEY_DATE, "") ?: ""

        if (storedDate != today) return 0

        val usageData = try {
            JSONObject(prefs.getString(KEY_USAGE_DATA, "{}") ?: "{}")
        } catch (_: Exception) { return 0 }

        var totalSeconds = 0L
        val keys = usageData.keys()
        while (keys.hasNext()) {
            totalSeconds += usageData.getLong(keys.next())
        }
        return totalSeconds / 60
    }

    /**
     * Check if daily limit is exceeded (fast check for service).
     */
    fun isLimitExceeded(context: Context, dailyLimitMinutes: Int): Boolean {
        if (dailyLimitMinutes <= 0) return false
        return getTotalMinutesToday(context) >= dailyLimitMinutes
    }

    private fun isSystemLauncher(context: Context, packageName: String): Boolean {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            resolveInfo?.activityInfo?.packageName == packageName
        } catch (_: Exception) { false }
    }
}
