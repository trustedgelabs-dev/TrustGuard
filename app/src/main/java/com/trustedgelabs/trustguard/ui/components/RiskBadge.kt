package com.trustedgelabs.trustguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.trustedgelabs.trustguard.data.model.RiskLevel
import com.trustedgelabs.trustguard.ui.theme.RiskGreen
import com.trustedgelabs.trustguard.ui.theme.RiskGreenSurface
import com.trustedgelabs.trustguard.ui.theme.RiskRed
import com.trustedgelabs.trustguard.ui.theme.RiskRedSurface
import com.trustedgelabs.trustguard.ui.theme.RiskYellow
import com.trustedgelabs.trustguard.ui.theme.RiskYellowSurface

private val TrustedBlue = Color(0xFF448AFF)
private val TrustedBlueSurface = Color(0xFF142040)

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (riskLevel) {
        RiskLevel.HIGH -> RiskRedSurface to RiskRed
        RiskLevel.MEDIUM -> RiskYellowSurface to RiskYellow
        RiskLevel.LOW -> RiskGreenSurface to RiskGreen
        RiskLevel.TRUSTED -> TrustedBlueSurface to TrustedBlue
    }

    Text(
        text = stringResource(riskLevel.labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
