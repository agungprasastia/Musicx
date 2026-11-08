package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.BrowsePage
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem

class YouTubeMusicRepository(private val client: YouTubeMusicClient) {
    suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogItem>> {
        if (query.isBlank()) return Result.failure(IllegalArgumentException("Search query is blank"))
        return client.search(query.trim(), filter).map { items -> items.mapNotNull(::toDomain) }
    }

    suspend fun browse(id: String): Result<BrowsePage> {
        if (id.isBlank()) return Result.failure(IllegalArgumentException("Browse id is blank"))
        return client.browse(id).map { page ->
            BrowsePage(id = page.id, title = page.title, items = page.items.mapNotNull(::toDomain))
        }
    }

    private fun toDomain(dto: CatalogDto): CatalogItem? {
        if (dto.id.isBlank() || dto.title.isBlank()) return null
        return when (dto) {
            is CatalogDto.Track -> dto.artist.takeIf(String::isNotBlank)?.let {
                CatalogItem.Track(dto.id, dto.title, it, dto.album, dto.artworkUrl, dto.durationMs)
            }
            is CatalogDto.Album -> CatalogItem.Album(dto.id, dto.title, dto.artist, dto.artworkUrl, dto.trackCount)
            is CatalogDto.Artist -> CatalogItem.Artist(dto.id, dto.title, dto.artworkUrl)
            is CatalogDto.Playlist -> CatalogItem.Playlist(dto.id, dto.title, dto.artworkUrl, dto.trackCount)
        }
    }
}
