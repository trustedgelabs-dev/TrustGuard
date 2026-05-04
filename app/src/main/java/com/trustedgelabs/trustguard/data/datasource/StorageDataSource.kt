package com.trustedgelabs.trustguard.data.datasource

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.trustedgelabs.trustguard.data.model.StorageInfo
import com.trustedgelabs.trustguard.data.model.StorageCategory
import java.io.File
import java.util.UUID

class StorageDataSource(private val context: Context) {

    fun getStorageInfo(): StorageInfo {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.totalBytes
        val freeBytes = statFs.availableBytes
        val usedBytes = totalBytes - freeBytes

        val categories = scanCategories()

        return StorageInfo(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
            categories = categories
        )
    }

    private fun scanCategories(): List<StorageCategory> {
        val categories = mutableListOf<StorageCategory>()

        // Images
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val imagesSize = getDirSize(imagesDir) + getDirSize(dcimDir)
        categories.add(StorageCategory("images", imagesSize, 0xFF42A5F5))

        // Videos
        val videosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val videosSize = getDirSize(videosDir)
        categories.add(StorageCategory("videos", videosSize, 0xFFEF5350))

        // Audio
        val audioDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val audioSize = getDirSize(audioDir)
        categories.add(StorageCategory("audio", audioSize, 0xFFAB47BC))

        // Documents
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val docsSize = getDirSize(docsDir) + getDirSize(downloadsDir)
        categories.add(StorageCategory("documents", docsSize, 0xFFFFCA28))

        // Apps (estimate via package manager)
        val appsSize = estimateAppStorageSize()
        categories.add(StorageCategory("apps", appsSize, 0xFF66BB6A))

        return categories.sortedByDescending { it.sizeBytes }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size = 0L
        try {
            val files = dir.listFiles() ?: return 0
            for (file in files) {
                size += if (file.isDirectory) getDirSize(file) else file.length()
            }
        } catch (_: Exception) {}
        return size
    }

    private fun estimateAppStorageSize(): Long {
        try {
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val uuid = StorageManager.UUID_DEFAULT

            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            var totalSize = 0L

            for (pkg in packages) {
                try {
                    val stats = storageStatsManager.queryStatsForPackage(
                        uuid, pkg.packageName, android.os.Process.myUserHandle()
                    )
                    totalSize += stats.appBytes + stats.cacheBytes + stats.dataBytes
                } catch (_: Exception) {}
            }
            return totalSize
        } catch (_: Exception) {
            return 0L
        }
    }
}
