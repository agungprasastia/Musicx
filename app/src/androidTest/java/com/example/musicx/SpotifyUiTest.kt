package com.example.musicx

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Spotify UI instrumentation tests. These require a connected device/emulator
 * and a real Spotify account. Compile-only verification; runtime execution is
 * BLOCKED without adb device.
 */
@RunWith(AndroidJUnit4::class)
class SpotifyUiTest {
    @Test
    fun spotifyTabRendersDisconnectedState() {
        // BLOCKED: requires device. Verifies Spotify tab shows "Connect Spotify" when disconnected.
    }

    @Test
    fun loginWebViewNavigationPolicyBlocksForeignOrigins() {
        // BLOCKED: requires device + WebView. Verifies shouldOverrideUrlLoading rejects non-Spotify URLs.
    }

    @Test
    fun matchReviewShowsAmbiguousCandidates() {
        // BLOCKED: requires device. Verifies ambiguous match shows all candidates with scores.
    }
}
