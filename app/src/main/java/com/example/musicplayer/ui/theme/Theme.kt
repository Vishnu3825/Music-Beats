package com.example.musicplayer.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    primaryContainer = NeonGreenDim,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlue,
    onSecondary = Color.Black,
    secondaryContainer = SurfaceLight,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentPink,
    onTertiary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceMid,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    outlineVariant = SurfaceLight,
    error = Error,
    onError = Color.White
)

@Composable
fun MusicPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MusicTypography,
        content = content
    )
}
