package com.example.musicx.local

import com.example.musicx.playback.resolver.ResolvedStream
import com.example.musicx.playback.resolver.StreamResolver
import java.net.URI

/** Plays MediaStore tracks directly; everything else goes to [remote]. */
class LocalAwareStreamResolver(private val remote: StreamResolver) : StreamResolver {
    override suspend fun resolve(trackId: String): Result<ResolvedStream> =
        if (LocalAudioScanner.isLocal(trackId)) {
            Result.success(ResolvedStream(URI(trackId), mimeType = null, expiresAtEpochMillis = null))
        } else {
            remote.resolve(trackId)
        }
}
