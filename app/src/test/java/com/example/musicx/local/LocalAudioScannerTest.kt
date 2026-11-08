package com.example.musicx.local

import com.example.musicx.playback.resolver.ResolvedStream
import com.example.musicx.playback.resolver.StreamResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI

class LocalAudioScannerTest {

    @Test
    fun `mapRow produces track with MediaStore content uri id`() {
        val track = LocalAudioScanner.mapRow(42L, "Test Song", "Test Artist", "Test Album", 240_000L, albumId = 7L)!!
        assertEquals("content://media/external/audio/media/42", track.id)
        assertEquals("Test Song", track.title)
        assertEquals("Test Artist", track.artist)
        assertEquals("Test Album", track.album)
        assertEquals(240_000L, track.durationMs)
        assertEquals("content://media/external/audio/albumart/7", track.artworkUrl)
    }

    @Test
    fun `mapRow falls back to Unknown artist for blank or unknown artist`() {
        assertEquals("Unknown artist", LocalAudioScanner.mapRow(1L, "X", "  ", null, 0L)!!.artist)
        assertEquals("Unknown artist", LocalAudioScanner.mapRow(1L, "X", "<unknown>", null, 0L)!!.artist)
    }

    @Test
    fun `mapRow rejects blank title and invalid id`() {
        assertNull(LocalAudioScanner.mapRow(1L, "", "A", null, 0L))
        assertNull(LocalAudioScanner.mapRow(1L, null, "A", null, 0L))
        assertNull(LocalAudioScanner.mapRow(0L, "X", "A", null, 0L))
    }

    @Test
    fun `mapRow drops non-positive duration and missing artwork`() {
        val track = LocalAudioScanner.mapRow(1L, "X", "Y", null, -1L)!!
        assertNull(track.durationMs)
        assertNull(track.artworkUrl)
    }

    @Test
    fun `local tracks bypass remote resolver`() = runBlocking {
        var remoteCalls = 0
        val remote = object : StreamResolver {
            override suspend fun resolve(trackId: String): Result<ResolvedStream> {
                remoteCalls++
                return Result.success(ResolvedStream(URI("https://example.com/a"), null, null))
            }
        }
        val resolver = LocalAwareStreamResolver(remote)

        val local = resolver.resolve("content://media/external/audio/media/5").getOrThrow()
        assertEquals(URI("content://media/external/audio/media/5"), local.uri)
        assertEquals(0, remoteCalls)

        resolver.resolve("dQw4w9WgXcQ").getOrThrow()
        assertEquals(1, remoteCalls)
    }
}
