package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color.Black,
    primaryContainer = CameraSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    tertiary = AccentRed,
    background = CameraBackground,
    onBackground = TextPrimary,
    surface = CameraSurface,
    onSurface = TextPrimary,
    surfaceVariant = CameraSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun UltraWideCameraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
