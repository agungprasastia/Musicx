package com.example.musicx

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.playback.PlaybackController
import com.example.musicx.playback.PlaybackService
import com.example.musicx.playback.PlaybackStatus
import com.example.musicx.playback.resolver.ResolvedStream
import com.example.musicx.playback.resolver.StreamResolveError
import com.example.musicx.playback.resolver.StreamResolver
import com.google.common.util.concurrent.ListenableFuture
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-level checks for the Media3 service boundary.
 *
 * These tests require an adb device/emulator. They are compiled locally but
 * cannot be claimed as passed without a connected runtime.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackSessionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun onMain(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
    }

    private val track = CatalogItem.Track(
        id = "test-track",
        title = "Test Track",
        artist = "Test Artist",
        album = null,
        artworkUrl = null,
        durationMs = 4_000L,
    )

    private class FakeStreamResolver(
        private val result: Result<ResolvedStream>,
    ) : StreamResolver {
        override suspend fun resolve(trackId: String): Result<ResolvedStream> = result
    }

    @Test
    fun mediaSessionServiceAcceptsControllerConnection() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        lateinit var future: ListenableFuture<MediaController>
        onMain { future = MediaController.Builder(context, token).buildAsync() }
        try {
            val controller = future.get(20, TimeUnit.SECONDS)
            assertNotNull(controller)
        } finally {
            onMain { MediaController.releaseFuture(future) }
        }
    }

    @Test
    fun resolvedStreamReachesPlayableStateWithTrackMetadata() = runBlocking {
        val resolver = FakeStreamResolver(
            Result.success(
                ResolvedStream(
                    uri = URI(silenceWavDataUri()),
                    mimeType = "audio/wav",
                    expiresAtEpochMillis = null,
                )
            )
        )
        var controller: PlaybackController? = null
        onMain { controller = PlaybackController(context, resolver) }
        val activeController = controller!!
        val seenStatuses = mutableListOf<PlaybackStatus>()
        val statusCollector = launch(Dispatchers.Default) {
            activeController.state.collect { state ->
                synchronized(seenStatuses) { seenStatuses += state.status }
            }
        }
        try {
            onMain { activeController.play(track) }

            val state = withTimeout(20_000) {
                activeController.state.first {
                    it.status == PlaybackStatus.READY ||
                        it.status == PlaybackStatus.PLAYING ||
                        it.status == PlaybackStatus.ENDED ||
                        it.status == PlaybackStatus.ERROR
                }
            }

            assertNotEquals(PlaybackStatus.ERROR, state.status)
            assertEquals(track.id, state.currentTrack?.id)
            assertEquals(track.title, state.currentTrack?.title)
            assertEquals(track.artist, state.currentTrack?.artist)
            assertTrue(
                synchronized(seenStatuses) { seenStatuses.contains(PlaybackStatus.RESOLVING) }
            )
        } finally {
            statusCollector.cancel()
            onMain { activeController.release() }
        }
    }

    @Test
    fun resolverFailureReachesErrorAndNeverReportsPlaying() = runBlocking {
        val resolver = FakeStreamResolver(Result.failure(StreamResolveError.LoginRequired))
        var controller: PlaybackController? = null
        onMain { controller = PlaybackController(context, resolver) }
        val activeController = controller!!
        try {
            onMain { activeController.play(track) }

            val state = withTimeout(10_000) {
                activeController.state.first { it.status == PlaybackStatus.ERROR }
            }

            assertFalse(state.isPlaying)
            assertEquals(track.id, state.currentTrack?.id)
            assertTrue(state.errorMessage.orEmpty().contains("sign-in"))
        } finally {
            onMain { activeController.release() }
        }
    }

    private companion object {
        /**
         * 0.25s of 8 kHz 8-bit mono PCM silence as a data URI. ExoPlayer loads
         * it through its data-scheme source without a network server.
         */
        fun silenceWavDataUri(): String {
            val sampleRate = 8_000
            val sampleCount = 2_000
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + sampleCount)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(1)
            header.putInt(sampleRate)
            header.putInt(sampleRate)
            header.putShort(1)
            header.putShort(8)
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(sampleCount)
            val bytes = header.array() + ByteArray(sampleCount) { 128.toByte() }
            return "data:audio/wav;base64," +
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }
}
