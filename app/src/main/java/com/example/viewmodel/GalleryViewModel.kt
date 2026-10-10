package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaStorageManager
import com.example.model.FolderStats
import com.example.model.MediaItem
import com.example.model.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class GalleryFilterTab(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    RAW("RAW DNG"),
    FAVORITES("Favorites")
}

data class GalleryUiState(
    val items: List<MediaItem> = emptyList(),
    val filteredItems: List<MediaItem> = emptyList(),
    val currentTab: GalleryFilterTab = GalleryFilterTab.ALL,
    val folderStats: FolderStats = FolderStats(),
    val selectedItem: MediaItem? = null,
    val isShowingExifDialog: Boolean = false,
    val isLoading: Boolean = false
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = MediaStorageManager(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val mediaList = storageManager.loadAllMedia()
            val stats = storageManager.getFolderStats()
            _uiState.update { state ->
                state.copy(
                    items = mediaList,
                    folderStats = stats,
                    isLoading = false
                )
            }
            applyFilter(_uiState.value.currentTab)
        }
    }

    fun setTab(tab: GalleryFilterTab) {
        _uiState.update { it.copy(currentTab = tab) }
        applyFilter(tab)
    }

    private fun applyFilter(tab: GalleryFilterTab) {
        val all = _uiState.value.items
        val filtered = when (tab) {
            GalleryFilterTab.ALL -> all
            GalleryFilterTab.PHOTOS -> all.filter { it.mediaType == MediaType.PHOTO }
            GalleryFilterTab.VIDEOS -> all.filter { it.mediaType == MediaType.VIDEO }
            GalleryFilterTab.RAW -> all.filter { it.mediaType == MediaType.RAW }
            GalleryFilterTab.FAVORITES -> all.filter { it.isFavorite }
        }
        _uiState.update { it.copy(filteredItems = filtered) }
    }

    fun selectItem(item: MediaItem?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun toggleFavorite(item: MediaItem) {
        val newFav = storageManager.toggleFavorite(item)
        val updated = item.copy(isFavorite = newFav)
        _uiState.update { state ->
            val newItems = state.items.map { if (it.id == item.id) updated else it }
            val newSelectedItem = if (state.selectedItem?.id == item.id) updated else state.selectedItem
            state.copy(items = newItems, selectedItem = newSelectedItem)
        }
        applyFilter(_uiState.value.currentTab)
    }

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch {
            storageManager.deleteMedia(item)
            val newItems = _uiState.value.items.filterNot { it.id == item.id }
            val stats = storageManager.getFolderStats()
            _uiState.update { state ->
                state.copy(
                    items = newItems,
                    selectedItem = null,
                    folderStats = stats
                )
            }
            applyFilter(_uiState.value.currentTab)
        }
    }

    fun toggleExifDialog(show: Boolean) {
        _uiState.update { it.copy(isShowingExifDialog = show) }
    }

    fun shareItem(item: MediaItem) {
        val context = getApplication<Application>()
        try {
            val file = File(item.filePath)
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                item.uri
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback direct intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
