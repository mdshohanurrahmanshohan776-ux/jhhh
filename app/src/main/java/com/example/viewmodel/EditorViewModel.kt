package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaStorageManager
import com.example.editor.EditAdjustments
import com.example.editor.PhotoEditorProcessor
import com.example.model.CameraFilter
import com.example.model.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorTab(val label: String) {
    FILTERS("Filters"),
    ADJUST("Adjust"),
    CROP_ROTATE("Transform")
}

data class EditorUiState(
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val adjustments: EditAdjustments = EditAdjustments(),
    val activeTab: EditorTab = EditorTab.FILTERS,
    val isComparing: Boolean = false,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccessItem: MediaItem? = null,
    val errorMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = MediaStorageManager(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var renderJob: Job? = null

    fun initializeWithMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val bitmap = storageManager.loadBitmap(mediaItem)
            if (bitmap != null) {
                _uiState.update {
                    it.copy(
                        originalBitmap = bitmap,
                        previewBitmap = bitmap,
                        adjustments = EditAdjustments(),
                        isProcessing = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Could not load image"
                    )
                }
            }
        }
    }

    fun setTab(tab: EditorTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setFilter(filter: CameraFilter) {
        val updated = _uiState.value.adjustments.copy(selectedFilter = filter)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun updateExposure(value: Float) {
        val updated = _uiState.value.adjustments.copy(exposure = value)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun updateContrast(value: Float) {
        val updated = _uiState.value.adjustments.copy(contrast = value)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun updateSaturation(value: Float) {
        val updated = _uiState.value.adjustments.copy(saturation = value)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun updateWarmth(value: Float) {
        val updated = _uiState.value.adjustments.copy(warmth = value)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun updateTint(value: Float) {
        val updated = _uiState.value.adjustments.copy(tint = value)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun updateVignette(value: Float) {
        val updated = _uiState.value.adjustments.copy(vignette = value)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun rotateClockwise() {
        val cur = _uiState.value.adjustments.rotationDegrees
        val next = (cur + 90) % 360
        val updated = _uiState.value.adjustments.copy(rotationDegrees = next)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun flipHorizontal() {
        val cur = _uiState.value.adjustments.flipHorizontal
        val updated = _uiState.value.adjustments.copy(flipHorizontal = !cur)
        _uiState.update { it.copy(adjustments = updated) }
        scheduleRender()
    }

    fun resetAdjustments() {
        _uiState.update {
            it.copy(
                adjustments = EditAdjustments(),
                previewBitmap = it.originalBitmap
            )
        }
    }

    fun setIsComparing(comparing: Boolean) {
        _uiState.update { it.copy(isComparing = comparing) }
    }

    private fun scheduleRender() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            delay(40) // Smooth debounce
            val orig = _uiState.value.originalBitmap ?: return@launch
            val adj = _uiState.value.adjustments
            try {
                val processed = PhotoEditorProcessor.processImage(orig, adj)
                _uiState.update { it.copy(previewBitmap = processed) }
            } catch (e: Exception) {
                Log.e("EditorViewModel", "Render error", e)
            }
        }
    }

    fun saveEditedPhoto(onSaved: (MediaItem) -> Unit) {
        val orig = _uiState.value.originalBitmap ?: return
        val adj = _uiState.value.adjustments

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val finalBitmap = PhotoEditorProcessor.processImage(orig, adj)
                val savedItem = storageManager.savePhoto(
                    bitmap = finalBitmap,
                    zoomRatio = 1.0f,
                    filterName = if (adj.selectedFilter != CameraFilter.NORMAL) adj.selectedFilter.displayName else "Edited",
                    isRaw = false
                )
                _uiState.update { it.copy(isSaving = false, saveSuccessItem = savedItem) }
                onSaved(savedItem)
            } catch (e: Exception) {
                Log.e("EditorViewModel", "Failed to save edited photo", e)
                _uiState.update { it.copy(isSaving = false, errorMessage = "Error saving edited photo") }
            }
        }
    }
}
