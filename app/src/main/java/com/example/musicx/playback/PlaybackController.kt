package com.example.musicx.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.playback.resolver.ResolvedStream
import com.example.musicx.playback.resolver.StreamResolveError
import com.example.musicx.playback.resolver.StreamResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackStatus {
    IDLE,
    RESOLVING,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR,
}

data class PlaybackUiState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTrack: CatalogItem.Track? = null,
    val queue: List<CatalogItem.Track> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val speed: Float = 1.0f,
    val skipSilence: Boolean = false,
    val sleepTimerRemainingMs: Long? = null,
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING
    val hasNext: Boolean get() = currentIndex >= 0 && currentIndex < queue.lastIndex
    val hasPrevious: Boolean get() = currentIndex > 0

    companion object {
        val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val SLEEP_TIMER_PRESETS_MS = listOf(
            15L * 60_000,  // 15 min
            30L * 60_000,  // 30 min
            45L * 60_000,  // 45 min
            60L * 60_000,  // 1 hour
            90L * 60_000,  // 1.5 hours
        )
    }
}

/**
 * UI-facing controller. Resolution happens before a playable MediaItem is
 * handed to the service, so the UI never reports playback for an unresolved
 * stream. The service keeps playing if this controller is released.
 */
class PlaybackController(
    context: Context,
    private val resolver: StreamResolver,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queue = PlaybackQueue()

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null
    private var released = false
    private var resolvingTrack: CatalogItem.Track? = null
    private var resolveJob: Job? = null
    private var pendingPlay: CatalogItem.Track? = null
    private val pendingEnqueues = mutableListOf<CatalogItem.Track>()
    private var sleepTimerJob: Job? = null
    private var speed = 1.0f
    private var skipSilence = false
    private var sleepTimerRemainingMs: Long? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishFromPlayer()
        }
    }

    init {
        controllerFuture.addListener(
            {
                try {
                    val connected = controllerFuture.get()
                    if (released) {
                        connected.release()
                        return@addListener
                    }
                    controller = connected
                    speed = connected.playbackParameters.speed
                    connected.addListener(playerListener)
                    publishFromPlayer()
                    flushPendingCommands()
                } catch (error: java.util.concurrent.CancellationException) {
                    // Controller was released before it connected.
                } catch (error: Exception) {
                    if (!released) {
                        publish(
                            PlaybackUiState(
                                status = PlaybackStatus.ERROR,
                                errorMessage = "Playback service is unavailable.",
                            )
                        )
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )

        scope.launch {
            while (isActive) {
                delay(POSITION_POLL_MS)
                publishFromPlayer()
            }
        }
    }

    fun play(track: CatalogItem.Track) {
        if (released) return
        clearError()
        val player = controller
        if (player == null) {
            pendingPlay = track
            publish(
                PlaybackUiState(
                    status = PlaybackStatus.RESOLVING,
                    currentTrack = track,
                    queue = listOf(track),
                    currentIndex = 0,
                )
            )
            return
        }

        resolveJob?.cancel()
        resolvingTrack = track
        publish(
            PlaybackUiState(
                status = PlaybackStatus.RESOLVING,
                currentTrack = track,
                queue = listOf(track),
                currentIndex = 0,
            )
        )
        resolveJob = scope.launch {
            resolver.resolve(track.id).fold(
                onSuccess = { stream ->
                    if (released || resolvingTrack?.id != track.id) return@launch
                    resolvingTrack = null
                    player.setMediaItem(track.toMediaItem(stream))
                    player.prepare()
                    player.play()
                    queue.setQueue(listOf(track), startIndex = 0)
                    publishFromPlayer()
                },
                onFailure = { error ->
                    if (released || resolvingTrack?.id != track.id) return@launch
                    resolvingTrack = null
                    publish(
                        PlaybackUiState(
                            status = PlaybackStatus.ERROR,
                            currentTrack = track,
                            queue = listOf(track),
                            currentIndex = 0,
                            errorMessage = error.toPlaybackMessage(),
                        )
                    )
                },
            )
        }
    }

    fun enqueue(track: CatalogItem.Track) {
        if (released) return
        val player = controller
        if (player == null) {
            pendingEnqueues += track
            return
        }

        scope.launch {
            resolver.resolve(track.id).fold(
                onSuccess = { stream ->
                    if (released || controller !== player) return@launch
                    val item = track.toMediaItem(stream)
                    if (player.mediaItemCount == 0) {
                        player.setMediaItem(item)
                        player.prepare()
                        queue.setQueue(listOf(track), startIndex = 0)
                    } else {
                        player.addMediaItem(item)
                        queue.append(track)
                    }
                    publishFromPlayer()
                },
                onFailure = { error ->
                    if (released) return@launch
                    val current = _state.value
                    publish(
                        current.copy(
                            status = if (current.currentTrack == null) PlaybackStatus.ERROR else current.status,
                            currentTrack = current.currentTrack ?: track,
                            queue = if (current.currentTrack == null) listOf(track) else current.queue,
                            currentIndex = if (current.currentTrack == null) 0 else current.currentIndex,
                            errorMessage = error.toPlaybackMessage(),
                        )
                    )
                },
            )
        }
    }

    fun playPause() {
        val player = controller ?: return
        clearError()
        if (player.playWhenReady) {
            player.pause()
        } else {
            when (player.playbackState) {
                Player.STATE_ENDED -> {
                    player.seekTo(0)
                    player.play()
                }

                Player.STATE_IDLE -> {
                    if (player.mediaItemCount > 0) {
                        player.prepare()
                        player.play()
                    }
                }

                else -> player.play()
            }
        }
        publishFromPlayer()
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        val duration = player.duration.takeIf { it > 0 }
        player.seekTo(
            if (duration == null) positionMs.coerceAtLeast(0)
            else positionMs.coerceIn(0, duration)
        )
        publishFromPlayer()
    }

    fun skipNext() {
        val player = controller ?: return
        clearError()
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        publishFromPlayer()
    }

    fun skipPrevious() {
        val player = controller ?: return
        clearError()
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
        publishFromPlayer()
    }

    fun setSpeed(value: Float) {
        val player = controller ?: return
        speed = value.coerceIn(0.25f, 3.0f)
        player.playbackParameters = PlaybackParameters(speed)
        publish(_state.value)
    }

    fun setSkipSilence(enabled: Boolean) {
        val player = controller ?: return
        val args = android.os.Bundle().apply { putBoolean(PlaybackService.EXTRA_ENABLED, enabled) }
        player.sendCustomCommand(PlaybackService.SKIP_SILENCE_COMMAND, args)
        skipSilence = enabled
        publish(_state.value)
    }

    // ponytail: timer lives in this controller's scope, so it dies with the UI.
    // Move into PlaybackService if the timer must survive Activity destruction.
    fun startSleepTimer(durationMs: Long) {
        if (durationMs <= 0) return
        cancelSleepTimer()
        val deadline = SystemClock.elapsedRealtime() + durationMs
        sleepTimerJob = scope.launch {
            while (isActive) {
                val remaining = deadline - SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                sleepTimerRemainingMs = remaining
                publish(_state.value)
                delay(minOf(1_000L, remaining))
            }
            sleepTimerJob = null
            sleepTimerRemainingMs = null
            controller?.pause()
            publish(_state.value)
            publishFromPlayer()
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerRemainingMs = null
        publish(_state.value)
    }

    fun release() {
        if (released) return
        released = true
        resolveJob?.cancel()
        sleepTimerJob?.cancel()
        scope.cancel()
        pendingPlay = null
        pendingEnqueues.clear()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        controllerFuture.cancel(true)
    }

    private fun flushPendingCommands() {
        val nextPlay = pendingPlay
        pendingPlay = null
        nextPlay?.let(::play)

        val enqueues = pendingEnqueues.toList()
        pendingEnqueues.clear()
        enqueues.forEach(::enqueue)
    }

    private fun publishFromPlayer() {
        val player = controller ?: return
        if (released || resolvingTrack != null) return

        val tracks = (0 until player.mediaItemCount).mapNotNull { index ->
            player.getMediaItemAt(index).toTrack()
        }
        queue.sync(tracks, player.currentMediaItemIndex)
        val current = queue.current

        if (current == null) {
            if (_state.value.status == PlaybackStatus.ERROR) return
            publish(PlaybackUiState())
            return
        }

        val status = when {
            player.playerError != null -> PlaybackStatus.ERROR
            player.playbackState == Player.STATE_ENDED -> PlaybackStatus.ENDED
            player.playbackState == Player.STATE_BUFFERING && player.playWhenReady -> PlaybackStatus.BUFFERING
            player.isPlaying -> PlaybackStatus.PLAYING
            player.playbackState == Player.STATE_READY && player.playWhenReady -> PlaybackStatus.READY
            player.playbackState == Player.STATE_READY -> PlaybackStatus.PAUSED
            else -> PlaybackStatus.PAUSED
        }

        val durationMs = player.duration.takeIf { it > 0 } ?: current.durationMs ?: 0L
        val playbackError = if (player.playerError != null) {
            "Playback failed. The stream may be unavailable."
        } else {
            _state.value.errorMessage
        }

        publish(
            PlaybackUiState(
                status = status,
                currentTrack = current,
                queue = queue.tracks,
                currentIndex = queue.currentIndex,
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = durationMs,
                errorMessage = playbackError,
            )
        )
    }

    private fun clearError() {
        if (_state.value.errorMessage != null) {
            publish(_state.value.copy(errorMessage = null))
        }
    }

    private fun publish(next: PlaybackUiState) {
        val stamped = next.copy(speed = speed, skipSilence = skipSilence, sleepTimerRemainingMs = sleepTimerRemainingMs)
        if (_state.value != stamped) _state.value = stamped
    }

    private fun CatalogItem.Track.toMediaItem(stream: ResolvedStream): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDurationMs(durationMs ?: C.TIME_UNSET)
            .apply {
                artworkUrl
                    ?.takeIf { it.startsWith("https://", ignoreCase = true) }
                    ?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(stream.uri.toString())
            .setMimeType(stream.mimeType)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toTrack(): CatalogItem.Track? {
        val id = mediaId.takeIf { it.isNotBlank() } ?: return null
        val metadata = mediaMetadata
        val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val artist = metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown artist"
        val album = metadata.albumTitle?.toString()
        val artwork = metadata.artworkUri?.toString()
        val duration = metadata.durationMs?.takeIf { it > 0 }
        return CatalogItem.Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artwork,
            durationMs = duration,
        )
    }

    private fun Throwable.toPlaybackMessage(): String = when (this) {
        is StreamResolveError.LoginRequired -> "YouTube Music requires sign-in to play this track."
        is StreamResolveError.CipherProtected ->
            "This track uses a protected stream that Musicx does not bypass."
        is StreamResolveError.Unavailable -> "This track is unavailable."
        is StreamResolveError.Expired -> "The stream link expired. Try again."
        is StreamResolveError.Timeout -> "The stream request timed out. Try again."
        is StreamResolveError.Http -> "YouTube Music rejected the stream request (HTTP $statusCode)."
        is StreamResolveError.Network -> "Could not reach the stream server. Check your connection."
        is StreamResolveError.EmptyTrackId -> "This track has no valid id."
        else -> "Could not prepare a playable stream."
    }

    private companion object {
        const val POSITION_POLL_MS = 500L
    }
}
