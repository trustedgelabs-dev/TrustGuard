package com.trustedgelabs.trustguard.service

import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PacketLog(
    val timestamp: Long,
    val protocol: String,
    val srcAddr: String,
    val srcPort: Int,
    val dstAddr: String,
    val dstPort: Int,
    val size: Int,
    val direction: String // "IN" or "OUT"
)

data class AppTrafficStats(
    val packageName: String,
    val appName: String,
    val txBytes: Long,
    val rxBytes: Long,
    val txPackets: Long,
    val rxPackets: Long,
    val uid: Int
)

class PacketSnifferManager(private val context: Context) {

    private val _packetLogs = MutableStateFlow<List<PacketLog>>(emptyList())
    val packetLogs: StateFlow<List<PacketLog>> = _packetLogs.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val maxLogs = 500

    fun addPacketLog(log: PacketLog) {
        val current = _packetLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > maxLogs) {
            _packetLogs.value = current.take(maxLogs)
        } else {
            _packetLogs.value = current
        }
    }

    fun clearLogs() {
        _packetLogs.value = emptyList()
    }

    fun setCapturing(capturing: Boolean) {
        _isCapturing.value = capturing
    }

    fun getAppTrafficStats(): List<AppTrafficStats> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps
            .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
            .mapNotNull { appInfo ->
                val uid = appInfo.uid
                val txBytes = TrafficStats.getUidTxBytes(uid)
                val rxBytes = TrafficStats.getUidRxBytes(uid)
                val txPackets = TrafficStats.getUidTxPackets(uid)
                val rxPackets = TrafficStats.getUidRxPackets(uid)

                if (txBytes > 0 || rxBytes > 0) {
                    AppTrafficStats(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        txBytes = txBytes,
                        rxBytes = rxBytes,
                        txPackets = txPackets,
                        rxPackets = rxPackets,
                        uid = uid
                    )
                } else null
            }
            .sortedByDescending { it.txBytes + it.rxBytes }
    }

    fun getProtocolName(protocol: Int): String = when (protocol) {
        1 -> "ICMP"
        6 -> "TCP"
        17 -> "UDP"
        else -> "Proto($protocol)"
    }
}
