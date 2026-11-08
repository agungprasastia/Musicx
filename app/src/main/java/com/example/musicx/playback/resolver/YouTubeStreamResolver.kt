package com.example.musicx.playback.resolver

import com.example.musicx.catalog.youtube.InnerTubeTransport
import com.example.musicx.core.model.CatalogError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI

interface YouTubePlayerClient {
    suspend fun player(trackId: String): Result<String>
}

/**
 * Guest WEB_REMIX player request. It deliberately requests the public player
 * response only; signature ciphers and DRM are never interpreted or bypassed.
 *
 * The endpoint currently answers `LOGIN_REQUIRED` for guest traffic from some
 * networks. That is surfaced as a typed failure instead of being worked around.
 */
class InnerTubeYouTubePlayerClient(
    private val transport: InnerTubeTransport,
    private val clientVersion: String = WEB_REMIX_CLIENT_VERSION,
) : YouTubePlayerClient {
    override suspend fun player(trackId: String): Result<String> {
        val id = trackId.trim()
        if (id.isEmpty()) return Result.failure(StreamResolveError.EmptyTrackId)
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", clientVersion)
                    put("hl", "en")
                    put("gl", "US")
                }
                putJsonObject("user") {
                    put("lockedSafetyMode", false)
                }
                putJsonObject("request") {
                    put("useSsl", true)
                }
            }
            put("videoId", id)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()

        return transport.post("player", body).fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(it.asStreamFailure()) },
        )
    }

    private companion object {
        const val WEB_REMIX_CLIENT_VERSION = "1.20250101.01.00"
    }
}

/**
 * Parses a public player response into a playable audio stream.
 *
 * Only direct HTTPS `googlevideo.com` URLs are accepted. When the response
 * contains `signatureCipher`/`cipher`, resolution fails with
 * [StreamResolveError.CipherProtected]; no deciphering is attempted.
 */
