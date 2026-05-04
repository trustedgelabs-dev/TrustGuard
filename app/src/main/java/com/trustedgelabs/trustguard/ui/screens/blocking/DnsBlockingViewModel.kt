package com.trustedgelabs.trustguard.ui.screens.blocking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.data.datasource.BlocklistDataSource
import com.trustedgelabs.trustguard.data.model.BlockingStats
import com.trustedgelabs.trustguard.data.model.BlocklistInfo
import com.trustedgelabs.trustguard.data.repository.BlockingStatsRepository
import com.trustedgelabs.trustguard.data.repository.BlocklistRepositoryImpl
import com.trustedgelabs.trustguard.service.AdNotificationInfo
import com.trustedgelabs.trustguard.service.AppFilterInfo
import com.trustedgelabs.trustguard.service.AppFilterManager
import com.trustedgelabs.trustguard.service.NotificationAdDetector
import com.trustedgelabs.trustguard.service.VpnControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DnsBlockingUiState(
    val vpnActive: Boolean = false,
    val blockedToday: Int = 0,
    val queriesToday: Int = 0,
    val blockedAllTime: Int = 0,
    val blocklists: List<BlocklistInfo> = emptyList(),
    val topBlockedDomains: List<Pair<String, Int>> = emptyList(),
    val installedApps: List<AppFilterInfo> = emptyList(),
    val excludedAppCount: Int = 0,
    val showAppFilter: Boolean = false,
    val appFilterQuery: String = "",
    val needsVpnRestart: Boolean = false,
    val adNotifications: List<AdNotificationInfo> = emptyList(),
    val notificationServiceActive: Boolean = false
)

class DnsBlockingViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource = BlocklistDataSource(application)
    private val blocklistRepository = BlocklistRepositoryImpl(application, dataSource)
    private val statsRepository = BlockingStatsRepository(application)
    private val appFilterManager = AppFilterManager(application)

    private val _uiState = MutableStateFlow(DnsBlockingUiState())
    val uiState: StateFlow<DnsBlockingUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeVpnState()
        observeStats()
        observeNotificationAds()
        loadAppFilterInfo()
    }

    private fun loadData() {
        viewModelScope.launch {
            val lists = blocklistRepository.getBlocklists()
            _uiState.value = _uiState.value.copy(blocklists = lists)
        }
    }

    private fun observeVpnState() {
        viewModelScope.launch {
            VpnControlManager.isVpnActive.collect { active ->
                _uiState.value = _uiState.value.copy(vpnActive = active)
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            statsRepository.stats.collect { stats ->
                _uiState.value = _uiState.value.copy(
                    blockedToday = stats.totalBlockedToday,
                    queriesToday = stats.totalQueriesToday,
                    blockedAllTime = stats.totalBlockedAllTime,
                    topBlockedDomains = stats.topBlockedDomains
                )
            }
        }
        viewModelScope.launch {
            VpnControlManager.blockingStats.collect { stats ->
                _uiState.value = _uiState.value.copy(
                    blockedToday = stats.totalBlockedToday,
                    queriesToday = stats.totalQueriesToday,
                    blockedAllTime = stats.totalBlockedAllTime,
                    topBlockedDomains = stats.topBlockedDomains
                )
            }
        }
    }

    private fun observeNotificationAds() {
        viewModelScope.launch {
            NotificationAdDetector.adNotifications.collect { ads ->
                _uiState.value = _uiState.value.copy(adNotifications = ads)
            }
        }
        viewModelScope.launch {
            NotificationAdDetector.isServiceActive.collect { active ->
                _uiState.value = _uiState.value.copy(notificationServiceActive = active)
            }
        }
    }

    private fun loadAppFilterInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appFilterManager.getInstalledUserApps()
            val excludedCount = appFilterManager.getExcludedApps().size
            _uiState.value = _uiState.value.copy(
                installedApps = apps,
                excludedAppCount = excludedCount
            )
        }
    }

    fun toggleBlocklist(id: String, enabled: Boolean) {
        blocklistRepository.toggleBlocklist(id, enabled)
        loadData()
    }

    fun toggleVpn(context: android.content.Context) {
        if (VpnControlManager.isVpnActive.value) {
            VpnControlManager.stopVpn(context)
        } else {
            VpnControlManager.startVpn(context)
        }
        _uiState.value = _uiState.value.copy(needsVpnRestart = false)
    }

    fun toggleShowAppFilter() {
        _uiState.value = _uiState.value.copy(showAppFilter = !_uiState.value.showAppFilter)
    }

    fun setAppFilterQuery(query: String) {
        _uiState.value = _uiState.value.copy(appFilterQuery = query)
    }

    fun toggleAppExclusion(packageName: String) {
        appFilterManager.toggleAppExclusion(packageName)
        loadAppFilterInfo()
        if (_uiState.value.vpnActive) {
            _uiState.value = _uiState.value.copy(needsVpnRestart = true)
        }
    }
}
