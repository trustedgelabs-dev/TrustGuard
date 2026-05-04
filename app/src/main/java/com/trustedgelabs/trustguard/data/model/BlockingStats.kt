package com.trustedgelabs.trustguard.data.model

data class BlockingStats(
    val totalBlockedToday: Int = 0,
    val totalQueriesToday: Int = 0,
    val totalBlockedAllTime: Int = 0,
    val topBlockedDomains: List<Pair<String, Int>> = emptyList(),
    val blockingEnabled: Boolean = false
)
