package com.trustedgelabs.trustguard.ui.screens.familyshield

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.model.ActivityLogEntry
import com.trustedgelabs.trustguard.data.model.ActivityType
import com.trustedgelabs.trustguard.data.model.ChildProfile
import com.trustedgelabs.trustguard.data.model.DailyUsageSummary
import com.trustedgelabs.trustguard.data.model.FamilyShieldConfig
import com.trustedgelabs.trustguard.service.AppBlockerService
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.ProGold
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import com.trustedgelabs.trustguard.util.FamilyShieldManager
import com.trustedgelabs.trustguard.util.ParentPinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ViewModel
data class FamilyShieldUiState(
    val isLoading: Boolean = true,
    val isPinSetup: Boolean = false,
    val isAuthenticated: Boolean = false,
    val config: FamilyShieldConfig = FamilyShieldConfig(),
    val usageSummary: DailyUsageSummary? = null,
    val recentActivity: List<ActivityLogEntry> = emptyList(),
    val pinError: String? = null,
    val hasUsagePermission: Boolean = false,
    val showForgotPin: Boolean = false,
    val showAddProfile: Boolean = false,
    val showProfileSwitcher: Boolean = false
)

class FamilyShieldViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(FamilyShieldUiState())
    val state: StateFlow<FamilyShieldUiState> = _state.asStateFlow()
    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    init {
        loadState()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000) // Refresh every 10 seconds
                if (_state.value.config.isEnabled && _state.value.isAuthenticated) {
                    refreshUsage()
                }
            }
        }
    }

    private fun refreshUsage() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val usage = withContext(Dispatchers.IO) {
                try { FamilyShieldManager.getTodayUsageSummary(ctx) } catch (_: Exception) { null }
            }
            if (usage != null) {
                _state.value = _state.value.copy(usageSummary = usage)
            }
        }
    }

    fun loadState() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val config = withContext(Dispatchers.IO) { FamilyShieldManager.getConfig(ctx) }
            val isPinSetup = ParentPinManager.isPinSetup(ctx)
            val hasUsagePerm = FamilyShieldManager.hasUsageStatsPermission(ctx)
            val usage = withContext(Dispatchers.IO) {
                try { FamilyShieldManager.getTodayUsageSummary(ctx) } catch (_: Exception) { null }
            }
            val log = withContext(Dispatchers.IO) {
                FamilyShieldManager.getActivityLog(ctx).take(20)
            }
            _state.value = _state.value.copy(
                isLoading = false,
                isPinSetup = isPinSetup,
                config = config,
                usageSummary = usage,
                recentActivity = log,
                hasUsagePermission = hasUsagePerm
            )

            // Safety: ensure AppBlockerService is alive when shield is enabled
            if (config.isEnabled) {
                try { AppBlockerService.start(ctx) } catch (_: Exception) { }
            }
        }
    }

    fun verifyPin(pin: String) {
        val ctx = getApplication<Application>()
        val lockout = ParentPinManager.getLockoutRemainingSeconds(ctx)
        if (lockout > 0) {
            _state.value = _state.value.copy(pinError = "Locked for ${lockout}s")
            return
        }
        if (ParentPinManager.verifyPin(ctx, pin)) {
            _state.value = _state.value.copy(isAuthenticated = true, pinError = null)
            loadState()
        } else {
            val fails = ParentPinManager.getFailedAttempts(ctx)
            _state.value = _state.value.copy(pinError = "Wrong PIN ($fails/5)")
        }
    }

    fun setupPin(pin: String, securityQuestionIndex: Int, securityAnswer: String) {
        val ctx = getApplication<Application>()
        ParentPinManager.setupPin(ctx, pin, securityQuestionIndex, securityAnswer)
        _state.value = _state.value.copy(isPinSetup = true, isAuthenticated = true)
    }

    fun resetPinWithAnswer(answer: String, newPin: String): Boolean {
        val ctx = getApplication<Application>()
        val success = ParentPinManager.resetPinWithSecurityAnswer(ctx, answer, newPin)
        if (success) {
            _state.value = _state.value.copy(isAuthenticated = true, pinError = null, showForgotPin = false)
            loadState()
        }
        return success
    }

    fun showForgotPin() {
        _state.value = _state.value.copy(showForgotPin = true)
    }

    fun hideForgotPin() {
        _state.value = _state.value.copy(showForgotPin = false)
    }

    fun enableShield(childName: String, childAge: Int) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                FamilyShieldManager.enableShield(ctx, childName, childAge)
            }
            // Start blocker service
            AppBlockerService.start(ctx)
            loadState()
        }
    }

    fun disableShield() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FamilyShieldManager.disableShield(ctx) }
            AppBlockerService.stop(ctx)
            loadState()
        }
    }

    fun addProfile(name: String, age: Int) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FamilyShieldManager.addProfile(ctx, name, age) }
            _state.value = _state.value.copy(showAddProfile = false)
            loadState()
        }
    }

    fun switchProfile(profileId: String) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { FamilyShieldManager.switchProfile(ctx, profileId) }
            _state.value = _state.value.copy(showProfileSwitcher = false)
            loadState()
        }
    }

    fun toggleAddProfile() {
        _state.value = _state.value.copy(showAddProfile = !_state.value.showAddProfile)
    }

    fun toggleProfileSwitcher() {
        _state.value = _state.value.copy(showProfileSwitcher = !_state.value.showProfileSwitcher)
    }

    fun clearLog() {
        val ctx = getApplication<Application>()
        FamilyShieldManager.clearActivityLog(ctx)
        _state.value = _state.value.copy(recentActivity = emptyList())
    }
}

// Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyShieldScreen(
    onNavigateBack: () -> Unit,
    onNavigateToParentPanel: () -> Unit,
    viewModel: FamilyShieldViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.fs_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TrustGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = MaterialTheme.colorScheme.onBackground)
        )

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.wifi_scanning), color = TextSecondary)
                }
            }

            // Step 1: PIN not set up
            !state.isPinSetup -> {
                PinSetupContent(onSetupPin = { pin, qIndex, answer ->
                    viewModel.setupPin(pin, qIndex, answer)
                })
            }

            // Forgot PIN flow
            state.showForgotPin -> {
                ForgotPinContent(
                    context = context,
                    onReset = { answer, newPin -> viewModel.resetPinWithAnswer(answer, newPin) },
                    onCancel = { viewModel.hideForgotPin() }
                )
            }

            // Step 2: PIN set but not authenticated
            !state.isAuthenticated -> {
                PinEntryContent(
                    error = state.pinError,
                    onVerify = { viewModel.verifyPin(it) },
                    onForgotPin = { viewModel.showForgotPin() },
                    hasSecurityQuestion = ParentPinManager.hasSecurityQuestion(context)
                )
            }

            // Step 3: Shield not enabled
            !state.config.isEnabled -> {
                ShieldSetupContent(onEnable = { name, age -> viewModel.enableShield(name, age) })
            }

            // Step 4: Active dashboard
            else -> {
                ShieldDashboard(
                    config = state.config,
                    usageSummary = state.usageSummary,
                    recentActivity = state.recentActivity,
                    hasUsagePermission = state.hasUsagePermission,
                    onOpenParentPanel = onNavigateToParentPanel,
                    onDisable = { viewModel.disableShield() },
                    onClearLog = { viewModel.clearLog() },
                    onRequestUsagePermission = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                    onAddProfile = { viewModel.toggleAddProfile() },
                    onSwitchProfile = { viewModel.toggleProfileSwitcher() },
                    onSelectProfile = { viewModel.switchProfile(it) },
                    showAddProfile = state.showAddProfile,
                    showProfileSwitcher = state.showProfileSwitcher,
                    onAddProfileConfirm = { name, age -> viewModel.addProfile(name, age) },
                    onDismissDialogs = {
                        viewModel.toggleAddProfile()
                    },
                    onRefresh = { viewModel.loadState() }
                )
            }
        }
    }
}

