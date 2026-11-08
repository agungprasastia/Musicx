package com.example.musicx

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicx.spotify.auth.EncryptedSpotifySecretStore
import com.example.musicx.spotify.auth.SpotifySecret
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-level checks for Keystore-backed storage. Compiled locally; runtime
 * is BLOCKED without an adb device.
 */
@RunWith(AndroidJUnit4::class)
class SpotifySecretStoreInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = EncryptedSpotifySecretStore(context)

    @Before
    @After
    fun clearStore() {
        store.clear()
    }

    @Test
    fun secretRoundTripsThroughAndroidKeystore() {
        val secret = SpotifySecret.parse("AQinstrumented-round-trip")!!

        store.saveSecret(secret)

        assertEquals(secret, store.loadSecret())
        assertEquals("AQinstrumented-round-trip", store.loadSecret()?.reveal())
    }

    @Test
    fun preferencesContainCiphertextNotTheRawCookie() {
        val raw = "AQnever-store-me-in-plaintext"
        store.saveSecret(SpotifySecret.parse(raw)!!)

        val preferences = context.getSharedPreferences(
            "spotify_session",
            Context.MODE_PRIVATE,
        )
        val stored = preferences.all.values.joinToString("|") { it.toString() }

        assertTrue(preferences.all.isNotEmpty())
        assertFalse(stored.contains(raw))
        assertEquals(raw, store.loadSecret()?.reveal())
    }

    @Test
    fun clearRemovesCiphertext() {
        store.saveSecret(SpotifySecret.parse("AQto-be-cleared")!!)

        store.clear()

        assertNull(store.loadSecret())
        val preferences = context.getSharedPreferences(
            "spotify_session",
            Context.MODE_PRIVATE,
        )
        assertTrue(preferences.all.isEmpty())
    }

    @Test
    fun partialCiphertextEntryIsClearedAsCorrupt() {
        val preferences = context.getSharedPreferences("spotify_session", Context.MODE_PRIVATE)
        preferences.edit().putString("iv", "broken").commit()

        assertNull(store.loadSecret())
        assertTrue(preferences.all.isEmpty())
    }
}
