package com.trustedgelabs.trustguard.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val permissions: List<PermissionInfo>,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val isSystemApp: Boolean,
    val dangerousPermissionCount: Int = 0,
    val totalPermissionCount: Int = 0,
    val adwareSuspicionScore: Int = 0,
    val adwareReasons: List<String> = emptyList()
) {
    val isAdwareSuspect: Boolean get() = adwareSuspicionScore >= 40
}
