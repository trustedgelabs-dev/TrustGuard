package com.trustedgelabs.trustguard.ui.screens.optimization

import android.text.format.Formatter
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.model.CleanCategory
import com.trustedgelabs.trustguard.data.model.CleanableFile
import com.trustedgelabs.trustguard.data.model.DuplicateGroup
import com.trustedgelabs.trustguard.data.model.JunkScanResult
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.DarkCardHighlight
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit,
    viewModel: OptimizationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val cleanedMsg = stringResource(R.string.optimize_cleaned)
    LaunchedEffect(state.cleanResultMessage) {
        state.cleanResultMessage?.let {
            snackbarHostState.showSnackbar("$it $cleanedMsg")
            viewModel.clearResultMessage()
        }
    }

    // Premium dialog
    if (state.showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPremiumDialog() },
            title = { Text(stringResource(R.string.premium_required), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.optimize_premium_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissPremiumDialog()
                        onNavigateToPremium()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrustGreen)
                ) { Text(stringResource(R.string.upgrade_premium)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPremiumDialog() }) {
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
                title = { Text(stringResource(R.string.optimize_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TrustGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )

            // Tab row
            TabRow(activeTab = state.activeTab, onTabSelect = { viewModel.setTab(it) })

            // Content
            when (state.activeTab) {
                OptimizationTab.JUNK -> JunkTabContent(state, viewModel, context)
                OptimizationTab.DUPLICATES -> DuplicateTabContent(state, viewModel, context)
                OptimizationTab.LARGE_FILES -> LargeFilesTabContent(state, viewModel, context)
                OptimizationTab.APP_CACHE -> AppCacheTabContent(state, viewModel, context)
                OptimizationTab.EMAIL -> EmailTabContent(state, viewModel, context)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(snackbarData = data, containerColor = TrustGreen, contentColor = DarkBackground)
        }
    }
}

@Composable
private fun TabRow(activeTab: OptimizationTab, onTabSelect: (OptimizationTab) -> Unit) {
    val tabs = listOf(
        OptimizationTab.JUNK to Triple(Icons.Default.DeleteSweep, R.string.optimize_tab_junk, Color(0xFFFF9100)),
        OptimizationTab.DUPLICATES to Triple(Icons.Default.ContentCopy, R.string.optimize_tab_duplicates, Color(0xFF448AFF)),
        OptimizationTab.LARGE_FILES to Triple(Icons.Default.SdStorage, R.string.optimize_tab_large, Color(0xFFAB47BC)),
        OptimizationTab.APP_CACHE to Triple(Icons.Default.PhoneAndroid, R.string.optimize_tab_cache, TrustGreen),
        OptimizationTab.EMAIL to Triple(Icons.Default.Email, R.string.optimize_tab_email, Color(0xFFFF1744)),
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(tabs.size) { index ->
            val (tab, triple) = tabs[index]
            val (icon, labelRes, color) = triple
            FilterChip(
                selected = activeTab == tab,
                onClick = { onTabSelect(tab) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(labelRes), fontSize = 12.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color,
                    selectedLeadingIconColor = color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = TextSecondary.copy(alpha = 0.3f),
                    selectedBorderColor = color.copy(alpha = 0.5f),
                    enabled = true,
                    selected = activeTab == tab
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// JUNK TAB
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun JunkTabContent(state: OptimizationUiState, viewModel: OptimizationViewModel, context: android.content.Context) {
    when {
        state.isJunkScanning -> ScanningIndicator(state.junkScanProgress)
        !state.junkScanComplete -> ScanPrompt(
            icon = Icons.Default.DeleteSweep,
            title = stringResource(R.string.optimize_junk_title),
            description = stringResource(R.string.optimize_junk_desc),
            color = Color(0xFFFF9100),
            onScan = { viewModel.scanJunk() }
        )
        state.junkResults.isEmpty() -> EmptyResult(stringResource(R.string.optimize_no_junk))
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Summary
                item {
                    ScanSummaryCard(
                        title = stringResource(R.string.optimize_junk_found),
                        size = Formatter.formatShortFileSize(context, state.totalJunkSize),
                        count = state.junkResults.sumOf { it.files.size },
                        color = Color(0xFFFF9100),
                        isPremium = state.isPremium,
                        isCleaning = state.isCleaning,
                        onClean = { viewModel.cleanJunk() }
                    )
                }

                // Categories
                items(state.junkResults) { result ->
                    JunkCategoryCard(result, context)
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun JunkCategoryCard(result: JunkScanResult, context: android.content.Context) {
    val (icon, label, color) = when (result.category) {
        CleanCategory.TEMP_FILES -> Triple(Icons.Default.DeleteSweep, R.string.optimize_cat_temp, Color(0xFFFF9100))
        CleanCategory.APK_FILES -> Triple(Icons.Default.PhoneAndroid, R.string.optimize_cat_apk, Color(0xFF448AFF))
        CleanCategory.THUMBNAILS -> Triple(Icons.Default.Image, R.string.optimize_cat_thumbs, Color(0xFFAB47BC))
        CleanCategory.SYSTEM_CACHE -> Triple(Icons.Default.CleaningServices, R.string.optimize_cat_system, TrustGreen)
        CleanCategory.EMPTY_FOLDERS -> Triple(Icons.Default.FolderOpen, R.string.optimize_cat_empty, TextSecondary)
        else -> Triple(Icons.Default.DeleteSweep, R.string.optimize_cat_temp, Color(0xFFFF9100))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${result.files.size} ${stringResource(R.string.optimize_files)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = Formatter.formatShortFileSize(context, result.totalSize),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// DUPLICATES TAB
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DuplicateTabContent(state: OptimizationUiState, viewModel: OptimizationViewModel, context: android.content.Context) {
    when {
        state.isDuplicateScanning -> ScanningIndicator(state.duplicateScanProgress)
        !state.duplicateScanComplete -> ScanPrompt(
            icon = Icons.Default.ContentCopy,
            title = stringResource(R.string.optimize_dup_title),
            description = stringResource(R.string.optimize_dup_desc),
            color = Color(0xFF448AFF),
            onScan = { viewModel.scanDuplicates() }
        )
        state.duplicateGroups.isEmpty() -> EmptyResult(stringResource(R.string.optimize_no_duplicates))
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ScanSummaryCard(
                        title = stringResource(R.string.optimize_dup_found, state.duplicateGroups.size),
                        size = Formatter.formatShortFileSize(context, state.totalDuplicateWaste),
                        count = state.duplicateGroups.sumOf { it.files.size },
                        color = Color(0xFF448AFF),
                        isPremium = state.isPremium,
                        isCleaning = state.isCleaning,
                        onClean = { viewModel.cleanDuplicates() },
                        cleanLabel = stringResource(R.string.optimize_delete_duplicates)
                    )
                }

                items(state.duplicateGroups) { group ->
                    DuplicateGroupCard(group, context)
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(group: DuplicateGroup, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(12.dp)
    ) {
        Text(
            text = "${group.files.size} ${stringResource(R.string.optimize_copies)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF448AFF)
        )
        Spacer(Modifier.height(8.dp))

        // Show thumbnails of duplicates
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(group.files) { file ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardHighlight)
                    ) {
                        if (file.uri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(file.uri)
                                    .crossfade(true)
                                    .size(144)
                                    .build(),
                                contentDescription = file.name,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(
                                Icons.Default.Image, null,
                                tint = TextSecondary,
                                modifier = Modifier.align(Alignment.Center).size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = Formatter.formatShortFileSize(context, file.size),
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = group.files.first().name,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// LARGE FILES TAB
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun LargeFilesTabContent(state: OptimizationUiState, viewModel: OptimizationViewModel, context: android.content.Context) {
    when {
        state.isLargeFileScanning -> ScanningIndicator(state.largeScanProgress)
        !state.largeFileScanComplete -> ScanPrompt(
            icon = Icons.Default.SdStorage,
            title = stringResource(R.string.optimize_large_title),
            description = stringResource(R.string.optimize_large_desc),
            color = Color(0xFFAB47BC),
            onScan = { viewModel.scanLargeFiles() }
        )
        state.largeFiles.isEmpty() -> EmptyResult(stringResource(R.string.optimize_no_large))
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ScanSummaryCard(
                        title = stringResource(R.string.optimize_large_found),
                        size = Formatter.formatShortFileSize(context, state.totalLargeFileSize),
                        count = state.largeFiles.size,
                        color = Color(0xFFAB47BC),
                        isPremium = state.isPremium,
                        isCleaning = state.isCleaning,
                        onClean = { viewModel.deleteLargeFiles(state.largeFiles) },
                        cleanLabel = stringResource(R.string.optimize_delete_selected)
                    )
                }

                items(state.largeFiles) { file ->
                    LargeFileCard(file, context)
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun LargeFileCard(file: CleanableFile, context: android.content.Context) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFAB47BC).copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.SdStorage, null, tint = Color(0xFFAB47BC), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.path.substringAfterLast("/storage/emulated/0/").substringBeforeLast("/"),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = Formatter.formatShortFileSize(context, file.size),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFAB47BC)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// APP CACHE TAB
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun AppCacheTabContent(state: OptimizationUiState, viewModel: OptimizationViewModel, context: android.content.Context) {
    when {
        state.isAppCacheScanning -> ScanningIndicator(state.appCacheScanProgress)
        !state.appCacheScanComplete -> ScanPrompt(
            icon = Icons.Default.PhoneAndroid,
            title = stringResource(R.string.optimize_cache_title),
            description = stringResource(R.string.optimize_cache_desc),
            color = TrustGreen,
            onScan = { viewModel.scanAppCaches() }
        )
        state.appCaches.isEmpty() -> EmptyResult(stringResource(R.string.optimize_no_cache))
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(TrustGreen.copy(alpha = 0.1f))
                            .border(1.dp, TrustGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = Formatter.formatShortFileSize(context, state.totalAppCacheSize),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TrustGreen
                            )
                            Text(
                                text = stringResource(R.string.optimize_cache_total, state.appCaches.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.optimize_cache_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                items(state.appCaches) { cache ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TrustGreen.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.PhoneAndroid, null, tint = TrustGreen, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cache.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = cache.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = Formatter.formatShortFileSize(context, cache.cacheSize),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TrustGreen
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// EMAIL TAB
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EmailTabContent(state: OptimizationUiState, viewModel: OptimizationViewModel, context: android.content.Context) {
    when {
        state.isEmailScanning -> ScanningIndicator(state.emailScanProgress)
        !state.emailScanComplete -> ScanPrompt(
            icon = Icons.Default.Email,
            title = stringResource(R.string.optimize_email_title),
            description = stringResource(R.string.optimize_email_desc),
            color = Color(0xFFFF1744),
            onScan = { viewModel.scanEmailCaches() }
        )
        state.emailCacheFiles.isEmpty() -> EmptyResult(stringResource(R.string.optimize_no_email))
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ScanSummaryCard(
                        title = stringResource(R.string.optimize_email_found),
                        size = Formatter.formatShortFileSize(context, state.totalEmailCacheSize),
                        count = state.emailCacheFiles.size,
                        color = Color(0xFFFF1744),
                        isPremium = state.isPremium,
                        isCleaning = state.isCleaning,
                        onClean = { viewModel.cleanEmailCaches() },
                        cleanLabel = stringResource(R.string.optimize_clean_email)
                    )
                }

                items(state.emailCacheFiles) { file ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF1744).copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Email, null, tint = Color(0xFFFF1744), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = Formatter.formatShortFileSize(context, file.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF1744)
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ScanPrompt(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onScan: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.1f))
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onScan,
            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.optimize_scan_now), color = color)
        }
    }
}

@Composable
private fun ScanningIndicator(progress: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = TrustGreen, modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.optimize_scanning),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (progress.isNotEmpty() && progress != "Complete") {
            Spacer(Modifier.height(8.dp))
            Text(text = progress, style = MaterialTheme.typography.bodyMedium, color = TrustGreen)
        }
    }
}

@Composable
private fun EmptyResult(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            Icons.Default.CleaningServices, null,
            tint = TrustGreen.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ScanSummaryCard(
    title: String,
    size: String,
    count: Int,
    color: Color,
    isPremium: Boolean,
    isCleaning: Boolean,
    onClean: () -> Unit,
    cleanLabel: String = stringResource(R.string.optimize_clean_all)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = size, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClean,
            enabled = !isCleaning,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCleaning) {
                CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            } else if (!isPremium) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = if (!isPremium) stringResource(R.string.optimize_premium_clean) else cleanLabel,
                fontWeight = FontWeight.Bold,
                color = DarkBackground
            )
        }
    }
}