// === PIN SETUP (with security question) ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupContent(onSetupPin: (String, Int, String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var securityQuestionIndex by remember { mutableIntStateOf(0) }
    var securityAnswer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val securityQuestions = listOf(
        stringResource(R.string.fs_security_q1),
        stringResource(R.string.fs_security_q2),
        stringResource(R.string.fs_security_q3),
        stringResource(R.string.fs_security_q4),
        stringResource(R.string.fs_security_q5)
    )

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Icon(Icons.Default.Lock, contentDescription = null, tint = ProGold, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.fs_pin_setup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.fs_pin_setup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text(stringResource(R.string.fs_pin_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrustGreen,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                label = { Text(stringResource(R.string.fs_pin_confirm)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrustGreen,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            // Security question
            Text(
                text = stringResource(R.string.fs_security_question_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = securityQuestions[securityQuestionIndex],
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrustGreen,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    securityQuestions.forEachIndexed { index, q ->
                        DropdownMenuItem(
                            text = { Text(q) },
                            onClick = {
                                securityQuestionIndex = index
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = securityAnswer,
                onValueChange = { securityAnswer = it },
                label = { Text(stringResource(R.string.fs_security_answer)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrustGreen,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = RiskRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        pin.length < 4 -> error = "PIN must be at least 4 digits"
                        pin != confirmPin -> error = "PINs don't match"
                        securityAnswer.isBlank() -> error = "Security answer is required"
                        else -> onSetupPin(pin, securityQuestionIndex, securityAnswer)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.fs_pin_set_button), fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// === PIN ENTRY ===
@Composable
private fun PinEntryContent(
    error: String?,
    onVerify: (String) -> Unit,
    onForgotPin: () -> Unit,
    hasSecurityQuestion: Boolean
) {
    var pin by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(Icons.Default.Shield, contentDescription = null, tint = ProGold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.fs_pin_enter_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fs_pin_enter_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text(stringResource(R.string.fs_pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = RiskRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onVerify(pin) },
            enabled = pin.length >= 4,
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.fs_pin_unlock), fontWeight = FontWeight.Bold, color = Color.Black)
        }

        if (hasSecurityQuestion) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onForgotPin) {
                Text(
                    stringResource(R.string.fs_forgot_pin),
                    color = ProGold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// === FORGOT PIN ===
@Composable
private fun ForgotPinContent(
    context: android.content.Context,
    onReset: (String, String) -> Boolean,
    onCancel: () -> Unit
) {
    var answer by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val questionIndex = ParentPinManager.getSecurityQuestionIndex(context)
    val securityQuestions = listOf(
        stringResource(R.string.fs_security_q1),
        stringResource(R.string.fs_security_q2),
        stringResource(R.string.fs_security_q3),
        stringResource(R.string.fs_security_q4),
        stringResource(R.string.fs_security_q5)
    )
    val questionText = if (questionIndex in securityQuestions.indices) securityQuestions[questionIndex] else "?"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = RiskYellow, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.fs_forgot_pin_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fs_forgot_pin_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // Security question
        Text(
            text = questionText,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = ProGold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            label = { Text(stringResource(R.string.fs_security_answer)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = newPin,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
            label = { Text(stringResource(R.string.fs_new_pin)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
            label = { Text(stringResource(R.string.fs_pin_confirm)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = RiskRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    answer.isBlank() -> error = "Enter your security answer"
                    newPin.length < 4 -> error = "PIN must be at least 4 digits"
                    newPin != confirmPin -> error = "PINs don't match"
                    else -> {
                        if (!onReset(answer, newPin)) {
                            error = "Incorrect security answer"
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.fs_reset_pin), fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.back), color = TextSecondary)
        }
    }
}

// === SHIELD SETUP (child name + age) ===
@Composable
private fun ShieldSetupContent(onEnable: (String, Int) -> Unit) {
    var childName by remember { mutableStateOf("") }
    var childAge by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(Icons.Default.ChildCare, contentDescription = null, tint = ProGold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.fs_setup_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fs_setup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = childName,
            onValueChange = { childName = it },
            label = { Text(stringResource(R.string.fs_child_name)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = childAge,
            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) childAge = it },
            label = { Text(stringResource(R.string.fs_child_age)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrustGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onEnable(childName, childAge.toIntOrNull() ?: 0) },
            enabled = childName.isNotBlank() && (childAge.toIntOrNull() ?: 0) > 0,
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.fs_enable_button), fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// === ACTIVE DASHBOARD ===
@Composable
private fun ShieldDashboard(
    config: FamilyShieldConfig,
    usageSummary: DailyUsageSummary?,
    recentActivity: List<ActivityLogEntry>,
    hasUsagePermission: Boolean,
    onOpenParentPanel: () -> Unit,
    onDisable: () -> Unit,
    onClearLog: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onAddProfile: () -> Unit,
    onSwitchProfile: () -> Unit,
    onSelectProfile: (String) -> Unit,
    showAddProfile: Boolean,
    showProfileSwitcher: Boolean,
    onAddProfileConfirm: (String, Int) -> Unit,
    onDismissDialogs: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    // Add profile dialog
    if (showAddProfile) {
        AddProfileDialog(
            onConfirm = onAddProfileConfirm,
            onDismiss = onDismissDialogs
        )
    }

    // Profile switcher dialog
    if (showProfileSwitcher) {
        ProfileSwitcherDialog(
            profiles = config.profiles,
            activeProfileId = config.activeProfileId,
            onSelect = onSelectProfile,
            onDismiss = { onSwitchProfile() }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Status header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TrustGreen.copy(alpha = 0.1f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TrustGreen.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = TrustGreen, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.fs_active_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TrustGreen
                    )
                    Text(
                        text = stringResource(R.string.fs_protecting, config.effectiveName),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Profile selector (if multiple profiles)
        if (config.profiles.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .clickable { onSwitchProfile() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ProGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.fs_active_profile),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            "${config.effectiveName} (${config.effectiveAge})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        stringResource(R.string.fs_switch_profile),
                        style = MaterialTheme.typography.labelSmall,
                        color = ProGold
                    )
                }
            }
        }

        // Add profile button
        item {
            TextButton(
                onClick = onAddProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TrustGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.fs_add_profile), color = TrustGreen)
            }
        }

        // Usage permission warning
        if (!hasUsagePermission) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RiskYellow.copy(alpha = 0.1f))
                        .clickable { onRequestUsagePermission() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RiskYellow, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.fs_usage_permission_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = RiskYellow
                        )
                        Text(
                            stringResource(R.string.fs_usage_permission_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Overlay permission warning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RiskRed.copy(alpha = 0.1f))
                        .clickable {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            )
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RiskRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.fs_overlay_permission_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = RiskRed
                        )
                    }
                }
            }
        }

        // Quick stats
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    icon = Icons.Default.Block,
                    label = stringResource(R.string.fs_blocked_apps),
                    value = "${config.effectiveBlockedApps.size}",
                    color = RiskRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.AccessTime,
                    label = stringResource(R.string.fs_today_usage),
                    value = "${usageSummary?.totalMinutes ?: 0}m",
                    color = if (usageSummary?.isLimitExceeded == true) RiskRed else TrustGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Daily usage progress
        if (usageSummary != null && config.effectiveDailyLimit > 0) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.fs_daily_limit_label), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "${usageSummary.totalMinutes}/${config.effectiveDailyLimit} min",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (usageSummary.isLimitExceeded) RiskRed else TrustGreen
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    val progress = (usageSummary.totalMinutes.toFloat() / config.effectiveDailyLimit).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (usageSummary.isLimitExceeded) RiskRed else TrustGreen,
                        trackColor = TextSecondary.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // Top used apps today
        if (usageSummary != null && usageSummary.appUsages.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.fs_top_apps_today),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(usageSummary.appUsages.take(5), key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text("${app.usageMinutes} min", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TrustGreen)
                }
            }
        }

        // Recent activity log
        if (recentActivity.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.fs_recent_activity),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onClearLog) {
                        Text(stringResource(R.string.fs_clear_log), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(recentActivity.take(10), key = { it.id }) { entry ->
                ActivityLogItem(entry)
            }
        }

        // Action buttons
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenParentPanel,
                colors = ButtonDefaults.buttonColors(containerColor = ProGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.fs_parent_panel), fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        item {
            Button(
                onClick = onDisable,
                colors = ButtonDefaults.buttonColors(containerColor = RiskRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.fs_disable_shield), fontWeight = FontWeight.Bold, color = RiskRed)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

// === DIALOGS ===
@Composable
private fun AddProfileDialog(onConfirm: (String, Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fs_add_profile), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.fs_child_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) age = it },
                    label = { Text(stringResource(R.string.fs_child_age)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, age.toIntOrNull() ?: 0) },
                enabled = name.isNotBlank() && (age.toIntOrNull() ?: 0) > 0
            ) {
                Text(stringResource(R.string.fs_add_profile_confirm), color = TrustGreen)
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
private fun ProfileSwitcherDialog(
    profiles: List<ChildProfile>,
    activeProfileId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fs_switch_profile), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (profile.id == activeProfileId) TrustGreen.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(profile.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (profile.id == activeProfileId) TrustGreen else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                profile.childName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (profile.id == activeProfileId) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${profile.childAge} years",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        if (profile.id == activeProfileId) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TrustGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.back), color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun ActivityLogItem(entry: ActivityLogEntry) {
    val (icon, color) = when (entry.type) {
        ActivityType.APP_INSTALLED -> Icons.Default.NewReleases to RiskYellow
        ActivityType.APP_BLOCKED_ATTEMPT -> Icons.Default.Block to RiskRed
        ActivityType.DAILY_LIMIT_EXCEEDED -> Icons.Default.AccessTime to RiskRed
        ActivityType.ADWARE_DETECTED -> Icons.Default.Warning to RiskRed
        ActivityType.SHIELD_ENABLED -> Icons.Default.CheckCircle to TrustGreen
        ActivityType.SHIELD_DISABLED -> Icons.Default.Warning to RiskYellow
        ActivityType.SETTINGS_CHANGED -> Icons.Default.History to TextSecondary
        ActivityType.PROFILE_ADDED -> Icons.Default.Person to TrustGreen
        ActivityType.PROFILE_SWITCHED -> Icons.Default.Person to ProGold
    }
    val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(entry.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (entry.appName.isNotEmpty()) entry.appName else entry.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (entry.details.isNotEmpty() && entry.appName.isNotEmpty()) {
                Text(entry.details, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
        Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
