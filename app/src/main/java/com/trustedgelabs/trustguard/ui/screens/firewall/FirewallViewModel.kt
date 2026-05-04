package com.trustedgelabs.trustguard.ui.screens.firewall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.service.FirewallManager
import com.trustedgelabs.trustguard.service.VpnControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FirewallUiState(
    val isFirewallEnabled: Boolean = false,
    val apps: List<FirewallManager.AppFirewallRule> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val vpnActive: Boolean = false,
    val needsVpnRestart: Boolean = false
)

class FirewallViewModel(application: Application) : AndroidViewModel(application) {

    private val firewallManager = FirewallManager(application)
    private val _uiState = MutableStateFlow(FirewallUiState())
    val uiState: StateFlow<FirewallUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        viewModelScope.launch {
            VpnControlManager.isVpnActive.collect { active ->
                _uiState.value = _uiState.value.copy(vpnActive = active)
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = firewallManager.getInstalledAppsWithRules()
            _uiState.value = _uiState.value.copy(
                isFirewallEnabled = firewallManager.isFirewallEnabled(),
                apps = apps,
                isLoading = false,
                vpnActive = VpnControlManager.isVpnActive.value
            )
        }
    }

    fun toggleFirewall(enabled: Boolean) {
        firewallManager.setFirewallEnabled(enabled)
        _uiState.value = _uiState.value.copy(isFirewallEnabled = enabled)
        restartVpnIfNeeded()
    }

    fun toggleWifiBlock(packageName: String) {
        firewallManager.toggleWifiBlock(packageName)
        refreshApp(packageName)
        markNeedsRestart()
    }

    fun toggleMobileBlock(packageName: String) {
        firewallManager.toggleMobileBlock(packageName)
        refreshApp(packageName)
        markNeedsRestart()
    }

    fun blockAll(packageName: String) {
        firewallManager.blockAppCompletely(packageName)
        refreshApp(packageName)
        markNeedsRestart()
    }

    fun unblockAll(packageName: String) {
        firewallManager.unblockAppCompletely(packageName)
        refreshApp(packageName)
        markNeedsRestart()
    }

    fun applyChanges() {
        restartVpnIfNeeded()
        _uiState.value = _uiState.value.copy(needsVpnRestart = false)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredApps(): List<FirewallManager.AppFirewallRule> {
        val query = _uiState.value.searchQuery.lowercase()
        if (query.isBlank()) return _uiState.value.apps
        return _uiState.value.apps.filter {
            it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }

    private fun markNeedsRestart() {
        if (_uiState.value.vpnActive) {
            _uiState.value = _uiState.value.copy(needsVpnRestart = true)
        }
    }

    private fun restartVpnIfNeeded() {
        val context = getApplication<Application>()
        if (VpnControlManager.isVpnActive.value) {
            viewModelScope.launch {
                VpnControlManager.stopVpn(context)
                delay(500)
                VpnControlManager.startVpn(context)
                _uiState.value = _uiState.value.copy(needsVpnRestart = false)
            }
        }
    }

    private fun refreshApp(packageName: String) {
        val apps = _uiState.value.apps.map { app ->
            if (app.packageName == packageName) {
                app.copy(
                    blockedWifi = firewallManager.isAppBlockedWifi(packageName),
                    blockedMobile = firewallManager.isAppBlockedMobile(packageName)
                )
            } else app
        }.sortedWith(
            compareByDescending<FirewallManager.AppFirewallRule> { it.blockedWifi || it.blockedMobile }
                .thenBy { it.appName.lowercase() }
        )
        _uiState.value = _uiState.value.copy(apps = apps)
    }
}
