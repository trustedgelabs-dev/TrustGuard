package com.trustedgelabs.trustguard.data.datasource

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.trustedgelabs.trustguard.data.model.CleanCategory
import com.trustedgelabs.trustguard.data.model.CleanableFile
import com.trustedgelabs.trustguard.data.model.DuplicateGroup
import com.trustedgelabs.trustguard.data.model.JunkScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class OptimizationDataSource(private val context: Context) {

    var onScanProgress: ((String, Int) -> Unit)? = null

    // ═══════════════════════════════════════════════════════════════════════
    // 1. SYSTEM JUNK / TEMP FILE SCANNER
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun scanSystemJunk(): List<JunkScanResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<JunkScanResult>()

        // Temp files
        onScanProgress?.invoke("Temp Files", 0)
        val tempFiles = scanTempFiles()
        if (tempFiles.isNotEmpty()) {
            results.add(JunkScanResult(CleanCategory.TEMP_FILES, tempFiles, tempFiles.sumOf { it.size }))
        }

        // APK files
        onScanProgress?.invoke("APK Files", results.sumOf { it.files.size })
        val apkFiles = scanApkFiles()
        if (apkFiles.isNotEmpty()) {
            results.add(JunkScanResult(CleanCategory.APK_FILES, apkFiles, apkFiles.sumOf { it.size }))
        }

        // Thumbnail caches
        onScanProgress?.invoke("Thumbnails", results.sumOf { it.files.size })
        val thumbFiles = scanThumbnailCaches()
        if (thumbFiles.isNotEmpty()) {
            results.add(JunkScanResult(CleanCategory.THUMBNAILS, thumbFiles, thumbFiles.sumOf { it.size }))
        }

        // System cache (accessible directories)
        onScanProgress?.invoke("System Cache", results.sumOf { it.files.size })
        val systemCache = scanSystemCache()
        if (systemCache.isNotEmpty()) {
            results.add(JunkScanResult(CleanCategory.SYSTEM_CACHE, systemCache, systemCache.sumOf { it.size }))
        }

        // Empty folders
        onScanProgress?.invoke("Empty Folders", results.sumOf { it.files.size })
        val emptyDirs = scanEmptyFolders()
        if (emptyDirs.isNotEmpty()) {
            results.add(JunkScanResult(CleanCategory.EMPTY_FOLDERS, emptyDirs, 0))
        }

        onScanProgress?.invoke("Complete", results.sumOf { it.files.size })
        results
    }

    private fun scanTempFiles(): List<CleanableFile> {
        val files = mutableListOf<CleanableFile>()
        val ext = Environment.getExternalStorageDirectory()
        val tempExtensions = setOf("tmp", "temp", "log", "bak", "old", "dmp", "crash")

        val dirsToScan = listOf(
            ext,
            File(ext, "Download"),
            File(ext, "Documents"),
            File(ext, "Android/data"),
        )

        for (dir in dirsToScan) {
            scanForExtensions(dir, tempExtensions, files, maxDepth = 3, category = CleanCategory.TEMP_FILES)
        }
        return files
    }

    private fun scanApkFiles(): List<CleanableFile> {
        val files = mutableListOf<CleanableFile>()
        val ext = Environment.getExternalStorageDirectory()

        val dirsToScan = listOf(
            File(ext, "Download"),
            File(ext, "Downloads"),
            ext,
            File(ext, "Documents"),
        )

        for (dir in dirsToScan) {
            scanForExtensions(dir, setOf("apk", "xapk", "apks"), files, maxDepth = 2, category = CleanCategory.APK_FILES)
        }
        return files
    }

    private fun scanThumbnailCaches(): List<CleanableFile> {
        val files = mutableListOf<CleanableFile>()
        val ext = Environment.getExternalStorageDirectory()

        val thumbDirs = listOf(
            File(ext, "DCIM/.thumbnails"),
            File(ext, "Pictures/.thumbnails"),
            File(ext, ".thumbnails"),
        )

        for (dir in thumbDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        files.add(CleanableFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            category = CleanCategory.THUMBNAILS
                        ))
                    }
                }
            }
        }
        return files
    }

    private fun scanSystemCache(): List<CleanableFile> {
        val files = mutableListOf<CleanableFile>()
        val ext = Environment.getExternalStorageDirectory()

        // Browser caches and download temp files
        val cacheDirs = listOf(
            File(ext, "Android/data/com.android.browser/cache"),
            File(ext, "Android/data/com.android.chrome/cache"),
            File(ext, "Android/data/com.google.android.gms/cache"),
        )

        for (dir in cacheDirs) {
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryForJunk(dir, files, maxDepth = 2, category = CleanCategory.SYSTEM_CACHE)
            }
        }
        return files
    }

    private fun scanEmptyFolders(): List<CleanableFile> {
        val files = mutableListOf<CleanableFile>()
        val ext = Environment.getExternalStorageDirectory()

        // Only scan user-facing directories
        val dirsToCheck = listOf("Download", "Downloads", "Documents", "Pictures", "Music", "Movies", "DCIM")
        for (dirName in dirsToCheck) {
            val dir = File(ext, dirName)
            if (dir.exists()) {
                findEmptyDirs(dir, files, maxDepth = 3)
            }
        }
        return files
    }

    private fun findEmptyDirs(dir: File, out: MutableList<CleanableFile>, maxDepth: Int, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            val children = dir.listFiles() ?: return
            if (children.isEmpty() && depth > 0) {
                out.add(CleanableFile(
                    path = dir.absolutePath,
                    name = dir.name,
                    size = 0,
                    category = CleanCategory.EMPTY_FOLDERS
                ))
            } else {
                children.filter { it.isDirectory }.forEach { findEmptyDirs(it, out, maxDepth, depth + 1) }
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. DUPLICATE PHOTO FINDER
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun scanDuplicatePhotos(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val photosBySize = mutableMapOf<Long, MutableList<File>>()
        val ext = Environment.getExternalStorageDirectory()

        val photoDirs = listOf(
            File(ext, "DCIM"),
            File(ext, "Pictures"),
            File(ext, "Download"),
        )

        val imageExts = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")

        // Phase 1: Group by size (fast pre-filter)
        onScanProgress?.invoke("Indexing photos...", 0)
        for (dir in photoDirs) {
            indexPhotosBySize(dir, imageExts, photosBySize, maxDepth = 4)
        }

        // Phase 2: Only check files with same size (potential duplicates)
        val candidates = photosBySize.filter { it.value.size > 1 }
        onScanProgress?.invoke("Comparing ${candidates.size} groups...", 0)

        val duplicates = mutableListOf<DuplicateGroup>()
        var processed = 0

        for ((_, sameSize) in candidates) {
            val hashGroups = mutableMapOf<String, MutableList<File>>()
            for (file in sameSize) {
                try {
                    val hash = quickFileHash(file)
                    hashGroups.getOrPut(hash) { mutableListOf() }.add(file)
                } catch (_: Exception) {}
            }

            for ((hash, group) in hashGroups) {
                if (group.size > 1) {
                    val cleanables = group.map { file ->
                        CleanableFile(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            category = CleanCategory.DUPLICATE_PHOTO,
                            uri = Uri.fromFile(file)
                        )
                    }
                    duplicates.add(DuplicateGroup(hash, cleanables))
                }
            }

            processed++
            if (processed % 10 == 0) {
                onScanProgress?.invoke("Comparing photos...", duplicates.size)
            }
        }

        onScanProgress?.invoke("Complete", duplicates.size)
        duplicates
    }

    private fun indexPhotosBySize(dir: File, exts: Set<String>, map: MutableMap<Long, MutableList<File>>, maxDepth: Int, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() in exts && file.length() > 10_240) {
                    map.getOrPut(file.length()) { mutableListOf() }.add(file)
                } else if (file.isDirectory && !file.name.startsWith(".") && depth < maxDepth - 1) {
                    indexPhotosBySize(file, exts, map, maxDepth, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Quick hash: reads first 8KB + last 8KB + file size for fast comparison.
     */
    private fun quickFileHash(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)

        FileInputStream(file).use { fis ->
            // Read first 8KB
            val firstRead = fis.read(buffer)
            if (firstRead > 0) digest.update(buffer, 0, firstRead)

            // Read last 8KB if file is large enough
            if (file.length() > 16384) {
                fis.channel.position(file.length() - 8192)
                val lastRead = fis.read(buffer)
                if (lastRead > 0) digest.update(buffer, 0, lastRead)
            }
        }

        // Include file size in hash
        digest.update(file.length().toString().toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. LARGE FILE FINDER
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun scanLargeFiles(minSizeMb: Long = 100): List<CleanableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<CleanableFile>()
        val minBytes = minSizeMb * 1024 * 1024
        val ext = Environment.getExternalStorageDirectory()

        onScanProgress?.invoke("Scanning large files...", 0)
        findLargeFiles(ext, files, minBytes, maxDepth = 5)

        onScanProgress?.invoke("Complete", files.size)
        files.sortedByDescending { it.size }
    }

    private fun findLargeFiles(dir: File, out: MutableList<CleanableFile>, minSize: Long, maxDepth: Int, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() >= minSize) {
                    out.add(CleanableFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = CleanCategory.LARGE_FILE,
                        uri = Uri.fromFile(file)
                    ))
                    if (out.size % 5 == 0) {
                        onScanProgress?.invoke("Scanning...", out.size)
                    }
                } else if (file.isDirectory && !file.name.startsWith(".") &&
                    file.name != "Android" && depth < maxDepth - 1) {
                    findLargeFiles(file, out, minSize, maxDepth, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. APP CACHE CLEANER
    // ═══════════════════════════════════════════════════════════════════════

    data class AppCacheInfo(
        val packageName: String,
        val appName: String,
        val cacheSize: Long
    )

    suspend fun scanAppCaches(): List<AppCacheInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<AppCacheInfo>()
        val pm = context.packageManager

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            onScanProgress?.invoke("Scanning app caches...", 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val uuid = StorageManager.UUID_DEFAULT

                for ((index, appInfo) in packages.withIndex()) {
                    try {
                        val stats = storageStatsManager.queryStatsForPackage(
                            uuid, appInfo.packageName, android.os.Process.myUserHandle()
                        )
                        val cacheSize = stats.cacheBytes
                        if (cacheSize > 1_048_576) { // > 1MB cache
                            results.add(AppCacheInfo(
                                packageName = appInfo.packageName,
                                appName = appInfo.loadLabel(pm).toString(),
                                cacheSize = cacheSize
                            ))
                        }
                    } catch (_: Exception) {}

                    if (index % 20 == 0) {
                        onScanProgress?.invoke("Scanning apps...", results.size)
                    }
                }
            } else {
                // Fallback: estimate from external cache dirs
                val ext = Environment.getExternalStorageDirectory()
                val dataDir = File(ext, "Android/data")
                if (dataDir.exists()) {
                    dataDir.listFiles()?.forEach { appDir ->
                        val cacheDir = File(appDir, "cache")
                        if (cacheDir.exists() && cacheDir.isDirectory) {
                            val size = dirSize(cacheDir)
                            if (size > 1_048_576) {
                                val appName = try {
                                    pm.getApplicationLabel(
                                        pm.getApplicationInfo(appDir.name, 0)
                                    ).toString()
                                } catch (_: Exception) { appDir.name }

                                results.add(AppCacheInfo(
                                    packageName = appDir.name,
                                    appName = appName,
                                    cacheSize = size
                                ))
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        onScanProgress?.invoke("Complete", results.size)
        results.sortedByDescending { it.cacheSize }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. EMAIL CACHE SCANNER
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun scanEmailCaches(): List<CleanableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<CleanableFile>()
        val ext = Environment.getExternalStorageDirectory()

        val emailPackages = listOf(
            "com.google.android.gm",                // Gmail
            "com.microsoft.office.outlook",          // Outlook
            "com.yahoo.mobile.client.android.mail",  // Yahoo Mail
            "com.samsung.android.email.provider",    // Samsung Mail
            "com.mail.mobile.android.mail",          // mail.com
            "org.mozilla.thunderbird",               // Thunderbird
        )

        onScanProgress?.invoke("Scanning email caches...", 0)

        for (pkg in emailPackages) {
            val cacheDir = File(ext, "Android/data/$pkg/cache")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                scanDirectoryForJunk(cacheDir, files, maxDepth = 3, category = CleanCategory.EMAIL_CACHE, packageName = pkg)
            }
            val filesDir = File(ext, "Android/data/$pkg/files")
            if (filesDir.exists() && filesDir.isDirectory) {
                // Look for cached attachments
                scanDirectoryForJunk(filesDir, files, maxDepth = 3, category = CleanCategory.EMAIL_CACHE, packageName = pkg)
            }
        }

        // Also check using StorageStats API for email app cache sizes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT

                for (pkg in emailPackages) {
                    try {
                        val stats = storageStatsManager.queryStatsForPackage(
                            uuid, pkg, android.os.Process.myUserHandle()
                        )
                        if (stats.cacheBytes > 1_048_576 && files.none { it.packageName == pkg }) {
                            files.add(CleanableFile(
                                path = "app_cache://$pkg",
                                name = getAppName(pkg),
                                size = stats.cacheBytes,
                                category = CleanCategory.EMAIL_CACHE,
                                packageName = pkg
                            ))
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        onScanProgress?.invoke("Complete", files.size)
        files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun deleteFiles(files: List<CleanableFile>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        for (f in files) {
            try {
                if (f.path.startsWith("app_cache://")) {
                    // Can't directly delete app cache without MANAGE permissions
                    // Direct user to app settings
                    continue
                }
                val file = File(f.path)
                if (file.exists()) {
                    if (file.isDirectory) {
                        if (file.delete()) deleted++
                    } else {
                        if (file.delete()) deleted++
                    }
                }
            } catch (_: Exception) {}
        }
        deleted
    }

    suspend fun deleteDuplicates(groups: List<DuplicateGroup>, keepFirst: Boolean = true): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        for (group in groups) {
            val toDelete = if (keepFirst) group.files.drop(1) else group.files
            for (f in toDelete) {
                try {
                    if (File(f.path).delete()) deleted++
                } catch (_: Exception) {}
            }
        }
        deleted
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private fun scanForExtensions(dir: File, extensions: Set<String>, out: MutableList<CleanableFile>, maxDepth: Int, category: CleanCategory, depth: Int = 0) {
        if (depth >= maxDepth || !dir.exists()) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() in extensions && file.length() > 0) {
                    out.add(CleanableFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = category
                    ))
                } else if (file.isDirectory && !file.name.startsWith(".") && depth < maxDepth - 1) {
                    scanForExtensions(file, extensions, out, maxDepth, category, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun scanDirectoryForJunk(dir: File, out: MutableList<CleanableFile>, maxDepth: Int, category: CleanCategory, packageName: String? = null, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() > 0) {
                    out.add(CleanableFile(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        category = category,
                        packageName = packageName
                    ))
                } else if (file.isDirectory && depth < maxDepth - 1) {
                    scanDirectoryForJunk(file, out, maxDepth, category, packageName, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun dirSize(dir: File): Long {
        var size = 0L
        try {
            dir.listFiles()?.forEach { file ->
                size += if (file.isFile) file.length()
                else dirSize(file)
            }
        } catch (_: Exception) {}
        return size
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast(".")
        }
    }
}
