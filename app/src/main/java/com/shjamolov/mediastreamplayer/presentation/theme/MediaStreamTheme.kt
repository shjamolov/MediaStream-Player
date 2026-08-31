package com.shjamolov.mediastreamplayer.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val AppBackground = Color(0xFF07131D)
val AppSurface = Color(0xFF0D202E)
val AppSurfaceRaised = Color(0xFF142D3D)
val AppAccent = Color(0xFF18BDF2)
val AppTextSecondary = Color(0xFF91A8B8)
val AppGold = Color(0xFFFFC857)

private val MediaStreamColors = darkColorScheme(
    primary = AppAccent,
    onPrimary = Color(0xFF00131B),
    secondary = Color(0xFF73D7F5),
    background = AppBackground,
    onBackground = Color(0xFFF4F8FA),
    surface = AppSurface,
    onSurface = Color(0xFFF4F8FA),
    surfaceVariant = AppSurfaceRaised,
    onSurfaceVariant = AppTextSecondary,
    error = Color(0xFFFF8A8A),
)

@Composable
fun MediaStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MediaStreamColors, content = content)
}
