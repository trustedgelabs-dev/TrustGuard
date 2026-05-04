package com.trustedgelabs.trustguard.data.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.trustedgelabs.trustguard.data.model.AppIntegrityInfo
import com.trustedgelabs.trustguard.data.model.InstallSource

class AppIntegrityDataSource(private val context: Context) {

    fun scanAllApps(): List<AppIntegrityInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        return packages.mapNotNull { pkg ->
            try {
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                val appName = pm.getApplicationLabel(appInfo).toString()
                val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                val installSource = getInstallSource(pm, pkg.packageName)

                AppIntegrityInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    installSource = installSource,
                    isSystemApp = isSystemApp,
                    versionName = pkg.versionName ?: "Unknown"
                )
            } catch (_: Exception) {
                null
            }
        }.sortedWith(
            compareBy<AppIntegrityInfo> { it.installSource != InstallSource.SIDELOADED }
                .thenBy { it.installSource != InstallSource.UNKNOWN }
                .thenBy { it.appName.lowercase() }
        )
    }

    private fun getInstallSource(pm: PackageManager, packageName: String): InstallSource {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }

            when (installer) {
                "com.android.vending" -> InstallSource.PLAY_STORE
                "com.google.android.packageinstaller",
                "com.samsung.android.packageinstaller",
                "com.miui.packageinstaller" -> InstallSource.SIDELOADED
                null -> InstallSource.SYSTEM
                else -> {
                    if (installer.contains("store") || installer.contains("market")) {
                        InstallSource.OTHER_STORE
                    } else {
                        InstallSource.UNKNOWN
                    }
                }
            }
        } catch (_: Exception) {
            InstallSource.UNKNOWN
        }
    }
}
