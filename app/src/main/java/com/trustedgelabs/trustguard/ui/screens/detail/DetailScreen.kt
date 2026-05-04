package com.trustedgelabs.trustguard.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.domain.TrustedAppTransparency
import com.trustedgelabs.trustguard.ui.components.PermissionItem
import com.trustedgelabs.trustguard.util.AppActivityTracker
import com.trustedgelabs.trustguard.ui.components.RiskBadge
import com.trustedgelabs.trustguard.ui.components.ScanProgressIndicator
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.DarkSurfaceVariant
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TextTertiary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import com.trustedgelabs.trustguard.util.toImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(packageName) {
        viewModel.loadApp(packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.detail_title),
                    style = MaterialTheme.typography.titleLarge,
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (state.isLoading) {
            ScanProgressIndicator()
        } else {
            val appInfo = state.appInfo
            if (appInfo != null) {
                val iconBitmap = remember(appInfo.packageName) {
                    appInfo.icon?.toImageBitmap()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = appInfo.appName,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(72.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = appInfo.appName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = appInfo.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            RiskBadge(riskLevel = appInfo.riskLevel)

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkCard)
                                    .padding(16.dp)
                            ) {
                                StatItem(
                                    value = "${appInfo.totalPermissionCount}",
                                    label = stringResource(R.string.total_permissions)
                                )
                                StatItem(
                                    value = "${appInfo.dangerousPermissionCount}",
                                    label = stringResource(R.string.dangerous),
                                    valueColor = if (appInfo.dangerousPermissionCount > 0) appInfo.riskLevel.color else TrustGreen
                                )
                                StatItem(
                                    value = "${appInfo.riskScore}",
                                    label = stringResource(R.string.risk_score),
                                    valueColor = appInfo.riskLevel.color
                                )
                            }
                        }
                    }

                    // Trusted App Transparency Section
                    val transparencyInfo = TrustedAppTransparency.getTransparencyInfo(appInfo.packageName)
                    if (transparencyInfo != null) {
                        item {
                            TransparencySection(
                                info = transparencyInfo,
                                permissions = appInfo.permissions.map { it.name }
                            )
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.permissions_header, appInfo.permissions.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (appInfo.permissions.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_permissions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(appInfo.permissions) { permission ->
                            PermissionItem(permission = permission)
                            HorizontalDivider(
                                color = DarkCard,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // App Activity Section
                    item {
                        AppActivitySection(
                            packageName = appInfo.packageName,
                            context = context
                        )
                    }

                    // Disclaimer
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${appInfo.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrustGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.open_settings),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun TransparencySection(
    info: TrustedAppTransparency.AppTransparencyInfo,
    permissions: List<String>
) {
    val trustBlue = Color(0xFF448AFF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Verified badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(trustBlue.copy(alpha = 0.1f))
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = trustBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.transparency_verified),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = trustBlue
                )
                Text(
                    text = stringResource(R.string.transparency_badge),
                    style = MaterialTheme.typography.bodySmall,
                    color = trustBlue.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Developer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.transparency_developer),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = info.developerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = trustBlue
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Privacy guarantee
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.transparency_privacy),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TrustGreen
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = info.privacyGuarantee,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }

        // Permission explanations (if available)
        if (info.permissionExplanations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.transparency_permissions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TrustGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Show explanations only for permissions the app actually uses
                permissions.forEach { perm ->
                    val explanation = info.permissionExplanations[perm]
                    if (explanation != null) {
                        val shortName = perm.substringAfterLast(".")
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "✓",
                                color = TrustGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(20.dp)
                            )
                            Column {
                                Text(
                                    text = shortName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Network connections
        if (info.networkConnections.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.transparency_network),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TrustGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                info.networkConnections.forEach { conn ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = conn.host,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = trustBlue
                        )
                        Text(
                            text = conn.purpose,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "↗ ${conn.dataTransferred}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TrustGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TrustGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.transparency_no_external),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TrustGreen,
                        fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                }
            }
        }

        // Data policy
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.transparency_data_policy),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TrustGreen
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = info.dataPolicy,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AppActivitySection(
    packageName: String,
    context: android.content.Context
) {
    val hasPermission = AppActivityTracker.hasUsagePermission(context)
    var report by remember { mutableStateOf<AppActivityTracker.AppActivityReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(packageName, hasPermission) {
        if (hasPermission) {
            isLoading = true
            report = AppActivityTracker.getActivityReport(context, packageName)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.activity_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (!hasPermission) {
            // Permission not granted
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.activity_no_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.activity_grant_permission),
                        color = TrustGreen
                    )
                }
            }
            return
        }

        if (isLoading) {
            LinearProgressIndicator(
                color = TrustGreen,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            )
            return
        }

        val r = report ?: return

        // Usage time card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = TrustGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.activity_foreground) + " / " + stringResource(R.string.activity_background),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TrustGreen
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ActivityStatBox(
                    label = stringResource(R.string.activity_foreground),
                    value = AppActivityTracker.formatDuration(r.foregroundTimeMs),
                    color = TrustGreen,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActivityStatBox(
                    label = stringResource(R.string.activity_background),
                    value = AppActivityTracker.formatDuration(r.backgroundTimeMs),
                    color = if (r.hasSuspiciousBackground) Color(0xFFFF9100) else TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (r.hasSuspiciousBackground) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.activity_suspicious_bg),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9100),
                    fontWeight = FontWeight.Bold
                )
            }

            if (r.lastUsedTimestamp > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val lastUsedStr = DateUtils.getRelativeTimeSpanString(
                    r.lastUsedTimestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                ).toString()
                Text(
                    text = "${stringResource(R.string.activity_last_used)}: $lastUsedStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Network usage card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFF448AFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.activity_network),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF448AFF)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ActivityStatBox(
                    label = "↑ ${stringResource(R.string.activity_sent)}",
                    value = AppActivityTracker.formatBytes(r.dataSentBytes),
                    color = Color(0xFFFF9100),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActivityStatBox(
                    label = "↓ ${stringResource(R.string.activity_received)}",
                    value = AppActivityTracker.formatBytes(r.dataReceivedBytes),
                    color = Color(0xFF448AFF),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActivityStatBox(
                    label = stringResource(R.string.activity_total_data),
                    value = AppActivityTracker.formatBytes(r.totalDataBytes),
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Privacy access card (camera, mic, location)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.activity_privacy),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAB47BC)
            )
            Spacer(modifier = Modifier.height(8.dp))

            PrivacyAccessRow(
                icon = Icons.Default.CameraAlt,
                label = stringResource(R.string.activity_camera),
                accessCount = r.cameraAccessCount,
                lastAccess = r.lastCameraAccess,
                color = Color(0xFFFF1744)
            )
            PrivacyAccessRow(
                icon = Icons.Default.Mic,
                label = stringResource(R.string.activity_microphone),
                accessCount = r.microphoneAccessCount,
                lastAccess = r.lastMicrophoneAccess,
                color = Color(0xFFFF9100)
            )
            PrivacyAccessRow(
                icon = Icons.Default.LocationOn,
                label = stringResource(R.string.activity_location),
                accessCount = r.locationAccessCount,
                lastAccess = r.lastLocationAccess,
                color = Color(0xFF448AFF)
            )
        }
    }
}

@Composable
private fun ActivityStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
    }
}

@Composable
private fun PrivacyAccessRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accessCount: Int,
    lastAccess: Long,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accessCount > 0) color else TextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (accessCount > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.activity_times_accessed, accessCount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (lastAccess > 0) {
                    val timeAgo = DateUtils.getRelativeTimeSpanString(
                        lastAccess, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                    Text(
                        text = stringResource(R.string.activity_last_access, timeAgo),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.activity_no_access),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}
