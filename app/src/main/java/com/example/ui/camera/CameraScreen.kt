package com.example.ui.camera

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.CameraMode
import com.example.model.LensPreset
import com.example.ui.components.CameraControls
import com.example.ui.components.CameraViewfinder
import com.example.ui.components.ExposureSlider
import com.example.ui.components.FilterSelectorStrip
import com.example.ui.components.ManualFocusSlider
import com.example.ui.components.TopControlBar
import com.example.ui.components.ZoomControlBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProControls by remember { mutableStateOf(false) }
    var showFilterStrip by remember { mutableStateOf(false) }

    // Toast/Status messaging
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // Bind CameraX dynamically according to camera facing and mode
    DisposableEffect(uiState.isFrontCamera, uiState.mode, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imgCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            imageCapture = imgCapture

            val cameraSelector = if (uiState.isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()

                if (uiState.mode == CameraMode.VIDEO) {
                    try {
                        val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
                        val videoCaps = Recorder.getVideoCapabilities(cameraInfo)
                        val supportedQualities = videoCaps.getSupportedQualities(DynamicRange.SDR)

                        if (supportedQualities.isNotEmpty()) {
                            val qualitySelector = QualitySelector.fromOrderedList(
                                supportedQualities,
                                FallbackStrategy.lowerQualityOrHigherThan(supportedQualities.last())
                            )
                            val recorder = Recorder.Builder()
                                .setQualitySelector(qualitySelector)
                                .build()
                            val vidCapture = VideoCapture.withOutput(recorder)
                            videoCapture = vidCapture

                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                vidCapture
                            )
                            viewModel.bindCamera(camera)
                        } else {
                            // Camera hardware has no supported video encoder profiles
                            Log.w("CameraScreen", "No supported encoder profiles on camera, falling back to ImageCapture")
                            videoCapture = null
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imgCapture
                            )
                            viewModel.bindCamera(camera)
                        }
                    } catch (videoError: Exception) {
                        Log.w("CameraScreen", "VideoCapture binding failed, falling back to Preview + ImageCapture", videoError)
                        videoCapture = null
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imgCapture
                        )
                        viewModel.bindCamera(camera)
                    }
                } else {
                    // PHOTO or PRO mode
                    videoCapture = null
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imgCapture
                    )
                    viewModel.bindCamera(camera)
                }
            } catch (exc: Exception) {
                Log.e("CameraScreen", "Use case binding failed", exc)
            }
        }, mainExecutor)

        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e("CameraScreen", "Error unbinding camera", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen_root")
    ) {
        // 1. Fullscreen Viewfinder
        CameraViewfinder(
            previewView = previewView,
            gridMode = uiState.gridMode,
            isLevelEnabled = uiState.isLevelSensorEnabled,
            horizonAngle = uiState.horizonRollAngle,
            isHorizonLevel = uiState.isHorizonLevel,
            tapFocusPoint = uiState.tapFocusPoint,
            selectedFilter = uiState.selectedFilter,
            zoomRatio = uiState.currentZoom,
            remainingCountdown = uiState.remainingCountdown,
            showShutterFlash = uiState.showShutterFlash,
            onTapToFocus = { x, y, w, h -> viewModel.onTapToFocus(x, y, w, h) }
        )

        // 2. Top Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            TopControlBar(
                flashMode = uiState.flashMode,
                isRawMode = uiState.isRawMode,
                timerMode = uiState.timerMode,
                gridMode = uiState.gridMode,
                isLevelEnabled = uiState.isLevelSensorEnabled,
                onCycleFlash = { viewModel.cycleFlashMode() },
                onToggleRaw = { viewModel.toggleRawMode() },
                onCycleTimer = { viewModel.cycleTimerMode() },
                onCycleGrid = { viewModel.cycleGridMode() },
                onToggleLevel = {
                    // Level sensor toggle
                },
                onOpenSettings = { showSettingsDialog = true }
            )
        }

        // 3. Pro HUD Controls Overlay (Center / Bottom portion)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Filters strip if toggled or in Pro mode
            AnimatedVisibility(
                visible = showFilterStrip,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                FilterSelectorStrip(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { viewModel.setFilter(it) },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            // Pro manual controls (Exposure + Manual Focus)
            AnimatedVisibility(
                visible = uiState.mode == CameraMode.PRO || showProControls,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExposureSlider(
                        exposureEv = uiState.exposureCompensationEv,
                        exposureIndex = uiState.exposureCompensationIndex,
                        minIndex = uiState.hardwareInfo?.minExposureIndex ?: -4,
                        maxIndex = uiState.hardwareInfo?.maxExposureIndex ?: 4,
                        onExposureChanged = { viewModel.setExposureIndex(it) }
                    )

                    ManualFocusSlider(
                        focusMode = uiState.focusMode,
                        focusDistance = uiState.manualFocusDistance,
                        onFocusModeChanged = { viewModel.setFocusMode(it) },
                        onFocusDistanceChanged = { viewModel.setManualFocusDistance(it) }
                    )
                }
            }

            // Quick toggle row for Filter & Pro panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter pill toggle
                Box(
                    modifier = Modifier
                        .background(if (showFilterStrip) AccentGold else Color(0x66000000), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showFilterStrip = !showFilterStrip }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("toggle_filters_pill")
                ) {
                    Text(
                        text = "🎨 ${uiState.selectedFilter.displayName}",
                        color = if (showFilterStrip) Color.Black else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pro Manual Tools toggle
                Box(
                    modifier = Modifier
                        .background(if (showProControls) AccentCyan else Color(0x66000000), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showProControls = !showProControls }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("toggle_pro_controls_pill")
                ) {
                    Text(
                        text = "⚡ EV & FOCUS",
                        color = if (showProControls) Color.Black else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Zoom Control Bar (0.5x, 1x, 2x, 5x, 10x)
            ZoomControlBar(
                currentZoom = uiState.currentZoom,
                selectedLens = uiState.selectedLens,
                onZoomChanged = { viewModel.setZoom(it) },
                onLensSelected = { viewModel.selectLensPreset(it) }
            )

            // Bottom Shutter Controls
            CameraControls(
                mode = uiState.mode,
                isRecording = uiState.isRecording,
                isRecordingPaused = uiState.isRecordingPaused,
                recordingDurationSeconds = uiState.recordingDurationSeconds,
                isAudioMuted = uiState.isAudioMuted,
                lastMediaItem = uiState.lastCapturedItem,
                onModeChanged = { viewModel.setCameraMode(it) },
                onShutterClicked = {
                    if (uiState.mode == CameraMode.VIDEO) {
                        if (uiState.isRecording) {
                            viewModel.stopVideoRecording()
                        } else if (videoCapture != null) {
                            viewModel.startVideoRecording(videoCapture)
                        } else {
                            Toast.makeText(context, "Hardware video encoder unavailable on this device/emulator. Photo captured instead.", Toast.LENGTH_SHORT).show()
                            viewModel.capturePhotoWithTimer(imageCapture)
                        }
                    } else {
                        viewModel.capturePhotoWithTimer(imageCapture)
                    }
                },
                onPauseResumeVideo = { viewModel.pauseResumeRecording() },
                onToggleAudioMute = { viewModel.toggleAudioMute() },
                onFlipCamera = { viewModel.toggleFrontBack() },
                onOpenGallery = onNavigateToGallery
            )
        }

        // Settings / Info Dialog
        if (showSettingsDialog) {
            CameraSettingsDialog(
                uiState = uiState,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun CameraSettingsDialog(
    uiState: com.example.viewmodel.CameraUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "UltraWide Pro Camera Specs",
                color = AccentGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "LENS & SENSOR SPECIFICATIONS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                val hw = uiState.hardwareInfo
                DetailRow("Ultra-Wide Lens Status:", if (hw?.hasPhysicalUltraWide == true) "Hardware Physical 0.5x Sensor Available" else "0.5x Wide-Angle Simulation Enabled")
                DetailRow("Supported Zoom Range:", "${hw?.minZoomRatio ?: 0.5f}x — ${hw?.maxZoomRatio ?: 10.0f}x")
                DetailRow("RAW DNG Capture:", if (hw?.supportsRawCapture == true) "Supported (Direct Sensor)" else "High-Res Uncompressed")
                DetailRow("Exposure Range:", "${hw?.minExposureIndex ?: -4} EV to +${hw?.maxExposureIndex ?: 4} EV")

                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "AUTO FILE ORGANIZER PATHS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                DetailRow("📸 Photos Folder:", "Pictures/UltraWideCamera/Photos")
                DetailRow("🎥 Videos Folder:", "Movies/UltraWideCamera/Videos")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentGold, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CameraSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DetailRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
