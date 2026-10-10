package com.example.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CameraFilter
import com.example.model.GridMode
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.GridLine
import kotlin.math.roundToInt

@Composable
fun CameraViewfinder(
    previewView: PreviewView,
    gridMode: GridMode,
    isLevelEnabled: Boolean,
    horizonAngle: Float,
    isHorizonLevel: Boolean,
    tapFocusPoint: Pair<Float, Float>?,
    selectedFilter: CameraFilter,
    zoomRatio: Float,
    remainingCountdown: Int?,
    showShutterFlash: Boolean,
    onTapToFocus: (xPercent: Float, yPercent: Float, width: Float, height: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val xPercent = offset.x / size.width
                    val yPercent = offset.y / size.height
                    onTapToFocus(xPercent, yPercent, size.width.toFloat(), size.height.toFloat())
                }
            }
            .testTag("camera_viewfinder")
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val targetScale = if (zoomRatio <= 0.65f) 0.90f else 1.0f
        val animatedScale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = tween(durationMillis = 250),
            label = "preview_scale"
        )

        // 1. CameraX PreviewView
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .scale(animatedScale)
        )

        // 2. Live Filter preview tint overlay
        if (selectedFilter != CameraFilter.NORMAL) {
            val filterColor = when (selectedFilter) {
                CameraFilter.VIVID_WIDE -> Color(0x2200E5FF)
                CameraFilter.GOLDEN_HOUR -> Color(0x2AFFA000)
                CameraFilter.CINEMATIC -> Color(0x2200897B)
                CameraFilter.NOIR -> Color(0x40000000)
                CameraFilter.CYBERPUNK -> Color(0x28E040FB)
                CameraFilter.VINTAGE -> Color(0x268D6E63)
                CameraFilter.MOODY_FOREST -> Color(0x241B5E20)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filterColor)
            )
        }

        // 3. Grid Overlays (Rule of Thirds / Golden Ratio)
        if (gridMode != GridMode.NONE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (gridMode == GridMode.RULE_OF_THIRDS) {
                    val oneThirdW = w / 3f
                    val twoThirdW = w * 2f / 3f
                    val oneThirdH = h / 3f
                    val twoThirdH = h * 2f / 3f

                    // Vertical lines
                    drawLine(GridLine, Offset(oneThirdW, 0f), Offset(oneThirdW, h), strokeWidth = 1.5f)
                    drawLine(GridLine, Offset(twoThirdW, 0f), Offset(twoThirdW, h), strokeWidth = 1.5f)
                    // Horizontal lines
                    drawLine(GridLine, Offset(0f, oneThirdH), Offset(w, oneThirdH), strokeWidth = 1.5f)
                    drawLine(GridLine, Offset(0f, twoThirdH), Offset(w, twoThirdH), strokeWidth = 1.5f)
                } else if (gridMode == GridMode.GOLDEN_RATIO) {
                    val phi = 0.618f
                    val gw1 = w * (1f - phi)
                    val gw2 = w * phi
                    val gh1 = h * (1f - phi)
                    val gh2 = h * phi

                    drawLine(GridLine, Offset(gw1, 0f), Offset(gw1, h), strokeWidth = 1.5f)
                    drawLine(GridLine, Offset(gw2, 0f), Offset(gw2, h), strokeWidth = 1.5f)
                    drawLine(GridLine, Offset(0f, gh1), Offset(w, gh1), strokeWidth = 1.5f)
                    drawLine(GridLine, Offset(0f, gh2), Offset(w, gh2), strokeWidth = 1.5f)
                }
            }
        }

        // 4. Horizon Level indicator line
        if (isLevelEnabled) {
            val animatedAngle by animateFloatAsState(
                targetValue = horizonAngle,
                animationSpec = tween(durationMillis = 100),
                label = "horizon_angle"
            )
            val levelColor = if (isHorizonLevel) AccentGreen else Color(0x88FFFFFF)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(animatedAngle)
            ) {
                Canvas(modifier = Modifier.size(width = 160.dp, height = 24.dp)) {
                    val midY = size.height / 2f
                    // Left line
                    drawLine(
                        color = levelColor,
                        start = Offset(0f, midY),
                        end = Offset(50f, midY),
                        strokeWidth = if (isHorizonLevel) 4f else 2f
                    )
                    // Center crosshair/dot
                    drawCircle(
                        color = levelColor,
                        radius = if (isHorizonLevel) 4f else 3f,
                        center = Offset(size.width / 2f, midY)
                    )
                    // Right line
                    drawLine(
                        color = levelColor,
                        start = Offset(size.width - 50f, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = if (isHorizonLevel) 4f else 2f
                    )
                }
            }
        }

        // 5. Ultra-Wide Lens active badge
        if (zoomRatio <= 0.65f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(16.dp))
                    .border(1.dp, AccentGold, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentGold, CircleShape)
                    )
                    Text(
                        text = " 0.5x ULTRA-WIDE LENS ACTIVE",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // 6. Tap-To-Focus animated reticle
        tapFocusPoint?.let { (xPercent, yPercent) ->
            val reticleX = (xPercent * containerWidth - 36).roundToInt()
            val reticleY = (yPercent * containerHeight - 36).roundToInt()

            val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(reticleX, reticleY) }
                    .size(72.dp)
                    .border(2.dp, AccentGold, CircleShape)
            ) {
                // Focus center crosshair
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = size.width / 2f
                    drawLine(AccentGold, Offset(c - 8f, c), Offset(c + 8f, c), strokeWidth = 2f)
                    drawLine(AccentGold, Offset(c, c - 8f), Offset(c, c + 8f), strokeWidth = 2f)
                }
            }
        }

        // 7. Big countdown timer overlay (3, 2, 1)
        AnimatedVisibility(
            visible = remainingCountdown != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            remainingCountdown?.let { count ->
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0x99000000), CircleShape)
                        .border(3.dp, AccentGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = AccentGold,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 8. Shutter Flash Feedback
        if (showShutterFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }
}
