package com.example.musicx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicx.catalog.youtube.CatalogRepository
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.spotify.SpotifyRepository
import com.example.musicx.spotify.match.MatchDecision
import com.example.musicx.spotify.match.SpotifyTrackMatcher
import com.example.musicx.spotify.match.TrackMatch
import com.example.musicx.spotify.model.SpotifyError
import com.example.musicx.spotify.model.SpotifyPlaylist
import com.example.musicx.spotify.model.SpotifySessionState
import com.example.musicx.spotify.model.SpotifyTrack
import com.example.musicx.core.model.CatalogFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SpotifyUiState {
    data object Disconnected : SpotifyUiState
    data object Connecting : SpotifyUiState
    data object Loading : SpotifyUiState
    data class Library(val playlists: List<SpotifyPlaylist>) : SpotifyUiState
    data class Tracks(val playlist: SpotifyPlaylist, val tracks: List<SpotifyTrack>) : SpotifyUiState
    data class Matching(val track: SpotifyTrack, val progress: String) : SpotifyUiState
    data class MatchReview(
        val track: SpotifyTrack,
        val decision: MatchDecision,
    ) : SpotifyUiState
    data class Error(val message: String) : SpotifyUiState
    data object Expired : SpotifyUiState
}

class SpotifyViewModel(
    private val repository: SpotifyRepository,
    private val catalogRepository: CatalogRepository,
    private val matcher: SpotifyTrackMatcher = SpotifyTrackMatcher(),
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val workScope: CoroutineScope = scope ?: viewModelScope

    private val _ui = MutableStateFlow<SpotifyUiState>(SpotifyUiState.Disconnected)
    val ui: StateFlow<SpotifyUiState> = _ui.asStateFlow()

    val session: StateFlow<SpotifySessionState> = repository.session

    private var currentJob: Job? = null
    private var cachedPlaylists: List<SpotifyPlaylist>? = null
    private var currentPlaylist: SpotifyPlaylist? = null
    private var currentTracks: List<SpotifyTrack>? = null

    init {
        workScope.launch {
            repository.session.collect { state ->
                when (state) {
                    SpotifySessionState.Disconnected -> _ui.value = SpotifyUiState.Disconnected
                    SpotifySessionState.Connecting -> _ui.value = SpotifyUiState.Connecting
                    SpotifySessionState.Connected -> loadLibrary()
                    SpotifySessionState.Expired -> _ui.value = SpotifyUiState.Expired
                    is SpotifySessionState.Error -> _ui.value = SpotifyUiState.Error(state.message)
                }
            }
        }
    }

    fun restore() {
        workScope.launch { repository.restoreSession() }
    }

    fun connect(secret: com.example.musicx.spotify.auth.SpotifySecret) {
        workScope.launch { repository.connect(secret) }
    }

    fun disconnect() {
        currentJob?.cancel()
        cachedPlaylists = null
        currentPlaylist = null
        currentTracks = null
        repository.disconnect()
    }

    fun loadLibrary() {
        currentJob?.cancel()
        currentJob = workScope.launch {
            _ui.value = SpotifyUiState.Loading
            try {
                val playlists = repository.playlists()
                cachedPlaylists = playlists
                _ui.value = SpotifyUiState.Library(playlists)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SpotifyError) {
                android.util.Log.e("SpotifyViewModel", "SpotifyError loading library: ${e.message}", e)
                _ui.value = SpotifyUiState.Error(e.message ?: "Failed to load library.")
            } catch (e: Exception) {
                android.util.Log.e("SpotifyViewModel", "Exception loading library: ${e.message}", e)
                _ui.value = SpotifyUiState.Error("Failed to load library.")
            }
        }
    }

    fun loadPlaylistTracks(playlist: SpotifyPlaylist) {
        currentJob?.cancel()
        currentPlaylist = playlist
        currentJob = workScope.launch {
            _ui.value = SpotifyUiState.Loading
            try {
                val tracks = if (playlist.id == SpotifyRepository.LIKED_ID) {
                    repository.likedTracks()
                } else {
                    repository.playlistTracks(playlist.id)
                }
                currentTracks = tracks
                _ui.value = SpotifyUiState.Tracks(playlist, tracks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SpotifyError) {
                _ui.value = SpotifyUiState.Error(e.message ?: "Failed to load tracks.")
            } catch (_: Exception) {
                _ui.value = SpotifyUiState.Error("Failed to load tracks.")
            }
        }
    }

    fun goBack(): Boolean {
        currentJob?.cancel()
        return when (_ui.value) {
            is SpotifyUiState.Matching, is SpotifyUiState.MatchReview -> {
                val p = currentPlaylist
                val t = currentTracks
                if (p != null && t != null) {
                    _ui.value = SpotifyUiState.Tracks(p, t)
                    true
                } else {
                    val playlists = cachedPlaylists
                    if (playlists != null) {
                        _ui.value = SpotifyUiState.Library(playlists)
                        true
                    } else {
                        false
                    }
                }
            }
            is SpotifyUiState.Tracks -> {
                currentPlaylist = null
                currentTracks = null
                val playlists = cachedPlaylists
                if (playlists != null) {
                    _ui.value = SpotifyUiState.Library(playlists)
                    true
                } else {
                    loadLibrary()
                    true
                }
            }
            is SpotifyUiState.Error -> {
                val playlists = cachedPlaylists
                if (playlists != null) {
                    _ui.value = SpotifyUiState.Library(playlists)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    fun matchTrack(track: SpotifyTrack): Job {
        currentJob?.cancel()
        val job = workScope.launch {
            _ui.value = SpotifyUiState.Matching(track, "Matching track…")
            try {
                val query = "${track.title} ${track.artistLine}"
                val searchResult = catalogRepository.search(query, CatalogFilter.SONGS)
                val candidates = searchResult.getOrNull()
                    ?.filterIsInstance<CatalogItem.Track>()
                    ?: emptyList()
                val decision = matcher.decide(track, candidates)
                _ui.value = SpotifyUiState.MatchReview(track, decision)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.value = SpotifyUiState.Error("Match failed: ${e.message ?: "unknown error"}")
            }
        }
        currentJob = job
        return job
    }
}
