package com.trustedgelabs.trustguard.ui.screens.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trustedgelabs.trustguard.service.VaultFile
import com.trustedgelabs.trustguard.service.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val isSetup: Boolean = false,
    val isUnlocked: Boolean = false,
    val files: List<VaultFile> = emptyList(),
    val totalSizeBytes: Long = 0,
    val isEncrypting: Boolean = false,
    val error: String? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val vaultManager = VaultManager(application)
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var currentPassword: String = ""

    init {
        _uiState.value = _uiState.value.copy(isSetup = vaultManager.isVaultSetup())
    }

    fun setupVault(password: String): Boolean {
        val result = vaultManager.setupVault(password)
        if (result) {
            currentPassword = password
            _uiState.value = _uiState.value.copy(isSetup = true, isUnlocked = true)
            refreshFiles()
        }
        return result
    }

    fun unlock(password: String): Boolean {
        val valid = vaultManager.verifyPassword(password)
        if (valid) {
            currentPassword = password
            _uiState.value = _uiState.value.copy(isUnlocked = true, error = null)
            refreshFiles()
        } else {
            _uiState.value = _uiState.value.copy(error = "Wrong password")
        }
        return valid
    }

    fun lock() {
        currentPassword = ""
        _uiState.value = _uiState.value.copy(isUnlocked = false, files = emptyList())
    }

    fun encryptFile(uri: Uri, fileName: String, mimeType: String) {
        if (currentPassword.isEmpty()) return
        _uiState.value = _uiState.value.copy(isEncrypting = true)
        viewModelScope.launch(Dispatchers.IO) {
            vaultManager.encryptAndStore(uri, fileName, mimeType, currentPassword)
            refreshFiles()
            _uiState.value = _uiState.value.copy(isEncrypting = false)
        }
    }

    fun deleteFile(vaultFile: VaultFile) {
        vaultManager.deleteFile(vaultFile)
        refreshFiles()
    }

    private fun refreshFiles() {
        val files = vaultManager.getVaultFiles()
        _uiState.value = _uiState.value.copy(
            files = files,
            totalSizeBytes = vaultManager.getVaultSizeBytes()
        )
    }
}
