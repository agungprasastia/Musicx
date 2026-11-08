package com.example.musicx.playback

import com.example.musicx.core.model.CatalogItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueTest {
    private val a = track("a")
    private val b = track("b")
    private val c = track("c")
    private val d = track("d")

    @Test
    fun setQueueAcceptsValidStartIndex() {
        val queue = PlaybackQueue()

        assertTrue(queue.setQueue(listOf(a, b, c), startIndex = 1))
        assertEquals(listOf(a, b, c), queue.tracks)
        assertEquals(1, queue.currentIndex)
        assertEquals(b, queue.current)
    }

    @Test
    fun setQueueRejectsEmptyOrInvalidIndexWithoutChangingExistingQueue() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b), startIndex = 1)

        assertFalse(queue.setQueue(emptyList(), startIndex = 0))
        assertFalse(queue.setQueue(listOf(c, d), startIndex = -1))
        assertFalse(queue.setQueue(listOf(c, d), startIndex = 2))
        assertEquals(listOf(a, b), queue.tracks)
        assertEquals(1, queue.currentIndex)
    }

    @Test
    fun appendAddsToEndAndSelectsFirstTrackWhenQueueWasEmpty() {
        val queue = PlaybackQueue()

        assertTrue(queue.append(a))
        assertEquals(listOf(a), queue.tracks)
        assertEquals(0, queue.currentIndex)

        assertTrue(queue.append(b))
        assertEquals(listOf(a, b), queue.tracks)
        assertEquals(0, queue.currentIndex)
    }

    @Test
    fun nextAndPreviousRespectBounds() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b), startIndex = 0)

        assertFalse(queue.previous())
        assertEquals(0, queue.currentIndex)
        assertTrue(queue.next())
        assertEquals(1, queue.currentIndex)
        assertFalse(queue.next())
        assertEquals(1, queue.currentIndex)
        assertTrue(queue.previous())
        assertEquals(0, queue.currentIndex)
    }

    @Test
    fun removeBeforeCurrentShiftsCurrentIndex() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b, c), startIndex = 2)

        assertTrue(queue.remove(0))
        assertEquals(listOf(b, c), queue.tracks)
        assertEquals(1, queue.currentIndex)
        assertEquals(c, queue.current)
    }

    @Test
    fun removeCurrentSelectsNextOrPreviousAtEnd() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b, c), startIndex = 1)

        assertTrue(queue.remove(1))
        assertEquals(listOf(a, c), queue.tracks)
        assertEquals(1, queue.currentIndex)
        assertEquals(c, queue.current)

        assertTrue(queue.remove(1))
        assertEquals(listOf(a), queue.tracks)
        assertEquals(0, queue.currentIndex)
        assertEquals(a, queue.current)
    }

    @Test
    fun removeAfterCurrentLeavesSelectionUnchanged() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b, c), startIndex = 1)

        assertTrue(queue.remove(2))
        assertEquals(listOf(a, b), queue.tracks)
        assertEquals(1, queue.currentIndex)
        assertEquals(b, queue.current)
    }

    @Test
    fun invalidRemoveLeavesQueueUnchanged() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b), startIndex = 0)

        assertFalse(queue.remove(-1))
        assertFalse(queue.remove(2))
        assertEquals(listOf(a, b), queue.tracks)
        assertEquals(0, queue.currentIndex)
    }

    @Test
    fun moveCurrentTrackFollowsIt() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b, c), startIndex = 1)

        assertTrue(queue.move(from = 1, to = 2))
        assertEquals(listOf(a, c, b), queue.tracks)
        assertEquals(2, queue.currentIndex)
        assertEquals(b, queue.current)
    }

    @Test
    fun moveTrackAcrossCurrentAdjustsCurrentIndex() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b, c, d), startIndex = 1)

        assertTrue(queue.move(from = 0, to = 3))
        assertEquals(listOf(b, c, d, a), queue.tracks)
        assertEquals(0, queue.currentIndex)
        assertEquals(b, queue.current)

        assertTrue(queue.move(from = 3, to = 0))
        assertEquals(listOf(a, b, c, d), queue.tracks)
        assertEquals(1, queue.currentIndex)
        assertEquals(b, queue.current)
    }

    @Test
    fun invalidMoveLeavesQueueUnchanged() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b), startIndex = 0)

        assertFalse(queue.move(-1, 0))
        assertFalse(queue.move(0, 2))
        assertEquals(listOf(a, b), queue.tracks)
        assertEquals(0, queue.currentIndex)
    }

    @Test
    fun emptyQueueHasNoCurrentAndNavigationFails() {
        val queue = PlaybackQueue()

        assertNull(queue.current)
        assertEquals(-1, queue.currentIndex)
        assertFalse(queue.next())
        assertFalse(queue.previous())
    }

    @Test
    fun clearResetsTracksAndSelection() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a, b), startIndex = 1)

        queue.clear()

        assertTrue(queue.tracks.isEmpty())
        assertEquals(-1, queue.currentIndex)
        assertNull(queue.current)
    }

    @Test
    fun syncFromPlayerReplacesTracksAndClampsInvalidIndex() {
        val queue = PlaybackQueue()
        queue.setQueue(listOf(a), startIndex = 0)

        queue.sync(listOf(b, c, d), currentIndex = 2)
        assertEquals(listOf(b, c, d), queue.tracks)
        assertEquals(2, queue.currentIndex)

        queue.sync(listOf(b), currentIndex = 9)
        assertEquals(listOf(b), queue.tracks)
        assertEquals(0, queue.currentIndex)

        queue.sync(emptyList(), currentIndex = 0)
        assertTrue(queue.tracks.isEmpty())
        assertEquals(-1, queue.currentIndex)
    }

    private fun track(id: String) = CatalogItem.Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = null,
        artworkUrl = null,
        durationMs = 1_000L,
    )
}
