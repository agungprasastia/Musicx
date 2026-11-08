package com.example.musicx.spotify

import com.example.musicx.spotify.auth.InMemorySpotifySecretStore
import com.example.musicx.spotify.auth.SpotifyClientToken
import com.example.musicx.spotify.auth.SpotifySecret
import com.example.musicx.spotify.auth.SpotifyTokenProvider
import com.example.musicx.spotify.auth.SpotifyWebToken
import com.example.musicx.spotify.model.SpotifyError
import com.example.musicx.spotify.model.SpotifySessionState
import com.example.musicx.spotify.net.SpotifyHttpTransport
import com.example.musicx.spotify.net.SpotifyLibraryClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyRepositoryTest {
    private fun fakeRepo(
        harvestResult: () -> SpotifyWebToken = { SpotifyWebToken("bearer", "cid", Long.MAX_VALUE, false) },
    ): SpotifyRepository {
        val store = InMemorySpotifySecretStore()
        val provider = SpotifyTokenProvider(
            store = store,
            harvester = { harvestResult() },
            clientTokenSource = { SpotifyClientToken("client", 3600) },
        )
        val client = SpotifyLibraryClient(provider, SpotifyHttpTransport())
        return SpotifyRepository(store, provider, client)
    }

    @Test
    fun connectStoresSecretAndReachesConnected() = runBlocking {
        val repo = fakeRepo()
        repo.connect(SpotifySecret.parse("AQgood-cookie")!!)

        assertEquals(SpotifySessionState.Connected, repo.session.value)
    }

    @Test
    fun failedConnectClearsSecretAndTransitionsToExpired() = runBlocking {
        val repo = fakeRepo(harvestResult = { throw SpotifyError.SessionExpired })
        repo.connect(SpotifySecret.parse("AQbad-cookie")!!)

        assertEquals(SpotifySessionState.Expired, repo.session.value)
    }

    @Test
    fun disconnectClearsSessionAndReturnsToDisconnected() = runBlocking {
        val repo = fakeRepo()
        repo.connect(SpotifySecret.parse("AQconnected")!!)
        assertEquals(SpotifySessionState.Connected, repo.session.value)

        repo.disconnect()

        assertEquals(SpotifySessionState.Disconnected, repo.session.value)
    }

    @Test
    fun restoreWithStoredSecretReconnects() = runBlocking {
        val store = InMemorySpotifySecretStore()
        store.saveSecret(SpotifySecret.parse("AQstored")!!)
        val provider = SpotifyTokenProvider(
            store = store,
            harvester = { SpotifyWebToken("bearer", "cid", Long.MAX_VALUE, false) },
            clientTokenSource = { SpotifyClientToken("client", 3600) },
        )
        val repo = SpotifyRepository(store, provider, SpotifyLibraryClient(provider, SpotifyHttpTransport()))

        assertTrue(repo.restoreSession())
        assertEquals(SpotifySessionState.Connected, repo.session.value)
    }

    @Test
    fun restoreWithoutStoredSecretReturnsFalse() = runBlocking {
        val repo = fakeRepo()

        assertEquals(false, repo.restoreSession())
        assertEquals(SpotifySessionState.Disconnected, repo.session.value)
    }
}
