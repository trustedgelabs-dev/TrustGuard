package com.trustedgelabs.trustguard.ui.screens.appsecurity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.service.ApkAnalysisResult
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSecurityScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppSecurityViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.appsec_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TrustGreen
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = DarkBackground,
            contentColor = TrustGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                    color = TrustGreen
                )
            }
        ) {
            Tab(
                selected = state.selectedTab == 0,
                onClick = { viewModel.selectTab(0) },
                text = { Text(stringResource(R.string.appsec_tab_analysis)) }
            )
            Tab(
                selected = state.selectedTab == 1,
                onClick = { viewModel.selectTab(1) },
                text = { Text(stringResource(R.string.appsec_tab_device)) }
            )
        }

        if (state.isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TrustGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.appsec_scanning), color = TextSecondary)
                }
            }
        } else {
            when (state.selectedTab) {
                0 -> AnalysisTab(state)
                1 -> DeviceSecurityTab(state)
            }
        }
    }
}

@Composable
private fun AnalysisTab(state: AppSecurityUiState) {
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = if (state.highRiskCount > 0) RiskRed else TrustGreen,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.appsec_summary, state.analysisResults.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.highRiskCount > 0) {
                            Text(
                                text = stringResource(R.string.appsec_high_risk, state.highRiskCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = RiskRed
                            )
                        }
                    }
                }
            }
        }

        items(state.analysisResults, key = { it.packageName }) { result ->
            ApkAnalysisCard(result)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ApkAnalysisCard(result: ApkAnalysisResult) {
    var expanded by remember { mutableStateOf(false) }

    val riskColor = when (result.riskLevel) {
        "High" -> RiskRed
        "Medium" -> RiskYellow
        else -> TrustGreen
    }

    val riskLabel = when (result.riskLevel) {
        "High" -> stringResource(R.string.appsec_risk_high)
        "Medium" -> stringResource(R.string.appsec_risk_medium)
        else -> stringResource(R.string.appsec_risk_low)
    }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = riskLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
            }
            Text(
                text = "v${result.versionName} | SDK ${result.minSdk}-${result.targetSdk}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Flags
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.isDebuggable) FlagChip(stringResource(R.string.appsec_flag_debug), RiskRed)
                if (result.hasBackupAllowed) FlagChip(stringResource(R.string.appsec_flag_backup), RiskYellow)
                if (result.isCleartextAllowed) FlagChip(stringResource(R.string.appsec_flag_cleartext), RiskYellow)
                FlagChip(stringResource(R.string.appsec_risky_perms, result.dangerousPermissions.size), riskColor)
            }

            if (!expanded && result.dangerousPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.appsec_tap_detail),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            }

            // Expanded: show dangerous permissions
            if (expanded && result.dangerousPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.appsec_perms_header),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                result.dangerousPermissions.forEach { perm ->
                    val shortPerm = perm.substringAfterLast(".")
                        .replace("_", " ")
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }
                    Text(
                        text = "• $shortPerm",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 8.dp, top = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlagChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun DeviceSecurityTab(state: AppSecurityUiState) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ADB Status
        item {
            SecurityCheckCard(
                icon = Icons.Default.Usb,
                title = stringResource(R.string.appsec_adb_status),
                status = if (state.adbEnabled) stringResource(R.string.appsec_adb_enabled)
                else stringResource(R.string.appsec_adb_disabled),
                isRisk = state.adbEnabled
            )
        }

        // Developer Options
        item {
            SecurityCheckCard(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.appsec_dev_options),
                status = if (state.devOptionsEnabled) stringResource(R.string.appsec_dev_enabled)
                else stringResource(R.string.appsec_dev_disabled),
                isRisk = state.devOptionsEnabled
            )
        }

        // Info
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.appsec_device_info),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SecurityCheckCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    isRisk: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isRisk) RiskRed else TrustGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRisk) RiskRed else TrustGreen
                )
            }
            if (isRisk) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = RiskRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
