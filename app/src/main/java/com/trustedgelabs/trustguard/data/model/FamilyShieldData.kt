package com.trustedgelabs.trustguard.data.model

/**
 * A single child profile within Family Shield.
 */
data class ChildProfile(
    val id: String = System.currentTimeMillis().toString(),
    val childName: String = "",
    val childAge: Int = 0,
    val blockedApps: Set<String> = emptySet(),
    val dailyTimeLimitMinutes: Int = 0,  // 0 = unlimited
    val ageFilterEnabled: Boolean = true,
    val adwareAutoBlockEnabled: Boolean = true,
    val newInstallAlertEnabled: Boolean = true
)

/**
 * Core configuration for Family Shield.
 * Supports multiple child profiles with an active selection.
 */
data class FamilyShieldConfig(
    val isEnabled: Boolean = false,
    val childName: String = "",        // Legacy / active profile name
    val childAge: Int = 0,             // Legacy / active profile age
    val blockedApps: Set<String> = emptySet(),
    val dailyTimeLimitMinutes: Int = 0,
    val ageFilterEnabled: Boolean = true,
    val adwareAutoBlockEnabled: Boolean = true,
    val newInstallAlertEnabled: Boolean = true,
    // Multi-profile support
    val profiles: List<ChildProfile> = emptyList(),
    val activeProfileId: String = ""
) {
    /** Get the currently active profile, or null if none */
    val activeProfile: ChildProfile?
        get() = profiles.find { it.id == activeProfileId }

    /** Effective values — uses active profile if available, else legacy fields */
    val effectiveName: String get() = activeProfile?.childName ?: childName
    val effectiveAge: Int get() = activeProfile?.childAge ?: childAge
    val effectiveBlockedApps: Set<String> get() = activeProfile?.blockedApps ?: blockedApps
    val effectiveDailyLimit: Int get() = activeProfile?.dailyTimeLimitMinutes ?: dailyTimeLimitMinutes
    val effectiveAgeFilter: Boolean get() = activeProfile?.ageFilterEnabled ?: ageFilterEnabled
    val effectiveAdwareBlock: Boolean get() = activeProfile?.adwareAutoBlockEnabled ?: adwareAutoBlockEnabled
    val effectiveInstallAlert: Boolean get() = activeProfile?.newInstallAlertEnabled ?: newInstallAlertEnabled
}

/**
 * A single activity log entry.
 */
data class ActivityLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: ActivityType,
    val packageName: String = "",
    val appName: String = "",
    val details: String = "",
    val profileId: String = ""
)

enum class ActivityType {
    APP_INSTALLED,
    APP_BLOCKED_ATTEMPT,
    DAILY_LIMIT_EXCEEDED,
    ADWARE_DETECTED,
    SHIELD_ENABLED,
    SHIELD_DISABLED,
    SETTINGS_CHANGED,
    PROFILE_ADDED,
    PROFILE_SWITCHED
}

/**
 * App usage entry for a single day.
 */
data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val usageMinutes: Long
)

/**
 * Summary of today's usage for the child.
 */
data class DailyUsageSummary(
    val date: String,
    val totalMinutes: Long,
    val appUsages: List<AppUsageEntry>,
    val limitMinutes: Int,
    val isLimitExceeded: Boolean
)

/**
 * App info for blocking/filtering UI.
 */
data class BlockableAppInfo(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean,
    val isAgeInappropriate: Boolean = false,
    val isAdwareSuspect: Boolean = false
)
