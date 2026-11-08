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
) : ViewModel() {
    private val _ui = MutableStateFlow<SpotifyUiState>(SpotifyUiState.Disconnected)
    val ui: StateFlow<SpotifyUiState> = _ui.asStateFlow()

    val session: StateFlow<SpotifySessionState> = repository.session

    private var currentJob: Job? = null

    init {
        viewModelScope.launch {
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
        viewModelScope.launch { repository.restoreSession() }
    }

    fun connect(secret: com.example.musicx.spotify.auth.SpotifySecret) {
        viewModelScope.launch { repository.connect(secret) }
    }

    fun disconnect() {
        currentJob?.cancel()
        repository.disconnect()
    }

    fun loadLibrary() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _ui.value = SpotifyUiState.Loading
            try {
                val playlists = repository.playlists()
                _ui.value = SpotifyUiState.Library(playlists)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SpotifyError) {
                _ui.value = SpotifyUiState.Error(e.message ?: "Failed to load library.")
            } catch (_: Exception) {
                _ui.value = SpotifyUiState.Error("Failed to load library.")
            }
        }
    }

    fun loadPlaylistTracks(playlist: SpotifyPlaylist) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _ui.value = SpotifyUiState.Loading
            try {
                val tracks = if (playlist.id == SpotifyRepository.LIKED_ID) {
                    repository.likedTracks()
                } else {
                    repository.playlistTracks(playlist.id)
                }
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

    fun matchTrack(track: SpotifyTrack): Job {
        currentJob?.cancel()
        val job = viewModelScope.launch {
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
