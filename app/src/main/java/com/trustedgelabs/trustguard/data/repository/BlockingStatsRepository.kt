package com.trustedgelabs.trustguard.data.repository

import android.content.Context
import com.trustedgelabs.trustguard.data.model.BlockingStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlockingStatsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("blocking_stats", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _stats = MutableStateFlow(loadStats())
    val stats: StateFlow<BlockingStats> = _stats.asStateFlow()

    fun recordQuery() {
        checkDateReset()
        val current = prefs.getInt("queries_today", 0) + 1
        prefs.edit().putInt("queries_today", current).apply()
        updateFlow()
    }

    fun recordBlock(domain: String) {
        checkDateReset()
        val blockedToday = prefs.getInt("blocked_today", 0) + 1
        val blockedAllTime = prefs.getInt("blocked_all_time", 0) + 1
        prefs.edit()
            .putInt("blocked_today", blockedToday)
            .putInt("blocked_all_time", blockedAllTime)
            .apply()

        // Update top blocked domains
        updateTopDomains(domain)
        updateFlow()
    }

    private fun checkDateReset() {
        val today = dateFormat.format(Date())
        val lastDate = prefs.getString("last_date", "") ?: ""
        if (today != lastDate) {
            prefs.edit()
                .putString("last_date", today)
                .putInt("blocked_today", 0)
                .putInt("queries_today", 0)
                .putString("top_domains", "{}")
                .apply()
        }
    }

    private fun updateTopDomains(domain: String) {
        try {
            val json = JSONObject(prefs.getString("top_domains", "{}") ?: "{}")
            val count = json.optInt(domain, 0) + 1
            json.put(domain, count)

            // Keep only top 20
            if (json.length() > 20) {
                val entries = mutableListOf<Pair<String, Int>>()
                json.keys().forEach { key ->
                    entries.add(key to json.getInt(key))
                }
                entries.sortByDescending { it.second }
                val trimmed = JSONObject()
                entries.take(20).forEach { (k, v) -> trimmed.put(k, v) }
                prefs.edit().putString("top_domains", trimmed.toString()).apply()
            } else {
                prefs.edit().putString("top_domains", json.toString()).apply()
            }
        } catch (_: Exception) {
        }
    }

    private fun loadStats(): BlockingStats {
        checkDateReset()
        return BlockingStats(
            totalBlockedToday = prefs.getInt("blocked_today", 0),
            totalQueriesToday = prefs.getInt("queries_today", 0),
            totalBlockedAllTime = prefs.getInt("blocked_all_time", 0),
            topBlockedDomains = loadTopDomains()
        )
    }

    private fun loadTopDomains(): List<Pair<String, Int>> {
        return try {
            val json = JSONObject(prefs.getString("top_domains", "{}") ?: "{}")
            val list = mutableListOf<Pair<String, Int>>()
            json.keys().forEach { key ->
                list.add(key to json.getInt(key))
            }
            list.sortedByDescending { it.second }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun updateFlow() {
        _stats.value = BlockingStats(
            totalBlockedToday = prefs.getInt("blocked_today", 0),
            totalQueriesToday = prefs.getInt("queries_today", 0),
            totalBlockedAllTime = prefs.getInt("blocked_all_time", 0),
            topBlockedDomains = loadTopDomains()
        )
    }
}
