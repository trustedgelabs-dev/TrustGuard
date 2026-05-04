package com.trustedgelabs.trustguard.ui.screens.virusscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.domain.VirusScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VirusScanUiState(
    val isScanning: Boolean = false,
    val scanComplete: Boolean = false,
    val scanProgress: Float = 0f,
    val scanPhase: ScanPhase = ScanPhase.IDLE,
    val result: VirusScanner.ScanResult? = null
)

enum class ScanPhase {
    IDLE,
    ANALYZING_APPS,
    CHECKING_PERMISSIONS,
    VERIFYING_SIGNATURES,
    CHECKING_SYSTEM,
    COMPLETE
}

class VirusScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VirusScanUiState())
    val uiState: StateFlow<VirusScanUiState> = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = VirusScanUiState(isScanning = true, scanProgress = 0f)

            // Phase 1: Analyzing apps
            _uiState.value = _uiState.value.copy(
                scanPhase = ScanPhase.ANALYZING_APPS,
                scanProgress = 0.1f
            )
            delay(400)

            // Phase 2: Checking permissions
            _uiState.value = _uiState.value.copy(
                scanPhase = ScanPhase.CHECKING_PERMISSIONS,
                scanProgress = 0.3f
            )
            delay(300)

            // Phase 3: Verifying signatures
            _uiState.value = _uiState.value.copy(
                scanPhase = ScanPhase.VERIFYING_SIGNATURES,
                scanProgress = 0.5f
            )
            delay(300)

            // Phase 4: System check
            _uiState.value = _uiState.value.copy(
                scanPhase = ScanPhase.CHECKING_SYSTEM,
                scanProgress = 0.7f
            )

            // Run actual scan
            val result = withContext(Dispatchers.IO) {
                VirusScanner.fullScan(getApplication())
            }

            _uiState.value = _uiState.value.copy(scanProgress = 0.9f)
            delay(200)

            _uiState.value = VirusScanUiState(
                isScanning = false,
                scanComplete = true,
                scanProgress = 1f,
                scanPhase = ScanPhase.COMPLETE,
                result = result
            )
        }
    }
}
