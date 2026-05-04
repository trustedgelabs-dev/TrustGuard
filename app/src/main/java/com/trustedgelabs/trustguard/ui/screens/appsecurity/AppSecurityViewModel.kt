package com.trustedgelabs.trustguard.ui.screens.appsecurity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.service.ApkAnalysisResult
import com.trustedgelabs.trustguard.service.AppSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppSecurityUiState(
    val analysisResults: List<ApkAnalysisResult> = emptyList(),
    val isScanning: Boolean = true,
    val adbEnabled: Boolean = false,
    val devOptionsEnabled: Boolean = false,
    val highRiskCount: Int = 0,
    val selectedTab: Int = 0
)

class AppSecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = AppSecurityManager(application)
    private val _uiState = MutableStateFlow(AppSecurityUiState())
    val uiState: StateFlow<AppSecurityUiState> = _uiState.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        viewModelScope.launch(Dispatchers.IO) {
            val results = manager.analyzeAllApps()
            _uiState.value = AppSecurityUiState(
                analysisResults = results,
                isScanning = false,
                adbEnabled = manager.isAdbEnabled(),
                devOptionsEnabled = manager.isDeveloperOptionsEnabled(),
                highRiskCount = results.count { it.riskLevel == "High" }
            )
        }
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
}
