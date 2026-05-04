package com.trustedgelabs.trustguard.ui.screens.network

import android.app.Application
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.model.formatBytes
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.NetworkTeal
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NetworkUsageInfo(
    val totalSent: Long,
    val totalReceived: Long,
    val appUsages: List<AppNetworkUsage>
)

data class AppNetworkUsage(
    val appName: String,
    val packageName: String,
    val sentBytes: Long,
    val receivedBytes: Long
) {
    val totalBytes: Long get() = sentBytes + receivedBytes
}

class NetworkMonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val _networkInfo = MutableStateFlow<NetworkUsageInfo?>(null)
    val networkInfo: StateFlow<NetworkUsageInfo?> = _networkInfo.asStateFlow()

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val info = withContext(Dispatchers.IO) { getNetworkUsage() }
                _networkInfo.value = info
            } catch (_: Exception) {
                _networkInfo.value = null
            }
            _isScanning.value = false
        }
    }

    private fun getNetworkUsage(): NetworkUsageInfo {
        val app = getApplication<Application>()
        val pm = app.packageManager
        val packages = pm.getInstalledPackages(0)

        val appUsages = mutableListOf<AppNetworkUsage>()

        for (pkg in packages) {
            try {
                val uid = pkg.applicationInfo?.uid ?: continue
                val sent = TrafficStats.getUidTxBytes(uid)
                val received = TrafficStats.getUidRxBytes(uid)

                if (sent > 1024 || received > 1024) {
                    val appName = pm.getApplicationLabel(pkg.applicationInfo!!).toString()
                    appUsages.add(
                        AppNetworkUsage(
                            appName = appName,
                            packageName = pkg.packageName,
                            sentBytes = if (sent == TrafficStats.UNSUPPORTED.toLong()) 0 else sent,
                            receivedBytes = if (received == TrafficStats.UNSUPPORTED.toLong()) 0 else received
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        val totalSent = TrafficStats.getTotalTxBytes().let { if (it == TrafficStats.UNSUPPORTED.toLong()) 0 else it }
        val totalReceived = TrafficStats.getTotalRxBytes().let { if (it == TrafficStats.UNSUPPORTED.toLong()) 0 else it }

        return NetworkUsageInfo(
            totalSent = totalSent,
            totalReceived = totalReceived,
            appUsages = appUsages.sortedByDescending { it.totalBytes }.take(15)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMonitorScreen(
    onNavigateBack: () -> Unit,
    viewModel: NetworkMonitorViewModel = viewModel()
) {
    val networkInfo by viewModel.networkInfo.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.network_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TrustGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = MaterialTheme.colorScheme.onBackground)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            val info = networkInfo
            if (isScanning || info == null) {
                Text(stringResource(R.string.network_scanning), color = TextSecondary)
            } else {
                // Total traffic cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NetworkStatCard(
                        icon = Icons.Default.ArrowUpward,
                        label = stringResource(R.string.network_total_sent),
                        value = formatBytes(info.totalSent),
                        color = NetworkTeal,
                        modifier = Modifier.weight(1f)
                    )
                    NetworkStatCard(
                        icon = Icons.Default.ArrowDownward,
                        label = stringResource(R.string.network_total_received),
                        value = formatBytes(info.totalReceived),
                        color = TrustGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Top consumers
                if (info.appUsages.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.network_top_consumers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    val maxUsage = info.appUsages.maxOf { it.totalBytes }.toFloat()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkCard)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        info.appUsages.forEach { app ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatBytes(app.totalBytes),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (app.totalBytes / maxUsage).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = NetworkTeal,
                                    trackColor = NetworkTeal.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun NetworkStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
