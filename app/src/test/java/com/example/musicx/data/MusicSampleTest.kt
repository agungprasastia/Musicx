package com.example.musicx.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MusicSampleTest {
    @Test
    fun tracksHaveUniqueIdsAndDisplayableMetadata() {
        val tracks = MusicSamples.tracks

        assertFalse(tracks.isEmpty())
        assertEquals(tracks.size, tracks.map { it.id }.distinct().size)
        tracks.forEach {
            assertFalse(it.title.isBlank())
            assertFalse(it.artist.isBlank())
            assert(it.artworkColors.size >= 2)
        }
    }
}
