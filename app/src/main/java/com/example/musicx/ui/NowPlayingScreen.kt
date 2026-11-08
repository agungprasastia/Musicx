package com.example.musicx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicx.data.MusicSample

@Composable
fun NowPlayingScreen(
    track: MusicSample,
    isDemoPlaying: Boolean,
    onPlayPause: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⌄  NOW PLAYING", Modifier.clickable(onClick = onBack), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("•••", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MusicArtwork(track, Modifier.fillMaxWidth().aspectRatio(1f).align(Alignment.CenterHorizontally))
        Text(track.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("DEMO PREVIEW · NO AUDIO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(progress = { 0.38f }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Demo timeline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(track.durationLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            Text("⇄", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("|◀", color = MaterialTheme.colorScheme.onSurface)
            Text(if (isDemoPlaying) "Ⅱ" else "▶", Modifier.clickable(onClick = onPlayPause), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("▶|", color = MaterialTheme.colorScheme.onSurface)
            Text("☷", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Lyrics · Demo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Queue · Demo", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
