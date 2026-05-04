package com.trustedgelabs.trustguard.data.repository

import com.trustedgelabs.trustguard.data.model.AppInfo

interface AppRepository {
    suspend fun getInstalledApps(): List<AppInfo>
    suspend fun getAppDetail(packageName: String): AppInfo?
    fun clearCache()
}
