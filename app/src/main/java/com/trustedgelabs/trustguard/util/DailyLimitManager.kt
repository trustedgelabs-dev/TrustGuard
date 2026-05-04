package com.trustedgelabs.trustguard.util

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest
import java.util.Calendar

object DailyLimitManager {

    private const val PREFS_NAME = "tg_app_metrics"
    private const val BACKUP_PREFS_NAME = "tg_analytics_cache"

    // Obfuscated key names — not obvious what they store
    private const val KEY_SCAN_COUNT = "m_evt_c"
    private const val KEY_LAST_SCAN_DATE = "m_evt_d"
    private const val KEY_INTEGRITY_HASH = "m_chk"
    private const val KEY_FIRST_SEEN = "m_fs"

    // Backup keys (second SharedPreferences file)
    private const val BKEY_SCAN_COUNT = "c_buf_c"
    private const val BKEY_LAST_SCAN_DATE = "c_buf_d"
    private const val BKEY_INTEGRITY_HASH = "c_chk"

    const val FREE_DAILY_SCAN_LIMIT = 2

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getBackupPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    /**
     * Creates a device-specific fingerprint that survives data clears.
     * Uses ANDROID_ID + package install time as seed.
     */
    private fun getDeviceFingerprint(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (_: Exception) { "unknown" }

        val installTime = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: Exception) { 0L }

        val raw = "$androidId-$installTime-${context.packageName}"
        return hashString(raw)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING).take(16)
    }

    /**
     * Encode a value with device fingerprint so it can't be trivially edited.
     */
    private fun encodeValue(value: String, context: Context): String {
        val fp = getDeviceFingerprint(context)
        val combined = "$fp:$value"
        return Base64.encodeToString(combined.toByteArray(), Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun decodeValue(encoded: String, context: Context): String? {
        return try {
            val decoded = String(Base64.decode(encoded, Base64.NO_WRAP or Base64.NO_PADDING))
            val fp = getDeviceFingerprint(context)
            if (decoded.startsWith("$fp:")) {
                decoded.removePrefix("$fp:")
            } else {
                null // tampered or from different device
            }
        } catch (_: Exception) { null }
    }

    private fun computeIntegrityHash(date: String, count: Int, context: Context): String {
        val fp = getDeviceFingerprint(context)
        return hashString("$fp|$date|$count|tg_salt_2024")
    }

    /**
     * Write scan data to both primary and backup SharedPreferences.
     */
    private fun writeData(context: Context, date: String, count: Int) {
        val encodedDate = encodeValue(date, context)
        val encodedCount = encodeValue(count.toString(), context)
        val integrityHash = computeIntegrityHash(date, count, context)

        getPrefs(context).edit()
            .putString(KEY_LAST_SCAN_DATE, encodedDate)
            .putString(KEY_SCAN_COUNT, encodedCount)
            .putString(KEY_INTEGRITY_HASH, integrityHash)
            .apply()

        // Backup copy — if user clears one, we recover from the other
        getBackupPrefs(context).edit()
            .putString(BKEY_LAST_SCAN_DATE, encodedDate)
            .putString(BKEY_SCAN_COUNT, encodedCount)
            .putString(BKEY_INTEGRITY_HASH, integrityHash)
            .apply()
    }

    /**
     * Read scan data. Tries primary prefs first, falls back to backup.
     * If both are cleared/tampered, treats it as if max scans were used (anti-abuse).
     */
    private fun readData(context: Context): Pair<String, Int> {
        val today = getTodayDateString()

        // Try primary
        val primaryResult = tryReadFrom(getPrefs(context), KEY_LAST_SCAN_DATE, KEY_SCAN_COUNT, KEY_INTEGRITY_HASH, context)
        if (primaryResult != null) {
            return primaryResult
        }

        // Try backup
        val backupResult = tryReadFrom(getBackupPrefs(context), BKEY_LAST_SCAN_DATE, BKEY_SCAN_COUNT, BKEY_INTEGRITY_HASH, context)
        if (backupResult != null) {
            // Restore primary from backup
            writeData(context, backupResult.first, backupResult.second)
            return backupResult
        }

        // Both cleared — check if this is truly first launch or a data clear
        val prefs = getPrefs(context)
        val firstSeen = prefs.getLong(KEY_FIRST_SEEN, 0L)
        if (firstSeen == 0L) {
            // Genuine first launch
            prefs.edit().putLong(KEY_FIRST_SEEN, System.currentTimeMillis()).apply()
            return today to 0
        }

        // Data was cleared (firstSeen existed before but scan data is gone)
        // Penalize: assume daily limit reached for today
        writeData(context, today, FREE_DAILY_SCAN_LIMIT)
        return today to FREE_DAILY_SCAN_LIMIT
    }

    private fun tryReadFrom(
        prefs: SharedPreferences,
        dateKey: String,
        countKey: String,
        hashKey: String,
        context: Context
    ): Pair<String, Int>? {
        val encodedDate = prefs.getString(dateKey, null) ?: return null
        val encodedCount = prefs.getString(countKey, null) ?: return null
        val storedHash = prefs.getString(hashKey, null) ?: return null

        val date = decodeValue(encodedDate, context) ?: return null
        val count = decodeValue(encodedCount, context)?.toIntOrNull() ?: return null

        // Verify integrity
        val expectedHash = computeIntegrityHash(date, count, context)
        if (storedHash != expectedHash) return null

        return date to count
    }

    fun getScansUsedToday(context: Context): Int {
        val (date, count) = readData(context)
        val today = getTodayDateString()
        return if (date == today) count else 0
    }

    fun getScansRemainingToday(context: Context): Int {
        return (FREE_DAILY_SCAN_LIMIT - getScansUsedToday(context)).coerceAtLeast(0)
    }

    fun canScanToday(context: Context): Boolean {
        return getScansUsedToday(context) < FREE_DAILY_SCAN_LIMIT
    }

    fun recordScan(context: Context) {
        val today = getTodayDateString()
        val (date, count) = readData(context)

        val newCount = if (date == today) count + 1 else 1
        writeData(context, today, newCount)
    }
}
