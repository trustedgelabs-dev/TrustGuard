package com.trustedgelabs.trustguard.ui.screens.recovery

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.model.ScanMode
import com.trustedgelabs.trustguard.ui.components.RecoverableFileCard
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.DarkCardHighlight
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecoveryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission launchers
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) {
            viewModel.onPermissionGranted()
        }
        viewModel.scanFiles()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.onPermissionGranted()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            allFilesAccessLauncher.launch(intent)
        } else {
            viewModel.scanFiles()
        }
    }

    fun requestPermissionsAndScan(mode: ScanMode) {
        viewModel.selectScanMode(mode)

        // Android 11+ needs MANAGE_EXTERNAL_STORAGE first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Go to All Files Access settings directly
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                allFilesAccessLauncher.launch(intent)
                return
            }
            // Already have full access, also request media permissions and scan
            viewModel.onPermissionGranted()
            viewModel.scanFiles(mode)
        } else {
            // Android 10 and below
            val permissions = getRequiredPermissions()
            permissionLauncher.launch(permissions)
        }
    }

    // Show recovery result snackbar
    val recoveryCount = state.recoveryResultMessage?.toIntOrNull() ?: 0
    val successMsg = stringResource(R.string.recovery_success, recoveryCount)
    LaunchedEffect(state.recoveryResultMessage) {
        state.recoveryResultMessage?.let {
            snackbarHostState.showSnackbar(message = successMsg)
            viewModel.clearResultMessage()
        }
    }

    // Free limit dialog
    if (state.showFreeLimitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFreeLimitDialog() },
            title = {
                Text(
                    text = stringResource(R.string.free_limit_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.free_limit_message))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissFreeLimitDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = TrustGreen)
                ) {
                    Text(stringResource(R.string.upgrade_premium))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissFreeLimitDialog() }) {
                    Text(stringResource(R.string.back))
                }
            },
            containerColor = DarkCard
        )
    }

    // Premium required dialog
    if (state.showPremiumRequiredDialog) {
        val message = when (state.premiumDialogMessage) {
            "premium_required_for_root" -> stringResource(R.string.premium_required_for_root)
            "premium_required_for_recovery" -> stringResource(R.string.premium_required_for_recovery)
            else -> stringResource(R.string.premium_required)
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissPremiumRequiredDialog() },
            title = {
                Text(
                    text = stringResource(R.string.premium_required),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = message)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissPremiumRequiredDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = TrustGreen)
                ) {
                    Text(stringResource(R.string.upgrade_premium))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPremiumRequiredDialog() }) {
                    Text(stringResource(R.string.back))
                }
            },
            containerColor = DarkCard
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.recovery_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = TrustGreen
                        )
                    }
                },
                actions = {
                    if (state.scanComplete && state.filteredFiles.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (state.selectedFiles.size == state.filteredFiles.size) {
                                    viewModel.deselectAll()
                                } else {
                                    viewModel.selectAll()
                                }
                            }
                        ) {
                            Text(
                                text = if (state.selectedFiles.size == state.filteredFiles.size) {
                                    stringResource(R.string.deselect_all)
                                } else {
                                    stringResource(R.string.select_all)
                                },
                                color = TrustGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )

            when {
                // Initial state - no scan yet
                !state.scanComplete && !state.isScanning -> {
                    ScanModeSelectionContent(
                        freeRecoveriesLeft = state.freeRecoveriesLeft,
                        isDeviceRooted = state.isDeviceRooted,
                        isPremium = state.isPremium,
                        onScanClick = { mode -> requestPermissionsAndScan(mode) }
                    )
                }

                // Scanning
                state.isScanning -> {
                    ScanningContent(
                        scanMode = state.selectedScanMode,
                        currentStrategy = state.currentScanStrategy,
                        filesFound = state.filesFoundDuringScan
                    )
                }

                // Scan complete
                state.scanComplete -> {
                    // Scan stats
                    if (state.allFiles.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.scan_stats,
                                state.allFiles.size,
                                state.scanSourceCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TrustGreen,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        // Post-scan quality warning
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFD600).copy(alpha = 0.06f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD600).copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.recovery_quality_note),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Filter chips
                    FileTypeFilterRow(
                        activeFilter = state.activeFilter,
                        onFilterChange = { viewModel.setFilter(it) }
                    )

                    if (state.filteredFiles.isEmpty()) {
                        NoFilesContent(
                            onRescan = { requestPermissionsAndScan(state.selectedScanMode) }
                        )
                    } else {
                        // File count + free recoveries info
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.files_found, state.filteredFiles.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (!state.isPremium) {
                                Text(
                                    text = stringResource(
                                        R.string.free_recoveries_left,
                                        state.freeRecoveriesLeft
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.freeRecoveriesLeft > 0) TrustGreen else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // File list
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(
                                items = state.filteredFiles,
                                key = { it.id }
                            ) { file ->
                                RecoverableFileCard(
                                    file = file,
                                    isSelected = file.id in state.selectedFiles,
                                    onToggleSelect = { viewModel.toggleFileSelection(file.id) }
                                )
                            }
                        }

                        // Bottom action bar
                        AnimatedVisibility(
                            visible = state.selectedFiles.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            RecoverBottomBar(
                                selectedCount = state.selectedFiles.size,
                                isRecovering = state.isRecovering,
                                onRecoverClick = { viewModel.recoverSelected() }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = TrustGreen,
                contentColor = DarkBackground
            )
        }
    }
}

@Composable
private fun ScanModeSelectionContent(
    freeRecoveriesLeft: Int,
    isDeviceRooted: Boolean,
    isPremium: Boolean,
    onScanClick: (ScanMode) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Icon(
                imageVector = Icons.Default.RestoreFromTrash,
                contentDescription = null,
                tint = TrustGreen.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(64.dp)
                    .padding(top = 16.dp)
            )
        }

        item {
            Text(
                text = stringResource(R.string.recovery_description),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (!isPremium) {
            item {
                Text(
                    text = stringResource(R.string.free_recoveries_left, freeRecoveriesLeft),
                    style = MaterialTheme.typography.bodySmall,
                    color = TrustGreen
                )
            }
        }

        // Android sandbox limitation warning
        item {
            val warningText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stringResource(R.string.recovery_warning_android11)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stringResource(R.string.recovery_warning_android10)
            } else {
                null
            }

            if (warningText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD600).copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFFFD600).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = warningText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Quick Scan Card
        item {
            ScanModeCard(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.quick_scan),
                description = stringResource(R.string.quick_scan_desc),
                accentColor = TrustGreen,
                badge = null,
                enabled = true,
                onClick = { onScanClick(ScanMode.QUICK) }
            )
        }

        // Deep Scan Card
        item {
            ScanModeCard(
                icon = Icons.AutoMirrored.Filled.ManageSearch,
                title = stringResource(R.string.deep_scan),
                description = stringResource(R.string.deep_scan_desc),
                accentColor = Color(0xFF448AFF),
                badge = if (!isPremium) stringResource(R.string.premium_required_for_recovery) else null,
                enabled = true,
                onClick = { onScanClick(ScanMode.DEEP) }
            )
        }

        // Root Scan Card
        item {
            ScanModeCard(
                icon = Icons.Default.AdminPanelSettings,
                title = stringResource(R.string.root_scan),
                description = stringResource(R.string.root_scan_desc),
                accentColor = Color(0xFFFF1744),
                badge = if (!isDeviceRooted) stringResource(R.string.root_not_available)
                else if (!isPremium) stringResource(R.string.premium_required_for_root)
                else null,
                enabled = isDeviceRooted,
                onClick = { onScanClick(ScanMode.ROOT) }
            )
        }

        // Root status indicator
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                val rootColor = if (isDeviceRooted) TrustGreen else TextSecondary.copy(alpha = 0.5f)
                Icon(
                    imageVector = if (isDeviceRooted) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                    contentDescription = null,
                    tint = rootColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (isDeviceRooted) R.string.device_rooted else R.string.device_not_rooted
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = rootColor
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ScanModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    badge: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f * alpha),
                shape = RoundedCornerShape(16.dp)
            )
            .background(DarkCard)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f * alpha))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = alpha),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = alpha)
                )
            }

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.7f * alpha),
                modifier = Modifier.size(24.dp)
            )
        }

        if (badge != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ScanningContent(
    scanMode: ScanMode,
    currentStrategy: String,
    filesFound: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = TrustGreen,
            modifier = Modifier.size(56.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.scanning_files),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Current strategy being scanned
        if (currentStrategy.isNotEmpty() && currentStrategy != "Complete") {
            Text(
                text = currentStrategy,
                style = MaterialTheme.typography.bodyMedium,
                color = TrustGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Scan mode description
        Text(
            text = when (scanMode) {
                ScanMode.QUICK -> stringResource(R.string.scanning_trashed)
                ScanMode.DEEP -> stringResource(R.string.scanning_caches)
                ScanMode.ROOT -> stringResource(R.string.scanning_root)
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        // Files found counter
        if (filesFound > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(TrustGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$filesFound ${stringResource(R.string.files_found_count)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TrustGreen
                )
            }
        }
    }
}

@Composable
private fun NoFilesContent(
    onRescan: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_files_found),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRescan,
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TrustGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.rescan),
                color = TrustGreen
            )
        }
    }
}

