package com.example.musicx.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.musicx.BuildConfig
import com.example.musicx.catalog.youtube.InnerTubeYouTubeMusicClient
import com.example.musicx.catalog.youtube.OkHttpInnerTubeTransport
import com.example.musicx.catalog.youtube.YouTubeMusicRepository
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.local.LocalAwareStreamResolver
import com.example.musicx.local.LocalLibraryRepository
import com.example.musicx.scrobble.ListenBrainzClient
import com.example.musicx.scrobble.ListenBrainzScrobbler
import com.example.musicx.scrobble.ListenBrainzTokenStore
import com.example.musicx.playback.PlaybackController
import com.example.musicx.playback.PlaybackStatus
import com.example.musicx.playback.PlaybackUiState
import com.example.musicx.playback.resolver.InnerTubeYouTubePlayerClient
import com.example.musicx.playback.resolver.YouTubeStreamResolver
import com.example.musicx.spotify.SpotifyRepository
import com.example.musicx.spotify.auth.EncryptedSpotifySecretStore
import com.example.musicx.spotify.auth.SpotifyClientToken
import com.example.musicx.spotify.auth.SpotifyTokenProvider
import com.example.musicx.spotify.auth.SpotifyWebTokenHarvester
import com.example.musicx.spotify.auth.parseClientToken
import com.example.musicx.spotify.net.SpotifyHttpTransport
import com.example.musicx.spotify.net.SpotifyLibraryClient

