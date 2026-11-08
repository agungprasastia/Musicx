package com.example.musicx.playback

import com.example.musicx.core.model.CatalogItem

/**
 * Pure queue model used for selection and up-next metadata. Media3 remains the
 * source of truth for playback ordering; [sync] mirrors its timeline into this
 * model after player events.
 */
class PlaybackQueue {
    private val items = mutableListOf<CatalogItem.Track>()

    val tracks: List<CatalogItem.Track> get() = items.toList()

    var currentIndex: Int = -1
        private set

    val current: CatalogItem.Track? get() = items.getOrNull(currentIndex)

    fun setQueue(tracks: List<CatalogItem.Track>, startIndex: Int): Boolean {
        if (tracks.isEmpty() || startIndex !in tracks.indices) return false
        items.clear()
        items.addAll(tracks)
        currentIndex = startIndex
        return true
    }

    fun append(track: CatalogItem.Track): Boolean {
        items.add(track)
        if (currentIndex == -1) currentIndex = 0
        return true
    }

    fun remove(index: Int): Boolean {
        if (index !in items.indices) return false
        items.removeAt(index)
        currentIndex = when {
            items.isEmpty() -> -1
            index < currentIndex -> currentIndex - 1
            index == currentIndex && currentIndex >= items.size -> items.lastIndex
            else -> currentIndex
        }
        return true
    }

    fun move(from: Int, to: Int): Boolean {
        if (from !in items.indices || to !in items.indices) return false
        if (from == to) return true

        val item = items.removeAt(from)
        items.add(to, item)
        currentIndex = when {
            currentIndex == from -> to
            from < currentIndex && to >= currentIndex -> currentIndex - 1
            from > currentIndex && to <= currentIndex -> currentIndex + 1
            else -> currentIndex
        }
        return true
    }

    fun next(): Boolean {
        if (currentIndex !in items.indices || currentIndex >= items.lastIndex) return false
        currentIndex++
        return true
    }

    fun previous(): Boolean {
        if (currentIndex !in items.indices || currentIndex <= 0) return false
        currentIndex--
        return true
    }

    fun clear() {
        items.clear()
        currentIndex = -1
    }

    fun sync(tracks: List<CatalogItem.Track>, currentIndex: Int) {
        items.clear()
        items.addAll(tracks)
        this.currentIndex = when {
            items.isEmpty() -> -1
            currentIndex in items.indices -> currentIndex
            else -> 0
        }
    }
}
