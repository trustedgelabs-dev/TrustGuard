package com.trustedgelabs.trustguard.data.datasource

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.trustedgelabs.trustguard.data.model.WifiSecurityInfo
import com.trustedgelabs.trustguard.data.model.WifiSecurityLevel

class WifiSecurityDataSource(private val context: Context) {

    fun scanCurrentNetwork(): WifiSecurityInfo? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null

            if (!wifiManager.isWifiEnabled) return null

            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo ?: return null
            if (wifiInfo.networkId == -1) return null

            val ssid = wifiInfo.ssid?.removePrefix("\"")?.removeSuffix("\"") ?: "Unknown"
            if (ssid == "<unknown ssid>" || ssid.isBlank()) return null

            val rssi = wifiInfo.rssi
            @Suppress("DEPRECATION")
            val signalLevel = WifiManager.calculateSignalLevel(rssi, 5)
            val linkSpeed = wifiInfo.linkSpeed
            val frequency = wifiInfo.frequency

            @Suppress("DEPRECATION")
            val dhcpInfo = wifiManager.dhcpInfo
            val gateway = if (dhcpInfo != null) intToIp(dhcpInfo.gateway) else "N/A"
            val ipAddress = if (dhcpInfo != null) intToIp(dhcpInfo.ipAddress) else "N/A"
            val dns = if (dhcpInfo != null) intToIp(dhcpInfo.dns1) else "N/A"

            val securityType = detectSecurityType(wifiManager)
            val securityLevel = when {
                securityType.contains("WPA3", ignoreCase = true) -> WifiSecurityLevel.SECURE
                securityType.contains("WPA2", ignoreCase = true) -> WifiSecurityLevel.SECURE
                securityType.contains("WPA", ignoreCase = true) -> WifiSecurityLevel.WARNING
                securityType.contains("WEP", ignoreCase = true) -> WifiSecurityLevel.DANGER
                securityType == "Unknown" -> WifiSecurityLevel.WARNING
                else -> WifiSecurityLevel.DANGER
            }

            WifiSecurityInfo(
                ssid = ssid,
                securityType = securityType,
                securityLevel = securityLevel,
                signalLevel = signalLevel,
                rssi = rssi,
                linkSpeed = linkSpeed,
                frequency = frequency,
                ipAddress = ipAddress,
                gateway = gateway,
                dns = dns
            )
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun detectSecurityType(wifiManager: WifiManager): String {
        return try {
            val scanResults = wifiManager.scanResults
            val connectedSSID = wifiManager.connectionInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"")

            val matchingNetwork = scanResults?.firstOrNull {
                it.SSID == connectedSSID
            }

            if (matchingNetwork != null) {
                val capabilities = matchingNetwork.capabilities
                when {
                    capabilities.contains("WPA3") -> "WPA3"
                    capabilities.contains("WPA2") -> "WPA2"
                    capabilities.contains("WPA") -> "WPA"
                    capabilities.contains("WEP") -> "WEP"
                    else -> "Open"
                }
            } else {
                "Unknown"
            }
        } catch (_: SecurityException) {
            "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
