package com.example.musicx.spotify.auth

import com.example.musicx.spotify.model.SpotifyError

class SpotifyTokenProvider(
    private val store: SpotifySecretStore,
    private val harvester: suspend (SpotifySecret) -> SpotifyWebToken,
    private val clientTokenSource: suspend () -> SpotifyClientToken,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Volatile
    private var cached: SpotifyTokens? = null

    suspend fun tokens(): SpotifyTokens {
        val now = clock()
        cached?.takeIf { it.expiresAtEpochMillis - now > REFRESH_BEFORE_EXPIRY_MS }?.let { return it }

        val secret = store.loadSecret() ?: throw SpotifyError.NotConnected
        val webToken = harvester(secret)
        if (webToken.isAnonymous) throw SpotifyError.LoginRejected
        if (webToken.accessToken.isBlank() || webToken.expiresAtEpochMillis <= now) {
            throw SpotifyError.SessionExpired
        }
        val clientToken = clientTokenSource()
        if (clientToken.token.isBlank() || clientToken.expiresInSeconds <= 0) {
            throw SpotifyError.InvalidResponse
        }
        return SpotifyTokens(
            bearer = webToken.accessToken,
            clientToken = clientToken.token,
            expiresAtEpochMillis = minOf(
                webToken.expiresAtEpochMillis,
                now + clientToken.expiresInSeconds * 1_000,
            ),
        ).also { cached = it }
    }

    fun clearCache() { cached = null }

    private companion object {
        const val REFRESH_BEFORE_EXPIRY_MS = 30_000L
    }
}