@Composable
private fun FileTypeFilterRow(
    activeFilter: FileTypeFilter,
    onFilterChange: (FileTypeFilter) -> Unit
) {
    val filters = listOf(
        FileTypeFilter.ALL to stringResource(R.string.all_files),
        FileTypeFilter.PHOTO to stringResource(R.string.photos),
        FileTypeFilter.VIDEO to stringResource(R.string.videos),
        FileTypeFilter.AUDIO to stringResource(R.string.audio),
        FileTypeFilter.DOCUMENT to stringResource(R.string.documents)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(filters.size) { index ->
            val (filter, label) = filters[index]
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TrustGreen.copy(alpha = 0.2f),
                    selectedLabelColor = TrustGreen
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = TextSecondary.copy(alpha = 0.3f),
                    selectedBorderColor = TrustGreen.copy(alpha = 0.5f),
                    enabled = true,
                    selected = activeFilter == filter
                )
            )
        }
    }
}

@Composable
private fun RecoverBottomBar(
    selectedCount: Int,
    isRecovering: Boolean,
    onRecoverClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Button(
            onClick = onRecoverClick,
            enabled = !isRecovering,
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isRecovering) {
                CircularProgressIndicator(
                    color = DarkBackground,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.recovering),
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RestoreFromTrash,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.recover_selected, selectedCount),
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
        }
    }
}

private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
