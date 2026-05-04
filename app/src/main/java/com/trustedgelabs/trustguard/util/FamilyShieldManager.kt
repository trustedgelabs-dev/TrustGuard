package com.trustedgelabs.trustguard.util

import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Base64
import com.trustedgelabs.trustguard.data.model.ActivityLogEntry
import com.trustedgelabs.trustguard.data.model.ActivityType
import com.trustedgelabs.trustguard.data.model.BlockableAppInfo
import com.trustedgelabs.trustguard.data.model.DailyUsageSummary
import com.trustedgelabs.trustguard.data.model.ChildProfile
import com.trustedgelabs.trustguard.data.model.FamilyShieldConfig
import com.trustedgelabs.trustguard.domain.AdwareDetector
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Core manager for Family Shield feature.
 * Handles config persistence, activity logging, usage tracking, multi-profile.
 * Uses anti-tamper dual SharedPreferences with integrity hashing.
 */
object FamilyShieldManager {

    private const val PREFS_NAME = "tg_fs_config"
    private const val BACKUP_PREFS_NAME = "tg_fs_config_bk"
    private const val LOG_PREFS_NAME = "tg_fs_log"

    // Config keys (obfuscated)
    private const val KEY_ENABLED = "fs_en"
    private const val KEY_CHILD_NAME = "fs_cn"
    private const val KEY_CHILD_AGE = "fs_ca"
    private const val KEY_BLOCKED_APPS = "fs_ba"
    private const val KEY_DAILY_LIMIT = "fs_dl"
    private const val KEY_AGE_FILTER = "fs_af"
    private const val KEY_ADWARE_BLOCK = "fs_ab"
    private const val KEY_INSTALL_ALERT = "fs_ia"
    private const val KEY_INTEGRITY = "fs_chk"
    private const val KEY_PROFILES = "fs_pr"
    private const val KEY_ACTIVE_PROFILE = "fs_ap"

    // Log keys
    private const val KEY_ACTIVITY_LOG = "fs_log"
    private const val KEY_LAST_CLEANUP = "fs_lc"

    private const val LOG_RETENTION_DAYS = 7
    private const val MAX_LOG_ENTRIES = 500

