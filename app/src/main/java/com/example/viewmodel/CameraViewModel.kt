package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraHardwareHelper
import com.example.camera.CameraHardwareInfo
import com.example.camera.LevelSensorHelper
import com.example.data.MediaStorageManager
import com.example.editor.EditAdjustments
import com.example.editor.PhotoEditorProcessor
import com.example.model.CameraFilter
import com.example.model.CameraMode
import com.example.model.FlashMode
import com.example.model.FocusMode
import com.example.model.GridMode
import com.example.model.LensPreset
import com.example.model.MediaItem
import com.example.model.TimerMode
import com.example.model.WhiteBalance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class CameraUiState(
    val mode: CameraMode = CameraMode.PHOTO,
    val currentZoom: Float = 1.0f,
    val selectedLens: LensPreset = LensPreset.WIDE_1,
    val exposureCompensationIndex: Int = 0,
    val exposureCompensationEv: Float = 0.0f,
    val focusMode: FocusMode = FocusMode.AF,
    val manualFocusDistance: Float = 0.5f, // 0.0 (macro) to 1.0 (infinity)
    val isRawMode: Boolean = false,
    val selectedFilter: CameraFilter = CameraFilter.NORMAL,
    val flashMode: FlashMode = FlashMode.OFF,
    val timerMode: TimerMode = TimerMode.OFF,
    val remainingCountdown: Int? = null,
    val gridMode: GridMode = GridMode.NONE,
    val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
    val isLevelSensorEnabled: Boolean = true,
    val horizonRollAngle: Float = 0f,
    val isHorizonLevel: Boolean = false,
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val isAudioMuted: Boolean = false,
    val isCapturing: Boolean = false,
    val showShutterFlash: Boolean = false,
    val lastCapturedItem: MediaItem? = null,
    val tapFocusPoint: Pair<Float, Float>? = null,
    val hardwareInfo: CameraHardwareInfo? = null,
    val statusMessage: String? = null,
    val isFrontCamera: Boolean = false
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = MediaStorageManager(application)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var activeCamera: Camera? = null
    private var activeRecording: Recording? = null
    private var recordingTimerJob: Job? = null
    private var countdownJob: Job? = null

    private val levelSensor = LevelSensorHelper(application) { angle, isLevel ->
        _uiState.update { it.copy(horizonRollAngle = angle, isHorizonLevel = isLevel) }
    }

    init {
        val hwInfo = CameraHardwareHelper.queryHardware(application)
        _uiState.update { it.copy(hardwareInfo = hwInfo) }
        levelSensor.start()
        refreshLastMedia()
    }

    override fun onCleared() {
        super.onCleared()
        levelSensor.stop()
        cameraExecutor.shutdown()
        activeRecording?.stop()
    }

    fun bindCamera(camera: Camera) {
        activeCamera = camera
        // Sync zoom ratio from current UI state
        applyZoom(_uiState.value.currentZoom)
        applyExposure(_uiState.value.exposureCompensationIndex)
    }

    fun setCameraMode(mode: CameraMode) {
        if (_uiState.value.isRecording) return
        _uiState.update { it.copy(mode = mode) }
    }

    fun selectLensPreset(preset: LensPreset) {
        setZoom(preset.ratio)
    }

    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(0.5f, 10.0f)
        val matchingPreset = LensPreset.entries.minByOrNull { kotlin.math.abs(it.ratio - clamped) }
            ?: LensPreset.WIDE_1

        _uiState.update {
            it.copy(
                currentZoom = clamped,
                selectedLens = matchingPreset
            )
        }
        applyZoom(clamped)
    }

    private fun applyZoom(ratio: Float) {
        val camera = activeCamera ?: return
        val zoomState = camera.cameraInfo.zoomState.value ?: return

        val minR = zoomState.minZoomRatio
        val maxR = zoomState.maxZoomRatio

        // If camera hardware natively supports 0.5x, pass ratio directly clamped to [minR, maxR]
        // Otherwise, if minR is 1.0f, map ratio smoothly to [minR, maxR]
        val targetRatio = if (ratio < minR) {
            minR
        } else {
            ratio.coerceIn(minR, maxR)
        }

        try {
            camera.cameraControl.setZoomRatio(targetRatio)
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Failed to set zoom ratio", e)
        }
    }

    fun setExposureIndex(index: Int) {
        val hw = _uiState.value.hardwareInfo
        val min = hw?.minExposureIndex ?: -4
        val max = hw?.maxExposureIndex ?: 4
        val clamped = index.coerceIn(min, max)
        val ev = clamped * (hw?.exposureStep ?: 0.5f)

        _uiState.update {
            it.copy(
                exposureCompensationIndex = clamped,
                exposureCompensationEv = ev
            )
        }
        applyExposure(clamped)
    }

    private fun applyExposure(index: Int) {
        val camera = activeCamera ?: return
        try {
            camera.cameraControl.setExposureCompensationIndex(index)
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Failed to set exposure index", e)
        }
    }

    fun setFocusMode(focusMode: FocusMode) {
        _uiState.update { it.copy(focusMode = focusMode) }
    }

    fun setManualFocusDistance(distance: Float) {
        _uiState.update { it.copy(manualFocusDistance = distance.coerceIn(0f, 1f)) }
        // When manual focus distance changes, we keep MF state active
    }

    fun onTapToFocus(xPercent: Float, yPercent: Float, surfaceWidth: Float, surfaceHeight: Float) {
        val camera = activeCamera ?: return
        _uiState.update { it.copy(tapFocusPoint = Pair(xPercent, yPercent)) }

        val factory = SurfaceOrientedMeteringPointFactory(surfaceWidth, surfaceHeight)
        val point = factory.createPoint(xPercent * surfaceWidth, yPercent * surfaceHeight)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        try {
            camera.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Tap to focus error", e)
        }

        viewModelScope.launch {
            delay(2800)
            _uiState.update { it.copy(tapFocusPoint = null) }
        }
    }

    fun toggleRawMode() {
        _uiState.update { it.copy(isRawMode = !it.isRawMode) }
    }

    fun setFilter(filter: CameraFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun cycleFlashMode() {
        val next = when (_uiState.value.flashMode) {
            FlashMode.OFF -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.TORCH
            FlashMode.TORCH -> FlashMode.OFF
        }
        _uiState.update { it.copy(flashMode = next) }
        applyTorch(next == FlashMode.TORCH)
    }

    private fun applyTorch(enabled: Boolean) {
        try {
            activeCamera?.cameraControl?.enableTorch(enabled)
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Torch error", e)
        }
    }

    fun cycleTimerMode() {
        val next = when (_uiState.value.timerMode) {
            TimerMode.OFF -> TimerMode.SEC_3
            TimerMode.SEC_3 -> TimerMode.SEC_5
            TimerMode.SEC_5 -> TimerMode.SEC_10
            TimerMode.SEC_10 -> TimerMode.OFF
        }
        _uiState.update { it.copy(timerMode = next) }
    }

    fun cycleGridMode() {
        val next = when (_uiState.value.gridMode) {
            GridMode.NONE -> GridMode.RULE_OF_THIRDS
            GridMode.RULE_OF_THIRDS -> GridMode.GOLDEN_RATIO
            GridMode.GOLDEN_RATIO -> GridMode.NONE
        }
        _uiState.update { it.copy(gridMode = next) }
    }

    fun toggleFrontBack() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun capturePhotoWithTimer(imageCapture: ImageCapture?) {
        val timerSec = _uiState.value.timerMode.seconds
        if (timerSec > 0) {
            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                for (s in timerSec downTo 1) {
                    _uiState.update { it.copy(remainingCountdown = s) }
                    delay(1000)
                }
                _uiState.update { it.copy(remainingCountdown = null) }
                performCapture(imageCapture)
            }
        } else {
            performCapture(imageCapture)
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(remainingCountdown = null) }
    }

    private fun performCapture(imageCapture: ImageCapture?) {
        if (imageCapture == null) {
            showToast("Camera is not ready yet")
            return
        }

        _uiState.update { it.copy(isCapturing = true, showShutterFlash = true) }

        viewModelScope.launch {
            delay(120)
            _uiState.update { it.copy(showShutterFlash = false) }
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processAndSaveCapturedImage(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraViewModel", "Capture error: ${exception.message}", exception)
                    _uiState.update { it.copy(isCapturing = false) }
                    showToast("Capture failed: ${exception.message}")
                }
            }
        )
    }

    private fun processAndSaveCapturedImage(image: ImageProxy) {
        viewModelScope.launch {
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                // Handle rotation
                val rotationDegrees = image.imageInfo.rotationDegrees
                val rotatedBitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                } else {
                    originalBitmap
                }
                image.close()

                val currentState = _uiState.value
                val filter = currentState.selectedFilter

                // Apply live filter if one is selected
                val finalBitmap = if (filter != CameraFilter.NORMAL) {
                    PhotoEditorProcessor.processImage(
                        rotatedBitmap,
                        EditAdjustments(selectedFilter = filter)
                    )
                } else {
                    rotatedBitmap
                }

                val savedItem = storageManager.savePhoto(
                    bitmap = finalBitmap,
                    zoomRatio = currentState.currentZoom,
                    filterName = if (filter != CameraFilter.NORMAL) filter.displayName else null,
                    isRaw = currentState.isRawMode
                )

                _uiState.update {
                    it.copy(
                        isCapturing = false,
                        lastCapturedItem = savedItem,
                        statusMessage = if (currentState.isRawMode) "Saved RAW DNG + High-Res Photo" else "Saved to Photos folder"
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error saving photo", e)
                _uiState.update { it.copy(isCapturing = false) }
                showToast("Error processing captured photo")
            }
        }
    }

    fun startVideoRecording(videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?) {
        if (videoCapture == null) {
            showToast("Video recorder not ready")
            return
        }

        val zoomRatio = _uiState.value.currentZoom
        val (outputFile, contentValues) = storageManager.createNewVideoOutputFile(zoomRatio)

        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        var pendingRecording = videoCapture.output
            .prepareRecording(getApplication(), outputOptions)

        if (!_uiState.value.isAudioMuted) {
            try {
                pendingRecording = pendingRecording.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w("CameraViewModel", "Audio permission not granted, continuing without audio", e)
            }
        }

        try {
            activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(getApplication())) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        _uiState.update {
                            it.copy(
                                isRecording = true,
                                isRecordingPaused = false,
                                recordingDurationSeconds = 0
                            )
                        }
                        startDurationTimer()
                    }
                    is VideoRecordEvent.Pause -> {
                        _uiState.update { it.copy(isRecordingPaused = true) }
                    }
                    is VideoRecordEvent.Resume -> {
                        _uiState.update { it.copy(isRecordingPaused = false) }
                    }
                    is VideoRecordEvent.Finalize -> {
                        stopDurationTimer()
                        _uiState.update {
                            it.copy(
                                isRecording = false,
                                isRecordingPaused = false,
                                recordingDurationSeconds = 0
                            )
                        }
                        if (recordEvent.hasError()) {
                            Log.e("CameraViewModel", "Video recording error: ${recordEvent.cause}")
                            showToast("Recording error: ${recordEvent.cause?.message ?: "Check camera encoder"}")
                        } else {
                            refreshLastMedia()
                            showToast("Video saved to Videos folder")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Failed to start recording", e)
            showToast("Cannot start video recording on this device/emulator: ${e.message}")
        }
    }

    fun pauseResumeRecording() {
        val recording = activeRecording ?: return
        if (_uiState.value.isRecordingPaused) {
            recording.resume()
        } else {
            recording.pause()
        }
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun toggleAudioMute() {
        _uiState.update { it.copy(isAudioMuted = !it.isAudioMuted) }
    }

    private fun startDurationTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_uiState.value.isRecording) {
                delay(1000)
                if (!_uiState.value.isRecordingPaused) {
                    _uiState.update { it.copy(recordingDurationSeconds = it.recordingDurationSeconds + 1) }
                }
            }
        }
    }

    private fun stopDurationTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    fun refreshLastMedia() {
        viewModelScope.launch {
            val all = storageManager.loadAllMedia()
            _uiState.update { it.copy(lastCapturedItem = all.firstOrNull()) }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }
}
