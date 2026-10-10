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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FocusMode
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CameraControlBacking
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ManualFocusSlider(
    focusMode: FocusMode,
    focusDistance: Float,
    onFocusModeChanged: (FocusMode) -> Unit,
    onFocusDistanceChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CameraControlBacking, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("manual_focus_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FOCUS MODE",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            // AF / MF switch pills
            Row(
                modifier = Modifier
                    .background(Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (focusMode == FocusMode.AF) Color.White else Color.Transparent)
                        .clickable { onFocusModeChanged(FocusMode.AF) }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .testTag("af_toggle_button")
                ) {
                    Text(
                        text = "AF",
                        color = if (focusMode == FocusMode.AF) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (focusMode == FocusMode.MF) AccentCyan else Color.Transparent)
                        .clickable { onFocusModeChanged(FocusMode.MF) }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                        .testTag("mf_toggle_button")
                ) {
                    Text(
                        text = "MF",
                        color = if (focusMode == FocusMode.MF) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // When MF is selected, show distance slider
        AnimatedVisibility(
            visible = focusMode == FocusMode.MF,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🌷 Macro (0.0)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f%%", focusDistance * 100),
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🏔️ Infinity (1.0)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Slider(
                    value = focusDistance,
                    onValueChange = onFocusDistanceChanged,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_distance_slider")
                )
            }
        }
    }
}