    // Age-inappropriate app categories
    private val ageRestrictedPrefixes = listOf(
        "com.tinder.", "com.bumble.", "com.badoo.",
        "com.grindr.",
        "com.zhiliaoapp.musically",
        "com.reddit.",
        "com.bet365.", "com.draftkings.", "com.fanduel.",
        "com.pokerstars.",
    )

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getBackupPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)

    private fun getLogPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(LOG_PREFS_NAME, Context.MODE_PRIVATE)

    private fun getDeviceFingerprint(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "x"
        } catch (_: Exception) { "x" }
        val installTime = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (_: Exception) { 0L }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$androidId-$installTime-fs_mgr".toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING).take(16)
    }

    private fun computeIntegrity(config: FamilyShieldConfig, context: Context): String {
        val fp = getDeviceFingerprint(context)
        val raw = "$fp|${config.isEnabled}|${config.childName}|${config.childAge}|${config.dailyTimeLimitMinutes}|${config.activeProfileId}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING).take(24)
    }

    // ============================
    // USAGE STATS PERMISSION CHECK
    // ============================

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // ============================
    // CONFIG MANAGEMENT
    // ============================

    fun getConfig(context: Context): FamilyShieldConfig {
        val prefs = getPrefs(context)
        return try {
            val config = readConfigFrom(prefs)
            val integrity = prefs.getString(KEY_INTEGRITY, null)
            if (integrity != null && integrity == computeIntegrity(config, context)) {
                config
            } else {
                val backup = getBackupPrefs(context)
                val bConfig = readConfigFrom(backup)
                val bIntegrity = backup.getString(KEY_INTEGRITY, null)
                if (bIntegrity != null && bIntegrity == computeIntegrity(bConfig, context)) {
                    saveConfig(context, bConfig)
                    bConfig
                } else {
                    FamilyShieldConfig()
                }
            }
        } catch (_: Exception) {
            FamilyShieldConfig()
        }
    }

    fun saveConfig(context: Context, config: FamilyShieldConfig) {
        val integrity = computeIntegrity(config, context)
        val blockedJson = JSONArray(config.blockedApps.toList()).toString()
        val profilesJson = serializeProfiles(config.profiles)

        writeConfigTo(getPrefs(context), config, blockedJson, profilesJson, integrity)
        writeConfigTo(getBackupPrefs(context), config, blockedJson, profilesJson, integrity)
    }

    fun isEnabled(context: Context): Boolean = getConfig(context).isEnabled

    fun enableShield(context: Context, childName: String, childAge: Int) {
        val config = getConfig(context)
        // Create initial profile
        val profile = ChildProfile(
            childName = childName,
            childAge = childAge
        )
        val newConfig = config.copy(
            isEnabled = true,
            childName = childName,
            childAge = childAge,
            profiles = config.profiles + profile,
            activeProfileId = profile.id
        )
        saveConfig(context, newConfig)
        addLogEntry(context, ActivityLogEntry(
            type = ActivityType.SHIELD_ENABLED,
            details = "Family Shield enabled for $childName (age $childAge)",
            profileId = profile.id
        ))
    }

    fun disableShield(context: Context) {
        val config = getConfig(context).copy(isEnabled = false)
        saveConfig(context, config)
        addLogEntry(context, ActivityLogEntry(
            type = ActivityType.SHIELD_DISABLED,
            details = "Family Shield disabled"
        ))
    }

    fun updateDailyLimit(context: Context, minutes: Int) {
        val config = getConfig(context)
        val updated = updateActiveProfile(config) { it.copy(dailyTimeLimitMinutes = minutes) }
        saveConfig(context, updated.copy(dailyTimeLimitMinutes = minutes))
    }

    fun toggleAppBlock(context: Context, packageName: String) {
        val config = getConfig(context)
        val effectiveBlocked = config.effectiveBlockedApps
        val newBlocked = if (packageName in effectiveBlocked) {
            effectiveBlocked - packageName
        } else {
            effectiveBlocked + packageName
        }

        val updated = updateActiveProfile(config) { it.copy(blockedApps = newBlocked) }
        saveConfig(context, updated.copy(blockedApps = newBlocked))
    }

    fun isAppBlocked(context: Context, packageName: String): Boolean {
        return packageName in getConfig(context).effectiveBlockedApps
    }

    fun updateAgeFilter(context: Context, enabled: Boolean) {
        val config = getConfig(context)
        val updated = updateActiveProfile(config) { it.copy(ageFilterEnabled = enabled) }
        saveConfig(context, updated.copy(ageFilterEnabled = enabled))
    }

    fun updateAdwareAutoBlock(context: Context, enabled: Boolean) {
        val config = getConfig(context)
        val updated = updateActiveProfile(config) { it.copy(adwareAutoBlockEnabled = enabled) }
        saveConfig(context, updated.copy(adwareAutoBlockEnabled = enabled))
    }

    fun updateInstallAlert(context: Context, enabled: Boolean) {
        val config = getConfig(context)
        val updated = updateActiveProfile(config) { it.copy(newInstallAlertEnabled = enabled) }
        saveConfig(context, updated.copy(newInstallAlertEnabled = enabled))
    }

    // ============================
    // MULTI-PROFILE MANAGEMENT
    // ============================

    fun addProfile(context: Context, childName: String, childAge: Int): ChildProfile {
        val config = getConfig(context)
        val profile = ChildProfile(childName = childName, childAge = childAge)
        val newConfig = config.copy(
            profiles = config.profiles + profile,
            activeProfileId = profile.id,
            childName = childName,
            childAge = childAge,
            blockedApps = emptySet(),
            dailyTimeLimitMinutes = 0
        )
        saveConfig(context, newConfig)
        addLogEntry(context, ActivityLogEntry(
            type = ActivityType.PROFILE_ADDED,
            details = "Profile added: $childName (age $childAge)",
            profileId = profile.id
        ))
        return profile
    }

    fun switchProfile(context: Context, profileId: String) {
        val config = getConfig(context)
        val profile = config.profiles.find { it.id == profileId } ?: return
        val newConfig = config.copy(
            activeProfileId = profileId,
            childName = profile.childName,
            childAge = profile.childAge,
            blockedApps = profile.blockedApps,
            dailyTimeLimitMinutes = profile.dailyTimeLimitMinutes,
            ageFilterEnabled = profile.ageFilterEnabled,
            adwareAutoBlockEnabled = profile.adwareAutoBlockEnabled,
            newInstallAlertEnabled = profile.newInstallAlertEnabled
        )
        saveConfig(context, newConfig)
        addLogEntry(context, ActivityLogEntry(
            type = ActivityType.PROFILE_SWITCHED,
            details = "Switched to profile: ${profile.childName}",
            profileId = profileId
        ))
    }

    fun removeProfile(context: Context, profileId: String) {
        val config = getConfig(context)
        val newProfiles = config.profiles.filter { it.id != profileId }
        val newActiveId = if (config.activeProfileId == profileId) {
            newProfiles.firstOrNull()?.id ?: ""
        } else config.activeProfileId
        val activeProfile = newProfiles.find { it.id == newActiveId }
        val newConfig = config.copy(
            profiles = newProfiles,
            activeProfileId = newActiveId,
            childName = activeProfile?.childName ?: "",
            childAge = activeProfile?.childAge ?: 0,
            blockedApps = activeProfile?.blockedApps ?: emptySet(),
            dailyTimeLimitMinutes = activeProfile?.dailyTimeLimitMinutes ?: 0,
            isEnabled = newProfiles.isNotEmpty() && config.isEnabled
        )
        saveConfig(context, newConfig)
    }

    private fun updateActiveProfile(config: FamilyShieldConfig, transform: (ChildProfile) -> ChildProfile): FamilyShieldConfig {
        if (config.activeProfileId.isEmpty()) return config
        val updatedProfiles = config.profiles.map {
            if (it.id == config.activeProfileId) transform(it) else it
        }
        return config.copy(profiles = updatedProfiles)
    }

    // ============================
    // SERIALIZATION
    // ============================

    private fun serializeProfiles(profiles: List<ChildProfile>): String {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.childName)
                put("age", p.childAge)
                put("blocked", JSONArray(p.blockedApps.toList()))
                put("limit", p.dailyTimeLimitMinutes)
                put("ageFilter", p.ageFilterEnabled)
                put("adwareBlock", p.adwareAutoBlockEnabled)
                put("installAlert", p.newInstallAlertEnabled)
            })
        }
        return arr.toString()
    }

    private fun deserializeProfiles(json: String): List<ChildProfile> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val blockedArr = obj.optJSONArray("blocked") ?: JSONArray()
                ChildProfile(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    childName = obj.optString("name", ""),
                    childAge = obj.optInt("age", 0),
                    blockedApps = (0 until blockedArr.length()).map { blockedArr.getString(it) }.toSet(),
                    dailyTimeLimitMinutes = obj.optInt("limit", 0),
                    ageFilterEnabled = obj.optBoolean("ageFilter", true),
                    adwareAutoBlockEnabled = obj.optBoolean("adwareBlock", true),
                    newInstallAlertEnabled = obj.optBoolean("installAlert", true)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun readConfigFrom(prefs: SharedPreferences): FamilyShieldConfig {
        val blockedApps = try {
            val json = JSONArray(prefs.getString(KEY_BLOCKED_APPS, "[]") ?: "[]")
            (0 until json.length()).map { json.getString(it) }.toSet()
        } catch (_: Exception) { emptySet() }

        val profiles = deserializeProfiles(prefs.getString(KEY_PROFILES, "[]") ?: "[]")

        return FamilyShieldConfig(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            childName = prefs.getString(KEY_CHILD_NAME, "") ?: "",
            childAge = prefs.getInt(KEY_CHILD_AGE, 0),
            blockedApps = blockedApps,
            dailyTimeLimitMinutes = prefs.getInt(KEY_DAILY_LIMIT, 0),
            ageFilterEnabled = prefs.getBoolean(KEY_AGE_FILTER, true),
            adwareAutoBlockEnabled = prefs.getBoolean(KEY_ADWARE_BLOCK, true),
            newInstallAlertEnabled = prefs.getBoolean(KEY_INSTALL_ALERT, true),
            profiles = profiles,
            activeProfileId = prefs.getString(KEY_ACTIVE_PROFILE, "") ?: ""
        )
    }

    private fun writeConfigTo(
        prefs: SharedPreferences,
        config: FamilyShieldConfig,
        blockedJson: String,
        profilesJson: String,
        integrity: String
    ) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.isEnabled)
            .putString(KEY_CHILD_NAME, config.childName)
            .putInt(KEY_CHILD_AGE, config.childAge)
            .putString(KEY_BLOCKED_APPS, blockedJson)
            .putInt(KEY_DAILY_LIMIT, config.dailyTimeLimitMinutes)
            .putBoolean(KEY_AGE_FILTER, config.ageFilterEnabled)
            .putBoolean(KEY_ADWARE_BLOCK, config.adwareAutoBlockEnabled)
            .putBoolean(KEY_INSTALL_ALERT, config.newInstallAlertEnabled)
            .putString(KEY_PROFILES, profilesJson)
            .putString(KEY_ACTIVE_PROFILE, config.activeProfileId)
            .putString(KEY_INTEGRITY, integrity)
            .apply()
    }

    // ============================
    // ACTIVITY LOG
    // ============================

    fun addLogEntry(context: Context, entry: ActivityLogEntry) {
        val prefs = getLogPrefs(context)
        val log = getActivityLog(context).toMutableList()
        log.add(0, entry)

        while (log.size > MAX_LOG_ENTRIES) log.removeAt(log.lastIndex)

        val json = JSONArray()
        log.forEach { e ->
            json.put(JSONObject().apply {
                put("id", e.id)
                put("ts", e.timestamp)
                put("type", e.type.name)
                put("pkg", e.packageName)
                put("app", e.appName)
                put("det", e.details)
                put("pid", e.profileId)
            })
        }
        prefs.edit().putString(KEY_ACTIVITY_LOG, json.toString()).apply()
        cleanupOldEntries(context)
    }

    fun getActivityLog(context: Context): List<ActivityLogEntry> {
        return try {
            val prefs = getLogPrefs(context)
            val json = JSONArray(prefs.getString(KEY_ACTIVITY_LOG, "[]") ?: "[]")
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                ActivityLogEntry(
                    id = obj.optLong("id", 0L),
                    timestamp = obj.optLong("ts", 0L),
                    type = try { ActivityType.valueOf(obj.getString("type")) } catch (_: Exception) { ActivityType.SETTINGS_CHANGED },
                    packageName = obj.optString("pkg", ""),
                    appName = obj.optString("app", ""),
                    details = obj.optString("det", ""),
                    profileId = obj.optString("pid", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearActivityLog(context: Context) {
        getLogPrefs(context).edit().putString(KEY_ACTIVITY_LOG, "[]").apply()
    }

    private fun cleanupOldEntries(context: Context) {
        val prefs = getLogPrefs(context)
        val lastCleanup = prefs.getLong(KEY_LAST_CLEANUP, 0L)
        val now = System.currentTimeMillis()
        if (now - lastCleanup < TimeUnit.DAYS.toMillis(1)) return

        val cutoff = now - TimeUnit.DAYS.toMillis(LOG_RETENTION_DAYS.toLong())
        val log = getActivityLog(context).filter { it.timestamp > cutoff }

        val json = JSONArray()
        log.forEach { e ->
            json.put(JSONObject().apply {
                put("id", e.id)
                put("ts", e.timestamp)
                put("type", e.type.name)
                put("pkg", e.packageName)
                put("app", e.appName)
                put("det", e.details)
                put("pid", e.profileId)
            })
        }
        prefs.edit()
            .putString(KEY_ACTIVITY_LOG, json.toString())
            .putLong(KEY_LAST_CLEANUP, now)
            .apply()
    }

    // ============================
    // APP USAGE TRACKING
    // Uses our own ScreenTimeTracker instead of unreliable UsageStatsManager
    // ============================

    fun getTodayUsageSummary(context: Context): DailyUsageSummary {
        val config = getConfig(context)
        return ScreenTimeTracker.getTodayUsageSummary(context, config.effectiveDailyLimit)
    }

    // ============================
    // APP LISTING FOR BLOCKING
    // ============================

    fun getBlockableApps(context: Context): List<BlockableAppInfo> {
        val pm = context.packageManager
        val config = getConfig(context)

        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }

        val effectiveBlocked = config.effectiveBlockedApps

        return packages
            .filter { pkg ->
                val appInfo = pkg.applicationInfo ?: return@filter false
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                        pkg.packageName != context.packageName
            }
            .map { pkg ->
                val appName = try {
                    pm.getApplicationLabel(pkg.applicationInfo!!).toString()
                } catch (_: Exception) { pkg.packageName }

                BlockableAppInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    isBlocked = pkg.packageName in effectiveBlocked,
                    isAgeInappropriate = isAgeRestricted(pkg.packageName, config.effectiveAge),
                    isAdwareSuspect = false
                )
            }
            .sortedWith(compareByDescending<BlockableAppInfo> { it.isBlocked }
                .thenByDescending { it.isAgeInappropriate }
                .thenBy { it.appName })
    }

    fun isAgeRestricted(packageName: String, childAge: Int): Boolean {
        if (childAge >= 18) return false
        return ageRestrictedPrefixes.any { packageName.startsWith(it) }
    }

    // ============================
    // NEW INSTALL HANDLING
    // ============================

    fun onNewAppInstalled(context: Context, packageName: String) {
        if (!isEnabled(context)) return
        val config = getConfig(context)

        val pm = context.packageManager
        val appName = try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) { packageName }

        if (config.effectiveInstallAlert) {
            addLogEntry(context, ActivityLogEntry(
                type = ActivityType.APP_INSTALLED,
                packageName = packageName,
                appName = appName,
                details = "New app installed: $appName",
                profileId = config.activeProfileId
            ))
        }

        // Auto-block if age restricted
        if (config.effectiveAgeFilter && isAgeRestricted(packageName, config.effectiveAge)) {
            val newBlocked = config.effectiveBlockedApps + packageName
            val updated = updateActiveProfile(config) { it.copy(blockedApps = newBlocked) }
            saveConfig(context, updated.copy(blockedApps = newBlocked))
            addLogEntry(context, ActivityLogEntry(
                type = ActivityType.APP_BLOCKED_ATTEMPT,
                packageName = packageName,
                appName = appName,
                details = "Auto-blocked: age-inappropriate app",
                profileId = config.activeProfileId
            ))
        }

        // Auto-block if adware suspect
        if (config.effectiveAdwareBlock) {
            try {
                val report = AdwareDetector.analyzeApp(context, packageName)
                if (report != null && report.isSuspicious) {
                    val newBlocked = config.effectiveBlockedApps + packageName
                    val updated = updateActiveProfile(config) { it.copy(blockedApps = newBlocked) }
                    saveConfig(context, updated.copy(blockedApps = newBlocked))
                    addLogEntry(context, ActivityLogEntry(
                        type = ActivityType.ADWARE_DETECTED,
                        packageName = packageName,
                        appName = appName,
                        details = "Auto-blocked: adware suspect (score: ${report.suspicionScore})",
                        profileId = config.activeProfileId
                    ))
                }
            } catch (_: Exception) { /* non-critical */ }
        }
    }
}
