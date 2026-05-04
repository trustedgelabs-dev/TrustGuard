package com.trustedgelabs.trustguard.data.model

import android.net.Uri

/**
 * Represents a file found during optimization scans.
 */
data class CleanableFile(
    val path: String,
    val name: String,
    val size: Long,
    val category: CleanCategory,
    val uri: Uri? = null,
    val packageName: String? = null
)

enum class CleanCategory {
    SYSTEM_CACHE,       // System-level cache
    APP_CACHE,          // Per-app cache
    TEMP_FILES,         // .tmp, .temp, .log files
    APK_FILES,          // Downloaded APK installers
    THUMBNAILS,         // .thumbnails directories
    EMPTY_FOLDERS,      // Empty directories
    LARGE_FILE,         // Large files (>100MB)
    DUPLICATE_PHOTO,    // Duplicate images
    EMAIL_CACHE         // Email app cache
}

/**
 * Result of a junk scan for a specific category.
 */
data class JunkScanResult(
    val category: CleanCategory,
    val files: List<CleanableFile>,
    val totalSize: Long
)

/**
 * Pair of duplicate photos.
 */
data class DuplicateGroup(
    val hash: String,
    val files: List<CleanableFile>
) {
    val wastedSize: Long get() = files.drop(1).sumOf { it.size }
}
