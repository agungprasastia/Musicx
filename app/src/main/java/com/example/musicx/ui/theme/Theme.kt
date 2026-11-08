package com.example.musicx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MusicxColorScheme = darkColorScheme(
    primary = MusicxAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1B3824),
    onPrimaryContainer = Color(0xFFB8F5CD),
    secondary = MusicxMuted,
    onSecondary = MusicxText,
    secondaryContainer = MusicxSurfaceVariant,
    onSecondaryContainer = MusicxText,
    background = MusicxBackground,
    onBackground = MusicxText,
    surface = MusicxSurface,
    onSurface = MusicxText,
    surfaceVariant = MusicxSurfaceVariant,
    onSurfaceVariant = MusicxMuted,
    outline = MusicxOutline,
)

@Composable
fun MusicxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MusicxColorScheme,
        typography = Typography,
        content = content
    )
}
