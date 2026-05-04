package com.trustedgelabs.trustguard.data.model

data class AppIntegrityInfo(
    val packageName: String,
    val appName: String,
    val installSource: InstallSource,
    val isSystemApp: Boolean,
    val versionName: String
)

enum class InstallSource {
    PLAY_STORE,
    OTHER_STORE,
    SIDELOADED,
    SYSTEM,
    UNKNOWN
}
