package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CameraMode
import com.example.model.MediaItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.ShutterBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CameraControls(
    mode: CameraMode,
    isRecording: Boolean,
    isRecordingPaused: Boolean,
    recordingDurationSeconds: Int,
    isAudioMuted: Boolean,
    lastMediaItem: MediaItem?,
    onModeChanged: (CameraMode) -> Unit,
    onShutterClicked: () -> Unit,
    onPauseResumeVideo: () -> Unit,
    onToggleAudioMute: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(top = 10.dp, bottom = 24.dp)
            .testTag("camera_controls_bar")
    ) {
        // Mode Selector: PHOTO | VIDEO | PRO
        if (!isRecording) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                CameraMode.entries.forEach { cameraMode ->
                    val isSelected = cameraMode == mode
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable { onModeChanged(cameraMode) }
                            .testTag("camera_mode_${cameraMode.name}")
                    ) {
                        Text(
                            text = cameraMode.label,
                            color = if (isSelected) AccentGold else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        } else {
            // Recording Duration HUD & Audio Mute
            val mins = recordingDurationSeconds / 60
            val secs = recordingDurationSeconds % 60
            val durationFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(AccentRed, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REC $durationFormatted",
                    color = AccentRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Audio Mute toggle
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isAudioMuted) AccentRed else Color(0x33FFFFFF))
                        .clickable { onToggleAudioMute() }
                        .padding(6.dp)
                        .testTag("audio_mute_toggle")
                ) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Audio toggle",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Shutter Row: [Gallery Button] [SHUTTER] [Flip Camera]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Gallery Thumbnail or Pause button during recording
            if (isRecording) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onPauseResumeVideo() }
                        .testTag("pause_resume_video_button")
                ) {
                    Icon(
                        imageVector = if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause / Resume",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CameraSurface)
                        .border(1.5.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { onOpenGallery() }
                        .testTag("gallery_thumbnail_button")
                ) {
                    if (lastMediaItem != null) {
                        AsyncImage(
                            model = lastMediaItem.uri,
                            contentDescription = "Last captured media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Open Gallery",
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Center: Main Shutter Button
            ShutterButton(
                mode = mode,
                isRecording = isRecording,
                onClick = onShutterClicked
            )

            // Right: Flip Camera Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(CameraSurface)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onFlipCamera() }
                    .testTag("flip_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(
    mode: CameraMode,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val isVideo = mode == CameraMode.VIDEO

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shutter_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(78.dp)
            .border(
                width = 4.dp,
                color = if (isVideo || isRecording) AccentRed else ShutterBorder,
                shape = CircleShape
            )
            .padding(5.dp)
            .clickable { onClick() }
            .testTag("main_shutter_button")
    ) {
        if (isRecording) {
            // Square stop icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .scale(pulseScale)
                    .background(AccentRed, RoundedCornerShape(6.dp))
            )
        } else if (isVideo) {
            // Red circular inner
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(AccentRed, CircleShape)
            )
        } else {
            // White circular inner for Photo
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
