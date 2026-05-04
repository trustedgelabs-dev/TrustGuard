package com.trustedgelabs.trustguard.ui.screens.adware

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.domain.AdwareDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdwareUiState(
    val isScanning: Boolean = false,
    val scanComplete: Boolean = false,
    val suspiciousApps: List<AdwareDetector.AdwareReport> = emptyList()
)

class AdwareViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AdwareUiState())
    val uiState: StateFlow<AdwareUiState> = _uiState.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanComplete = false)

            val results = withContext(Dispatchers.IO) {
                AdwareDetector.scanAllApps(getApplication())
            }

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                scanComplete = true,
                suspiciousApps = results
            )
        }
    }
}
