package com.trustedgelabs.trustguard.service

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom

data class ApkAnalysisResult(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val isDebuggable: Boolean,
    val hasBackupAllowed: Boolean,
    val isCleartextAllowed: Boolean,
    val permissionCount: Int,
    val dangerousPermissions: List<String>,
    val riskLevel: String // "Low", "Medium", "High"
)

class AppSecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_APP_LOCK_PIN = "app_lock_pin"
        private const val KEY_LOCKED_APPS = "locked_apps"
        private const val KEY_PANIC_ENABLED = "panic_enabled"
    }

    // === APK Analyzer ===
    fun analyzeApk(packageName: String): ApkAnalysisResult? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)

            val dangerousPerms = pkgInfo.requestedPermissions?.filter { perm ->
                try {
                    val permInfo = pm.getPermissionInfo(perm, 0)
                    permInfo.protectionLevel and android.content.pm.PermissionInfo.PROTECTION_DANGEROUS != 0
                } catch (_: Exception) { false }
            } ?: emptyList()

            val isDebuggable = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
            val hasBackup = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0
            val isCleartext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                appInfo.flags and 0x10000000 != 0 // FLAG_USES_CLEARTEXT_TRAFFIC
            } else true

            val risk = when {
                isDebuggable || dangerousPerms.size > 10 -> "High"
                dangerousPerms.size > 5 || isCleartext -> "Medium"
                else -> "Low"
            }

            ApkAnalysisResult(
                packageName = packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                versionName = pkgInfo.versionName ?: "unknown",
                minSdk = appInfo.minSdkVersion,
                targetSdk = appInfo.targetSdkVersion,
                isDebuggable = isDebuggable,
                hasBackupAllowed = hasBackup,
                isCleartextAllowed = isCleartext,
                permissionCount = pkgInfo.requestedPermissions?.size ?: 0,
                dangerousPermissions = dangerousPerms,
                riskLevel = risk
            )
        } catch (e: Exception) { null }
    }

    fun analyzeAllApps(): List<ApkAnalysisResult> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
            .mapNotNull { analyzeApk(it.packageName) }
            .sortedWith(compareByDescending<ApkAnalysisResult> {
                when (it.riskLevel) { "High" -> 2; "Medium" -> 1; else -> 0 }
            }.thenByDescending { it.dangerousPermissions.size })
    }

    // === App Lock ===
    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setupAppLock(pin: String) {
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .putString(KEY_APP_LOCK_PIN, pin)
            .apply()
    }

    fun verifyAppLockPin(pin: String): Boolean {
        return prefs.getString(KEY_APP_LOCK_PIN, null) == pin
    }

    fun getLockedApps(): Set<String> = prefs.getStringSet(KEY_LOCKED_APPS, emptySet()) ?: emptySet()

    fun toggleAppLock(packageName: String) {
        val locked = getLockedApps().toMutableSet()
        if (packageName in locked) locked.remove(packageName)
        else locked.add(packageName)
        prefs.edit().putStringSet(KEY_LOCKED_APPS, locked).apply()
    }

    // === Panic Button ===
    fun isPanicEnabled(): Boolean = prefs.getBoolean(KEY_PANIC_ENABLED, false)
    fun setPanicEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PANIC_ENABLED, enabled).apply()

    // === File Shredder ===
    fun shredFile(file: File): Boolean {
        return try {
            if (!file.exists()) return false
            val length = file.length()
            val random = SecureRandom()

            // 3-pass overwrite (DoD standard)
            repeat(3) {
                FileOutputStream(file).use { fos ->
                    val buffer = ByteArray(4096)
                    var remaining = length
                    while (remaining > 0) {
                        val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                        random.nextBytes(buffer)
                        fos.write(buffer, 0, toWrite)
                        remaining -= toWrite
                    }
                    fos.fd.sync()
                }
            }

            // Zero-fill final pass
            FileOutputStream(file).use { fos ->
                val buffer = ByteArray(4096)
                var remaining = length
                while (remaining > 0) {
                    val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                    fos.write(buffer, 0, toWrite)
                    remaining -= toWrite
                }
                fos.fd.sync()
            }

            file.delete()
            true
        } catch (e: Exception) {
            android.util.Log.e("FileShredder", "Shred failed", e)
            false
        }
    }

    // === USB/ADB Guard ===
    fun isAdbEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (_: Exception) { false }
    }

    fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (_: Exception) { false }
    }
}
