package com.trustedgelabs.trustguard.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class BloatwareApp(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val isDisabled: Boolean,
    val category: BloatwareCategory,
    val sizeBytes: Long
)

enum class BloatwareCategory {
    CARRIER,      // Carrier pre-installed apps
    MANUFACTURER, // OEM bloatware
    GOOGLE,       // Google apps that can be disabled
    SOCIAL,       // Pre-installed social media
    OTHER         // Other system apps
}

class BloatwareManager(private val context: Context) {

    private val knownBloatware = mapOf(
        // Carrier
        "com.sprint" to BloatwareCategory.CARRIER,
        "com.verizon" to BloatwareCategory.CARRIER,
        "com.att" to BloatwareCategory.CARRIER,
        "com.tmobile" to BloatwareCategory.CARRIER,
        "com.vodafone" to BloatwareCategory.CARRIER,
        "com.turkcell" to BloatwareCategory.CARRIER,
        "tr.com.turkcell" to BloatwareCategory.CARRIER,
        "com.avea" to BloatwareCategory.CARRIER,
        // OEM
        "com.samsung.android.game" to BloatwareCategory.MANUFACTURER,
        "com.samsung.android.voc" to BloatwareCategory.MANUFACTURER,
        "com.samsung.android.app.tips" to BloatwareCategory.MANUFACTURER,
        "com.samsung.android.bixby" to BloatwareCategory.MANUFACTURER,
        "com.samsung.android.ardrawing" to BloatwareCategory.MANUFACTURER,
        "com.miui.analytics" to BloatwareCategory.MANUFACTURER,
        "com.miui.msa.global" to BloatwareCategory.MANUFACTURER,
        "com.xiaomi.glgm" to BloatwareCategory.MANUFACTURER,
        "com.coloros.gamespace" to BloatwareCategory.MANUFACTURER,
        // Google optional
        "com.google.android.apps.magazines" to BloatwareCategory.GOOGLE,
        "com.google.android.apps.tachyon" to BloatwareCategory.GOOGLE,
        "com.google.android.videos" to BloatwareCategory.GOOGLE,
        "com.google.android.music" to BloatwareCategory.GOOGLE,
        "com.google.android.apps.youtube.music" to BloatwareCategory.GOOGLE,
        // Social
        "com.facebook.system" to BloatwareCategory.SOCIAL,
        "com.facebook.appmanager" to BloatwareCategory.SOCIAL,
        "com.facebook.services" to BloatwareCategory.SOCIAL,
        "com.linkedin.android" to BloatwareCategory.SOCIAL,
        "com.netflix.partner.activation" to BloatwareCategory.SOCIAL,
        "com.spotify.music" to BloatwareCategory.SOCIAL
    )

    fun detectBloatware(): List<BloatwareApp> {
        val pm = context.packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return allApps
            .filter { isLikelyBloatware(it) }
            .map { appInfo ->
                val category = detectCategory(appInfo)
                BloatwareApp(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    isDisabled = !appInfo.enabled,
                    category = category,
                    sizeBytes = getAppSize(appInfo)
                )
            }
            .sortedWith(
                compareByDescending<BloatwareApp> { !it.isDisabled }
                    .thenByDescending { it.sizeBytes }
            )
    }

    private fun isLikelyBloatware(appInfo: ApplicationInfo): Boolean {
        val pkg = appInfo.packageName

        // Check known bloatware list
        if (knownBloatware.keys.any { pkg.startsWith(it) }) return true

        // System apps that are not core
        if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            val isUpdated = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
            if (!isUpdated && !isCoreSystemApp(pkg)) {
                return true
            }
        }

        return false
    }

    private fun isCoreSystemApp(packageName: String): Boolean {
        val core = listOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.dialer",
            "com.android.contacts",
            "com.android.mms",
            "com.android.launcher",
            "com.android.providers",
            "com.android.inputmethod",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.server",
            "com.android.shell",
            "com.android.keychain",
            "com.android.packageinstaller",
            "com.android.documentsui",
            "com.android.permissioncontroller",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.ext",
            "com.google.android.permissioncontroller"
        )
        return core.any { packageName.startsWith(it) }
    }

    private fun detectCategory(appInfo: ApplicationInfo): BloatwareCategory {
        val pkg = appInfo.packageName
        for ((prefix, category) in knownBloatware) {
            if (pkg.startsWith(prefix)) return category
        }
        return BloatwareCategory.OTHER
    }

    private fun getAppSize(appInfo: ApplicationInfo): Long {
        return try {
            val sourceDir = appInfo.sourceDir
            if (sourceDir != null) java.io.File(sourceDir).length() else 0
        } catch (_: Exception) { 0 }
    }
}
