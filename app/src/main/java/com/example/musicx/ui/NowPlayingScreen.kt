package com.example.musicx.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
    onSetSpeed: (Float) -> Unit = {},
    onSetSkipSilence: (Boolean) -> Unit = {},
    onStartSleepTimer: (Long) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
) {
    val track = state.currentTrack
    var draggedPosition by remember(track?.id) { mutableStateOf<Float?>(null) }
    val durationMs = state.durationMs.coerceAtLeast(0)
    val sliderMax = if (durationMs > 0) durationMs.toFloat() else 1f
    val sliderValue = draggedPosition
        ?: state.positionMs.coerceIn(0, durationMs).toFloat()

    val trackAccent = remember(track?.id) { track?.let(::trackDynamicColor) ?: Color.Transparent }

    Column(
        Modifier.fillMaxSize()
            .background(if (track != null) trackAccent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "⌄  NOW PLAYING",
                Modifier.clickable(role = Role.Button, onClick = onBack).padding(vertical = 8.dp),
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
                onSetSpeed = onSetSpeed,
                onSetSkipSilence = onSetSkipSilence,
                onStartSleepTimer = onStartSleepTimer,
                onCancelSleepTimer = onCancelSleepTimer,
                onOpenEqualizer = onOpenEqualizer,
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
    onSetSpeed: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onStartSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    Crossfade(targetState = track.id, animationSpec = tween(400), label = "artwork") { _ ->
        CatalogArtwork(track, Modifier.fillMaxWidth().aspectRatio(1f))
    }
    Text(track.title, style = MaterialTheme.typography.headlineMedium)
    Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
    track.album?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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

    PlaybackControls(
        speed = state.speed,
        skipSilence = state.skipSilence,
        sleepTimerRemainingMs = state.sleepTimerRemainingMs,
        onSetSpeed = onSetSpeed,
        onSetSkipSilence = onSetSkipSilence,
        onStartSleepTimer = onStartSleepTimer,
        onCancelSleepTimer = onCancelSleepTimer,
        onOpenEqualizer = onOpenEqualizer,
    )

    SectionHeading("Lyrics")
    Text(
        "Lyrics are not available. A lyrics provider has not been configured.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (state.queue.size > 1) {
        SectionHeading("Queue")
        state.queue.forEachIndexed { index, queued ->
            val isCurrent = index == state.currentIndex
            Row(
                Modifier.fillMaxWidth()
                    .then(if (isCurrent) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) else Modifier)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CatalogArtwork(queued, Modifier.size(40.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        queued.title,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    Text(queued.artist, style = MaterialTheme.typography.labelSmall)
                }
                if (isCurrent) Text("▶", color = MaterialTheme.colorScheme.primary)
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

private fun trackDynamicColor(track: CatalogItem.Track): Color {
    val hue = (track.id.hashCode() and 0x7FFFFFFF) % 360
    return Color.hsl(hue.toFloat(), 0.45f, 0.38f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaybackControls(
    speed: Float,
    skipSilence: Boolean,
    sleepTimerRemainingMs: Long?,
    onSetSpeed: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onStartSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    SectionHeading("Playback")

    Text("Speed", style = MaterialTheme.typography.labelMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PlaybackUiState.SPEED_OPTIONS.forEach { option ->
            FilterChip(
                selected = (speed - option).let { it > -0.01f && it < 0.01f },
                onClick = { onSetSpeed(option) },
                label = { Text("${option}x") },
            )
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Skip silence", style = MaterialTheme.typography.labelMedium)
        Switch(checked = skipSilence, onCheckedChange = onSetSkipSilence)
    }

    Text("Sleep timer", style = MaterialTheme.typography.labelMedium)
    if (sleepTimerRemainingMs != null) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Stops in ${formatDuration(sleepTimerRemainingMs)}",
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onCancelSleepTimer) { Text("Cancel") }
        }
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PlaybackUiState.SLEEP_TIMER_PRESETS_MS.forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onStartSleepTimer(preset) },
                    label = { Text("${preset / 60_000} min") },
                )
            }
        }
    }

    TextButton(onClick = onOpenEqualizer) { Text("System Equalizer") }
}
