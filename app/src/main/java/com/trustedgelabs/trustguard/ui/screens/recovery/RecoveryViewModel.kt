package com.trustedgelabs.trustguard.ui.screens.recovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.data.model.FileType
import com.trustedgelabs.trustguard.data.model.RecoverableFile
import com.trustedgelabs.trustguard.data.model.RecoverySource
import com.trustedgelabs.trustguard.data.model.ScanMode
import com.trustedgelabs.trustguard.data.repository.RecoveryRepositoryImpl
import com.trustedgelabs.trustguard.util.RootDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecoveryUiState(
    val isScanning: Boolean = false,
    val scanComplete: Boolean = false,
    val isRecovering: Boolean = false,
    val allFiles: List<RecoverableFile> = emptyList(),
    val filteredFiles: List<RecoverableFile> = emptyList(),
    val selectedFiles: Set<Long> = emptySet(),
    val activeFilter: FileTypeFilter = FileTypeFilter.ALL,
    val freeRecoveriesLeft: Int = 3,
    val showFreeLimitDialog: Boolean = false,
    val showPremiumRequiredDialog: Boolean = false,
    val premiumDialogMessage: String = "",
    val recoveryResultMessage: String? = null,
    val hasStoragePermission: Boolean = false,
    val selectedScanMode: ScanMode = ScanMode.QUICK,
    val isDeviceRooted: Boolean = false,
    val isPremium: Boolean = false,
    val scanStatusMessage: String? = null,
    val scanSourceCount: Int = 0,
    val currentScanStrategy: String = "",
    val filesFoundDuringScan: Int = 0
)

enum class FileTypeFilter {
    ALL, PHOTO, VIDEO, AUDIO, DOCUMENT
}

class RecoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecoveryRepositoryImpl(application)

    private val _uiState = MutableStateFlow(RecoveryUiState(
        freeRecoveriesLeft = repository.getFreeRecoveriesLeft(),
        isPremium = repository.isPremium()
    ))
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    init {
        // Check root status on init
        viewModelScope.launch {
            val rooted = withContext(Dispatchers.IO) { RootDetector.isDeviceRooted() }
            _uiState.value = _uiState.value.copy(isDeviceRooted = rooted)
        }
    }

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasStoragePermission = true)
    }

    fun selectScanMode(mode: ScanMode) {
        _uiState.value = _uiState.value.copy(selectedScanMode = mode)
    }

    fun scanFiles(mode: ScanMode = _uiState.value.selectedScanMode) {
        // Root scan requires premium
        if (mode == ScanMode.ROOT && !repository.isPremium()) {
            _uiState.value = _uiState.value.copy(
                showPremiumRequiredDialog = true,
                premiumDialogMessage = "premium_required_for_root"
            )
            return
        }

        // Root scan requires rooted device
        if (mode == ScanMode.ROOT && !_uiState.value.isDeviceRooted) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanComplete = false,
                allFiles = emptyList(),
                filteredFiles = emptyList(),
                selectedFiles = emptySet(),
                recoveryResultMessage = null,
                selectedScanMode = mode,
                currentScanStrategy = "",
                filesFoundDuringScan = 0
            )

            try {
                // Set up progress callback
                repository.dataSource.onScanProgress = { strategy, filesFound ->
                    _uiState.value = _uiState.value.copy(
                        currentScanStrategy = strategy,
                        filesFoundDuringScan = filesFound
                    )
                }

                val files = repository.scanFiles(mode)
                val sourceCount = files.map { it.source }.toSet().size
                repository.dataSource.onScanProgress = null

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanComplete = true,
                    allFiles = files,
                    filteredFiles = files,
                    scanSourceCount = sourceCount
                )
            } catch (_: Exception) {
                repository.dataSource.onScanProgress = null
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanComplete = true
                )
            }
        }
    }

    fun setFilter(filter: FileTypeFilter) {
        val allFiles = _uiState.value.allFiles
        val filtered = when (filter) {
            FileTypeFilter.ALL -> allFiles
            FileTypeFilter.PHOTO -> allFiles.filter { it.type == FileType.PHOTO }
            FileTypeFilter.VIDEO -> allFiles.filter { it.type == FileType.VIDEO }
            FileTypeFilter.AUDIO -> allFiles.filter { it.type == FileType.AUDIO }
            FileTypeFilter.DOCUMENT -> allFiles.filter { it.type == FileType.DOCUMENT }
        }
        _uiState.value = _uiState.value.copy(
            activeFilter = filter,
            filteredFiles = filtered,
            selectedFiles = _uiState.value.selectedFiles.intersect(filtered.map { it.id }.toSet())
        )
    }

    fun toggleFileSelection(fileId: Long) {
        val current = _uiState.value.selectedFiles.toMutableSet()
        if (current.contains(fileId)) {
            current.remove(fileId)
        } else {
            current.add(fileId)
        }
        _uiState.value = _uiState.value.copy(selectedFiles = current)
    }

    fun selectAll() {
        val allIds = _uiState.value.filteredFiles.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedFiles = allIds)
    }

    fun deselectAll() {
        _uiState.value = _uiState.value.copy(selectedFiles = emptySet())
    }

    fun recoverSelected() {
        val state = _uiState.value
        val selectedIds = state.selectedFiles
        val filesToRecover = state.allFiles.filter { it.id in selectedIds }

        if (filesToRecover.isEmpty()) return

        // Check if any selected files are from deep/root scan and require premium
        val hasDeepFiles = filesToRecover.any {
            it.source in listOf(RecoverySource.THUMBNAIL, RecoverySource.CACHE, RecoverySource.HIDDEN)
        }
        val hasRootFiles = filesToRecover.any { it.source == RecoverySource.ROOT_SCAN }

        if (hasRootFiles && !repository.isPremium()) {
            _uiState.value = state.copy(
                showPremiumRequiredDialog = true,
                premiumDialogMessage = "premium_required_for_root"
            )
            return
        }

        if (hasDeepFiles && !repository.isPremium()) {
            _uiState.value = state.copy(
                showPremiumRequiredDialog = true,
                premiumDialogMessage = "premium_required_for_recovery"
            )
            return
        }

        // Check free limit for trash files
        val freeLeft = repository.getFreeRecoveriesLeft()
        if (freeLeft <= 0 && !repository.isPremium()) {
            _uiState.value = state.copy(showFreeLimitDialog = true)
            return
        }

        val maxCanRecover = if (repository.isPremium()) filesToRecover.size
            else freeLeft.coerceAtMost(filesToRecover.size)
        val batch = filesToRecover.take(maxCanRecover)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRecovering = true, recoveryResultMessage = null)

            var successCount = 0
            for (file in batch) {
                val success = repository.recoverFile(file)
                if (success) {
                    successCount++
                    repository.useRecovery()
                }
            }

            // Remove recovered files from lists
            val recoveredIds = batch.take(successCount).map { it.id }.toSet()
            val newAllFiles = state.allFiles.filter { it.id !in recoveredIds }
            val newFreeLeft = repository.getFreeRecoveriesLeft()

            _uiState.value = _uiState.value.copy(
                isRecovering = false,
                allFiles = newAllFiles,
                filteredFiles = newAllFiles.let { files ->
                    when (state.activeFilter) {
                        FileTypeFilter.ALL -> files
                        FileTypeFilter.PHOTO -> files.filter { it.type == FileType.PHOTO }
                        FileTypeFilter.VIDEO -> files.filter { it.type == FileType.VIDEO }
                        FileTypeFilter.AUDIO -> files.filter { it.type == FileType.AUDIO }
                        FileTypeFilter.DOCUMENT -> files.filter { it.type == FileType.DOCUMENT }
                    }
                },
                selectedFiles = state.selectedFiles - recoveredIds,
                freeRecoveriesLeft = newFreeLeft,
                recoveryResultMessage = if (successCount > 0) "$successCount" else null,
                showFreeLimitDialog = !repository.isPremium() && newFreeLeft <= 0 && filesToRecover.size > maxCanRecover
            )
        }
    }

    fun dismissFreeLimitDialog() {
        _uiState.value = _uiState.value.copy(showFreeLimitDialog = false)
    }

    fun dismissPremiumRequiredDialog() {
        _uiState.value = _uiState.value.copy(showPremiumRequiredDialog = false)
    }

    fun clearResultMessage() {
        _uiState.value = _uiState.value.copy(recoveryResultMessage = null)
    }
}
