package com.example.musicx.spotify

import com.example.musicx.spotify.auth.SpotifySecret
import com.example.musicx.spotify.auth.SpotifySecretStore
import com.example.musicx.spotify.auth.SpotifyTokenProvider
import com.example.musicx.spotify.auth.SpotifyWebToken
import com.example.musicx.spotify.auth.SpotifyClientToken
import com.example.musicx.spotify.model.SpotifyError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyTokenProviderTest {
    @Test
    fun reusesTokensUntilWithinThirtySecondsOfExpiry() = runBlocking {
        var now = 1_000L
        var harvests = 0
        val provider = SpotifyTokenProvider(
            store = Store(SpotifySecret.parse("cookie")!!),
            harvester = { harvests++; webToken(now + 60_000) },
            clientTokenSource = { SpotifyClientToken("client", 600) },
            clock = { now },
        )

        val first = provider.tokens()
        now += 20_000
        val cached = provider.tokens()
        now += 11_000
        val refreshed = provider.tokens()

        assertEquals(first, cached)
        assertEquals(2, harvests)
        assertTrue(refreshed.expiresAtEpochMillis > cached.expiresAtEpochMillis)
    }

    @Test
    fun missingSecretReturnsTypedNotConnectedError() = runBlocking {
        val provider = SpotifyTokenProvider(Store(null), { error("must not harvest") }, { error("must not fetch") })

        assertTrue(runCatching { provider.tokens() }.exceptionOrNull() is SpotifyError.NotConnected)
    }

    @Test
    fun rejectsAnonymousHarvestAndDoesNotCacheIt() = runBlocking {
        var calls = 0
        val provider = SpotifyTokenProvider(
            Store(SpotifySecret.parse("cookie")!!),
            { calls++; webToken(100_000, anonymous = true) },
            { SpotifyClientToken("client", 600) },
            clock = { 1_000 },
        )

        repeat(2) { assertTrue(runCatching { provider.tokens() }.exceptionOrNull() is SpotifyError.LoginRejected) }
        assertEquals(2, calls)
    }

    private fun webToken(expiry: Long, anonymous: Boolean = false) = SpotifyWebToken("bearer", "client-id", expiry, anonymous)

    private class Store(private var secret: SpotifySecret?) : SpotifySecretStore {
        override fun loadSecret() = secret
        override fun saveSecret(secret: SpotifySecret) { this.secret = secret }
        override fun clear() { secret = null }
    }
}
