package com.trustedgelabs.trustguard.domain.usecase

import com.trustedgelabs.trustguard.data.model.AppInfo
import com.trustedgelabs.trustguard.data.repository.AppRepository

class ScanAppsUseCase(private val repository: AppRepository) {

    suspend operator fun invoke(includeSystemApps: Boolean = false): List<AppInfo> {
        val apps = repository.getInstalledApps()
        val filtered = if (includeSystemApps) apps else apps.filter { !it.isSystemApp }
        return filtered.sortedByDescending { it.riskScore }
    }
}
