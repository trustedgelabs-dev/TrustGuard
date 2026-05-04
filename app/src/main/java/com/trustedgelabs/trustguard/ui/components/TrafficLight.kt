package com.trustedgelabs.trustguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.ui.theme.RiskGreen
import com.trustedgelabs.trustguard.ui.theme.RiskGreenSurface
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskRedSurface
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.RiskYellowSurface
import com.trustedgelabs.trustguard.ui.theme.TextSecondary

@Composable
fun TrafficLightRow(
    redCount: Int,
    yellowCount: Int,
    greenCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrafficLightCard(
            count = redCount,
            label = stringResource(R.string.high_risk),
            color = RiskRed,
            bgColor = RiskRedSurface,
            modifier = Modifier.weight(1f)
        )
        TrafficLightCard(
            count = yellowCount,
            label = stringResource(R.string.medium_risk),
            color = RiskYellow,
            bgColor = RiskYellowSurface,
            modifier = Modifier.weight(1f)
        )
        TrafficLightCard(
            count = greenCount,
            label = stringResource(R.string.safe),
            color = RiskGreen,
            bgColor = RiskGreenSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrafficLightCard(
    count: Int,
    label: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
