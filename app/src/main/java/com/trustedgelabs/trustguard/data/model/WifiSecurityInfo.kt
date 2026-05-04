package com.trustedgelabs.trustguard.data.model

data class WifiSecurityInfo(
    val ssid: String,
    val securityType: String,
    val securityLevel: WifiSecurityLevel,
    val signalLevel: Int,
    val rssi: Int,
    val linkSpeed: Int,
    val frequency: Int,
    val ipAddress: String,
    val gateway: String,
    val dns: String,
    val isPublicNetwork: Boolean = false
)

enum class WifiSecurityLevel {
    SECURE,     // WPA2/WPA3 — encryption strong, but network risks still exist
    WARNING,    // WPA or weak config
    DANGER      // Open/WEP — no meaningful encryption
}