class YouTubeStreamResolver(
    private val client: YouTubePlayerClient,
    private val clock: () -> Long = System::currentTimeMillis,
    private val requestTimeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : StreamResolver {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun resolve(trackId: String): Result<ResolvedStream> {
        val id = trackId.trim()
        if (id.isEmpty()) return Result.failure(StreamResolveError.EmptyTrackId)
        return try {
            withTimeout(requestTimeoutMs) {
                client.player(id).fold(
                    onSuccess = { raw -> parse(raw) },
                    onFailure = { Result.failure(it.asStreamFailure()) },
                )
            }
        } catch (error: TimeoutCancellationException) {
            Result.failure(StreamResolveError.Timeout)
        } catch (error: CancellationException) {
            throw error
        }
    }

    private fun parse(raw: String): Result<ResolvedStream> {
        val root = try {
            json.parseToJsonElement(raw)
        } catch (error: Exception) {
            return Result.failure(StreamResolveError.InvalidResponse)
        } as? JsonObject ?: return Result.failure(StreamResolveError.InvalidResponse)

        val status = root.obj("playabilityStatus")?.text("status")
            ?: return Result.failure(StreamResolveError.InvalidResponse)
        when (status) {
            "OK" -> Unit
            "LOGIN_REQUIRED" -> return Result.failure(StreamResolveError.LoginRequired)
            else -> return Result.failure(StreamResolveError.Unavailable)
        }

        val streaming = root.obj("streamingData")
            ?: return Result.failure(StreamResolveError.NoAudioStream)
        val expiresInSeconds = streaming.long("expiresInSeconds")
        val candidates = mutableListOf<Candidate>()
        var sawAudio = false
        var sawCipher = false
        var sawExpired = false
        var sawUnsupportedUrl = false

        (streaming.arr("adaptiveFormats").orEmpty() + streaming.arr("formats").orEmpty()).forEach { element ->
            val format = element as? JsonObject ?: return@forEach
            val mimeType = format.text("mimeType") ?: return@forEach
            val hasAudio = mimeType.startsWith("audio/", ignoreCase = true) ||
                mimeType.contains("mp4a", ignoreCase = true) ||
                mimeType.contains("opus", ignoreCase = true)
            if (!hasAudio) return@forEach
            sawAudio = true

            val directUrl = format.text("url")
            if (directUrl.isNullOrBlank()) {
                if (format.containsKey("signatureCipher") || format.containsKey("cipher")) sawCipher = true
                return@forEach
            }

            val uri = parseAllowedMediaUri(directUrl)
            if (uri == null) {
                sawUnsupportedUrl = true
                return@forEach
            }

            val expiresAt = expiresAt(streaming, uri, expiresInSeconds)
            if (expiresAt != null && expiresAt <= clock()) {
                sawExpired = true
                return@forEach
            }

            candidates += Candidate(
                uri = uri,
                mimeType = mimeType,
                expiresAtEpochMillis = expiresAt,
                bitrate = format.long("bitrate") ?: 0L,
            )
        }

        if (candidates.isEmpty()) {
            val error = when {
                sawExpired -> StreamResolveError.Expired
                sawCipher -> StreamResolveError.CipherProtected
                sawUnsupportedUrl -> StreamResolveError.UnsupportedStream
                else -> StreamResolveError.NoAudioStream
            }
            return Result.failure(error)
        }

        val best = candidates.maxWithOrNull(
            compareBy<Candidate> { candidate -> if (candidate.mimeType.startsWith("audio/", ignoreCase = true)) 1 else 0 }
                .thenBy { candidate -> candidate.bitrate }
                .thenBy { candidate -> if (candidate.mimeType.contains("mp4", ignoreCase = true)) 1 else 0 }
        ) ?: return Result.failure(StreamResolveError.NoAudioStream)

        return Result.success(
            ResolvedStream(
                uri = best.uri,
                mimeType = best.mimeType,
                expiresAtEpochMillis = best.expiresAtEpochMillis,
            )
        )
    }

    private fun expiresAt(
        streaming: JsonObject,
        uri: URI,
        expiresInSeconds: Long?,
    ): Long? {
        if (expiresInSeconds != null) return clock() + expiresInSeconds * 1_000L
        val expire = queryParameter(uri.rawQuery, "expire")?.toLongOrNull() ?: return null
        return expire * 1_000L
    }

    private fun parseAllowedMediaUri(value: String): URI? = try {
        val uri = URI(value)
        val host = uri.host?.lowercase()
        if (
            !uri.scheme.equals("https", ignoreCase = true) ||
            host == null ||
            (host != GOOGLEVIDEO_HOST && !host.endsWith(".$GOOGLEVIDEO_HOST")) ||
            uri.userInfo != null
        ) {
            null
        } else {
            uri
        }
    } catch (error: Exception) {
        null
    }

    private fun queryParameter(query: String?, name: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split('&')
            .map { pair -> pair.split('=', limit = 2) }
            .firstOrNull { pair -> pair.size == 2 && pair[0] == name }
            ?.get(1)
    }

    private fun JsonElement?.obj(key: String): JsonObject? =
        (this as? JsonObject)?.get(key) as? JsonObject

    private fun JsonElement?.arr(key: String): JsonArray? =
        (this as? JsonObject)?.get(key) as? JsonArray

    private fun JsonElement?.text(key: String): String? =
        (this as? JsonObject)?.get(key).text()

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement?.long(key: String): Long? =
        (this as? JsonObject)?.get(key)?.let { (it as? JsonPrimitive)?.longOrNull }

    private data class Candidate(
        val uri: URI,
        val mimeType: String,
        val expiresAtEpochMillis: Long?,
        val bitrate: Long,
    )

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 20_000L
        const val GOOGLEVIDEO_HOST = "googlevideo.com"
    }
}

private fun Throwable.asStreamFailure(): Throwable {
    if (this is CancellationException) throw this
    return when (this) {
        is StreamResolveError -> this
        is CatalogError.Http -> StreamResolveError.Http(statusCode)
        is CatalogError.Network -> StreamResolveError.Network(this)
        is CatalogError.ResponseTooLarge -> StreamResolveError.ResponseTooLarge
        is CatalogError.InvalidResponse -> StreamResolveError.InvalidResponse
        else -> StreamResolveError.Network(this)
    }
}
