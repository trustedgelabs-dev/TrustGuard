package com.trustedgelabs.trustguard.ui.screens.packetsniffer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.service.AppTrafficStats
import com.trustedgelabs.trustguard.service.PacketSnifferManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PacketSnifferUiState(
    val appTraffic: List<AppTrafficStats> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0 // 0 = Traffic, 1 = Logs
)

class PacketSnifferViewModel(application: Application) : AndroidViewModel(application) {

    private val snifferManager = PacketSnifferManager(application)
    private val _uiState = MutableStateFlow(PacketSnifferUiState())
    val uiState: StateFlow<PacketSnifferUiState> = _uiState.asStateFlow()
    val packetLogs = snifferManager.packetLogs

    init {
        loadTrafficStats()
    }

    fun loadTrafficStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = snifferManager.getAppTrafficStats()
            _uiState.value = _uiState.value.copy(
                appTraffic = stats,
                isLoading = false
            )
        }
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun clearLogs() {
        snifferManager.clearLogs()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadTrafficStats()
    }
}
