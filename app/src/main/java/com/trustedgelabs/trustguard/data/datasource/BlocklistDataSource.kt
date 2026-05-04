package com.trustedgelabs.trustguard.data.datasource

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlocklistDataSource(private val context: Context) {

    suspend fun loadDomainsFromAsset(assetPath: String): Set<String> = withContext(Dispatchers.IO) {
        val domains = HashSet<String>(50000)
        try {
            context.assets.open(assetPath).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith('#')) {
                        // Normalize: remove www. prefix, lowercase
                        val domain = trimmed.lowercase().removePrefix("www.")
                        domains.add(domain)
                    }
                }
            }
        } catch (e: Exception) {
            // Asset not found or read error - return empty set
        }
        domains
    }
}
