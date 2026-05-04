package com.trustedgelabs.trustguard.data.model

import android.net.Uri

data class RecoverableFile(
    val id: Long,
    val uri: Uri,
    val name: String,
    val path: String,
    val size: Long,
    val type: FileType,
    val mimeType: String,
    val deletedDate: Long,
    val thumbnailUri: Uri? = null,
    val source: RecoverySource = RecoverySource.TRASH
)

enum class FileType(val labelResId: Int) {
    PHOTO(com.trustedgelabs.trustguard.R.string.photos),
    VIDEO(com.trustedgelabs.trustguard.R.string.videos),
    AUDIO(com.trustedgelabs.trustguard.R.string.audio),
    DOCUMENT(com.trustedgelabs.trustguard.R.string.documents)
}

enum class RecoverySource(val labelResId: Int) {
    TRASH(com.trustedgelabs.trustguard.R.string.source_trash),
    THUMBNAIL(com.trustedgelabs.trustguard.R.string.source_thumbnail),
    CACHE(com.trustedgelabs.trustguard.R.string.source_cache),
    HIDDEN(com.trustedgelabs.trustguard.R.string.source_hidden),
    ROOT_SCAN(com.trustedgelabs.trustguard.R.string.source_root)
}

enum class ScanMode {
    QUICK,  // Free: MediaStore trash + basic filesystem
    DEEP,   // Free to scan, Premium to recover: thumbnails + caches + hidden
    ROOT    // Premium only: full filesystem via su
}
