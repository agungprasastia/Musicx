package com.example.musicx.scrobble

import com.example.musicx.core.model.CatalogItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTrackerTest {
    private var now = 0L
    private val wallStart = 1_700_000_000_000L
    private val tracker = ListenTracker(monotonicMs = { now }, wallClockMs = { wallStart + now })

    private fun track(id: String = "a", durationMs: Long? = 200_000L, artist: String = "Artist") =
        CatalogItem.Track(id, "Title", artist, "Album", null, durationMs)

    private fun play(t: CatalogItem.Track?, forMs: Long, stepMs: Long = 500L): Listen? {
        var result: Listen? = null
        var elapsed = 0L
        tracker.onTick(t, isPlaying = true)?.let { result = it }
        while (elapsed < forMs) {
            now += stepMs
            elapsed += stepMs
            tracker.onTick(t, isPlaying = true)?.let { result = it }
        }
        return result
    }

    @Test
    fun `submits after half duration`() {
        val t = track(durationMs = 200_000L)
        assertNull(play(t, 99_000L))
        val listen = play(t, 1_500L)
        assertNotNull(listen)
        assertEquals(wallStart / 1000, listen!!.listenedAtEpochSec)
    }

    @Test
    fun `long tracks submit after four minutes`() {
        val t = track(durationMs = 20 * 60_000L)
        assertNull(play(t, 239_000L))
        assertNotNull(play(t, 1_500L))
    }

    @Test
    fun `unknown duration uses four minutes`() {
        val t = track(durationMs = null)
        assertNull(play(t, 239_000L))
        assertNotNull(play(t, 1_500L))
    }

    @Test
    fun `tracks of thirty seconds or less never submit`() {
        assertNull(play(track(durationMs = 30_000L), 60_000L))
    }

    @Test
    fun `paused time does not count`() {
        val t = track(durationMs = 200_000L)
        play(t, 50_000L)
        repeat(400) {
            now += 500L
            assertNull(tracker.onTick(t, isPlaying = false))
        }
        assertNull(play(t, 40_000L))
        assertNotNull(play(t, 15_000L))
    }

    @Test
    fun `gaps between ticks are capped`() {
        val t = track(durationMs = 200_000L)
        tracker.onTick(t, isPlaying = true)
        now += 10 * 60_000L
        assertNull(tracker.onTick(t, isPlaying = true))
    }

    @Test
    fun `submits once per play and resets on track change`() {
        val a = track("a")
        assertNotNull(play(a, 101_000L))
        assertNull(play(a, 100_000L))
        assertNotNull(play(track("b"), 101_000L))
    }

    @Test
    fun `unknown artist is not submitted`() {
        assertNull(play(track(artist = "Unknown artist"), 150_000L))
    }

    @Test
    fun `payload matches ListenBrainz single listen schema`() {
        val listen = Listen(track(durationMs = 200_000L), listenedAtEpochSec = 1_700_000_000L)
        val root = Json.parseToJsonElement(listenBrainzPayload(listen, clientVersion = "1.0")).jsonObject

        assertEquals("single", root["listen_type"]!!.jsonPrimitive.content)
        val item = root["payload"]!!.jsonArray.single().jsonObject
        assertEquals(1_700_000_000L, item["listened_at"]!!.jsonPrimitive.long)
        val meta = item["track_metadata"]!!.jsonObject
        assertEquals("Artist", meta["artist_name"]!!.jsonPrimitive.content)
        assertEquals("Title", meta["track_name"]!!.jsonPrimitive.content)
        assertEquals("Album", meta["release_name"]!!.jsonPrimitive.content)
        val info = meta["additional_info"]!!.jsonObject
        assertEquals("Musicx", info["submission_client"]!!.jsonPrimitive.content)
        assertEquals(200_000, info["duration_ms"]!!.jsonPrimitive.int)
    }

    @Test
    fun `token validation only accepts uuid shaped tokens`() {
        assertTrue(isValidListenBrainzToken("0b1e7d6c-1a2b-4c3d-9e8f-0123456789ab"))
        assertFalse(isValidListenBrainzToken(""))
        assertFalse(isValidListenBrainzToken("abc\r\nX-Evil: 1"))
        assertFalse(isValidListenBrainzToken("0b1e7d6c-1a2b-4c3d-9e8f-0123456789ab "))
    }

    @Test
    fun `validate response parses user name`() {
        assertEquals("rob", parseValidateResponse("""{"code":200,"valid":true,"user_name":"rob"}"""))
        assertNull(parseValidateResponse("""{"code":200,"valid":false}"""))
        assertNull(parseValidateResponse("not json"))
    }
}
