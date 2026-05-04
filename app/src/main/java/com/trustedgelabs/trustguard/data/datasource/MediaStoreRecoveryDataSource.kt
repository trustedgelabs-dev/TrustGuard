package com.trustedgelabs.trustguard.data.datasource

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.trustedgelabs.trustguard.data.model.FileType
import com.trustedgelabs.trustguard.data.model.RecoverableFile
import com.trustedgelabs.trustguard.data.model.RecoverySource
import com.trustedgelabs.trustguard.data.model.ScanMode
import com.trustedgelabs.trustguard.util.RootDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreRecoveryDataSource(private val context: Context) {

    private var nextId = -1L

    // Media file extensions
    private val imageExts = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "raw", "cr2", "nef", "arw")
    private val videoExts = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "wmv", "m4v", "ts")
    private val audioExts = setOf("mp3", "wav", "aac", "ogg", "m4a", "flac", "wma", "opus", "amr")
    private val docExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "odt")
    private val allMediaExts = imageExts + videoExts + audioExts + docExts

    /**
     * Progress callback: (strategyName, filesFoundSoFar)
     */
    var onScanProgress: ((String, Int) -> Unit)? = null

    /**
     * Main scan entry point. Strategies vary by scan mode:
     * QUICK: MediaStore trash + basic filesystem trash
     * DEEP:  Quick + thumbnails + app caches + .nomedia/hidden dirs
     * ROOT:  Deep + full filesystem scan via su
     */
    suspend fun scanFiles(mode: ScanMode): List<RecoverableFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<RecoverableFile>()
        val seenPaths = mutableSetOf<String>()

        fun addUnique(list: List<RecoverableFile>) {
            for (f in list) {
                val key = f.path.ifEmpty { "${f.name}_${f.size}" }
                if (seenPaths.add(key)) files.add(f)
            }
        }

        fun report(strategy: String) {
            onScanProgress?.invoke(strategy, files.size)
        }

        // ═══ QUICK SCAN strategies ═══
        // 1. MediaStore IS_TRASHED (Android 11+)
        report("MediaStore Trash")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addUnique(queryMediaStoreTrashed())
        }
        // 2. Filesystem trash directories
        report("Trash Directories")
        addUnique(scanFilesystemTrash())
        // 3. MediaStore path-based trash query
        report("MediaStore Paths")
        addUnique(queryMediaStoreTrashPaths())
        // 4. .trashed- and .pending- files in common directories
        report("Recently Deleted")
        addUnique(scanTrashedNamedFiles())

        if (mode == ScanMode.DEEP || mode == ScanMode.ROOT) {
            // ═══ DEEP SCAN strategies ═══
            // 5. Thumbnail hunting - THE BIG SECRET
            report("Thumbnails")
            addUnique(scanThumbnailDirectories())
            // 6. .nomedia hidden directories
            report("Hidden Folders")
            addUnique(scanNoMediaDirectories())
            // 7. App cache directories (WhatsApp, Telegram, etc.)
            report("App Caches")
            addUnique(scanAppCacheDirectories())
            // 8. Sent/received media in messaging apps
            report("Messaging Apps")
            addUnique(scanMessagingAppMedia())
        }

        if (mode == ScanMode.ROOT) {
            // ═══ ROOT SCAN strategies ═══
            // 9. Root filesystem deep scan
            report("Root Filesystem")
            addUnique(rootDeepScan())
        }

        report("Complete")

        // Filter out junk: system thumbnails, tiny files, temp data, and unrecognizable files
        val filtered = files.filter { f ->
            // Minimum size: 10KB for photos/videos, 1KB for audio/docs
            val minFileSize = when (f.type) {
                FileType.PHOTO -> 10_240L
                FileType.VIDEO -> 51_200L
                FileType.AUDIO -> 1_024L
                FileType.DOCUMENT -> 512L
            }
            if (f.size < minFileSize) return@filter false

            val nameLower = f.name.lowercase()
            // Exclude system junk patterns
            if (nameLower.startsWith(".thumbdata")) return@filter false
            if (nameLower.endsWith(".nomedia")) return@filter false
            if (nameLower == "thumbs.db" || nameLower == "desktop.ini") return@filter false
            if (nameLower.endsWith(".tmp") || nameLower.endsWith(".temp")) return@filter false
            if (nameLower.endsWith(".log") || nameLower.endsWith(".cache")) return@filter false
            if (nameLower.startsWith("thumb_") && f.size < 51_200L) return@filter false
            // Exclude extremely generic temp names
            if (nameLower.matches(Regex("^[0-9a-f]{32}$"))) return@filter false
            if (nameLower.matches(Regex("^\\d+$"))) return@filter false

            true
        }

        filtered.sortedByDescending { it.deletedDate }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 1: MediaStore IS_TRASHED (Android 11+)
    // ═══════════════════════════════════════════════════════════════════════

    private fun queryMediaStoreTrashed(): List<RecoverableFile> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val files = mutableListOf<RecoverableFile>()

        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to FileType.PHOTO,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to FileType.VIDEO,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to FileType.AUDIO,
            MediaStore.Files.getContentUri("external") to FileType.DOCUMENT
        )

        for ((uri, fallbackType) in collections) {
            try {
                val queryArgs = Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.MediaColumns.DATE_MODIFIED))
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 500)
                }
                queryMediaStore(uri, queryArgs, null, null, fallbackType, RecoverySource.TRASH, files)
            } catch (_: Exception) {}
        }
        return files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 2: Filesystem trash directories
    // ═══════════════════════════════════════════════════════════════════════

    private fun scanFilesystemTrash(): List<RecoverableFile> {
        if (!hasFileAccess()) return emptyList()
        val files = mutableListOf<RecoverableFile>()
        val ext = Environment.getExternalStorageDirectory()

        val trashDirs = listOf(
            ".Trash", ".trash", ".Recycle", ".Trash-0", ".Trash-1000", ".Trash-1000/files",
            "DCIM/.trash", "Pictures/.trash", "Pictures/.trashed",
            "Download/.trash", "Movies/.trash", "Music/.trash", "Documents/.trash",
            "Android/media/.trash"
        ).map { File(ext, it) }

        for (dir in trashDirs) {
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryRecursive(dir, files, maxDepth = 3, source = RecoverySource.TRASH)
            }
        }
        return files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 3: MediaStore path-based trash query
    // ═══════════════════════════════════════════════════════════════════════

    private fun queryMediaStoreTrashPaths(): List<RecoverableFile> {
        val files = mutableListOf<RecoverableFile>()
        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to FileType.PHOTO,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to FileType.VIDEO,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to FileType.AUDIO
        )

        val selection = "${MediaStore.MediaColumns.DATA} LIKE ? OR " +
                "${MediaStore.MediaColumns.DATA} LIKE ? OR " +
                "${MediaStore.MediaColumns.DATA} LIKE ? OR " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%/.Trash/%", "%/.trash/%", "%/.Recycle/%", ".trashed-%")

        for ((uri, fallbackType) in collections) {
            try {
                queryMediaStore(uri, null, selection, args, fallbackType, RecoverySource.TRASH, files)
            } catch (_: Exception) {}
        }
        return files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 4: .trashed- and .pending- named files
    // ═══════════════════════════════════════════════════════════════════════

    private fun scanTrashedNamedFiles(): List<RecoverableFile> {
        if (!hasFileAccess()) return emptyList()
        val files = mutableListOf<RecoverableFile>()
        val ext = Environment.getExternalStorageDirectory()

        val dirsToScan = listOf("DCIM/Camera", "DCIM", "Pictures", "Download", "Movies", "Music", "Documents")
        for (dirName in dirsToScan) {
            val dir = File(ext, dirName)
            if (dir.exists()) {
                scanForPrefixedFiles(dir, files, maxDepth = 2)
            }
        }
        return files
    }

    private fun scanForPrefixedFiles(dir: File, files: MutableList<RecoverableFile>, maxDepth: Int, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.startsWith(".trashed-") || file.name.startsWith(".pending-"))) {
                    files.add(fileToRecoverable(file, RecoverySource.TRASH))
                } else if (file.isDirectory && depth < maxDepth - 1) {
                    scanForPrefixedFiles(file, files, maxDepth, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 5: THUMBNAIL HUNTING — En Büyük Sır!
    // Android deletes the original but often keeps the thumbnail.
    // .thumbnails directories contain JPEG thumbnails of deleted photos/videos.
    // ═══════════════════════════════════════════════════════════════════════

    private fun scanThumbnailDirectories(): List<RecoverableFile> {
        if (!hasFileAccess()) return emptyList()
        val files = mutableListOf<RecoverableFile>()
        val ext = Environment.getExternalStorageDirectory()

        // Known thumbnail directories
        val thumbDirs = listOf(
            "DCIM/.thumbnails",
            "Pictures/.thumbnails",
            "Movies/.thumbnails",
            ".thumbnails",
            "Android/data/com.google.android.apps.photos/cache",
            "Android/data/com.google.android.apps.photos/files/trash",
            "Android/data/com.sec.android.gallery3d/cache",      // Samsung Gallery
            "Android/data/com.miui.gallery/cache",                // Xiaomi Gallery
            "Android/data/com.oneplus.gallery/cache",             // OnePlus Gallery
        )

        for (dirName in thumbDirs) {
            val dir = File(ext, dirName)
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryForMedia(dir, files, maxDepth = 3, source = RecoverySource.THUMBNAIL, minSize = 1024) // Min 1KB
            }
        }

        // Also scan for .thumbdata files (SQLite DBs containing thumbnails)
        val dcimDir = File(ext, "DCIM/.thumbnails")
        if (dcimDir.exists()) {
            dcimDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith(".thumbdata") && file.length() > 10240) {
                    files.add(fileToRecoverable(file, RecoverySource.THUMBNAIL))
                }
            }
        }

        return files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 6: .nomedia Hidden Directories
    // Folders with .nomedia file are hidden from gallery but files still exist.
    // Great for finding "hidden" photos and media.
    // ═══════════════════════════════════════════════════════════════════════

    private fun scanNoMediaDirectories(): List<RecoverableFile> {
        if (!hasFileAccess()) return emptyList()
        val files = mutableListOf<RecoverableFile>()
        val ext = Environment.getExternalStorageDirectory()

        // Search for .nomedia files and then scan those directories
        findNoMediaDirs(ext, files, maxDepth = 4)

        return files
    }

    private fun findNoMediaDirs(dir: File, files: MutableList<RecoverableFile>, maxDepth: Int, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            val children = dir.listFiles() ?: return
            val hasNoMedia = children.any { it.name == ".nomedia" }

            if (hasNoMedia) {
                // Found a .nomedia directory! Scan for media files
                scanDirectoryForMedia(dir, files, maxDepth = 2, source = RecoverySource.HIDDEN, minSize = 1024)
            } else {
                // Keep searching subdirectories
                children.forEach { child ->
                    if (child.isDirectory && !child.name.startsWith(".") &&
                        child.name != "Android" && child.name != "obb"
                    ) {
                        findNoMediaDirs(child, files, maxDepth, depth + 1)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 7: App Cache Directories
    // WhatsApp, Telegram, and other apps keep media in cache/data folders.
    // ═══════════════════════════════════════════════════════════════════════

    private fun scanAppCacheDirectories(): List<RecoverableFile> {
        if (!hasFileAccess()) return emptyList()
        val files = mutableListOf<RecoverableFile>()
        val ext = Environment.getExternalStorageDirectory()

        // App-specific media directories (accessible without root on most devices)
        val appDirs = listOf(
            // WhatsApp
            "WhatsApp/Media/WhatsApp Images",
            "WhatsApp/Media/WhatsApp Video",
            "WhatsApp/Media/WhatsApp Audio",
            "WhatsApp/Media/WhatsApp Documents",
            "WhatsApp/Media/.Statuses",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video",
            "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            // WhatsApp Business
            "WhatsApp Business/Media/WhatsApp Business Images",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media",
            // Telegram
            "Telegram/Telegram Images",
            "Telegram/Telegram Video",
            "Telegram/Telegram Audio",
            "Telegram/Telegram Documents",
            "Android/data/org.telegram.messenger/cache",
            // Signal
            "Android/data/org.thoughtcrime.securesms/cache",
            // Instagram
            "Android/data/com.instagram.android/cache",
            // Twitter/X
            "Android/data/com.twitter.android/cache",
            // TikTok
            "Android/data/com.zhiliaoapp.musically/cache",
            // Snapchat
            "Android/data/com.snapchat.android/cache",
            // Facebook Messenger
            "Android/data/com.facebook.orca/cache",
            // Other common apps
            "Android/data/com.google.android.gm/cache",        // Gmail
            "Android/data/com.discord/cache",                    // Discord
        )

        // Sent folders (WhatsApp keeps sent media separately)
        val sentFolders = listOf(
            "WhatsApp/Media/WhatsApp Images/Sent",
            "WhatsApp/Media/WhatsApp Video/Sent",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent",
            "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Sent",
        )

        for (dirName in appDirs + sentFolders) {
            val dir = File(ext, dirName)
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryForMedia(dir, files, maxDepth = 2, source = RecoverySource.CACHE, minSize = 2048)
            }
        }

        return files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 8: Messaging app media scanning
    // ═══════════════════════════════════════════════════════════════════════

    private fun scanMessagingAppMedia(): List<RecoverableFile> {
        if (!hasFileAccess()) return emptyList()
        val files = mutableListOf<RecoverableFile>()
        val ext = Environment.getExternalStorageDirectory()

        // Scan Android/media/ for app-specific folders
        val mediaDir = File(ext, "Android/media")
        if (mediaDir.exists() && mediaDir.isDirectory) {
            mediaDir.listFiles()?.forEach { appDir ->
                if (appDir.isDirectory) {
                    // Look for cache, files/trash, and media subdirectories
                    val subDirs = listOf("cache", "files/trash", "files/.trash", "files/Trash")
                    for (sub in subDirs) {
                        val subDir = File(appDir, sub)
                        if (subDir.exists() && subDir.isDirectory) {
                            scanDirectoryForMedia(subDir, files, maxDepth = 3, source = RecoverySource.CACHE, minSize = 1024)
                        }
                    }
                }
            }
        }

        return files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STRATEGY 9: ROOT Deep Scan
    // Uses 'su' to access /data/media, /data/data/*/cache, etc.
    // ═══════════════════════════════════════════════════════════════════════

    private fun rootDeepScan(): List<RecoverableFile> {
        if (!RootDetector.isDeviceRooted()) return emptyList()
        val files = mutableListOf<RecoverableFile>()

        // Root-only directories to scan
        val rootPaths = listOf(
            "/data/media/0/.thumbnails",
            "/data/media/0/DCIM/.thumbnails",
            "/data/media/0/.Trash",
            "/data/media/0/.trash",
        )

        for (path in rootPaths) {
            try {
                // Use root to list files
                val output = RootDetector.executeRootCommand("find $path -type f -size +1k 2>/dev/null | head -200")
                output?.forEach { filePath ->
                    if (filePath.isNotBlank()) {
                        val file = File(filePath.trim())
                        if (isMediaFile(file.extension.lowercase())) {
                            files.add(fileToRecoverable(file, RecoverySource.ROOT_SCAN))
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Scan app caches via root
        try {
            val cacheDirs = RootDetector.executeRootCommand(
                "find /data/data/*/cache -type f \\( -name '*.jpg' -o -name '*.mp4' -o -name '*.png' -o -name '*.webp' \\) -size +5k 2>/dev/null | head -300"
            )
            cacheDirs?.forEach { filePath ->
                if (filePath.isNotBlank()) {
                    val file = File(filePath.trim())
                    files.add(fileToRecoverable(file, RecoverySource.ROOT_SCAN))
                }
            }
        } catch (_: Exception) {}

        // Scan for deleted but not overwritten inodes (requires special tools)
        // This is the most advanced recovery technique
        try {
            val deletedFiles = RootDetector.executeRootCommand(
                "find /data/media/0 -name '.trashed-*' -o -name '.pending-*' 2>/dev/null | head -200"
            )
            deletedFiles?.forEach { filePath ->
                if (filePath.isNotBlank()) {
                    val file = File(filePath.trim())
                    if (file.length() > 0) {
                        files.add(fileToRecoverable(file, RecoverySource.ROOT_SCAN))
                    }
                }
            }
        } catch (_: Exception) {}

        return files
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Recovery
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun recoverFile(file: RecoverableFile): Boolean = withContext(Dispatchers.IO) {
        try {
            when {
                // MediaStore trashed file
                file.id > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0) }
                    context.contentResolver.update(file.uri, values, null, null) > 0
                }
                // Root-scanned file: copy via su
                file.source == RecoverySource.ROOT_SCAN && file.path.startsWith("/data/") -> {
                    recoverViaRoot(file)
                }
                // Filesystem file
                file.path.isNotEmpty() && File(file.path).exists() -> {
                    recoverFromFilesystem(file)
                }
                // Content URI
                else -> recoverFromContentUri(file)
            }
        } catch (e: Exception) {
            android.util.Log.e("RecoveryDS", "Recovery failed: ${file.name}", e)
            false
        }
    }

    private fun recoverFromFilesystem(file: RecoverableFile): Boolean {
        val src = File(file.path)
        if (!src.exists()) return recoverFromContentUri(file)

        val destDir = getRecoveryDirectory(file.type)
        if (!destDir.exists()) destDir.mkdirs()

        val destName = extractOriginalName(src.name) ?: src.name
        return try {
            src.copyTo(File(destDir, destName), overwrite = true)
            true
        } catch (_: Exception) { false }
    }

    private fun recoverFromContentUri(file: RecoverableFile): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(file.uri) ?: return false
            val destDir = getRecoveryDirectory(file.type)
            if (!destDir.exists()) destDir.mkdirs()

            File(destDir, file.name).outputStream().use { output -> input.copyTo(output) }
            input.close()
            true
        } catch (_: Exception) { false }
    }

    private fun recoverViaRoot(file: RecoverableFile): Boolean {
        val destDir = getRecoveryDirectory(file.type)
        if (!destDir.exists()) destDir.mkdirs()

        val destName = extractOriginalName(File(file.path).name) ?: file.name
        val destPath = File(destDir, destName).absolutePath
        val result = RootDetector.executeRootCommand("cp '${file.path}' '$destPath' && chmod 644 '$destPath'")
        return result != null
    }

    private fun getRecoveryDirectory(type: FileType): File {
        val baseName = when (type) {
            FileType.PHOTO -> Environment.DIRECTORY_PICTURES
            FileType.VIDEO -> Environment.DIRECTORY_MOVIES
            FileType.AUDIO -> Environment.DIRECTORY_MUSIC
            FileType.DOCUMENT -> Environment.DIRECTORY_DOCUMENTS
        }
        return File(Environment.getExternalStoragePublicDirectory(baseName), "TrustGuard_Recovered")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Shared Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun queryMediaStore(
        collectionUri: Uri, queryArgs: Bundle?, selection: String?, selectionArgs: Array<String>?,
        fallbackType: FileType, source: RecoverySource, out: MutableList<RecoverableFile>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_MODIFIED
        )

        val cursor = if (queryArgs != null) {
            context.contentResolver.query(collectionUri, projection, queryArgs, null)
        } else {
            context.contentResolver.query(collectionUri, projection, selection, selectionArgs,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
        }

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = extractOriginalName(it.getString(nameCol) ?: "Unknown") ?: (it.getString(nameCol) ?: "Unknown")
                val path = it.getString(dataCol) ?: ""
                val size = it.getLong(sizeCol)
                val mime = it.getString(mimeCol) ?: ""
                val date = it.getLong(dateCol) * 1000L
                val type = resolveFileType(mime, fallbackType)
                val contentUri = Uri.withAppendedPath(collectionUri, id.toString())

                out.add(RecoverableFile(id, contentUri, name, path, size, type, mime, date,
                    if (type == FileType.PHOTO || type == FileType.VIDEO) contentUri else null, source))
            }
        }
    }

    private fun scanDirectoryRecursive(dir: File, out: MutableList<RecoverableFile>, maxDepth: Int, source: RecoverySource, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() > 0) {
                    out.add(fileToRecoverable(file, source))
                } else if (file.isDirectory && depth < maxDepth - 1) {
                    scanDirectoryRecursive(file, out, maxDepth, source, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Scan directory only for media files with minimum size filter.
     */
    private fun scanDirectoryForMedia(dir: File, out: MutableList<RecoverableFile>, maxDepth: Int, source: RecoverySource, minSize: Long, depth: Int = 0) {
        if (depth >= maxDepth) return
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.length() >= minSize && isMediaFile(file.extension.lowercase())) {
                    out.add(fileToRecoverable(file, source))
                } else if (file.isDirectory && depth < maxDepth - 1) {
                    scanDirectoryForMedia(file, out, maxDepth, source, minSize, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun fileToRecoverable(file: File, source: RecoverySource): RecoverableFile {
        val id = nextId--
        val ext = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: ""
        val type = resolveFileType(mime, guessFileTypeFromExtension(ext))
        val name = extractOriginalName(file.name) ?: file.name

        return RecoverableFile(id, Uri.fromFile(file), name, file.absolutePath, file.length(),
            type, mime, file.lastModified(),
            if (type == FileType.PHOTO || type == FileType.VIDEO) Uri.fromFile(file) else null, source)
    }

    private fun extractOriginalName(fileName: String): String? {
        for (prefix in listOf(".trashed-", ".pending-")) {
            if (fileName.startsWith(prefix)) {
                val rest = fileName.removePrefix(prefix)
                val dash = rest.indexOf('-')
                if (dash > 0) return rest.substring(dash + 1)
            }
        }
        return null
    }

    private fun hasFileAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
    }

    private fun isMediaFile(ext: String) = ext in allMediaExts
    private fun resolveFileType(mime: String, fallback: FileType) = when {
        mime.startsWith("image/") -> FileType.PHOTO
        mime.startsWith("video/") -> FileType.VIDEO
        mime.startsWith("audio/") -> FileType.AUDIO
        mime.startsWith("application/") || mime.startsWith("text/") -> FileType.DOCUMENT
        else -> fallback
    }
    private fun guessFileTypeFromExtension(ext: String) = when (ext) {
        in imageExts -> FileType.PHOTO
        in videoExts -> FileType.VIDEO
        in audioExts -> FileType.AUDIO
        in docExts -> FileType.DOCUMENT
        else -> FileType.DOCUMENT
    }
}
