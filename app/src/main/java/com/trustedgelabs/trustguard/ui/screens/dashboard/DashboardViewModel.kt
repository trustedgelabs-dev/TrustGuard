package com.trustedgelabs.trustguard.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.data.datasource.PackageManagerDataSource
import com.trustedgelabs.trustguard.data.model.AppInfo
import com.trustedgelabs.trustguard.data.model.RiskLevel
import com.trustedgelabs.trustguard.data.repository.AppRepositoryImpl
import com.trustedgelabs.trustguard.domain.AdwareDetector
import com.trustedgelabs.trustguard.domain.usecase.ScanAppsUseCase
import com.trustedgelabs.trustguard.service.VpnControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isScanning: Boolean = false,
    val scanComplete: Boolean = false,
    val totalApps: Int = 0,
    val redCount: Int = 0,
    val yellowCount: Int = 0,
    val greenCount: Int = 0,
    val overallScore: Int = 100,
    val topRiskApps: List<AppInfo> = emptyList(),
    val allApps: List<AppInfo> = emptyList(),
    val vpnActive: Boolean = false,
    val blockedTodayCount: Int = 0,
    val adwareSuspiciousCount: Int = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource = PackageManagerDataSource(application)
    private val repository = AppRepositoryImpl(dataSource)
    private val scanAppsUseCase = ScanAppsUseCase(repository)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        scan()
        observeVpnState()
        scanAdware()
    }

    private fun observeVpnState() {
        viewModelScope.launch {
            VpnControlManager.isVpnActive.collect { active ->
                _uiState.value = _uiState.value.copy(vpnActive = active)
            }
        }
        viewModelScope.launch {
            VpnControlManager.blockingStats.collect { stats ->
                _uiState.value = _uiState.value.copy(blockedTodayCount = stats.totalBlockedToday)
            }
        }
    }

    fun scan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanComplete = false)

            try {
                val apps = scanAppsUseCase(includeSystemApps = false)

                val redCount = apps.count { it.riskLevel == RiskLevel.HIGH }
                val yellowCount = apps.count { it.riskLevel == RiskLevel.MEDIUM }
                val greenCount = apps.count { it.riskLevel == RiskLevel.LOW || it.riskLevel == RiskLevel.TRUSTED }

                val score = calculateSecurityScore(apps)

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanComplete = true,
                    totalApps = apps.size,
                    redCount = redCount,
                    yellowCount = yellowCount,
                    greenCount = greenCount,
                    overallScore = score,
                    topRiskApps = apps.take(5),
                    allApps = apps
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    private fun scanAdware() {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                AdwareDetector.scanAllApps(getApplication())
            }
            _uiState.value = _uiState.value.copy(adwareSuspiciousCount = results.size)
        }
    }

    fun rescan() {
        repository.clearCache()
        scan()
        scanAdware()
    }

    fun toggleVpn(context: android.content.Context) {
        if (VpnControlManager.isVpnActive.value) {
            VpnControlManager.stopVpn(context)
        } else {
            VpnControlManager.startVpn(context)
        }
    }

    private fun calculateSecurityScore(apps: List<AppInfo>): Int {
        if (apps.isEmpty()) return 100

        val totalPossibleRisk = apps.size * 3
        var actualRisk = 0
        for (app in apps) {
            actualRisk += when (app.riskLevel) {
                RiskLevel.HIGH -> 3
                RiskLevel.MEDIUM -> 1
                RiskLevel.LOW -> 0
                RiskLevel.TRUSTED -> 0
            }
        }

        val riskRatio = actualRisk.toFloat() / totalPossibleRisk.toFloat()
        return ((1f - riskRatio) * 100).toInt().coerceIn(0, 100)
    }
}
