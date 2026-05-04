package com.trustedgelabs.trustguard.ui.screens.battery

import android.app.Application
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.datasource.BatteryDataSource
import com.trustedgelabs.trustguard.data.model.BatteryAppUsage
import com.trustedgelabs.trustguard.data.model.BatteryInfo
import com.trustedgelabs.trustguard.ui.theme.BatteryGreen
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.RiskGreen
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatteryHealthViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = BatteryDataSource(application)

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo.asStateFlow()

    private val _topDrainers = MutableStateFlow<List<BatteryAppUsage>>(emptyList())
    val topDrainers: StateFlow<List<BatteryAppUsage>> = _topDrainers.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) { dataSource.getBatteryInfo() }
                _batteryInfo.value = info
            } catch (_: Exception) {
                _batteryInfo.value = null
            }
            try {
                val drainers = withContext(Dispatchers.IO) { dataSource.getTopBatteryDrainers() }
                _topDrainers.value = drainers
            } catch (_: Exception) {
                _topDrainers.value = emptyList()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHealthScreen(
    onNavigateBack: () -> Unit,
    viewModel: BatteryHealthViewModel = viewModel()
) {
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val topDrainers by viewModel.topDrainers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.battery_title), fontWeight = FontWeight.Bold) },
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

            val info = batteryInfo
            if (info == null) {
                Text(stringResource(R.string.battery_scanning), color = TextSecondary)
            } else {
                // Battery level circle
                val batteryColor = when {
                    info.percentage > 60 -> RiskGreen
                    info.percentage > 20 -> RiskYellow
                    else -> RiskRed
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(batteryColor.copy(alpha = 0.15f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (info.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = batteryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "${info.percentage}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = batteryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (info.status) {
                        "Charging" -> stringResource(R.string.battery_charging)
                        "Discharging" -> stringResource(R.string.battery_discharging)
                        "Full" -> stringResource(R.string.battery_full)
                        else -> stringResource(R.string.battery_not_charging)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = batteryColor,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Battery details card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .padding(16.dp)
                ) {
                    BatteryDetailRow(stringResource(R.string.battery_health), when (info.health) {
                        "Good" -> stringResource(R.string.battery_health_good)
                        "Overheat" -> stringResource(R.string.battery_health_overheat)
                        "Dead" -> stringResource(R.string.battery_health_dead)
                        else -> stringResource(R.string.battery_health_unknown)
                    })
                    BatteryDetailRow(stringResource(R.string.battery_temperature), "%.1f°C".format(info.temperature))
                    BatteryDetailRow(stringResource(R.string.battery_voltage), "%.2f V".format(info.voltage))
                    BatteryDetailRow(stringResource(R.string.battery_technology), info.technology)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Top drainers
                if (topDrainers.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.battery_top_drainers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    val maxUsage = topDrainers.maxOf { it.usageTimeMs }.toFloat()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkCard)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        topDrainers.forEach { app ->
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
                                        text = app.usageTimeFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (app.usageTimeMs / maxUsage).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = BatteryGreen,
                                    trackColor = BatteryGreen.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Battery tips
                Text(
                    text = stringResource(R.string.battery_tips),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BatteryTip(stringResource(R.string.battery_tip_brightness))
                    BatteryTip(stringResource(R.string.battery_tip_background))
                    BatteryTip(stringResource(R.string.battery_tip_location))
                    BatteryTip(stringResource(R.string.battery_tip_sync))
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun BatteryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BatteryTip(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BatteryGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
