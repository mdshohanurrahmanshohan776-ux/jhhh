package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LensPreset
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CameraControlBacking
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ZoomControlBar(
    currentZoom: Float,
    selectedLens: LensPreset,
    onZoomChanged: (Float) -> Unit,
    onLensSelected: (LensPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFineSlider by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("zoom_control_bar")
    ) {
        // Continuous fine slider (expandable or can be toggled)
        AnimatedVisibility(
            visible = showFineSlider,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(CameraControlBacking, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0.5x Ultra-Wide",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format(Locale.US, "%.1fx", currentZoom),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "10x SuperZoom",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Slider(
                    value = currentZoom,
                    onValueChange = { onZoomChanged(it) },
                    valueRange = 0.5f..10.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGold,
                        activeTrackColor = AccentGold,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fine_zoom_slider")
                )
            }
        }

        // Lens preset pills (0.5x, 1x, 2x, 5x, 10x)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(CameraControlBacking, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            LensPreset.entries.forEach { preset ->
                val isSelected = kotlin.math.abs(preset.ratio - currentZoom) < 0.15f
                val isUltraWide = preset == LensPreset.ULTRA_WIDE_05

                val bg = when {
                    isSelected && isUltraWide -> AccentGold
                    isSelected -> Color.White
                    else -> Color.Transparent
                }
                val textColor = when {
                    isSelected -> Color.Black
                    isUltraWide -> AccentGold
                    else -> TextPrimary
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 46.dp, height = 36.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable {
                            onLensSelected(preset)
                        }
                        .testTag("lens_preset_${preset.label}")
                ) {
                    Text(
                        text = preset.label,
                        color = textColor,
                        fontSize = if (isSelected) 13.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Toggle fine zoom slider button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (showFineSlider) AccentCyan else Color(0x22FFFFFF))
                    .clickable { showFineSlider = !showFineSlider }
                    .testTag("toggle_fine_zoom_button")
            ) {
                Text(
                    text = if (showFineSlider) "✕" else "⚙",
                    color = if (showFineSlider) Color.Black else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
