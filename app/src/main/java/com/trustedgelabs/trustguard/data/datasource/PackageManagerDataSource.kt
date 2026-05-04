package com.trustedgelabs.trustguard.data.datasource

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.trustedgelabs.trustguard.data.model.AppInfo
import com.trustedgelabs.trustguard.data.model.PermissionInfo
import com.trustedgelabs.trustguard.data.model.RiskLevel
import com.trustedgelabs.trustguard.domain.PermissionClassifier
import com.trustedgelabs.trustguard.util.PermissionDescriptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageManagerDataSource(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = getInstalledPackages(pm)

        packages.map { packageInfo ->
            mapToAppInfo(pm, packageInfo)
        }
    }

    suspend fun getAppByPackageName(packageName: String): AppInfo? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            mapToAppInfo(pm, packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getInstalledPackages(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
    }

    private fun mapToAppInfo(pm: PackageManager, packageInfo: PackageInfo): AppInfo {
        val appInfo = packageInfo.applicationInfo
        val appName = appInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName
        val icon = appInfo?.loadIcon(pm)
        val isSystemApp = appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0

        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

        val permissions = requestedPermissions.map { permName ->
            val riskLevel = PermissionClassifier.classify(permName)
            val shortName = permName.substringAfterLast(".")
            val description = PermissionDescriptions.get(permName)

            PermissionInfo(
                name = permName,
                shortName = shortName,
                description = description,
                riskLevel = riskLevel
            )
        }.sortedBy { it.riskLevel.ordinal }

        val overallRisk = PermissionClassifier.classifyApp(requestedPermissions.toList(), packageInfo.packageName, context)
        val riskScore = PermissionClassifier.calculateScore(requestedPermissions.toList(), packageInfo.packageName, context)
        val dangerousCount = permissions.count { it.riskLevel == RiskLevel.HIGH }

        return AppInfo(
            packageName = packageInfo.packageName,
            appName = appName,
            icon = icon,
            permissions = permissions,
            riskLevel = overallRisk,
            riskScore = riskScore,
            isSystemApp = isSystemApp,
            dangerousPermissionCount = dangerousCount,
            totalPermissionCount = permissions.size
        )
    }
}
