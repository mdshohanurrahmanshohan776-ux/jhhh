package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import com.example.model.FlashMode
import com.example.model.GridMode
import com.example.model.TimerMode
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CameraControlBacking
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopControlBar(
    flashMode: FlashMode,
    isRawMode: Boolean,
    timerMode: TimerMode,
    gridMode: GridMode,
    isLevelEnabled: Boolean,
    onCycleFlash: () -> Unit,
    onToggleRaw: () -> Unit,
    onCycleTimer: () -> Unit,
    onCycleGrid: () -> Unit,
    onToggleLevel: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("top_control_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode Icon Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (flashMode != FlashMode.OFF) Color(0x33FFA000) else Color.Transparent)
                .clickable { onCycleFlash() }
                .testTag("top_flash_button")
        ) {
            val icon = when (flashMode) {
                FlashMode.OFF -> Icons.Default.FlashOff
                FlashMode.AUTO -> Icons.Default.FlashAuto
                FlashMode.ON -> Icons.Default.FlashOn
                FlashMode.TORCH -> Icons.Default.Highlight
            }
            val tint = when (flashMode) {
                FlashMode.OFF -> TextSecondary
                FlashMode.AUTO -> AccentGold
                FlashMode.ON -> AccentGold
                FlashMode.TORCH -> AccentGold
            }
            Icon(imageVector = icon, contentDescription = "Flash", tint = tint, modifier = Modifier.size(20.dp))
        }

        // RAW Badge Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isRawMode) AccentGold else Color(0x33FFFFFF))
                .border(1.dp, if (isRawMode) AccentGold else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable { onToggleRaw() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("top_raw_toggle_button")
        ) {
            Text(
                text = if (isRawMode) "RAW" else "JPEG",
                color = if (isRawMode) Color.Black else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Timer Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (timerMode != TimerMode.OFF) Color(0x3300E5FF) else Color.Transparent)
                .clickable { onCycleTimer() }
                .testTag("top_timer_button")
        ) {
            if (timerMode == TimerMode.OFF) {
                Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = TextSecondary, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = timerMode.label,
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Grid Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (gridMode != GridMode.NONE) Color(0x33FFFFFF) else Color.Transparent)
                .clickable { onCycleGrid() }
                .testTag("top_grid_button")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = "Grid",
                tint = if (gridMode != GridMode.NONE) AccentGold else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Level Sensor Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isLevelEnabled) Color(0x33FFFFFF) else Color.Transparent)
                .clickable { onToggleLevel() }
                .testTag("top_level_sensor_button")
        ) {
            Icon(
                imageVector = Icons.Default.Straighten,
                contentDescription = "Horizon Level",
                tint = if (isLevelEnabled) AccentCyan else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Settings / More Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable { onOpenSettings() }
                .testTag("top_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
