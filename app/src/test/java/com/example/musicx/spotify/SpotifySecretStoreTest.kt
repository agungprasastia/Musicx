package com.example.musicx.spotify

import com.example.musicx.spotify.auth.InMemorySpotifySecretStore
import com.example.musicx.spotify.auth.SpotifySecret
import com.example.musicx.spotify.auth.parseSpDcCookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifySecretStoreTest {
    @Test
    fun emptyStoreHasNoSecret() {
        assertNull(InMemorySpotifySecretStore().loadSecret())
    }

    @Test
    fun savedSecretRoundTrips() {
        val store = InMemorySpotifySecretStore()
        val secret = parseSpDcCookie("sp_dc=AQround-trip")!!

        store.saveSecret(secret)

        assertEquals(secret, store.loadSecret())
        assertEquals("AQround-trip", store.loadSecret()?.reveal())
    }

    @Test
    fun savingAgainReplacesThePreviousSecret() {
        val store = InMemorySpotifySecretStore()
        store.saveSecret(parseSpDcCookie("sp_dc=first")!!)

        store.saveSecret(parseSpDcCookie("sp_dc=second")!!)

        assertEquals("second", store.loadSecret()?.reveal())
    }

    @Test
    fun clearRemovesTheSecret() {
        val store = InMemorySpotifySecretStore()
        store.saveSecret(parseSpDcCookie("sp_dc=to-be-cleared")!!)

        store.clear()

        assertNull(store.loadSecret())
    }

    @Test
    fun storeNeverExposesSecretThroughStringRepresentation() {
        val store = InMemorySpotifySecretStore()
        val secret = parseSpDcCookie("sp_dc=never-print-me")!!

        store.saveSecret(secret)

        val rendered = store.loadSecret().toString()
        assertEquals(false, rendered.contains("never-print-me"))
        assertEquals(true, rendered.contains("redacted"))
    }

    @Test
    fun parsedSecretIsRejectedWhenItContainsWhitespace() {
        assertNull(SpotifySecret.parse("cookie value"))
    }
}
