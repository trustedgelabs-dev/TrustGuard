package com.trustedgelabs.trustguard.data.repository

import com.trustedgelabs.trustguard.data.datasource.PackageManagerDataSource
import com.trustedgelabs.trustguard.data.model.AppInfo

class AppRepositoryImpl(
    private val dataSource: PackageManagerDataSource
) : AppRepository {

    private var cachedApps: List<AppInfo>? = null

    override suspend fun getInstalledApps(): List<AppInfo> {
        cachedApps?.let { return it }

        val apps = dataSource.getInstalledApps()
        cachedApps = apps
        return apps
    }

    override suspend fun getAppDetail(packageName: String): AppInfo? {
        cachedApps?.find { it.packageName == packageName }?.let { return it }
        return dataSource.getAppByPackageName(packageName)
    }

    override fun clearCache() {
        cachedApps = null
    }
}
