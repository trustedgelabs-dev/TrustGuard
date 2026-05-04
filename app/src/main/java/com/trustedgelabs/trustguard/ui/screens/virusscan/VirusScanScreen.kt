package com.trustedgelabs.trustguard.ui.screens.virusscan

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.domain.VirusScanner
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.DarkSurface
import com.trustedgelabs.trustguard.ui.theme.DarkSurfaceVariant
import com.trustedgelabs.trustguard.ui.theme.RiskGreen
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirusScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: VirusScanViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    stringResource(R.string.virus_scan_title),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TrustGreen)
                }
            },
            actions = {
                if (state.scanComplete) {
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TrustGreen)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = Color.White
            )
        )

        when {
            // Not started yet
            !state.isScanning && !state.scanComplete -> {
                StartScanContent(onStartScan = { viewModel.startScan() })
            }
            // Scanning
            state.isScanning -> {
                ScanningContent(
                    progress = state.scanProgress,
                    phase = state.scanPhase
                )
            }
            // Results
            state.scanComplete && state.result != null -> {
                ScanResultsContent(
                    result = state.result!!,
                    onRescan = { viewModel.startScan() }
                )
            }
        }
    }
}

@Composable
private fun StartScanContent(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Shield icon with glow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            // Outer ring
            Canvas(modifier = Modifier.size(140.dp)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            TrustGreen.copy(alpha = 0.3f),
                            TrustGreen.copy(alpha = 0.8f),
                            TrustGreen.copy(alpha = 0.3f)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
            }
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = TrustGreen,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            stringResource(R.string.virus_scan_ready_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            stringResource(R.string.virus_scan_ready_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onStartScan,
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Radar, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.virus_scan_start),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ScanningContent(progress: Float, phase: ScanPhase) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated scanning icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            Canvas(modifier = Modifier
                .size(160.dp)
                .rotate(rotation)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            TrustGreen.copy(alpha = 0.6f),
                            TrustGreen
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                    size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx())
                )
            }
            Icon(
                Icons.Default.Radar,
                contentDescription = null,
                tint = TrustGreen,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            stringResource(R.string.virus_scan_scanning),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (phase) {
                ScanPhase.ANALYZING_APPS -> stringResource(R.string.virus_scan_phase_apps)
                ScanPhase.CHECKING_PERMISSIONS -> stringResource(R.string.virus_scan_phase_perms)
                ScanPhase.VERIFYING_SIGNATURES -> stringResource(R.string.virus_scan_phase_sigs)
                ScanPhase.CHECKING_SYSTEM -> stringResource(R.string.virus_scan_phase_system)
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = TrustGreen,
            trackColor = DarkSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ScanResultsContent(
    result: VirusScanner.ScanResult,
    onRescan: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ═══ Status header ═══
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (icon, color, title) = when (result.overallStatus) {
                    VirusScanner.ScanStatus.CLEAN -> Triple(
                        Icons.Default.GppGood, RiskGreen,
                        R.string.virus_scan_clean
                    )
                    VirusScanner.ScanStatus.WARNINGS -> Triple(
                        Icons.Default.GppMaybe, RiskYellow,
                        R.string.virus_scan_warnings
                    )
                    VirusScanner.ScanStatus.THREATS_FOUND -> Triple(
                        Icons.Default.GppBad, RiskRed,
                        R.string.virus_scan_threats
                    )
                    VirusScanner.ScanStatus.CRITICAL -> Triple(
                        Icons.Default.GppBad, RiskRed,
                        R.string.virus_scan_critical
                    )
                }

                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    stringResource(title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    stringResource(
                        R.string.virus_scan_summary,
                        result.totalAppsScanned,
                        result.scanDurationMs / 1000.0
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // ═══ Stats cards ═══
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    color = RiskGreen,
                    value = "${result.safeAppsCount}",
                    label = stringResource(R.string.virus_scan_safe)
                )
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Warning,
                    color = RiskYellow,
                    value = "${result.warnings.size}",
                    label = stringResource(R.string.virus_scan_warning_count)
                )
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Error,
                    color = RiskRed,
                    value = "${result.threats.size}",
                    label = stringResource(R.string.virus_scan_threat_count)
                )
            }
        }

        // ═══ Threats section ═══
        if (result.threats.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.virus_scan_threats_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RiskRed
                )
            }

            items(result.threats) { threat ->
                ThreatCard(threat = threat, onUninstall = {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${threat.packageName}")
                    }
                    context.startActivity(intent)
                })
            }
        }

        // ═══ Warnings section ═══
        if (result.warnings.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.virus_scan_warnings_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RiskYellow
                )
            }

            items(result.warnings) { warning ->
                ThreatCard(threat = warning, onUninstall = {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${warning.packageName}")
                    }
                    context.startActivity(intent)
                })
            }
        }

        // ═══ System Security ═══
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.virus_scan_system_security),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(result.systemIssues) { issue ->
            SystemIssueCard(issue = issue)
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatMiniCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    color: Color,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThreatCard(
    threat: VirusScanner.ThreatInfo,
    onUninstall: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val (severityColor, severityText) = when (threat.severity) {
        VirusScanner.ThreatSeverity.CRITICAL -> RiskRed to R.string.virus_severity_critical
        VirusScanner.ThreatSeverity.HIGH -> RiskRed.copy(alpha = 0.85f) to R.string.virus_severity_high
        VirusScanner.ThreatSeverity.MEDIUM -> RiskYellow to R.string.virus_severity_medium
        VirusScanner.ThreatSeverity.LOW -> TextSecondary to R.string.virus_severity_low
    }

    val threatTypeText = when (threat.threatType) {
        VirusScanner.ThreatType.KNOWN_MALWARE -> R.string.virus_type_malware
        VirusScanner.ThreatType.DANGEROUS_PERMISSIONS -> R.string.virus_type_dangerous_perms
        VirusScanner.ThreatType.FAKE_APP -> R.string.virus_type_fake_app
        VirusScanner.ThreatType.SIDELOADED_RISKY -> R.string.virus_type_sideloaded
        VirusScanner.ThreatType.HIDDEN_APP -> R.string.virus_type_hidden
        VirusScanner.ThreatType.SUSPICIOUS_CERTIFICATE -> R.string.virus_type_cert
        VirusScanner.ThreatType.TROJAN_PATTERN -> R.string.virus_type_trojan
        VirusScanner.ThreatType.SPYWARE_PATTERN -> R.string.virus_type_spyware
        VirusScanner.ThreatType.RANSOMWARE_PATTERN -> R.string.virus_type_ransomware
        VirusScanner.ThreatType.CRYPTO_MINER -> R.string.virus_type_miner
        VirusScanner.ThreatType.BANKING_TROJAN -> R.string.virus_type_banking
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(severityColor.copy(alpha = 0.08f))
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                when (threat.severity) {
                    VirusScanner.ThreatSeverity.CRITICAL -> Icons.Default.GppBad
                    VirusScanner.ThreatSeverity.HIGH -> Icons.Default.Error
                    VirusScanner.ThreatSeverity.MEDIUM -> Icons.Default.Warning
                    VirusScanner.ThreatSeverity.LOW -> Icons.Default.Info
                },
                contentDescription = null,
                tint = severityColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    threat.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    stringResource(threatTypeText),
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor
                )
            }
            // Severity badge
            Text(
                stringResource(severityText),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = severityColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(severityColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    threat.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onUninstall,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.virus_scan_uninstall))
                }
            }
        }
    }
}

