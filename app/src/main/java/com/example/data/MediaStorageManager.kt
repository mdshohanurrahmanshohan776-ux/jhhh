package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.FolderStats
import com.example.model.MediaItem
import com.example.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ultrawide_favorites", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "MediaStorageManager"
        const val SUBFOLDER_PHOTOS = "UltraWideCamera/Photos"
        const val SUBFOLDER_VIDEOS = "UltraWideCamera/Videos"
    }

    private fun getInternalPhotosDir(): File {
        val dir = File(context.filesDir, SUBFOLDER_PHOTOS)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getInternalVideosDir(): File {
        val dir = File(context.filesDir, SUBFOLDER_VIDEOS)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExternalPhotosDir(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SUBFOLDER_PHOTOS)
        if (!dir.exists()) dir.mkdirs()
        return if (dir.exists()) dir else getInternalPhotosDir()
    }

    fun getExternalVideosDir(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), SUBFOLDER_VIDEOS)
        if (!dir.exists()) dir.mkdirs()
        return if (dir.exists()) dir else getInternalVideosDir()
    }

    suspend fun savePhoto(
        bitmap: Bitmap,
        zoomRatio: Float,
        filterName: String?,
        isRaw: Boolean = false
    ): MediaItem = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val prefix = if (isRaw) "RAW" else "IMG"
        val extension = if (isRaw) "dng" else "jpg"
        val fileName = "${prefix}_${timeStamp}_${(zoomRatio * 10).toInt()}x.$extension"
        val isUltraWide = zoomRatio <= 0.65f

        val targetDir = getExternalPhotosDir()
        val file = File(targetDir, fileName)

        // Write file bytes
        FileOutputStream(file).use { out ->
            if (isRaw) {
                // High-fidelity uncompressed PNG/DNG simulation container
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }
            out.flush()
        }

        // Also register in MediaStore so it appears in system galleries under UltraWideCamera/Photos
        var contentUri: Uri = Uri.fromFile(file)
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, if (isRaw) "image/x-adobe-dng" else "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$SUBFOLDER_PHOTOS")
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
            }
            val insertedUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (insertedUri != null) {
                context.contentResolver.openOutputStream(insertedUri)?.use { out ->
                    if (isRaw) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
                    }
                }
                contentUri = insertedUri
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore insert fallback to local file: ${e.message}")
        }

        MediaItem(
            id = file.absolutePath,
            uri = contentUri,
            filePath = file.absolutePath,
            fileName = fileName,
            mediaType = if (isRaw) MediaType.RAW else MediaType.PHOTO,
            dateAdded = System.currentTimeMillis(),
            sizeBytes = file.length(),
            width = bitmap.width,
            height = bitmap.height,
            zoomRatio = zoomRatio,
            isUltraWide = isUltraWide,
            filterName = filterName,
            isFavorite = false,
            folderType = "Photos"
        )
    }

    fun createNewVideoOutputFile(zoomRatio: Float): Pair<File, ContentValues> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "VID_${timeStamp}_${(zoomRatio * 10).toInt()}x.mp4"
        val targetDir = getExternalVideosDir()
        val file = File(targetDir, fileName)

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$SUBFOLDER_VIDEOS")
            }
        }

        return Pair(file, contentValues)
    }

    suspend fun loadAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaItem>()
        val photosDir = getExternalPhotosDir()
        val videosDir = getExternalVideosDir()
        val internalPhotosDir = getInternalPhotosDir()
        val internalVideosDir = getInternalVideosDir()

        val allPhotoDirs = listOf(photosDir, internalPhotosDir).distinctBy { it.absolutePath }
        val allVideoDirs = listOf(videosDir, internalVideosDir).distinctBy { it.absolutePath }

        for (dir in allPhotoDirs) {
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true) || file.extension.equals("png", true) || file.extension.equals("dng", true))) {
                        val isRaw = file.extension.equals("dng", true) || file.name.startsWith("RAW_")
                        val isFavorite = prefs.getBoolean(file.absolutePath, false)
                        val zoomRatio = parseZoomFromFileName(file.name)
                        result.add(
                            MediaItem(
                                id = file.absolutePath,
                                uri = Uri.fromFile(file),
                                filePath = file.absolutePath,
                                fileName = file.name,
                                mediaType = if (isRaw) MediaType.RAW else MediaType.PHOTO,
                                dateAdded = file.lastModified(),
                                sizeBytes = file.length(),
                                zoomRatio = zoomRatio,
                                isUltraWide = zoomRatio <= 0.65f,
                                isFavorite = isFavorite,
                                folderType = "Photos"
                            )
                        )
                    }
                }
            }
        }

        for (dir in allVideoDirs) {
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension.equals("mp4", true) || file.extension.equals("mkv", true))) {
                        val isFavorite = prefs.getBoolean(file.absolutePath, false)
                        val zoomRatio = parseZoomFromFileName(file.name)
                        result.add(
                            MediaItem(
                                id = file.absolutePath,
                                uri = Uri.fromFile(file),
                                filePath = file.absolutePath,
                                fileName = file.name,
                                mediaType = MediaType.VIDEO,
                                dateAdded = file.lastModified(),
                                sizeBytes = file.length(),
                                zoomRatio = zoomRatio,
                                isUltraWide = zoomRatio <= 0.65f,
                                isFavorite = isFavorite,
                                folderType = "Videos"
                            )
                        )
                    }
                }
            }
        }

        // Sort descending by date
        result.sortByDescending { it.dateAdded }
        result
    }

    private fun parseZoomFromFileName(name: String): Float {
        return try {
            val parts = name.substringBeforeLast(".").split("_")
            val zoomPart = parts.lastOrNull { it.endsWith("x") }
            if (zoomPart != null) {
                val num = zoomPart.removeSuffix("x").toIntOrNull()
                if (num != null) num / 10.0f else 1.0f
            } else 1.0f
        } catch (e: Exception) {
            1.0f
        }
    }

    suspend fun getFolderStats(): FolderStats = withContext(Dispatchers.IO) {
        val all = loadAllMedia()
        val photos = all.filter { it.mediaType == MediaType.PHOTO }
        val videos = all.filter { it.mediaType == MediaType.VIDEO }
        val raws = all.filter { it.mediaType == MediaType.RAW }
        val totalBytes = all.sumOf { it.sizeBytes }
        val totalMb = totalBytes / (1024f * 1024f)

        FolderStats(
            photosCount = photos.size,
            videosCount = videos.size,
            rawCount = raws.size,
            totalSizeMb = String.format(Locale.US, "%.1f", totalMb).toFloatOrNull() ?: 0f,
            photosPath = SUBFOLDER_PHOTOS,
            videosPath = SUBFOLDER_VIDEOS
        )
    }

    fun toggleFavorite(item: MediaItem): Boolean {
        val newFav = !item.isFavorite
        prefs.edit().putBoolean(item.filePath, newFav).apply()
        return newFav
    }

    suspend fun deleteMedia(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
            prefs.edit().remove(item.filePath).apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file", e)
            false
        }
    }

    suspend fun loadBitmap(item: MediaItem): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                val input: InputStream? = context.contentResolver.openInputStream(item.uri)
                input?.use { BitmapFactory.decodeStream(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap: ${e.message}")
            null
        }
    }
}
