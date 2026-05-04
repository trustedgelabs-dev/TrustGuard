package com.trustedgelabs.trustguard.ui.screens.bloatware

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.service.BloatwareApp
import com.trustedgelabs.trustguard.service.BloatwareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BloatwareUiState(
    val apps: List<BloatwareApp> = emptyList(),
    val isScanning: Boolean = true,
    val totalSizeBytes: Long = 0
)

class BloatwareViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = BloatwareManager(application)
    private val _uiState = MutableStateFlow(BloatwareUiState())
    val uiState: StateFlow<BloatwareUiState> = _uiState.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        viewModelScope.launch(Dispatchers.IO) {
            val apps = manager.detectBloatware()
            _uiState.value = BloatwareUiState(
                apps = apps,
                isScanning = false,
                totalSizeBytes = apps.sumOf { it.sizeBytes }
            )
        }
    }
}
