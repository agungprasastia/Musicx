package com.example.musicx.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerFeaturesTest {

    @Test
    fun `PlaybackUiState default speed is 1_0`() {
        val state = PlaybackUiState()
        assertEquals(1.0f, state.speed, 0.001f)
    }

    @Test
    fun `PlaybackUiState default skipSilence is false`() {
        val state = PlaybackUiState()
        assertFalse(state.skipSilence)
    }

    @Test
    fun `PlaybackUiState default sleepTimerRemainingMs is null`() {
        val state = PlaybackUiState()
        assertEquals(null, state.sleepTimerRemainingMs)
    }

    @Test
    fun `PlaybackUiState speed values clamped in SPEED_OPTIONS`() {
        val options = PlaybackUiState.SPEED_OPTIONS
        assertTrue(options.contains(0.5f))
        assertTrue(options.contains(1.0f))
        assertTrue(options.contains(1.5f))
        assertTrue(options.contains(2.0f))
        assertEquals(options, options.sorted())
    }

    @Test
    fun `PlaybackUiState sleep timer presets contain common values`() {
        val presets = PlaybackUiState.SLEEP_TIMER_PRESETS_MS
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.all { it > 0 })
    }
}
