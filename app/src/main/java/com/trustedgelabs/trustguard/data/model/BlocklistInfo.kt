package com.trustedgelabs.trustguard.data.model

data class BlocklistInfo(
    val id: String,
    val nameResId: Int,
    val descResId: Int,
    val assetPath: String?,
    val isBuiltIn: Boolean,
    val isEnabled: Boolean,
    val domainCount: Int,
    val isPremium: Boolean
)
