package com.trustedgelabs.trustguard.util

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Detects whether the device is rooted using passive checks only.
 *
 * Policy: No shell commands are executed during detection (isDeviceRooted).
 * This avoids Play Store policy flags for "su" execution.
 * Shell execution (executeRootCommand) is only used when the user explicitly
 * initiates a root-level recovery scan.
 */
object RootDetector {

    fun isDeviceRooted(): Boolean {
        return checkSuBinary() || checkRootManagers() || checkBuildProperties()
    }

    /**
     * Check if 'su' binary exists in common paths.
     * This is a passive file existence check — no execution.
     */
    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/su/bin/su",
            "/su/bin",
            "/magisk/.core/bin/su",
        )
        return paths.any { File(it).exists() }
    }

    /**
     * Check if known root management apps are installed by checking APK/data paths.
     * No PackageManager queries needed — just file existence.
     */
    private fun checkRootManagers(): Boolean {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/Magisk.apk",
            "/data/app/eu.chainfire.supersu",
            "/data/app/com.topjohnwu.magisk",
            "/data/app/com.noshufou.android.su",
            "/data/app/com.koushikdutta.superuser",
            "/data/app/com.zachspong.temprootremovejb",
            "/data/app/com.ramdroid.appquarantine",
        )
        return rootPaths.any { File(it).exists() }
    }

    /**
     * Check build properties for signs of custom/rooted ROMs.
     * Passive check via Build class — no shell execution.
     */
    private fun checkBuildProperties(): Boolean {
        val suspiciousTags = listOf("test-keys", "dev-keys")
        val suspiciousProps = listOf(
            Build.TAGS,
            Build.DISPLAY,
            Build.FINGERPRINT,
            Build.HOST
        )

        // test-keys in build tags is a strong indicator of custom ROM
        if (suspiciousTags.any { Build.TAGS?.contains(it) == true }) {
            return true
        }

        // Check for common root-related build fingerprint markers
        val lowerFingerprint = Build.FINGERPRINT.lowercase()
        if (lowerFingerprint.contains("lineage") ||
            lowerFingerprint.contains("cyanogen") ||
            lowerFingerprint.contains("unofficial")
        ) {
            return true
        }

        return false
    }

    /**
     * Check root using PackageManager for known root management apps.
     * Optional — requires context.
     */
    fun isDeviceRooted(context: Context): Boolean {
        if (isDeviceRooted()) return true

        val rootPackages = listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloak2",
            "de.robv.android.xposed.installer",
        )
        val pm = context.packageManager
        return rootPackages.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Execute a command as root and return output lines.
     * Returns null if root access is not available.
     *
     * NOTE: This is ONLY called when the user explicitly initiates a root
     * recovery scan. It is never called passively or during detection.
     */
    fun executeRootCommand(command: String): List<String>? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readLines()
            val exitCode = process.waitFor()
            if (exitCode == 0) output else null
        } catch (_: Exception) {
            null
        }
    }
}
