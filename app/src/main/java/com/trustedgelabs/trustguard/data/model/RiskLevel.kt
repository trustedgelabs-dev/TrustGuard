package com.trustedgelabs.trustguard.data.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.trustedgelabs.trustguard.R

enum class RiskLevel(
    @StringRes val labelRes: Int,
    val score: Int,
    val color: Color
) {
    HIGH(R.string.high_risk, 3, Color(0xFFFF1744)),
    MEDIUM(R.string.medium_risk, 1, Color(0xFFFFD600)),
    LOW(R.string.safe, 0, Color(0xFF00E676)),
    TRUSTED(R.string.trusted, 0, Color(0xFF448AFF))
}
