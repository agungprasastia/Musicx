package com.example.musicx.spotify

import com.example.musicx.spotify.auth.SpotifySecret
import com.example.musicx.spotify.auth.SpotifySecretStore
import com.example.musicx.spotify.auth.SpotifyTokenProvider
import com.example.musicx.spotify.model.SpotifyError
import com.example.musicx.spotify.model.SpotifyPlaylist
import com.example.musicx.spotify.model.SpotifySessionState
import com.example.musicx.spotify.model.SpotifyTrack
import com.example.musicx.spotify.net.SpotifyLibraryClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class SpotifyRepository(
    private val store: SpotifySecretStore,
    private val tokenProvider: SpotifyTokenProvider,
    private val libraryClient: SpotifyLibraryClient,
) {
    private val _session = MutableStateFlow<SpotifySessionState>(SpotifySessionState.Disconnected)
    open val session: StateFlow<SpotifySessionState> = _session.asStateFlow()

    open suspend fun connect(secret: SpotifySecret) {
        _session.value = SpotifySessionState.Connecting
        try {
            store.saveSecret(secret)
            tokenProvider.tokens()
            _session.value = SpotifySessionState.Connected
        } catch (error: CancellationException) {
            throw error
        } catch (error: SpotifyError) {
            store.clear()
            tokenProvider.clearCache()
            _session.value = when (error) {
                is SpotifyError.LoginRejected, is SpotifyError.SessionExpired ->
                    SpotifySessionState.Expired
                else -> SpotifySessionState.Error(error.message ?: "Connection failed.")
            }
        } catch (_: Exception) {
            store.clear()
            tokenProvider.clearCache()
            _session.value = SpotifySessionState.Error("Connection failed.")
        }
    }

    open fun disconnect() {
        store.clear()
        tokenProvider.clearCache()
        _session.value = SpotifySessionState.Disconnected
    }

    open suspend fun restoreSession(): Boolean {
        val secret = store.loadSecret() ?: return false
        connect(secret)
        return _session.value == SpotifySessionState.Connected
    }

    open suspend fun playlists(): List<SpotifyPlaylist> = withSessionRecovery {
        val liked = SpotifyPlaylist(
            id = LIKED_ID,
            name = "Liked Songs",
            owner = null,
            imageUrl = LIKED_COVER,
            trackCount = null,
        )
        listOf(liked) + libraryClient.playlists()
    }

    open suspend fun playlistTracks(id: String): List<SpotifyTrack> = withSessionRecovery {
        libraryClient.playlistTracks(id)
    }

    open suspend fun likedTracks(): List<SpotifyTrack> = withSessionRecovery {
        libraryClient.likedTracks()
    }

    private suspend fun <T> withSessionRecovery(block: suspend () -> T): T {
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: SpotifyError.SessionExpired) {
            _session.value = SpotifySessionState.Expired
            throw error
        } catch (error: SpotifyError.NotConnected) {
            _session.value = SpotifySessionState.Disconnected
            throw error
        } catch (error: SpotifyError) {
            throw error
        }
    }

    companion object {
        const val LIKED_ID = "liked"
        private const val LIKED_COVER = "https://misc.scdn.co/liked-songs/liked-songs-640.png"
    }
}
