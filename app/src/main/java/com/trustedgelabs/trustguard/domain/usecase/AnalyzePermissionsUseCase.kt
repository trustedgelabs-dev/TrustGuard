package com.trustedgelabs.trustguard.domain.usecase

import com.trustedgelabs.trustguard.data.model.AppInfo
import com.trustedgelabs.trustguard.data.repository.AppRepository

class AnalyzePermissionsUseCase(private val repository: AppRepository) {

    suspend operator fun invoke(packageName: String): AppInfo? {
        return repository.getAppDetail(packageName)
    }
}
