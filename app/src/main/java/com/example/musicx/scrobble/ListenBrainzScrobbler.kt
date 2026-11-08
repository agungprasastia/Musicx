package com.example.musicx.scrobble

import android.os.SystemClock
import com.example.musicx.playback.PlaybackUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScrobbleState {
    data object Disconnected : ScrobbleState
    data object Validating : ScrobbleState
    data class Connected(val userName: String, val lastError: String? = null) : ScrobbleState
    data class Error(val message: String) : ScrobbleState
}

/**
 * ponytail: listens are fed from the UI's playback state and failed submissions
 * are dropped, not queued. Move into PlaybackService with a persisted retry
 * queue if background-only listening or offline scrobbling matters.
 */
class ListenBrainzScrobbler(
    private val store: ListenBrainzTokenStore,
    private val client: ListenBrainzClient,
    private val scope: CoroutineScope,
    private val clientVersion: String,
) {
    private val tracker = ListenTracker(SystemClock::elapsedRealtime, System::currentTimeMillis)
    private var token: String? = null

    private val _state = MutableStateFlow<ScrobbleState>(ScrobbleState.Disconnected)
    val state: StateFlow<ScrobbleState> = _state.asStateFlow()

    fun restore() {
        val saved = store.load() ?: return
        validate(saved, persist = false)
    }

    fun connect(rawToken: String) {
        val candidate = rawToken.trim()
        if (!isValidListenBrainzToken(candidate)) {
            _state.value = ScrobbleState.Error("That does not look like a ListenBrainz user token.")
            return
        }
        validate(candidate, persist = true)
    }

    fun disconnect() {
        token = null
        store.clear()
        _state.value = ScrobbleState.Disconnected
    }

    fun onPlayback(playback: PlaybackUiState) {
        val listen = tracker.onTick(playback.currentTrack, playback.isPlaying) ?: return
        val activeToken = token ?: return
        scope.launch {
            val error = try {
                client.submit(activeToken, listen, clientVersion)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                "Last listen could not be sent to ListenBrainz."
            }
            (_state.value as? ScrobbleState.Connected)?.let { _state.value = it.copy(lastError = error) }
        }
    }

    private fun validate(candidate: String, persist: Boolean) {
        _state.value = ScrobbleState.Validating
        scope.launch {
            _state.value = try {
                val user = client.validate(candidate)
                if (user == null) {
                    store.clear()
                    ScrobbleState.Error("ListenBrainz rejected this token.")
                } else {
                    if (persist) store.save(candidate)
                    token = candidate
                    ScrobbleState.Connected(user)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep a saved token on network failure; it was valid before.
                if (!persist) token = candidate
                if (persist) ScrobbleState.Error("Could not reach ListenBrainz. Check your connection.")
                else ScrobbleState.Connected("ListenBrainz", lastError = "Could not verify token (offline).")
            }
        }
    }
}
