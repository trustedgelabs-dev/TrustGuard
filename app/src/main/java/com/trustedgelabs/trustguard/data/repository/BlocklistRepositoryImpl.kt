package com.trustedgelabs.trustguard.data.repository

import android.content.Context
import com.trustedgelabs.trustguard.R
import com.trustedgelabs.trustguard.data.datasource.BlocklistDataSource
import com.trustedgelabs.trustguard.data.model.BlocklistInfo

class BlocklistRepositoryImpl(
    private val context: Context,
    private val dataSource: BlocklistDataSource
) : BlocklistRepository {

    private val prefs = context.getSharedPreferences("blocklist_prefs", Context.MODE_PRIVATE)

    @Volatile
    private var blockedDomains: Set<String> = emptySet()

    private val builtInLists = listOf(
        BlocklistDef("adware", R.string.blocklist_ads, R.string.blocklist_ads_desc, "blocklists/adware_domains.txt", false),
        BlocklistDef("trackers", R.string.blocklist_trackers, R.string.blocklist_trackers_desc, "blocklists/tracker_domains.txt", false),
        BlocklistDef("inapp_ads", R.string.blocklist_inapp_ads, R.string.blocklist_inapp_ads_desc, "blocklists/inapp_ad_sdk_domains.txt", false)
    )

    override suspend fun loadBlockedDomains() {
        val allDomains = HashSet<String>()
        for (list in builtInLists) {
            if (isBlocklistEnabled(list.id)) {
                val domains = dataSource.loadDomainsFromAsset(list.assetPath)
                allDomains.addAll(domains)
            }
        }
        blockedDomains = allDomains
    }

    override suspend fun getBlocklists(): List<BlocklistInfo> {
        return builtInLists.map { def ->
            val domains = dataSource.loadDomainsFromAsset(def.assetPath)
            BlocklistInfo(
                id = def.id,
                nameResId = def.nameRes,
                descResId = def.descRes,
                assetPath = def.assetPath,
                isBuiltIn = true,
                isEnabled = isBlocklistEnabled(def.id),
                domainCount = domains.size,
                isPremium = def.isPremium
            )
        }
    }

    override fun toggleBlocklist(id: String, enabled: Boolean) {
        prefs.edit().putBoolean("blocklist_${id}_enabled", enabled).apply()
    }

    override fun isDomainBlocked(domain: String): Boolean {
        val normalized = domain.lowercase().removePrefix("www.")
        // Check domain and all parent domains
        var current = normalized
        while (current.contains('.')) {
            if (current in blockedDomains) return true
            current = current.substringAfter('.')
        }
        return current in blockedDomains
    }

    private fun isBlocklistEnabled(id: String): Boolean {
        // Tum listeler varsayilan olarak aktif
        val default = true
        return prefs.getBoolean("blocklist_${id}_enabled", default)
    }

    private data class BlocklistDef(
        val id: String,
        val nameRes: Int,
        val descRes: Int,
        val assetPath: String,
        val isPremium: Boolean
    )
}
