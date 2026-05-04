package com.trustedgelabs.trustguard.data.model

data class PermissionInfo(
    val name: String,
    val shortName: String,
    val description: String,
    val riskLevel: RiskLevel,
    val group: String? = null
)
