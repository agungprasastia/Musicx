package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogFilter

sealed interface CatalogDto {
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
    ) : CatalogDto

    data class Album(
        override val id: String,
        override val title: String,
        val artist: String?,
        override val artworkUrl: String?,
        val trackCount: Int?,
    ) : CatalogDto

    data class Artist(
        override val id: String,
        override val title: String,
        override val artworkUrl: String?,
    ) : CatalogDto

    data class Playlist(
        override val id: String,
        override val title: String,
        override val artworkUrl: String?,
        val trackCount: Int?,
    ) : CatalogDto
}

data class BrowseDto(val id: String, val title: String, val items: List<CatalogDto>)

interface YouTubeMusicClient {
    suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogDto>>
    suspend fun browse(id: String): Result<BrowseDto>
}
