package com.trustedgelabs.trustguard.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.data.datasource.PackageManagerDataSource
import com.trustedgelabs.trustguard.data.model.AppInfo
import com.trustedgelabs.trustguard.data.repository.AppRepositoryImpl
import com.trustedgelabs.trustguard.domain.usecase.AnalyzePermissionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val appInfo: AppInfo? = null
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource = PackageManagerDataSource(application)
    private val repository = AppRepositoryImpl(dataSource)
    private val analyzeUseCase = AnalyzePermissionsUseCase(repository)

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadApp(packageName: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            val app = analyzeUseCase(packageName)
            _uiState.value = DetailUiState(isLoading = false, appInfo = app)
        }
    }
}
