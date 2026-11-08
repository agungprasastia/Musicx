package com.example.musicx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicx.playback.PlaybackStatus
import com.example.musicx.playback.PlaybackUiState
import com.example.musicx.core.model.CatalogItem

@Composable
fun NowPlayingScreen(
    state: PlaybackUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val track = state.currentTrack
    var draggedPosition by remember(track?.id) { mutableStateOf<Float?>(null) }
    val durationMs = state.durationMs.coerceAtLeast(0)
    val sliderMax = if (durationMs > 0) durationMs.toFloat() else 1f
    val sliderValue = draggedPosition
        ?: state.positionMs.coerceIn(0, durationMs).toFloat()

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "⌄  NOW PLAYING",
                Modifier.clickable(onClick = onBack),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(statusLabel(state.status), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (track == null) {
            Text("Nothing playing", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Choose a song from Home or Search to resolve a stream.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TrackDetails(
                state = state,
                track = track,
                sliderValue = sliderValue,
                sliderMax = sliderMax,
                durationMs = durationMs,
                sliderEnabled = durationMs > 0 && state.status != PlaybackStatus.RESOLVING,
                onSliderChange = { draggedPosition = it },
                onSliderFinished = {
                    draggedPosition?.let { onSeek(it.toLong()) }
                    draggedPosition = null
                },
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onRetry = onRetry,
            )
        }

        Text(
            "YouTube Music guest access is unofficial. Playback can stop when the service changes.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrackDetails(
    state: PlaybackUiState,
    track: CatalogItem.Track,
    sliderValue: Float,
    sliderMax: Float,
    durationMs: Long,
    sliderEnabled: Boolean,
    onSliderChange: (Float) -> Unit,
    onSliderFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onRetry: () -> Unit,
) {
    CatalogArtwork(track, Modifier.fillMaxWidth().aspectRatio(1f))
    Text(track.title, style = MaterialTheme.typography.headlineMedium)
    Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(statusLine(state), color = MaterialTheme.colorScheme.onSurfaceVariant)

    Slider(
        value = sliderValue,
        onValueChange = onSliderChange,
        onValueChangeFinished = onSliderFinished,
        valueRange = 0f..sliderMax,
        enabled = sliderEnabled,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDuration(sliderValue.toLong()), style = MaterialTheme.typography.labelSmall)
        Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall)
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSkipPrevious, enabled = state.hasPrevious) { Text("Previous") }
        Button(
            onClick = onPlayPause,
            enabled = state.status != PlaybackStatus.RESOLVING && state.status != PlaybackStatus.ERROR,
        ) {
            Text(if (state.isPlaying) "Pause" else "Play")
        }
        TextButton(onClick = onSkipNext, enabled = state.hasNext) { Text("Next") }
    }

    if (state.status == PlaybackStatus.ERROR) {
        state.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = onRetry) { Text("Retry") }
    }

    val upcoming = state.queue.drop(state.currentIndex + 1)
    if (upcoming.isNotEmpty()) {
        SectionHeading("Up next")
        upcoming.take(8).forEach { queued ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CatalogArtwork(queued, Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(queued.title, color = MaterialTheme.colorScheme.onBackground)
                    Text(queued.artist, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun statusLabel(status: PlaybackStatus): String = when (status) {
    PlaybackStatus.IDLE -> "IDLE"
    PlaybackStatus.RESOLVING -> "RESOLVING"
    PlaybackStatus.BUFFERING -> "BUFFERING"
    PlaybackStatus.READY -> "READY"
    PlaybackStatus.PLAYING -> "PLAYING"
    PlaybackStatus.PAUSED -> "PAUSED"
    PlaybackStatus.ENDED -> "ENDED"
    PlaybackStatus.ERROR -> "ERROR"
}

private fun statusLine(state: PlaybackUiState): String = when (state.status) {
    PlaybackStatus.IDLE -> "Nothing playing"
    PlaybackStatus.RESOLVING -> "Resolving stream…"
    PlaybackStatus.BUFFERING -> "Buffering audio…"
    PlaybackStatus.READY -> "Ready"
    PlaybackStatus.PLAYING -> "Playing"
    PlaybackStatus.PAUSED -> "Paused"
    PlaybackStatus.ENDED -> "Finished"
    PlaybackStatus.ERROR -> state.errorMessage ?: "Playback error"
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
