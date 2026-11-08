package com.example.musicx.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.playback.PlaybackStatus
import com.example.musicx.playback.PlaybackUiState

private enum class NowPlayingTab(val label: String) {
    PLAYER("Player"),
    QUEUE("Queue"),
    LYRICS("Lyrics"),
    CONTROLS("Settings")
}

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
    var selectedTab by rememberSaveable { mutableStateOf(NowPlayingTab.PLAYER) }
    var draggedPosition by remember(track?.id) { mutableStateOf<Float?>(null) }
    val durationMs = state.durationMs.coerceAtLeast(0)
    val sliderMax = if (durationMs > 0) durationMs.toFloat() else 1f
    val sliderValue = draggedPosition
        ?: state.positionMs.coerceIn(0, durationMs).toFloat()

    val trackAccent = remember(track?.id) { track?.let(::trackDynamicColor) ?: Color.Transparent }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (track != null) trackAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable(role = Role.Button, onClick = onBack),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "⌄ NOW PLAYING",
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mode Selector Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NowPlayingTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        modifier = Modifier.clickable(role = Role.Tab) { selectedTab = tab },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            tab.label,
                            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (track == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nothing playing", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Choose a song from Home or Search to resolve a stream.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            when (selectedTab) {
                NowPlayingTab.PLAYER -> {
                    PlayerView(
                        state = state,
                        track = track,
                        sliderValue = sliderValue,
                        sliderMax = sliderMax,
                        durationMs = durationMs,
                        onSliderChange = { draggedPosition = it },
                        onSliderFinished = {
                            draggedPosition?.let { onSeek(it.toLong()) }
                            draggedPosition = null
                        },
                        onPlayPause = onPlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onRetry = onRetry,
                        onOpenQueue = { selectedTab = NowPlayingTab.QUEUE },
                        onOpenControls = { selectedTab = NowPlayingTab.CONTROLS }
                    )
                }

                NowPlayingTab.QUEUE -> {
                    QueueView(
                        state = state,
                        modifier = Modifier.weight(1f)
                    )
                }

                NowPlayingTab.LYRICS -> {
                    LyricsView(
                        modifier = Modifier.weight(1f)
                    )
                }

                NowPlayingTab.CONTROLS -> {
                    ControlsView(
                        state = state,
                        onSetSpeed = onSetSpeed,
                        onSetSkipSilence = onSetSkipSilence,
                        onStartSleepTimer = onStartSleepTimer,
                        onCancelSleepTimer = onCancelSleepTimer,
                        onOpenEqualizer = onOpenEqualizer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerView(
    state: PlaybackUiState,
    track: CatalogItem.Track,
    sliderValue: Float,
    sliderMax: Float,
    durationMs: Long,
    onSliderChange: (Float) -> Unit,
    onSliderFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onRetry: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenControls: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Artwork
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .padding(top = 10.dp)
                .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black, ambientColor = Color.Black)
        ) {
            Crossfade(targetState = track.id, animationSpec = tween(400), label = "artwork") { _ ->
                CatalogArtwork(track, Modifier.fillMaxSize())
            }
        }

        // Track Details
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                track.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            track.album?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                statusLine(state),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }

        // Seek Bar
        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                onValueChangeFinished = onSliderFinished,
                valueRange = 0f..sliderMax,
                enabled = durationMs > 0 && state.status != PlaybackStatus.RESOLVING,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(sliderValue.toLong()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Hero Transport Controls (Matching now-playing.html)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(enabled = state.hasPrevious, role = Role.Button, onClick = onSkipPrevious),
                color = if (state.hasPrevious) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = CircleShape
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("⏮", fontSize = 22.sp, color = if (state.hasPrevious) Color.White else Color.Gray)
                }
            }

            // Big Play/Pause Center Button
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        enabled = state.status != PlaybackStatus.RESOLVING && state.status != PlaybackStatus.ERROR,
                        role = Role.Button,
                        onClick = onPlayPause
                    ),
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.isPlaying) "⏸" else "▶",
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Next Button
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(enabled = state.hasNext, role = Role.Button, onClick = onSkipNext),
                color = if (state.hasNext) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = CircleShape
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("⏭", fontSize = 22.sp, color = if (state.hasNext) Color.White else Color.Gray)
                }
            }
        }

        if (state.status == PlaybackStatus.ERROR) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.errorMessage ?: "Playback error", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Retry")
                    }
                }
            }
        }

        // Quick Indicators Bar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenControls),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡ ${state.speed}x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (state.sleepTimerRemainingMs != null) {
                        Text("· ⏱ ${formatDuration(state.sleepTimerRemainingMs)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Surface(
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenQueue),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    "☷ Queue (${state.queue.size})",
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QueueView(
    state: PlaybackUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeading("Queue (${state.queue.size})")

        if (state.queue.isEmpty()) {
            Text("Queue is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.queue.forEachIndexed { index, queued ->
                val isCurrent = index == state.currentIndex
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CatalogArtwork(queued, Modifier.size(44.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                queued.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                            )
                            Text(queued.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isCurrent) {
                            Text("▶ Now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsView(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeading("Lyrics")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Lyrics are not available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "A lyrics provider has not been configured in this build. Synchronized lyrics from sources like LRCLIB or lrc.red can be added in a future milestone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlsView(
    state: PlaybackUiState,
    onSetSpeed: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onStartSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeading("Playback Settings")

        // Playback Speed
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Playback Speed", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlaybackUiState.SPEED_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = (state.speed - option).let { it > -0.01f && it < 0.01f },
                        onClick = { onSetSpeed(option) },
                        label = { Text("${option}x") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Skip Silence Switch
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Skip silence", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text("Automatically skip non-audible parts of tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.skipSilence, onCheckedChange = onSetSkipSilence)
            }
        }

        // Sleep Timer
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            if (state.sleepTimerRemainingMs != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Stops in ${formatDuration(state.sleepTimerRemainingMs)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Button(
                            onClick = onCancelSleepTimer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlaybackUiState.SLEEP_TIMER_PRESETS_MS.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { onStartSleepTimer(preset) },
                            label = { Text("${preset / 60_000} min") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // System Equalizer
        Button(
            onClick = onOpenEqualizer,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text("Launch System Equalizer", fontWeight = FontWeight.SemiBold)
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
    return Color.hsl(hue.toFloat(), 0.55f, 0.45f)
}
