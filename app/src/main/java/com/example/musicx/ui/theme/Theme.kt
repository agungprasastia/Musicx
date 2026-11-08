package com.example.musicx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MusicxColorScheme = darkColorScheme(
    primary = MusicxAccent,
    onPrimary = Color(0xFF1C0F08),
    primaryContainer = Color(0xFF3B2217),
    onPrimaryContainer = Color(0xFFFFDBCF),
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
