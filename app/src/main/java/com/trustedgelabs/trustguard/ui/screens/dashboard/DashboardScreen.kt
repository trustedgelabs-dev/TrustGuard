package com.trustedgelabs.trustguard.ui.screens.dashboard

import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.ui.components.AppCard
import com.trustedgelabs.trustguard.ui.components.ProDashboardHeader
import com.trustedgelabs.trustguard.ui.components.QuickActionBar
import com.trustedgelabs.trustguard.ui.components.ScanProgressIndicator
import com.trustedgelabs.trustguard.ui.components.SecurityScoreArc
import com.trustedgelabs.trustguard.ui.components.SectionHeader
import com.trustedgelabs.trustguard.ui.components.ToolCard
import com.trustedgelabs.trustguard.ui.components.ToolCardGrid
import com.trustedgelabs.trustguard.ui.components.ToolGridItem
import com.trustedgelabs.trustguard.ui.components.TrafficLightRow
import com.trustedgelabs.trustguard.ui.components.VpnStatusCard
import com.trustedgelabs.trustguard.ui.theme.BatteryGreen
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.IntegrityIndigo
import com.trustedgelabs.trustguard.ui.theme.NetworkTeal
import com.trustedgelabs.trustguard.ui.theme.OptimizationOrange
import com.trustedgelabs.trustguard.ui.theme.ProGold
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskGreen
import com.trustedgelabs.trustguard.ui.theme.SecurityBlue
import com.trustedgelabs.trustguard.ui.theme.StorageCyan
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import com.trustedgelabs.trustguard.ui.theme.WifiPurple
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAppList: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBlocking: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    onNavigateToAdware: () -> Unit,
    onNavigateToPremium: () -> Unit = {},
    onNavigateToOptimization: () -> Unit,
    onNavigateToWifi: () -> Unit = {},
    onNavigateToBattery: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToIntegrity: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToFamilyShield: () -> Unit = {},
    onNavigateToVirusScan: () -> Unit = {},
    onNavigateToFirewall: () -> Unit = {},
    onNavigateToFakeIdentity: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToPacketSniffer: () -> Unit = {},
    onNavigateToBloatware: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAppSecurity: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isPremium = true
    val context = LocalContext.current

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.toggleVpn(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TrustGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.by_company),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = TextSecondary
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = { viewModel.rescan() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.rescan),
                        tint = TrustGreen
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = TrustGreen
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (state.isScanning && !state.scanComplete) {
            ScanProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // === PRO USERS: Enhanced header with stats ===
                if (isPremium) {
                    ProDashboardHeader(
                        securityScore = state.overallScore,
                        blockedThreats = state.blockedTodayCount,
                        securityEvents = state.adwareSuspiciousCount
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // === FREE USERS: Clean score arc ===
                    SecurityScoreArc(score = state.overallScore)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.apps_scanned, state.totalApps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // === QUICK ACTIONS (VPN + Protection) ===
                QuickActionBar(
                    vpnActive = state.vpnActive,
                    blockedCount = state.blockedTodayCount,
                    onVpnClick = {
                        if (!state.vpnActive) {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) {
                                vpnPermissionLauncher.launch(prepareIntent)
                            } else {
                                viewModel.toggleVpn(context)
                            }
                        } else {
                            viewModel.toggleVpn(context)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // === TRAFFIC LIGHT SUMMARY ===
                TrafficLightRow(
                    redCount = state.redCount,
                    yellowCount = state.yellowCount,
                    greenCount = state.greenCount
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ============================================
                // SECTION: Security Tools (FREE)
                // ============================================
                SectionHeader(title = stringResource(R.string.dashboard_section_security))

                Spacer(modifier = Modifier.height(4.dp))

                // Virus Scanner
                ToolCard(
                    icon = Icons.Default.Radar,
                    iconColor = RiskRed,
                    title = stringResource(R.string.virus_scan_card_title),
                    subtitle = stringResource(R.string.virus_scan_card_subtitle),
                    onClick = onNavigateToVirusScan,
                    statusColor = RiskRed
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Permission Analysis
                ToolCard(
                    icon = Icons.Default.Security,
                    iconColor = SecurityBlue,
                    title = stringResource(R.string.app_list_title),
                    subtitle = stringResource(R.string.apps_scanned, state.totalApps),
                    onClick = onNavigateToAppList,
                    statusText = "${state.redCount}",
                    statusColor = if (state.redCount > 0) RiskRed else TrustGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ad & Tracker Blocking
                ToolCard(
                    icon = Icons.Default.Shield,
                    iconColor = TrustGreen,
                    title = stringResource(R.string.dns_blocking_title),
                    subtitle = if (state.vpnActive)
                        stringResource(R.string.blocked_today, state.blockedTodayCount)
                    else
                        stringResource(R.string.vpn_disabled),
                    onClick = onNavigateToBlocking,
                    statusText = if (state.vpnActive) stringResource(R.string.vpn_enabled) else null,
                    statusColor = TrustGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Adware Detection
                ToolCard(
                    icon = Icons.Default.BugReport,
                    iconColor = OptimizationOrange,
                    title = stringResource(R.string.adware_card_title),
                    subtitle = if (state.adwareSuspiciousCount > 0)
                        stringResource(R.string.adware_found, state.adwareSuspiciousCount)
                    else
                        stringResource(R.string.adware_none_found),
                    onClick = onNavigateToAdware,
                    statusText = if (state.adwareSuspiciousCount > 0) "${state.adwareSuspiciousCount}" else null,
                    statusColor = if (state.adwareSuspiciousCount > 0) RiskRed else TrustGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Wi-Fi Security (FREE)
                ToolCard(
                    icon = Icons.Default.Wifi,
                    iconColor = WifiPurple,
                    title = stringResource(R.string.wifi_card_title),
                    subtitle = stringResource(R.string.wifi_card_subtitle),
                    onClick = onNavigateToWifi
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Family Shield
                ToolCard(
                    icon = Icons.Default.ChildCare,
                    iconColor = ProGold,
                    title = stringResource(R.string.fs_card_title),
                    subtitle = stringResource(R.string.fs_card_subtitle),
                    onClick = onNavigateToFamilyShield
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Per-App Firewall
                ToolCard(
                    icon = Icons.Default.Block,
                    iconColor = RiskRed,
                    title = stringResource(R.string.firewall_card_title),
                    subtitle = stringResource(R.string.firewall_card_subtitle),
                    onClick = onNavigateToFirewall
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ============================================
                // SECTION: Optimization (FREE)
                // ============================================
                SectionHeader(title = stringResource(R.string.dashboard_section_optimization))

                Spacer(modifier = Modifier.height(4.dp))

                ToolCardGrid(
                    items = listOf(
                        ToolGridItem(
                            icon = Icons.Default.BatteryStd,
                            iconColor = BatteryGreen,
                            title = stringResource(R.string.battery_card_title),
                            onClick = onNavigateToBattery
                        ),
                        ToolGridItem(
                            icon = Icons.Default.Storage,
                            iconColor = StorageCyan,
                            title = stringResource(R.string.storage_card_title),
                            onClick = onNavigateToStorage
                        ),
                        ToolGridItem(
                            icon = Icons.Default.CleaningServices,
                            iconColor = OptimizationOrange,
                            title = stringResource(R.string.optimize_title),
                            onClick = onNavigateToOptimization
                        ),
                        ToolGridItem(
                            icon = Icons.Default.RestoreFromTrash,
                            iconColor = SecurityBlue,
                            title = stringResource(R.string.recovery_card_title),
                            onClick = onNavigateToRecovery
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ============================================
                // SECTION: Advanced/Pro Tools
                // ============================================
                if (isPremium) {
                    // Pro users see all advanced tools unlocked
                    SectionHeader(title = stringResource(R.string.dashboard_section_advanced))

                    Spacer(modifier = Modifier.height(4.dp))

                    ToolCard(
                        icon = Icons.Default.VerifiedUser,
                        iconColor = IntegrityIndigo,
                        title = stringResource(R.string.integrity_card_title),
                        subtitle = stringResource(R.string.integrity_card_subtitle),
                        onClick = onNavigateToIntegrity,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.NetworkCheck,
                        iconColor = NetworkTeal,
                        title = stringResource(R.string.network_card_title),
                        subtitle = stringResource(R.string.network_card_subtitle),
                        onClick = onNavigateToNetwork,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.Fingerprint,
                        iconColor = WifiPurple,
                        title = stringResource(R.string.fake_id_card_title),
                        subtitle = stringResource(R.string.fake_id_card_subtitle),
                        onClick = onNavigateToFakeIdentity,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.Lock,
                        iconColor = StorageCyan,
                        title = stringResource(R.string.vault_card_title),
                        subtitle = stringResource(R.string.vault_card_subtitle),
                        onClick = onNavigateToVault,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.CellTower,
                        iconColor = NetworkTeal,
                        title = stringResource(R.string.sniffer_card_title),
                        subtitle = stringResource(R.string.sniffer_card_subtitle),
                        onClick = onNavigateToPacketSniffer,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.DeleteSweep,
                        iconColor = OptimizationOrange,
                        title = stringResource(R.string.bloatware_card_title),
                        subtitle = stringResource(R.string.bloatware_card_subtitle),
                        onClick = onNavigateToBloatware,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.PrivacyTip,
                        iconColor = WifiPurple,
                        title = stringResource(R.string.privacy_card_title),
                        subtitle = stringResource(R.string.privacy_card_subtitle),
                        onClick = onNavigateToPrivacy,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.GppGood,
                        iconColor = SecurityBlue,
                        title = stringResource(R.string.appsec_card_title),
                        subtitle = stringResource(R.string.appsec_card_subtitle),
                        onClick = onNavigateToAppSecurity,
                        isProFeature = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // Free users see a subtle peek at what Pro offers
                    SectionHeader(title = stringResource(R.string.dashboard_section_pro_tools))

                    Spacer(modifier = Modifier.height(4.dp))

                    ToolCard(
                        icon = Icons.Default.VerifiedUser,
                        iconColor = IntegrityIndigo,
                        title = stringResource(R.string.integrity_card_title),
                        subtitle = stringResource(R.string.integrity_card_subtitle),
                        onClick = onNavigateToPremium,
                        isProFeature = true,
                        isLocked = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ToolCard(
                        icon = Icons.Default.NetworkCheck,
                        iconColor = NetworkTeal,
                        title = stringResource(R.string.network_card_title),
                        subtitle = stringResource(R.string.network_card_subtitle),
                        onClick = onNavigateToPremium,
                        isProFeature = true,
                        isLocked = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ============================================
                // TOP RISK APPS
                // ============================================
                if (state.topRiskApps.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.top_risks),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = onNavigateToAppList) {
                            Text(
                                text = stringResource(R.string.view_all),
                                color = TrustGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.topRiskApps.take(3).forEach { app ->
                            AppCard(
                                appInfo = app,
                                onClick = { onNavigateToDetail(app.packageName) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
