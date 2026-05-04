package com.trustedgelabs.trustguard.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.ui.theme.DarkBackground
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.TextSecondary
import com.trustedgelabs.trustguard.ui.theme.TrustGreen
import com.trustedgelabs.trustguard.ui.theme.TrustGuardTheme

/**
 * Full-screen activity shown when a blocked app is launched.
 * Prevents the child from using the blocked app.
 */
class BlockedAppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra("blocked_app_name") ?: "App"
        val isScreenTimeLimit = intent.getBooleanExtra("is_screen_time_limit", false)

        setContent {
            TrustGuardTheme {
                BlockedScreen(
                    appName = appName,
                    isScreenTimeLimit = isScreenTimeLimit,
                    onGoBack = { goToHomeScreen() }
                )
            }
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Navigate to home instead of back to blocked app
        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
private fun BlockedScreen(appName: String, isScreenTimeLimit: Boolean = false, onGoBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isScreenTimeLimit) Icons.Default.AccessTime else Icons.Default.Block,
            contentDescription = null,
            tint = RiskRed,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isScreenTimeLimit) stringResource(R.string.fs_screen_time_exceeded_title)
                   else stringResource(R.string.fs_blocked_app_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = RiskRed,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isScreenTimeLimit) stringResource(R.string.fs_screen_time_exceeded_message)
                   else stringResource(R.string.fs_blocked_app_message, appName),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isScreenTimeLimit) stringResource(R.string.fs_screen_time_exceeded_hint)
                   else stringResource(R.string.fs_blocked_app_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            tint = TrustGreen.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.fs_blocked_by_family_shield),
            style = MaterialTheme.typography.labelMedium,
            color = TrustGreen.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                // Go to home screen
                onGoBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = TrustGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                stringResource(R.string.fs_go_home),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
