package com.trustedgelabs.trustguard.data.model

data class BatteryInfo(
    val percentage: Int,
    val status: String,
    val health: String,
    val temperature: Float,
    val voltage: Float,
    val technology: String,
    val isCharging: Boolean
)

data class BatteryAppUsage(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long
) {
    val usageTimeFormatted: String
        get() {
            val hours = usageTimeMs / (1000 * 60 * 60)
            val minutes = (usageTimeMs / (1000 * 60)) % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}
