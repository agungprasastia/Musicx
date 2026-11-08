package com.example.musicx.scrobble

import com.example.musicx.core.model.CatalogItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.addJsonObject

data class Listen(val track: CatalogItem.Track, val listenedAtEpochSec: Long)

/**
 * Decides when a play counts as a listen, using the ListenBrainz/Last.fm rule:
 * track longer than 30 s, played for half its length or 4 minutes, whichever
 * comes first. Only time spent playing counts; gaps between ticks are capped so
 * pauses and a backgrounded UI do not inflate play time.
 */
class ListenTracker(
    private val monotonicMs: () -> Long,
    private val wallClockMs: () -> Long,
) {
    private var trackId: String? = null
    private var playedMs = 0L
    private var lastTickMs: Long? = null
    private var startedAtWallMs = 0L
    private var submitted = false

    fun onTick(track: CatalogItem.Track?, isPlaying: Boolean): Listen? {
        val now = monotonicMs()
        if (track?.id != trackId) {
            trackId = track?.id
            playedMs = 0L
            lastTickMs = null
            submitted = false
            startedAtWallMs = wallClockMs()
        }
        if (track == null) return null

        if (isPlaying) {
            lastTickMs?.let { playedMs += (now - it).coerceIn(0L, MAX_TICK_GAP_MS) }
            lastTickMs = now
        } else {
            lastTickMs = null
        }

        val threshold = thresholdMs(track.durationMs) ?: return null
        if (submitted || playedMs < threshold || track.artist == UNKNOWN_ARTIST) return null
        submitted = true
        return Listen(track, startedAtWallMs / 1000)
    }

    private fun thresholdMs(durationMs: Long?): Long? = when {
        durationMs == null -> MAX_THRESHOLD_MS
        durationMs <= MIN_TRACK_MS -> null
        else -> minOf(durationMs / 2, MAX_THRESHOLD_MS)
    }

    private companion object {
        const val MIN_TRACK_MS = 30_000L
        const val MAX_THRESHOLD_MS = 240_000L
        const val MAX_TICK_GAP_MS = 2_000L
        const val UNKNOWN_ARTIST = "Unknown artist"
    }
}

fun listenBrainzPayload(listen: Listen, clientVersion: String): String = buildJsonObject {
    put("listen_type", "single")
    putJsonArray("payload") {
        addJsonObject {
            put("listened_at", listen.listenedAtEpochSec)
            putJsonObject("track_metadata") {
                put("artist_name", listen.track.artist)
                put("track_name", listen.track.title)
                listen.track.album?.let { put("release_name", it) }
                putJsonObject("additional_info") {
                    put("submission_client", "Musicx")
                    put("submission_client_version", clientVersion)
                    listen.track.durationMs?.let { put("duration_ms", it) }
                }
            }
        }
    }
}.toString()

private val TOKEN_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

fun isValidListenBrainzToken(token: String): Boolean = TOKEN_PATTERN.matches(token)

fun parseValidateResponse(body: String): String? {
    val root = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return null
    if (root["valid"]?.jsonPrimitive?.booleanOrNull != true) return null
    return root["user_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}
