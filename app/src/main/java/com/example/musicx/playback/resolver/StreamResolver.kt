package com.example.musicx.playback.resolver

import java.net.URI

/**
 * A stream URI returned by a resolver. Callers must not persist this value:
 * YouTube stream URLs are short-lived and may encode account/session grants.
 */
data class ResolvedStream(
    val uri: URI,
    val mimeType: String?,
    val expiresAtEpochMillis: Long?,
)

interface StreamResolver {
    suspend fun resolve(trackId: String): Result<ResolvedStream>
}

/**
 * Typed resolver failures. Messages are deliberately generic: upstream reasons
 * and stream URLs can contain temporary signatures and must not reach the UI or
 * logs.
 */
sealed class StreamResolveError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object EmptyTrackId : StreamResolveError("Track id is blank")
    object LoginRequired : StreamResolveError("YouTube Music requires sign-in for this stream")
    object Unavailable : StreamResolveError("This track is not available")
    object InvalidResponse : StreamResolveError("Stream response was invalid")
    object NoAudioStream : StreamResolveError("No playable audio stream was returned")
    object CipherProtected : StreamResolveError("This stream is signature-protected and is not supported")
    object Expired : StreamResolveError("The stream URL has expired")
    object UnsupportedStream : StreamResolveError("The stream URL is not supported")
    object ResponseTooLarge : StreamResolveError("Stream response exceeded the size limit")
    class Http(val statusCode: Int) : StreamResolveError("Stream request rejected (HTTP $statusCode)")
    class Network(cause: Throwable) : StreamResolveError("Stream request failed", cause)
    object Timeout : StreamResolveError("Stream request timed out")
}
