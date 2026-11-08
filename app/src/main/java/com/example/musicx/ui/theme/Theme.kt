package com.example.musicx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MusicxColorScheme = darkColorScheme(
    primary = MusicxAccent,
    onPrimary = MusicxBackground,
    secondary = MusicxMuted,
    background = MusicxBackground,
    onBackground = MusicxText,
    surface = MusicxSurface,
    onSurface = MusicxText,
    surfaceVariant = MusicxSurfaceVariant,
    onSurfaceVariant = MusicxMuted
)

@Composable
fun MusicxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MusicxColorScheme,
        typography = Typography,
        content = content
    )
}
