package com.trustedgelabs.trustguard.data.repository

import com.trustedgelabs.trustguard.data.model.BlocklistInfo

interface BlocklistRepository {
    suspend fun loadBlockedDomains()
    suspend fun getBlocklists(): List<BlocklistInfo>
    fun toggleBlocklist(id: String, enabled: Boolean)
    fun isDomainBlocked(domain: String): Boolean
}
