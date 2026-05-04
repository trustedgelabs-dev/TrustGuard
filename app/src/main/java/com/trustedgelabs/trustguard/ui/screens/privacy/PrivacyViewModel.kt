package com.trustedgelabs.trustguard.ui.screens.privacy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.service.AppTrackerInfo
import com.trustedgelabs.trustguard.service.PrivacyToolsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val clipboardGuardEnabled: Boolean = false,
    val cameraMonitorEnabled: Boolean = false,
    val micMonitorEnabled: Boolean = false,
    val cameraAppsCount: Int = 0,
    val micAppsCount: Int = 0,
    val locationAppsCount: Int = 0,
    val cameraAppNames: List<String> = emptyList(),
    val micAppNames: List<String> = emptyList(),
    val locationAppNames: List<String> = emptyList(),
    val trackerMap: List<AppTrackerInfo> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0
)

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = PrivacyToolsManager(application)
    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val trackers = manager.getTrackerMap()
            val cameraApps = manager.getAppsWithCameraPermission()
            val micApps = manager.getAppsWithMicPermission()
            val locationApps = manager.getAppsWithLocationPermission()
            _uiState.value = PrivacyUiState(
                clipboardGuardEnabled = manager.isClipboardGuardEnabled(),
                cameraMonitorEnabled = manager.isCameraMonitorEnabled(),
                micMonitorEnabled = manager.isMicMonitorEnabled(),
                cameraAppsCount = cameraApps.size,
                micAppsCount = micApps.size,
                locationAppsCount = locationApps.size,
                cameraAppNames = cameraApps,
                micAppNames = micApps,
                locationAppNames = locationApps,
                trackerMap = trackers,
                isLoading = false
            )
        }
    }

    fun toggleClipboardGuard(enabled: Boolean) {
        manager.setClipboardGuardEnabled(enabled)
        _uiState.value = _uiState.value.copy(clipboardGuardEnabled = enabled)
    }

    fun toggleCameraMonitor(enabled: Boolean) {
        manager.setCameraMonitorEnabled(enabled)
        _uiState.value = _uiState.value.copy(cameraMonitorEnabled = enabled)
    }

    fun toggleMicMonitor(enabled: Boolean) {
        manager.setMicMonitorEnabled(enabled)
        _uiState.value = _uiState.value.copy(micMonitorEnabled = enabled)
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
}
