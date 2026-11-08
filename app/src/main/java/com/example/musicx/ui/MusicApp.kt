package com.example.musicx.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicx.catalog.youtube.InnerTubeYouTubeMusicClient
import com.example.musicx.catalog.youtube.OkHttpInnerTubeTransport
import com.example.musicx.catalog.youtube.YouTubeMusicRepository
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.playback.PlaybackController
import com.example.musicx.playback.PlaybackStatus
import com.example.musicx.playback.PlaybackUiState
import com.example.musicx.playback.resolver.InnerTubeYouTubePlayerClient
import com.example.musicx.playback.resolver.YouTubeStreamResolver

@Composable
fun MusicxApp(
    viewModel: MusicViewModel = viewModel(factory = MusicViewModelFactory),
    playbackController: PlaybackController = rememberDefaultPlaybackController(),
) {
    val state by viewModel.uiState.collectAsState()
    val playbackState by playbackController.state.collectAsState()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(playbackState.currentTrack?.id) {
        if (
            !notificationPermissionRequested &&
            playbackState.currentTrack != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequested = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.Home) }
    var browseOrigin by remember { mutableStateOf<BrowseOrigin?>(null) }
    var lastSearch by remember { mutableStateOf<SearchOrigin?>(null) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        viewModel.clear()
        browseOrigin = null
    }

    val onTrackSelected: (CatalogItem.Track) -> Unit = { playbackController.play(it) }
    val closeBrowse: () -> Unit = {
        val origin = browseOrigin
        browseOrigin = null
        when (origin) {
            is BrowseOrigin.Search -> origin.request?.let { viewModel.search(it.query, it.filter) }
                ?: viewModel.clear()
            BrowseOrigin.Home -> viewModel.clear()
            null -> Unit
        }
    }

    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
    BackHandler(enabled = browseOrigin != null && !showNowPlaying) { closeBrowse() }

    when {
        showNowPlaying -> NowPlayingScreen(
            state = playbackState,
            onPlayPause = playbackController::playPause,
            onSeek = playbackController::seekTo,
            onSkipNext = playbackController::skipNext,
            onSkipPrevious = playbackController::skipPrevious,
            onRetry = { playbackState.currentTrack?.let(playbackController::play) },
            onBack = { showNowPlaying = false },
        )

        browseOrigin != null -> BrowseScreen(
            state = state,
            onBack = closeBrowse,
            onTrackSelected = onTrackSelected,
            onBrowseSelected = { item -> viewModel.browse(item.id) },
            onRetry = viewModel::retry,
        )

        else -> Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    MiniPlayer(
                        state = playbackState,
                        onOpen = { if (playbackState.currentTrack != null) showNowPlaying = true },
                        onPlayPause = playbackController::playPause,
                    )
                    Row(
                        Modifier.fillMaxWidth().height(64.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicTab.entries.forEach { tab ->
                            Column(
                                Modifier.weight(1f).clickable(role = Role.Tab) { selectedTab = tab }.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    tab.name,
                                    color = if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                when (selectedTab) {
                    MusicTab.Home -> HomeScreen(
                        onBrowseSelected = { id ->
                            browseOrigin = BrowseOrigin.Home
                            viewModel.browse(id)
                        }
                    )

                    MusicTab.Search -> SearchScreen(
                        state = state,
                        onSearch = { query, filter ->
                            lastSearch = if (query.isBlank()) null else SearchOrigin(query.trim(), filter)
                            viewModel.search(query, filter)
                        },
                        onTrackSelected = onTrackSelected,
                        onBrowseSelected = { item ->
                            browseOrigin = BrowseOrigin.Search(lastSearch)
                            viewModel.browse(item.id)
                        },
                        onRetry = viewModel::retry
                    )

                    MusicTab.Library -> LibraryScreen()
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    state: PlaybackUiState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val track = state.currentTrack
    Row(
        Modifier.fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = track != null, onClick = onOpen)
            .semantics {
                contentDescription = track?.let { "Open player: ${it.title}" } ?: "Nothing playing"
            }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (track == null) {
            Text("Nothing playing", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CatalogArtwork(track, Modifier.size(44.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    miniPlayerStatus(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.status != PlaybackStatus.RESOLVING &&
                state.status != PlaybackStatus.ERROR &&
                state.status != PlaybackStatus.IDLE
            ) {
                Text(
                    if (state.isPlaying) "Pause" else "Play",
                    Modifier.clickable(onClick = onPlayPause).padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun miniPlayerStatus(state: PlaybackUiState): String {
    val artist = state.currentTrack?.artist.orEmpty()
    return when (state.status) {
        PlaybackStatus.IDLE -> listOf(artist, "Selected").filter { it.isNotBlank() }.joinToString(" · ")
        PlaybackStatus.RESOLVING -> "Resolving stream…"
        PlaybackStatus.BUFFERING -> "Buffering…"
        PlaybackStatus.READY -> listOf(artist, "Ready").filter { it.isNotBlank() }.joinToString(" · ")
        PlaybackStatus.PLAYING -> listOf(artist, "Playing").filter { it.isNotBlank() }.joinToString(" · ")
        PlaybackStatus.PAUSED -> listOf(artist, "Paused").filter { it.isNotBlank() }.joinToString(" · ")
        PlaybackStatus.ENDED -> listOf(artist, "Finished").filter { it.isNotBlank() }.joinToString(" · ")
        PlaybackStatus.ERROR -> state.errorMessage ?: "Playback error"
    }
}

@Composable
private fun rememberDefaultPlaybackController(): PlaybackController {
    val context = LocalContext.current.applicationContext
    val controller = remember(context) {
        PlaybackController(
            context = context,
            resolver = YouTubeStreamResolver(
                InnerTubeYouTubePlayerClient(OkHttpInnerTubeTransport())
            ),
        )
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

private data class SearchOrigin(val query: String, val filter: CatalogFilter)

private sealed interface BrowseOrigin {
    data object Home : BrowseOrigin
    data class Search(val request: SearchOrigin?) : BrowseOrigin
}

private object MusicViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = YouTubeMusicRepository(
            InnerTubeYouTubeMusicClient(OkHttpInnerTubeTransport())
        )
        @Suppress("UNCHECKED_CAST")
        return MusicViewModel(repository) as T
    }
}
