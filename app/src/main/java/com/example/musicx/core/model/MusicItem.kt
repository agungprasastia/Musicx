package com.example.musicx.core.model

sealed interface CatalogItem {
    val id: String
    val title: String
    val artworkUrl: String?

    data class Track(
        override val id: String,
        override val title: String,
        val artist: String,
        val album: String?,
        override val artworkUrl: String?,
        val durationMs: Long?,
    ) : CatalogItem

    data class Album(
        override val id: String,
        override val title: String,
        val artist: String?,
        override val artworkUrl: String?,
        val trackCount: Int?,
    ) : CatalogItem

    data class Artist(
        override val id: String,
        override val title: String,
        override val artworkUrl: String?,
    ) : CatalogItem

    data class Playlist(
        override val id: String,
        override val title: String,
        override val artworkUrl: String?,
        val trackCount: Int?,
    ) : CatalogItem
}

enum class CatalogFilter { ALL, SONGS, ALBUMS, ARTISTS, PLAYLISTS }

data class BrowsePage(val id: String, val title: String, val items: List<CatalogItem>)
