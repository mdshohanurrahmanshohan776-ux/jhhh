package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraFilter
import com.example.model.MediaItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorTab
import com.example.viewmodel.EditorViewModel
import java.util.Locale

@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onSaved: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBack() }

    LaunchedEffect(mediaItem.id) {
        viewModel.initializeWithMedia(mediaItem)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("photo_editor_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Editor Bar
            EditorTopBar(
                isSaving = uiState.isSaving,
                onBack = onBack,
                onSave = { viewModel.saveEditedPhoto(onSaved) },
                onCompareStart = { viewModel.setIsComparing(true) },
                onCompareEnd = { viewModel.setIsComparing(false) }
            )

            // 2. Image Canvas Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = if (uiState.isComparing) {
                    uiState.originalBitmap
                } else {
                    uiState.previewBitmap
                }

                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Edited Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (uiState.isProcessing) {
                    CircularProgressIndicator(color = AccentGold)
                }

                if (uiState.isComparing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ORIGINAL PHOTO",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Category Tabs (Filters | Adjust | Transform)
            TabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = CameraSurface,
                contentColor = AccentGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                        color = AccentGold
                    )
                }
            ) {
                EditorTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                color = if (uiState.activeTab == tab) AccentGold else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("editor_tab_${tab.name}")
                    )
                }
            }

            // 4. Tab Content Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(CameraSurface)
                    .padding(12.dp)
            ) {
                when (uiState.activeTab) {
                    EditorTab.FILTERS -> {
                        FiltersPanel(
                            selectedFilter = uiState.adjustments.selectedFilter,
                            onSelectFilter = { viewModel.setFilter(it) }
                        )
                    }
                    EditorTab.ADJUST -> {
                        AdjustmentsPanel(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                    EditorTab.CROP_ROTATE -> {
                        TransformPanel(
                            adjustments = uiState.adjustments,
                            onRotate = { viewModel.rotateClockwise() },
                            onFlip = { viewModel.flipHorizontal() },
                            onReset = { viewModel.resetAdjustments() }
                        )
                    }
                }
            }
        }

        // Saving Spinner Overlay
        if (uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Saving high-resolution photo...", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onCompareStart: () -> Unit,
    onCompareEnd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CameraSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = TextPrimary)
        }

        Text(
            text = "Pro Studio Editor",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Touch & hold compare button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CameraSurfaceVariant)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onCompareStart()
                                tryAwaitRelease()
                                onCompareEnd()
                            }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("editor_compare_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Compare, contentDescription = "Compare", tint = AccentGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hold", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Save Copy Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGold)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("editor_save_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures { if (!isSaving) onSave() }
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FiltersPanel(
    selectedFilter: CameraFilter,
    onSelectFilter: (CameraFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("editor_filters_row")
    ) {
        items(CameraFilter.entries) { filter ->
            val isSelected = filter == selectedFilter

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) AccentGold else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .background(CameraSurfaceVariant)
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { onSelectFilter(filter) }
                    }
                    .testTag("editor_filter_${filter.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(filter.previewHue))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = filter.displayName,
                    color = if (isSelected) AccentGold else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AdjustmentsPanel(
    uiState: com.example.viewmodel.EditorUiState,
    viewModel: EditorViewModel
) {
    val adj = uiState.adjustments

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SliderRow("Exposure", adj.exposure, -100f..100f) { viewModel.updateExposure(it) }
        SliderRow("Contrast", adj.contrast, -100f..100f) { viewModel.updateContrast(it) }
        SliderRow("Saturation", adj.saturation, -100f..100f) { viewModel.updateSaturation(it) }
        SliderRow("Warmth", adj.warmth, -100f..100f) { viewModel.updateWarmth(it) }
        SliderRow("Tint", adj.tint, -100f..100f) { viewModel.updateTint(it) }
        SliderRow("Vignette", adj.vignette, 0f..100f) { viewModel.updateVignette(it) }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = String.format(Locale.US, "%+d", value.toInt()),
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentGold,
                activeTrackColor = AccentGold,
                inactiveTrackColor = Color(0x33FFFFFF)
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}

@Composable
private fun TransformPanel(
    adjustments: com.example.editor.EditAdjustments,
    onRotate: () -> Unit,
    onFlip: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rotate 90
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CameraSurfaceVariant)
                .padding(14.dp)
                .pointerInput(Unit) { detectTapGestures { onRotate() } }
                .testTag("editor_rotate_button")
        ) {
            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = AccentCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Rotate 90° (${adjustments.rotationDegrees}°)", color = TextPrimary, fontSize = 11.sp)
        }

        // Flip
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CameraSurfaceVariant)
                .padding(14.dp)
                .pointerInput(Unit) { detectTapGestures { onFlip() } }
                .testTag("editor_flip_button")
        ) {
            Icon(Icons.Default.Flip, contentDescription = "Flip", tint = AccentCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(if (adjustments.flipHorizontal) "Flipped" else "Normal", color = TextPrimary, fontSize = 11.sp)
        }

        // Reset
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CameraSurfaceVariant)
                .padding(14.dp)
                .pointerInput(Unit) { detectTapGestures { onReset() } }
                .testTag("editor_reset_button")
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = AccentGold, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Reset All", color = TextPrimary, fontSize = 11.sp)
        }
    }
}
