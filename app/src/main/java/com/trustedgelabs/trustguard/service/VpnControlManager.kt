package com.trustedgelabs.trustguard.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.trustedgelabs.trustguard.data.model.BlockingStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnControlManager {

    const val ACTION_START = "com.trustedgelabs.trustguard.VPN_START"
    const val ACTION_STOP = "com.trustedgelabs.trustguard.VPN_STOP"

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _blockingStats = MutableStateFlow(BlockingStats())
    val blockingStats: StateFlow<BlockingStats> = _blockingStats.asStateFlow()

    fun setVpnActive(active: Boolean) {
        _isVpnActive.value = active
    }

    fun updateStats(stats: BlockingStats) {
        _blockingStats.value = stats
    }

    fun startVpn(context: Context) {
        val intent = Intent(context, TrustGuardVpnService::class.java).apply {
            action = ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopVpn(context: Context) {
        val intent = Intent(context, TrustGuardVpnService::class.java).apply {
            action = ACTION_STOP
        }
        context.startService(intent)
    }
}
