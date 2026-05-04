package com.trustedgelabs.trustguard.ui.screens.familyshield

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.model.BlockableAppInfo
import com.trustedgelabs.trustguard.data.model.FamilyShieldConfig
import com.trustedgelabs.trustguard.util.ParentPinManager
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.ProGold
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import com.trustedgelabs.trustguard.service.AppBlockerService
import com.trustedgelabs.trustguard.service.ScreenTimeService
import com.trustedgelabs.trustguard.util.FamilyShieldManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel
data class ParentPanelUiState(
    val isLoading: Boolean = true,
    val config: FamilyShieldConfig = FamilyShieldConfig(),
    val blockableApps: List<BlockableAppInfo> = emptyList(),
    val showChangePinDialog: Boolean = false,
    val pinChangeMessage: String? = null
)

class ParentPanelViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ParentPanelUiState())
    val state: StateFlow<ParentPanelUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val config = withContext(Dispatchers.IO) { FamilyShieldManager.getConfig(ctx) }
            val apps = withContext(Dispatchers.IO) { FamilyShieldManager.getBlockableApps(ctx) }
            _state.value = ParentPanelUiState(
                isLoading = false,
                config = config,
                blockableApps = apps
            )
        }
    }

    fun toggleAppBlock(packageName: String) {
        val ctx = getApplication<Application>()
        FamilyShieldManager.toggleAppBlock(ctx, packageName)
        val config = FamilyShieldManager.getConfig(ctx)
        val updatedApps = _state.value.blockableApps.map {
            if (it.packageName == packageName) it.copy(isBlocked = packageName in config.blockedApps) else it
        }
        _state.value = _state.value.copy(config = config, blockableApps = updatedApps)
    }

    fun updateDailyLimit(minutes: Int) {
        val ctx = getApplication<Application>()
        FamilyShieldManager.updateDailyLimit(ctx, minutes)
        _state.value = _state.value.copy(config = _state.value.config.copy(dailyTimeLimitMinutes = minutes))
    }

    fun updateAgeFilter(enabled: Boolean) {
        val ctx = getApplication<Application>()
        FamilyShieldManager.updateAgeFilter(ctx, enabled)
        _state.value = _state.value.copy(config = _state.value.config.copy(ageFilterEnabled = enabled))
    }

    fun updateAdwareBlock(enabled: Boolean) {
        val ctx = getApplication<Application>()
        FamilyShieldManager.updateAdwareAutoBlock(ctx, enabled)
        _state.value = _state.value.copy(config = _state.value.config.copy(adwareAutoBlockEnabled = enabled))
    }

    fun updateInstallAlert(enabled: Boolean) {
        val ctx = getApplication<Application>()
        FamilyShieldManager.updateInstallAlert(ctx, enabled)
        _state.value = _state.value.copy(config = _state.value.config.copy(newInstallAlertEnabled = enabled))
    }

    fun showChangePinDialog() {
        _state.value = _state.value.copy(showChangePinDialog = true, pinChangeMessage = null)
    }

    fun hideChangePinDialog() {
        _state.value = _state.value.copy(showChangePinDialog = false, pinChangeMessage = null)
    }

    fun changePin(oldPin: String, newPin: String) {
        val ctx = getApplication<Application>()
        val success = ParentPinManager.changePin(ctx, oldPin, newPin)
        _state.value = if (success) {
            _state.value.copy(showChangePinDialog = false, pinChangeMessage = null)
        } else {
            _state.value.copy(pinChangeMessage = "Wrong current PIN")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPanelScreen(
    onNavigateBack: () -> Unit,
    viewModel: ParentPanelViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.fs_parent_panel), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TrustGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = MaterialTheme.colorScheme.onBackground)
        )

        // Change PIN dialog
        if (state.showChangePinDialog) {
            ChangePinDialog(
                message = state.pinChangeMessage,
                onConfirm = { old, new -> viewModel.changePin(old, new) },
                onDismiss = { viewModel.hideChangePinDialog() }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // === CHANGE PIN ===
            item {
                SectionTitle(stringResource(R.string.fs_pin_section), Icons.Default.Lock)
            }
            item {
                Button(
                    onClick = { viewModel.showChangePinDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = ProGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.fs_change_pin), fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // === DAILY TIME LIMIT ===
            item {
                SectionTitle(stringResource(R.string.fs_daily_limit_section), Icons.Default.AccessTime)
            }
            item {
                DailyLimitSlider(
                    currentMinutes = state.config.dailyTimeLimitMinutes,
                    onUpdate = { viewModel.updateDailyLimit(it) }
                )
            }

            // === PROTECTION TOGGLES ===
            item {
                SectionTitle(stringResource(R.string.fs_protection_section), Icons.Default.ChildCare)
            }
            item {
                ToggleRow(
                    label = stringResource(R.string.fs_age_filter),
                    description = stringResource(R.string.fs_age_filter_desc),
                    checked = state.config.ageFilterEnabled,
                    onToggle = { viewModel.updateAgeFilter(it) }
                )
            }
            item {
                ToggleRow(
                    label = stringResource(R.string.fs_adware_autoblock),
                    description = stringResource(R.string.fs_adware_autoblock_desc),
                    checked = state.config.adwareAutoBlockEnabled,
                    onToggle = { viewModel.updateAdwareBlock(it) }
                )
            }
            item {
                ToggleRow(
                    label = stringResource(R.string.fs_install_alerts),
                    description = stringResource(R.string.fs_install_alerts_desc),
                    checked = state.config.newInstallAlertEnabled,
                    onToggle = { viewModel.updateInstallAlert(it) }
                )
            }

            // === APP BLOCKING LIST ===
            item {
                SectionTitle(stringResource(R.string.fs_blocked_apps_section), Icons.Default.Block)
            }

            if (state.blockableApps.isEmpty() && !state.isLoading) {
                item {
                    Text(stringResource(R.string.fs_no_apps), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            items(state.blockableApps, key = { it.packageName }) { app ->
                AppBlockRow(
                    app = app,
                    onToggle = { viewModel.toggleAppBlock(app.packageName) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, contentDescription = null, tint = ProGold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun DailyLimitSlider(currentMinutes: Int, onUpdate: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sliderValue by remember(currentMinutes) { mutableFloatStateOf(currentMinutes.toFloat()) }
    val displayValue = sliderValue.toInt()
    val displayText = if (displayValue == 0) stringResource(R.string.fs_unlimited)
    else "$displayValue ${stringResource(R.string.fs_minutes)}"
    val isTimerActive = remember { mutableStateOf(ScreenTimeService.isActive(context)) }
    val remainingMin = remember { mutableStateOf(ScreenTimeService.getRemainingMinutes(context)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.fs_daily_limit_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(displayText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TrustGreen)
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 0f..480f,
            steps = 15,
            colors = SliderDefaults.colors(
                thumbColor = TrustGreen,
                activeTrackColor = TrustGreen,
                inactiveTrackColor = TextSecondary.copy(alpha = 0.2f)
            )
        )

        // Timer status
        if (isTimerActive.value && remainingMin.value > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.fs_timer_active, remainingMin.value),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TrustGreen
            )
        }

        Spacer(Modifier.height(12.dp))

        // Apply button
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val mins = sliderValue.toInt()
                    onUpdate(mins)
                    if (mins > 0) {
                        ScreenTimeService.start(context, mins)
                        isTimerActive.value = true
                        remainingMin.value = mins
                    } else {
                        ScreenTimeService.stop(context)
                        isTimerActive.value = false
                    }
                    // Safety: always ensure AppBlockerService stays alive
                    try { AppBlockerService.start(context) } catch (_: Exception) { }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.fs_apply_timer), fontWeight = FontWeight.Bold, color = Color.Black)
            }

            if (isTimerActive.value) {
                Button(
                    onClick = {
                        ScreenTimeService.stop(context)
                        isTimerActive.value = false
                        remainingMin.value = 0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.fs_stop_timer), fontWeight = FontWeight.Bold, color = RiskRed)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.fs_daily_limit_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ToggleRow(label: String, description: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.labelSmall, color = TextSecondary, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TrustGreen,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = TextSecondary.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun ChangePinDialog(message: String?, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fs_change_pin), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) oldPin = it },
                    label = { Text(stringResource(R.string.fs_current_pin)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustGreen),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text(stringResource(R.string.fs_new_pin)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustGreen),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text(stringResource(R.string.fs_pin_confirm)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrustGreen),
                    modifier = Modifier.fillMaxWidth()
                )
                if (message != null || error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(message ?: error ?: "", color = RiskRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        oldPin.length < 4 -> error = "Enter current PIN"
                        newPin.length < 4 -> error = "New PIN must be at least 4 digits"
                        newPin != confirmPin -> error = "PINs don't match"
                        else -> {
                            error = null
                            onConfirm(oldPin, newPin)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.fs_change_pin), color = TrustGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.back), color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
private fun AppBlockRow(app: BlockableAppInfo, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.appName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (app.isAgeInappropriate) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RiskYellow, modifier = Modifier.size(14.dp))
                }
                if (app.isAdwareSuspect) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = RiskRed, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = app.isBlocked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RiskRed,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = TextSecondary.copy(alpha = 0.2f)
            )
        )
    }
}
