package com.example.musicx.spotify.model

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val owner: String? = null,
    val imageUrl: String? = null,
    val trackCount: Int? = null,
)

data class SpotifyTrack(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val durationMs: Long? = null,
    val imageUrl: String? = null,
) {
    val artistLine: String get() = artists.joinToString(", ")
}

sealed interface SpotifySessionState {
    data object Disconnected : SpotifySessionState
    data object Connecting : SpotifySessionState
    data object Connected : SpotifySessionState
    data object Expired : SpotifySessionState
    data class Error(val message: String) : SpotifySessionState
}

/**
 * Typed Spotify failures. Messages are constant so a cookie, bearer token, or
 * upstream response body cannot leak through an error string.
 */
sealed class SpotifyError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object NotConnected : SpotifyError("Spotify is not connected.")
    object SessionExpired : SpotifyError("Spotify session expired. Reconnect to continue.")
    object LoginRejected : SpotifyError("Spotify sign-in was rejected.")
    object ApiChanged : SpotifyError("Spotify returned an unexpected response.")
    object InvalidResponse : SpotifyError("Spotify returned an invalid response.")
    object ResponseTooLarge : SpotifyError("Spotify returned too much data.")
    object Timeout : SpotifyError("Spotify request timed out.")
    class Http(val statusCode: Int) : SpotifyError("Spotify request failed (HTTP $statusCode).")
    class Network(cause: Throwable) : SpotifyError("Could not reach Spotify.", cause)
}
