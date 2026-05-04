package com.trustedgelabs.trustguard.ui.screens.applist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.data.datasource.PackageManagerDataSource
import com.trustedgelabs.trustguard.data.model.AppInfo
import com.trustedgelabs.trustguard.data.model.RiskLevel
import com.trustedgelabs.trustguard.data.repository.AppRepositoryImpl
import com.trustedgelabs.trustguard.domain.usecase.ScanAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class FilterType {
    ALL, HIGH, MEDIUM, LOW
}

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val currentFilter: FilterType = FilterType.ALL,
    val showSystemApps: Boolean = false,
    val searchQuery: String = ""
)

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource = PackageManagerDataSource(application)
    private val repository = AppRepositoryImpl(dataSource)
    private val scanAppsUseCase = ScanAppsUseCase(repository)

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val apps = scanAppsUseCase(includeSystemApps = true)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                apps = apps,
                filteredApps = filterApps(apps, FilterType.ALL, false, "")
            )
        }
    }

    fun setFilter(filter: FilterType) {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentFilter = filter,
            filteredApps = filterApps(state.apps, filter, state.showSystemApps, state.searchQuery)
        )
    }

    fun toggleSystemApps() {
        val state = _uiState.value
        val newShowSystem = !state.showSystemApps
        _uiState.value = state.copy(
            showSystemApps = newShowSystem,
            filteredApps = filterApps(state.apps, state.currentFilter, newShowSystem, state.searchQuery)
        )
    }

    fun setSearchQuery(query: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            searchQuery = query,
            filteredApps = filterApps(state.apps, state.currentFilter, state.showSystemApps, query)
        )
    }

    private fun filterApps(
        apps: List<AppInfo>,
        filter: FilterType,
        showSystem: Boolean,
        query: String
    ): List<AppInfo> {
        return apps
            .filter { app ->
                if (!showSystem && app.isSystemApp) false else true
            }
            .filter { app ->
                when (filter) {
                    FilterType.ALL -> true
                    FilterType.HIGH -> app.riskLevel == RiskLevel.HIGH
                    FilterType.MEDIUM -> app.riskLevel == RiskLevel.MEDIUM
                    FilterType.LOW -> app.riskLevel == RiskLevel.LOW || app.riskLevel == RiskLevel.TRUSTED
                }
            }
            .filter { app ->
                if (query.isBlank()) true
                else app.appName.contains(query, ignoreCase = true)
            }
    }
}
