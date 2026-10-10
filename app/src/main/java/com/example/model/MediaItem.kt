package com.example.model

import android.net.Uri

enum class MediaType {
    PHOTO,
    VIDEO,
    RAW
}

data class MediaItem(
    val id: String,
    val uri: Uri,
    val filePath: String,
    val fileName: String,
    val mediaType: MediaType,
    val dateAdded: Long,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val zoomRatio: Float = 1.0f,
    val isUltraWide: Boolean = false,
    val filterName: String? = null,
    val isFavorite: Boolean = false,
    val folderType: String = if (mediaType == MediaType.VIDEO) "Videos" else "Photos"
)

data class FolderStats(
    val photosCount: Int = 0,
    val videosCount: Int = 0,
    val rawCount: Int = 0,
    val totalSizeMb: Float = 0f,
    val photosPath: String = "UltraWideCamera/Photos",
    val videosPath: String = "UltraWideCamera/Videos"
)
