package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.BrowsePage
import com.example.musicx.core.model.CatalogError
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.CancellationException

interface CatalogRepository {
    suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogItem>>
    suspend fun browse(id: String): Result<BrowsePage>
}

class YouTubeMusicRepository(private val client: YouTubeMusicClient) : CatalogRepository {
    override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogItem>> {
        if (query.isBlank()) return Result.failure(CatalogError.EmptyQuery)
        return client.search(query.trim(), filter).fold(
            onSuccess = { items -> Result.success(items.mapNotNull(::toDomain)) },
            onFailure = { Result.failure(it.asCatalogFailure()) },
        )
    }

    override suspend fun browse(id: String): Result<BrowsePage> {
        if (id.isBlank()) return Result.failure(CatalogError.EmptyBrowseId)
        return client.browse(id.trim()).fold(
            onSuccess = { page ->
                if (page.id.isBlank() || page.title.isBlank()) {
                    Result.failure(CatalogError.InvalidResponse)
                } else {
                    Result.success(
                        BrowsePage(id = page.id, title = page.title, items = page.items.mapNotNull(::toDomain))
                    )
                }
            },
            onFailure = { Result.failure(it.asCatalogFailure()) },
        )
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

    private fun Throwable.asCatalogFailure(): Throwable {
        if (this is CancellationException) throw this
        return this as? CatalogError ?: CatalogError.Network(this)
    }
}
