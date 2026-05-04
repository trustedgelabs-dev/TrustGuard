package com.trustedgelabs.trustguard.ui.screens.wifi

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.datasource.WifiSecurityDataSource
import com.trustedgelabs.trustguard.data.model.WifiSecurityInfo
import com.trustedgelabs.trustguard.data.model.WifiSecurityLevel
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.RiskGreen
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import com.trustedgelabs.trustguard.ui.theme.WifiPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WifiScanState(
    val isScanning: Boolean = true,
    val wifiInfo: WifiSecurityInfo? = null,
    val errorMessage: String? = null,
    val needsLocationPermission: Boolean = false
)

class WifiSecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = WifiSecurityDataSource(application)

    private val _state = MutableStateFlow(WifiScanState())
    val state: StateFlow<WifiScanState> = _state.asStateFlow()

    init {
        checkPermissionsAndScan()
    }

    fun checkPermissionsAndScan() {
        val context = getApplication<Application>()
        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocation) {
            _state.value = WifiScanState(
                isScanning = false,
                needsLocationPermission = true
            )
            return
        }

        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true, errorMessage = null)
            try {
                val info = withContext(Dispatchers.IO) { dataSource.scanCurrentNetwork() }
                _state.value = WifiScanState(
                    isScanning = false,
                    wifiInfo = info,
                    errorMessage = null,
                    needsLocationPermission = false
                )
            } catch (e: Exception) {
                _state.value = WifiScanState(
                    isScanning = false,
                    wifiInfo = null,
                    errorMessage = e.message ?: "Unknown error",
                    needsLocationPermission = false
                )
            }
        }
    }

    fun onPermissionGranted() {
        scan()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSecurityScreen(
    onNavigateBack: () -> Unit,
    viewModel: WifiSecurityViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.wifi_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TrustGreen)
                }
            },
            actions = {
                if (!state.isScanning && !state.needsLocationPermission) {
                    IconButton(onClick = { viewModel.scan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.rescan), tint = TrustGreen)
                    }
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
            Spacer(modifier = Modifier.height(24.dp))

            when {
                // === SCANNING ===
                state.isScanning -> {
                    Spacer(modifier = Modifier.height(60.dp))
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = WifiPurple,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.wifi_scanning),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }

                // === LOCATION PERMISSION NEEDED ===
                state.needsLocationPermission -> {
                    LocationPermissionRequest(
                        onGrantClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }

                // === ERROR ===
                state.errorMessage != null -> {
                    ErrorState(
                        message = state.errorMessage!!,
                        onRetry = { viewModel.scan() }
                    )
                }

                // === NOT CONNECTED ===
                state.wifiInfo == null -> {
                    NotConnectedState()
                }

                // === WIFI INFO AVAILABLE ===
                else -> {
                    WifiInfoContent(info = state.wifiInfo!!)
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionRequest(onGrantClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(RiskYellow.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                tint = RiskYellow,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.wifi_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Wi-Fi network details require location permission. This is an Android requirement — your location data is never collected or sent anywhere.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGrantClick,
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.grant_permission),
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCard)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TrustGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Android requires location permission to read Wi-Fi network names. TrustGuard only uses this to check your network security type — nothing is stored or shared.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(RiskRed.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = RiskRed,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Unable to scan Wi-Fi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Make sure Wi-Fi is turned on and you are connected to a network. Some devices may require location services to be enabled.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.rescan),
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black
            )
        }
    }
}

@Composable
private fun NotConnectedState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(TextSecondary.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.wifi_not_connected),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect to a Wi-Fi network to analyze its security settings. TrustGuard will check encryption type, signal strength, and provide security recommendations.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun WifiInfoContent(info: WifiSecurityInfo) {
    val encryptionColor = when (info.securityLevel) {
        WifiSecurityLevel.SECURE -> RiskGreen
        WifiSecurityLevel.WARNING -> RiskYellow
        WifiSecurityLevel.DANGER -> RiskRed
    }
    val encryptionLabel = when (info.securityLevel) {
        WifiSecurityLevel.SECURE -> stringResource(R.string.wifi_encryption_strong)
        WifiSecurityLevel.WARNING -> stringResource(R.string.wifi_encryption_weak)
        WifiSecurityLevel.DANGER -> stringResource(R.string.wifi_encryption_none)
    }

    // Encryption Status Badge
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(encryptionColor.copy(alpha = 0.15f))
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = encryptionColor,
            modifier = Modifier.size(40.dp)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = encryptionLabel,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = encryptionColor
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = info.ssid,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(24.dp))

    // === ENCRYPTION ASSESSMENT ===
    val assessmentText = when {
        info.securityType.contains("WPA3") -> stringResource(R.string.wifi_wpa3_network)
        info.securityType.contains("WPA2") -> stringResource(R.string.wifi_wpa2_network)
        info.securityType.contains("WPA") -> stringResource(R.string.wifi_wpa_network)
        info.securityType.contains("WEP") -> stringResource(R.string.wifi_wep_network)
        info.securityType == "Unknown" -> stringResource(R.string.wifi_unknown_security)
        else -> stringResource(R.string.wifi_open_network)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(encryptionColor.copy(alpha = 0.1f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = encryptionColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = assessmentText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }

    Spacer(modifier = Modifier.height(12.dp))

    // === NETWORK-LEVEL RISKS (always shown, even for WPA3) ===
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RiskYellow.copy(alpha = 0.06f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = RiskYellow, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.wifi_network_risks_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = RiskYellow
            )
        }
        Text(
            text = stringResource(R.string.wifi_network_risks_body),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp
        )
        // Specific risk items
        NetworkRiskItem(stringResource(R.string.wifi_risk_dns_hijack))
        NetworkRiskItem(stringResource(R.string.wifi_risk_mitm))
        NetworkRiskItem(stringResource(R.string.wifi_risk_rogue_ap))
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Network Details
    Text(
        text = stringResource(R.string.wifi_network_name),
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
            .padding(16.dp)
    ) {
        DetailRow(stringResource(R.string.wifi_security_type), info.securityType)
        DetailRow(stringResource(R.string.wifi_signal_strength), getSignalLabel(info.signalLevel) + " (${info.rssi} dBm)")
        DetailRow(stringResource(R.string.wifi_frequency), "${info.frequency} MHz")
        DetailRow(stringResource(R.string.wifi_speed), "${info.linkSpeed} Mbps")
        DetailRow(stringResource(R.string.wifi_ip_address), info.ipAddress)
        DetailRow(stringResource(R.string.wifi_gateway), info.gateway)
        DetailRow(stringResource(R.string.wifi_dns), info.dns)
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Security Tips
    Text(
        text = stringResource(R.string.wifi_security_tips),
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
        TipItem(stringResource(R.string.wifi_tip_vpn))
        TipItem(stringResource(R.string.wifi_tip_https))
        TipItem(stringResource(R.string.wifi_tip_auto))
        TipItem(stringResource(R.string.wifi_tip_dns))
    }

    Spacer(modifier = Modifier.height(80.dp))
}

@Composable
private fun NetworkRiskItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(
            text = "\u2022",
            style = MaterialTheme.typography.bodySmall,
            color = RiskYellow.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary.copy(alpha = 0.9f),
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
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
private fun TipItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TrustGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun getSignalLabel(level: Int): String {
    return when (level) {
        4 -> stringResource(R.string.wifi_signal_excellent)
        3 -> stringResource(R.string.wifi_signal_good)
        2 -> stringResource(R.string.wifi_signal_fair)
        else -> stringResource(R.string.wifi_signal_weak)
    }
}
