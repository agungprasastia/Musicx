package com.example.musicx.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.musicx.playlist.PlaylistRepository

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
    playlistRepo: PlaylistRepository? = null,
) {
    val track = state.currentTrack
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showAddToPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showNewPlaylistPrompt by rememberSaveable { mutableStateOf(false) }

    val likedTracks by playlistRepo?.likedTracks?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val playlists by playlistRepo?.playlists?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val isFavorite = track != null && likedTracks.any { it.id == track.id }
    var isShuffle by rememberSaveable { mutableStateOf(false) }
    var isRepeat by rememberSaveable { mutableStateOf(false) }

    var draggedPosition by remember(track?.id) { mutableStateOf<Float?>(null) }
    val durationMs = state.durationMs.coerceAtLeast(0)
    val sliderMax = if (durationMs > 0) durationMs.toFloat() else 1f
    val sliderValue = draggedPosition
        ?: state.positionMs.coerceIn(0, durationMs).toFloat()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar: Spotify Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = {
                        if (showQueue) showQueue = false else onBack()
                    }),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = MusicIcons.ChevronDown,
                        contentDescription = "Close player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    if (showQueue) "QUEUE" else "PLAYING FROM PLAYLIST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    track?.album ?: "Musicx Library",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = { showSettingsDialog = !showSettingsDialog }),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = MusicIcons.Settings,
                        contentDescription = "Audio settings",
                        tint = if (showSettingsDialog) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
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
                    "Choose a song from Home or Search to begin listening.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else if (showQueue) {
            QueueSpotifyView(
                state = state,
                onClose = { showQueue = false },
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Artwork (Spotify Large Artwork)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(16.dp), spotColor = Color.Black, ambientColor = Color.Black)
                ) {
                    Crossfade(targetState = track.id, animationSpec = tween(350), label = "art") { _ ->
                        CatalogArtwork(track, Modifier.fillMaxSize())
                    }
                }

                // Song Title & Artist + Favorite Heart
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = {
                                    if (track != null) playlistRepo?.toggleLike(track)
                                }),
                            color = Color.Transparent,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = MusicIcons.Heart,
                                    contentDescription = if (isFavorite) "Liked" else "Like",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (playlistRepo != null && track != null) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button, onClick = {
                                        showAddToPlaylistDialog = true
                                    }),
                                color = Color.Transparent,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = MusicIcons.Plus,
                                        contentDescription = "Add to playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Status info (if error or resolving)
                if (state.status == PlaybackStatus.RESOLVING || state.status == PlaybackStatus.BUFFERING || state.status == PlaybackStatus.ERROR) {
                    Text(
                        statusLine(state),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.status == PlaybackStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                // Progress Bar
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { draggedPosition = it },
                        onValueChangeFinished = {
                            draggedPosition?.let { onSeek(it.toLong()) }
                            draggedPosition = null
                        },
                        valueRange = 0f..sliderMax,
                        enabled = durationMs > 0 && state.status != PlaybackStatus.RESOLVING,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
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

                // Spotify Primary Transport Bar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { isShuffle = !isShuffle }) {
                        Icon(
                            imageVector = MusicIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onSkipPrevious,
                        enabled = state.hasPrevious
                    ) {
                        Icon(
                            imageVector = MusicIcons.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (state.hasPrevious) Color.White else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Main Big Play/Pause
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .clickable(
                                enabled = state.status != PlaybackStatus.RESOLVING,
                                role = Role.Button,
                                onClick = onPlayPause
                            ),
                        color = Color.White,
                        shape = CircleShape
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isPlaying) MusicIcons.Pause else MusicIcons.Play,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Next
                    IconButton(
                        onClick = onSkipNext,
                        enabled = state.hasNext
                    ) {
                        Icon(
                            imageVector = MusicIcons.SkipNext,
                            contentDescription = "Next",
                            tint = if (state.hasNext) Color.White else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = { isRepeat = !isRepeat }) {
                        Icon(
                            imageVector = MusicIcons.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (state.status == PlaybackStatus.ERROR) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
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

                // Bottom Utility Row (Device / Speed on Left, Queue on Right)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = { showSettingsDialog = true }),
                        color = Color.Transparent
                    ) {
                        Row(
                            Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MusicIcons.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("${state.speed}x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (state.sleepTimerRemainingMs != null) {
                                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(
                                    imageVector = MusicIcons.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(formatDuration(state.sleepTimerRemainingMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = { showQueue = true }),
                        color = Color.Transparent
                    ) {
                        Row(
                            Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MusicIcons.Queue,
                                contentDescription = "Queue",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Queue (${state.queue.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Spotify Style Lyrics Card (Preview)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Lyrics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Synced lyrics from external providers will appear here in a future update.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Audio Settings Panel (when toggled)
                AnimatedVisibility(
                    visible = showSettingsDialog,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ControlsSpotifySheet(
                        state = state,
                        onSetSpeed = onSetSpeed,
                        onSetSkipSilence = onSetSkipSilence,
                        onStartSleepTimer = onStartSleepTimer,
                        onCancelSleepTimer = onCancelSleepTimer,
                        onOpenEqualizer = onOpenEqualizer,
                        onClose = { showSettingsDialog = false }
                    )
                }
            }
        }
    }

    if (showAddToPlaylistDialog && track != null && playlistRepo != null) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddToPlaylistDialog = false
                                showNewPlaylistPrompt = true
                            }
                            .padding(vertical = 8.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = MusicIcons.Plus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "New playlist",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (playlists.isEmpty()) {
                        Text(
                            "No custom playlists yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        playlists.forEach { pl ->
                            val alreadyIn = pl.tracks.any { it.id == track.id }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playlistRepo.addTrackToPlaylist(pl.id, track)
                                        showAddToPlaylistDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        pl.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (alreadyIn) {
                                        Text(
                                            "Added",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showNewPlaylistPrompt && track != null && playlistRepo != null) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPlaylistPrompt = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("My Playlist #${playlists.size + 1}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val created = playlistRepo.createPlaylist(newName)
                        playlistRepo.addTrackToPlaylist(created.id, track)
                        showNewPlaylistPrompt = false
                    }
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylistPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QueueSpotifyView(
    state: PlaybackUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Up Next (${state.queue.size})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onClose),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    "Done",
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (state.queue.isEmpty()) {
            Text("No upcoming tracks in queue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.queue.forEachIndexed { index, queued ->
                val isCurrent = index == state.currentIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
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
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CatalogArtwork(queued, Modifier.size(46.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                queued.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                queued.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isCurrent) {
                            Text(
                                "Now",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlsSpotifySheet(
    state: PlaybackUiState,
    onSetSpeed: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onStartSleepTimer: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Audio Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "✕",
                    Modifier
                        .clickable(onClick = onClose)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Speed
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Speed", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
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

            // Skip silence
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Skip silence", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text("Skip silent intervals automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.skipSilence, onCheckedChange = onSetSkipSilence)
            }

            // Sleep Timer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sleep Timer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                if (state.sleepTimerRemainingMs != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Stops in ${formatDuration(state.sleepTimerRemainingMs)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = onCancelSleepTimer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PlaybackUiState.SLEEP_TIMER_PRESETS_MS.forEach { preset ->
                            FilterChip(
                                selected = false,
                                onClick = { onStartSleepTimer(preset) },
                                label = { Text("${preset / 60_000}m") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Equalizer
            Button(
                onClick = onOpenEqualizer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Launch System Equalizer", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun statusLine(state: PlaybackUiState): String = when (state.status) {
    PlaybackStatus.IDLE -> "Nothing playing"
    PlaybackStatus.RESOLVING -> "Loading audio…"
    PlaybackStatus.BUFFERING -> "Buffering…"
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