@Composable
fun MusicxApp(
    viewModel: MusicViewModel = viewModel(factory = MusicViewModelFactory),
    spotifyViewModel: SpotifyViewModel = viewModel(factory = SpotifyViewModelFactory.create(LocalContext.current)),
    playbackController: PlaybackController = rememberDefaultPlaybackController(),
) {
    val state by viewModel.uiState.collectAsState()
    val playbackState by playbackController.state.collectAsState()
    val spotifyUi by spotifyViewModel.ui.collectAsState()
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
    var showSpotifyLogin by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val scrobbleScope = rememberCoroutineScope()
    val scrobbler = remember(context) {
        ListenBrainzScrobbler(
            store = ListenBrainzTokenStore(context.applicationContext),
            client = ListenBrainzClient(),
            scope = scrobbleScope,
            clientVersion = BuildConfig.VERSION_NAME,
        )
    }
    val scrobbleState by scrobbler.state.collectAsState()

    LaunchedEffect(scrobbler) { scrobbler.restore() }
    LaunchedEffect(playbackState) { scrobbler.onPlayback(playbackState) }

    LaunchedEffect(Unit) { spotifyViewModel.restore() }

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

    BackHandler(enabled = showSpotifyLogin) { showSpotifyLogin = false }
    BackHandler(enabled = showSettings && !showSpotifyLogin) { showSettings = false }
    BackHandler(enabled = showNowPlaying && !showSpotifyLogin && !showSettings) { showNowPlaying = false }
    BackHandler(enabled = browseOrigin != null && !showNowPlaying && !showSpotifyLogin && !showSettings) { closeBrowse() }

    when {
        showSpotifyLogin -> SpotifyLoginScreen(
            onCookieHarvested = { secret ->
                showSpotifyLogin = false
                spotifyViewModel.connect(secret)
            },
            onCancel = { showSpotifyLogin = false },
        )

        showSettings -> SettingsScreen(
            scrobble = scrobbleState,
            onConnectListenBrainz = scrobbler::connect,
            onDisconnectListenBrainz = scrobbler::disconnect,
            onBack = { showSettings = false },
        )

        showNowPlaying -> NowPlayingScreen(
            state = playbackState,
            onPlayPause = playbackController::playPause,
            onSeek = playbackController::seekTo,
            onSkipNext = playbackController::skipNext,
            onSkipPrevious = playbackController::skipPrevious,
            onRetry = { playbackState.currentTrack?.let(playbackController::play) },
            onBack = { showNowPlaying = false },
            onSetSpeed = playbackController::setSpeed,
            onSetSkipSilence = playbackController::setSkipSilence,
            onStartSleepTimer = playbackController::startSleepTimer,
            onCancelSleepTimer = playbackController::cancelSleepTimer,
            onOpenEqualizer = {
                try {
                    val eqIntent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, android.media.audiofx.AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                    context.startActivity(eqIntent)
                } catch (_: android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(context, "No equalizer app is available on this device.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
        )

        else -> Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    MiniPlayer(
                        state = playbackState,
                        onOpen = { if (playbackState.currentTrack != null) showNowPlaying = true },
                        onPlayPause = playbackController::playPause,
                    )
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        MusicTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val icon = when (tab) {
                                MusicTab.Home -> MusicIcons.Home
                                MusicTab.Search -> MusicIcons.Search
                                MusicTab.Library -> MusicIcons.Library
                                MusicTab.Spotify -> MusicIcons.Spotify
                            }
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (browseOrigin != null) browseOrigin = null
                                    selectedTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tab.name,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        tab.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (browseOrigin != null) {
                    BrowseScreen(
                        state = state,
                        onBack = closeBrowse,
                        onTrackSelected = onTrackSelected,
                        onBrowseSelected = { item -> viewModel.browse(item.id) },
                        onRetry = viewModel::retry,
                    )
                } else {
                    when (selectedTab) {
                        MusicTab.Home -> HomeScreen(
                            onBrowseSelected = { id ->
                                browseOrigin = BrowseOrigin.Home
                                viewModel.browse(id)
                            },
                            onTrackSelected = onTrackSelected,
                            onOpenSettings = { showSettings = true },
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

                        MusicTab.Library -> LocalLibraryTab(onTrackSelected = onTrackSelected)

                        MusicTab.Spotify -> SpotifyTabContent(
                            state = spotifyUi,
                            onLogin = { showSpotifyLogin = true },
                            onDisconnect = spotifyViewModel::disconnect,
                            onPlaylistSelected = spotifyViewModel::loadPlaylistTracks,
                            onTrackSelected = { track -> spotifyViewModel.matchTrack(track) },
                            onPlayMatched = { catalogTrack ->
                                playbackController.play(catalogTrack)
                                showNowPlaying = true
                            },
                            onRetry = spotifyViewModel::loadLibrary,
                            onBack = spotifyViewModel::loadLibrary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalLibraryTab(onTrackSelected: (CatalogItem.Track) -> Unit) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    var tracks by remember { mutableStateOf<List<CatalogItem.Track>?>(null) }
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            tracks = runCatching { LocalLibraryRepository(context.applicationContext).loadAll() }.getOrDefault(emptyList())
        }
    }
    LibraryScreen(
        tracks = tracks.orEmpty(),
        isLoading = hasPermission && tracks == null,
        hasPermission = hasPermission,
        onRequestPermission = { launcher.launch(permission) },
        onTrackSelected = onTrackSelected,
    )
}

@Composable
private fun SpotifyTabContent(
    state: SpotifyUiState,
    onLogin: () -> Unit,
    onDisconnect: () -> Unit,
    onPlaylistSelected: (com.example.musicx.spotify.model.SpotifyPlaylist) -> Unit,
    onTrackSelected: (com.example.musicx.spotify.model.SpotifyTrack) -> Unit,
    onPlayMatched: (CatalogItem.Track) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    when (state) {
        SpotifyUiState.Disconnected -> SpotifyDisconnectedScreen(onLogin)
        SpotifyUiState.Connecting -> SpotifyLoadingScreen()
        SpotifyUiState.Loading -> SpotifyLoadingScreen()
        SpotifyUiState.Expired -> SpotifyExpiredScreen(onLogin, onDisconnect)
        is SpotifyUiState.Library -> SpotifyLibraryScreen(state.playlists, onPlaylistSelected, onDisconnect)
        is SpotifyUiState.Tracks -> SpotifyTracksScreen(state.playlist, state.tracks, onTrackSelected, onBack)
        is SpotifyUiState.Matching -> SpotifyMatchingScreen(state.track, state.progress)
        is SpotifyUiState.MatchReview -> SpotifyMatchReviewScreen(state.track, state.decision, onPlayMatched, onBack)
        is SpotifyUiState.Error -> SpotifyErrorScreen(state.message, onRetry, onDisconnect)
    }
}

@Composable
private fun MiniPlayer(
    state: PlaybackUiState,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val track = state.currentTrack
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(enabled = track != null, onClick = onOpen)
            .semantics {
                contentDescription = track?.let { "Open player: ${it.title}" } ?: "Nothing playing"
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (track == null) {
                Text(
                    "Nothing playing",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                CatalogArtwork(track, Modifier.size(46.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        track.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
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
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPlayPause),
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isPlaying) MusicIcons.Pause else MusicIcons.Play,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
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
            resolver = LocalAwareStreamResolver(
                YouTubeStreamResolver(
                    InnerTubeYouTubePlayerClient(OkHttpInnerTubeTransport())
                )
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

private object SpotifyViewModelFactory {
    fun create(context: android.content.Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val store = EncryptedSpotifySecretStore(appContext)
            val harvester = SpotifyWebTokenHarvester(appContext)
            val transport = SpotifyHttpTransport()
            val tokenProvider = SpotifyTokenProvider(
                store = store,
                harvester = { secret -> harvester.harvest(secret) },
                clientTokenSource = { fetchClientToken(transport) },
            )
            val libraryClient = SpotifyLibraryClient(tokenProvider, transport)
            val repository = SpotifyRepository(store, tokenProvider, libraryClient)
            val catalogRepository = YouTubeMusicRepository(
                InnerTubeYouTubeMusicClient(OkHttpInnerTubeTransport())
            )
            @Suppress("UNCHECKED_CAST")
            return SpotifyViewModel(repository, catalogRepository) as T
        }
    }
}

private suspend fun fetchClientToken(transport: SpotifyHttpTransport): SpotifyClientToken {
    val body = """{"client_data":{"client_version":"1.2.52.442","client_id":"d8a5ed958d274c2e8ee717e6a4b0971d","js_sdk_data":{}}}"""
    val response = transport.post(
        "https://clienttoken.spotify.com/v1/clienttoken",
        mapOf("Accept" to "application/json"),
        body,
    )
    return com.example.musicx.spotify.auth.parseClientToken(response).getOrThrow()
}