@Composable
private fun SystemIssueCard(issue: VirusScanner.SystemIssue) {
    val (icon, title) = when (issue.type) {
        VirusScanner.SystemIssueType.USB_DEBUGGING -> Icons.Default.PhonelinkLock to R.string.virus_system_usb
        VirusScanner.SystemIssueType.UNKNOWN_SOURCES -> Icons.Default.Warning to R.string.virus_system_unknown_sources
        VirusScanner.SystemIssueType.DEVELOPER_OPTIONS -> Icons.Default.Info to R.string.virus_system_dev_options
        VirusScanner.SystemIssueType.ROOT_DETECTED -> Icons.Default.Security to R.string.virus_system_root
        VirusScanner.SystemIssueType.SCREEN_LOCK -> Icons.Default.PhonelinkLock to R.string.virus_system_screen_lock
        VirusScanner.SystemIssueType.ENCRYPTION -> Icons.Default.Security to R.string.virus_system_encryption
        VirusScanner.SystemIssueType.PLAY_PROTECT -> Icons.Default.Shield to R.string.virus_system_play_protect
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (issue.isSecure) RiskGreen else RiskYellow,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            stringResource(title),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (issue.isSecure) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (issue.isSecure) RiskGreen else RiskYellow,
            modifier = Modifier.size(20.dp)
        )
    }
}
