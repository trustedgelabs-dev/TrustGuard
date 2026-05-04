package com.trustedgelabs.trustguard.ui.screens.integrity

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.trustedgelabs.trustguard.data.datasource.AppIntegrityDataSource
import com.trustedgelabs.trustguard.data.model.AppIntegrityInfo
import com.trustedgelabs.trustguard.data.model.InstallSource
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.DarkCard
import com.trustedgelabs.trustguard.ui.theme.IntegrityIndigo
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

class AppIntegrityViewModel(application: Application) : AndroidViewModel(application) {
    private val dataSource = AppIntegrityDataSource(application)

    private val _apps = MutableStateFlow<List<AppIntegrityInfo>>(emptyList())
    val apps: StateFlow<List<AppIntegrityInfo>> = _apps.asStateFlow()

    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val results = withContext(Dispatchers.IO) { dataSource.scanAllApps() }
                _apps.value = results
            } catch (_: Exception) {
                _apps.value = emptyList()
            }
            _isScanning.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIntegrityScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppIntegrityViewModel = viewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.integrity_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TrustGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground, titleContentColor = MaterialTheme.colorScheme.onBackground)
        )

        if (isScanning) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.integrity_scanning), color = TextSecondary)
            }
        } else {
            val sideloadedCount = apps.count { it.installSource == InstallSource.SIDELOADED || it.installSource == InstallSource.UNKNOWN }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary card
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkCard)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val summaryColor = if (sideloadedCount == 0) RiskGreen else RiskYellow
                        val summaryIcon = if (sideloadedCount == 0) Icons.Default.CheckCircle else Icons.Default.Warning

                        Icon(
                            imageVector = summaryIcon,
                            contentDescription = null,
                            tint = summaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (sideloadedCount == 0)
                                stringResource(R.string.integrity_all_safe)
                            else
                                stringResource(R.string.integrity_sideloaded_count, sideloadedCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = summaryColor
                        )
                        if (sideloadedCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.integrity_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // App list
                items(apps.filter { !it.isSystemApp }) { app ->
                    IntegrityAppItem(app = app)
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun IntegrityAppItem(app: AppIntegrityInfo) {
    val (sourceText, sourceColor) = when (app.installSource) {
        InstallSource.PLAY_STORE -> stringResource(R.string.integrity_play_store) to RiskGreen
        InstallSource.OTHER_STORE -> "Store" to RiskGreen
        InstallSource.SIDELOADED -> stringResource(R.string.integrity_sideloaded) to RiskYellow
        InstallSource.SYSTEM -> stringResource(R.string.integrity_system) to IntegrityIndigo
        InstallSource.UNKNOWN -> stringResource(R.string.integrity_unknown_source) to RiskRed
    }

    val sourceIcon = when (app.installSource) {
        InstallSource.PLAY_STORE, InstallSource.OTHER_STORE -> Icons.Default.VerifiedUser
        InstallSource.SYSTEM -> Icons.Default.Shield
        InstallSource.SIDELOADED -> Icons.Default.Warning
        InstallSource.UNKNOWN -> Icons.Default.Info
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(sourceColor.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = sourceIcon,
                contentDescription = null,
                tint = sourceColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Text(
            text = sourceText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = sourceColor
        )
    }
}
