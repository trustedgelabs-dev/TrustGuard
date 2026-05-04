package com.trustedgelabs.trustguard.ui.screens.optimization

import android.app.Application
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.billing.BillingManager
import com.trustedgelabs.trustguard.data.datasource.OptimizationDataSource
import com.trustedgelabs.trustguard.data.model.CleanableFile
import com.trustedgelabs.trustguard.data.model.DuplicateGroup
import com.trustedgelabs.trustguard.data.model.JunkScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OptimizationUiState(
    // General
    val activeTab: OptimizationTab = OptimizationTab.JUNK,
    val isPremium: Boolean = false,
    val showPremiumDialog: Boolean = false,

    // Junk scan
    val isJunkScanning: Boolean = false,
    val junkScanComplete: Boolean = false,
    val junkResults: List<JunkScanResult> = emptyList(),
    val totalJunkSize: Long = 0,
    val junkScanProgress: String = "",

    // Duplicate photos
    val isDuplicateScanning: Boolean = false,
    val duplicateScanComplete: Boolean = false,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val totalDuplicateWaste: Long = 0,
    val duplicateScanProgress: String = "",

    // Large files
    val isLargeFileScanning: Boolean = false,
    val largeFileScanComplete: Boolean = false,
    val largeFiles: List<CleanableFile> = emptyList(),
    val totalLargeFileSize: Long = 0,
    val largeScanProgress: String = "",

    // App cache
    val isAppCacheScanning: Boolean = false,
    val appCacheScanComplete: Boolean = false,
    val appCaches: List<OptimizationDataSource.AppCacheInfo> = emptyList(),
    val totalAppCacheSize: Long = 0,
    val appCacheScanProgress: String = "",

    // Email cache
    val isEmailScanning: Boolean = false,
    val emailScanComplete: Boolean = false,
    val emailCacheFiles: List<CleanableFile> = emptyList(),
    val totalEmailCacheSize: Long = 0,
    val emailScanProgress: String = "",

    // Cleaning
    val isCleaning: Boolean = false,
    val cleanResultMessage: String? = null
)

enum class OptimizationTab {
    JUNK, DUPLICATES, LARGE_FILES, APP_CACHE, EMAIL
}

class OptimizationViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource = OptimizationDataSource(application)

    private val _uiState = MutableStateFlow(OptimizationUiState(
        isPremium = true
    ))
    val uiState: StateFlow<OptimizationUiState> = _uiState.asStateFlow()

    fun setTab(tab: OptimizationTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    // ═══ JUNK SCAN ═══
    fun scanJunk() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isJunkScanning = true, junkScanComplete = false, junkResults = emptyList()
            )
            dataSource.onScanProgress = { strategy, count ->
                _uiState.value = _uiState.value.copy(junkScanProgress = strategy)
            }
            try {
                val results = dataSource.scanSystemJunk()
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(
                    isJunkScanning = false,
                    junkScanComplete = true,
                    junkResults = results,
                    totalJunkSize = results.sumOf { it.totalSize }
                )
            } catch (_: Exception) {
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(isJunkScanning = false, junkScanComplete = true)
            }
        }
    }

    // ═══ DUPLICATE PHOTOS ═══
    fun scanDuplicates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDuplicateScanning = true, duplicateScanComplete = false, duplicateGroups = emptyList()
            )
            dataSource.onScanProgress = { strategy, _ ->
                _uiState.value = _uiState.value.copy(duplicateScanProgress = strategy)
            }
            try {
                val groups = dataSource.scanDuplicatePhotos()
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(
                    isDuplicateScanning = false,
                    duplicateScanComplete = true,
                    duplicateGroups = groups,
                    totalDuplicateWaste = groups.sumOf { it.wastedSize }
                )
            } catch (_: Exception) {
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(isDuplicateScanning = false, duplicateScanComplete = true)
            }
        }
    }

    // ═══ LARGE FILES ═══
    fun scanLargeFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLargeFileScanning = true, largeFileScanComplete = false, largeFiles = emptyList()
            )
            dataSource.onScanProgress = { strategy, _ ->
                _uiState.value = _uiState.value.copy(largeScanProgress = strategy)
            }
            try {
                val files = dataSource.scanLargeFiles()
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(
                    isLargeFileScanning = false,
                    largeFileScanComplete = true,
                    largeFiles = files,
                    totalLargeFileSize = files.sumOf { it.size }
                )
            } catch (_: Exception) {
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(isLargeFileScanning = false, largeFileScanComplete = true)
            }
        }
    }

    // ═══ APP CACHE ═══
    fun scanAppCaches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAppCacheScanning = true, appCacheScanComplete = false, appCaches = emptyList()
            )
            dataSource.onScanProgress = { strategy, _ ->
                _uiState.value = _uiState.value.copy(appCacheScanProgress = strategy)
            }
            try {
                val caches = dataSource.scanAppCaches()
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(
                    isAppCacheScanning = false,
                    appCacheScanComplete = true,
                    appCaches = caches,
                    totalAppCacheSize = caches.sumOf { it.cacheSize }
                )
            } catch (_: Exception) {
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(isAppCacheScanning = false, appCacheScanComplete = true)
            }
        }
    }

    // ═══ EMAIL CACHE ═══
    fun scanEmailCaches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isEmailScanning = true, emailScanComplete = false, emailCacheFiles = emptyList()
            )
            dataSource.onScanProgress = { strategy, _ ->
                _uiState.value = _uiState.value.copy(emailScanProgress = strategy)
            }
            try {
                val files = dataSource.scanEmailCaches()
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(
                    isEmailScanning = false,
                    emailScanComplete = true,
                    emailCacheFiles = files,
                    totalEmailCacheSize = files.sumOf { it.size }
                )
            } catch (_: Exception) {
                dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(isEmailScanning = false, emailScanComplete = true)
            }
        }
    }

    // ═══ CLEANING ACTIONS (Premium) ═══

    fun cleanJunk() {
        if (!checkPremium()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true)
            val allFiles = _uiState.value.junkResults.flatMap { it.files }
            val deleted = dataSource.deleteFiles(allFiles)
            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                cleanResultMessage = "$deleted",
                junkScanComplete = false,
                junkResults = emptyList(),
                totalJunkSize = 0
            )
        }
    }

    fun cleanDuplicates() {
        if (!checkPremium()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true)
            val deleted = dataSource.deleteDuplicates(_uiState.value.duplicateGroups)
            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                cleanResultMessage = "$deleted",
                duplicateScanComplete = false,
                duplicateGroups = emptyList(),
                totalDuplicateWaste = 0
            )
        }
    }

    fun deleteLargeFiles(files: List<CleanableFile>) {
        if (!checkPremium()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true)
            val deleted = dataSource.deleteFiles(files)
            val remaining = _uiState.value.largeFiles.filter { it !in files }
            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                cleanResultMessage = "$deleted",
                largeFiles = remaining,
                totalLargeFileSize = remaining.sumOf { it.size }
            )
        }
    }

    fun cleanEmailCaches() {
        if (!checkPremium()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true)
            val deleted = dataSource.deleteFiles(_uiState.value.emailCacheFiles)
            _uiState.value = _uiState.value.copy(
                isCleaning = false,
                cleanResultMessage = "$deleted",
                emailScanComplete = false,
                emailCacheFiles = emptyList(),
                totalEmailCacheSize = 0
            )
        }
    }

    private fun checkPremium(): Boolean {
        return true
    }

    fun dismissPremiumDialog() {
        _uiState.value = _uiState.value.copy(showPremiumDialog = false)
    }

    fun clearResultMessage() {
        _uiState.value = _uiState.value.copy(cleanResultMessage = null)
    }
}
