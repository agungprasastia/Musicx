package com.example.musicx.playlist

import com.example.musicx.core.model.CatalogItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlaylistRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sampleTrack1 = CatalogItem.Track(
        id = "track_1",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        artworkUrl = "https://example.com/art1.jpg",
        durationMs = 200_000L
    )

    private val sampleTrack2 = CatalogItem.Track(
        id = "track_2",
        title = "Espresso",
        artist = "Sabrina Carpenter",
        album = "Short n' Sweet",
        artworkUrl = "https://example.com/art2.jpg",
        durationMs = 175_000L
    )

    @Test
    fun `toggleLike adds and removes track and persists across reloads`() {
        val repo = PlaylistRepository(tempFolder.root)

        assertFalse(repo.isLiked("track_1"))
        assertTrue(repo.toggleLike(sampleTrack1))
        assertTrue(repo.isLiked("track_1"))
        assertEquals(1, repo.likedTracks.value.size)

        // Reload from disk
        val reloaded = PlaylistRepository(tempFolder.root)
        assertTrue(reloaded.isLiked("track_1"))
        assertEquals(1, reloaded.likedTracks.value.size)
        assertEquals("Blinding Lights", reloaded.likedTracks.value[0].title)

        // Unlike
        assertFalse(reloaded.toggleLike(sampleTrack1))
        assertFalse(reloaded.isLiked("track_1"))
        assertEquals(0, reloaded.likedTracks.value.size)
    }

    @Test
    fun `createPlaylist adds new playlist and adds tracks`() {
        val repo = PlaylistRepository(tempFolder.root)

        val pl = repo.createPlaylist("Night Drive")
        assertEquals("Night Drive", pl.name)
        assertEquals(1, repo.playlists.value.size)

        assertTrue(repo.addTrackToPlaylist(pl.id, sampleTrack1))
        // Duplicate track rejected
        assertFalse(repo.addTrackToPlaylist(pl.id, sampleTrack1))
        assertTrue(repo.addTrackToPlaylist(pl.id, sampleTrack2))

        val current = repo.playlists.value.first { it.id == pl.id }
        assertEquals(2, current.tracks.size)

        // Reload
        val reloaded = PlaylistRepository(tempFolder.root)
        val reloadedPl = reloaded.playlists.value.first { it.id == pl.id }
        assertEquals(2, reloadedPl.tracks.size)
        assertEquals("Blinding Lights", reloadedPl.tracks[0].title)
        assertEquals("Espresso", reloadedPl.tracks[1].title)

        // Remove track
        reloaded.removeTrackFromPlaylist(pl.id, "track_1")
        val afterRemove = reloaded.playlists.value.first { it.id == pl.id }
        assertEquals(1, afterRemove.tracks.size)
        assertEquals("Espresso", afterRemove.tracks[0].title)

        // Delete playlist
        reloaded.deletePlaylist(pl.id)
        assertEquals(0, reloaded.playlists.value.size)
    }
}
