package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem

import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeMusicRepositoryTest {
    private class FakeClient(
        private val items: List<CatalogDto>,
    ) : YouTubeMusicClient {
        var searches = 0
        override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogDto>> {
            searches++
            return Result.success(items)
        }

        override suspend fun browse(id: String): Result<BrowseDto> =
            Result.success(BrowseDto(id = id, title = "Album", items = items))
    }

    @Test
    fun mapsSupportedCatalogKindsAndDropsMalformedItems() = runBlocking {
        val client = FakeClient(
            listOf(
                CatalogDto.Track(id = "track-1", title = "Track", artist = "Artist", album = "Album", artworkUrl = null, durationMs = 123_000),
                CatalogDto.Album(id = "album-1", title = "Album", artist = "Artist", artworkUrl = null, trackCount = 10),
                CatalogDto.Artist(id = "artist-1", title = "Artist", artworkUrl = null),
                CatalogDto.Playlist(id = "playlist-1", title = "Mix", artworkUrl = null, trackCount = 20),
                CatalogDto.Track(id = "", title = "Invalid", artist = "Artist", album = null, artworkUrl = null, durationMs = null),
                CatalogDto.Artist(id = "artist-2", title = " ", artworkUrl = null)
            )
        )

        val result = YouTubeMusicRepository(client).search("song", CatalogFilter.ALL).getOrThrow()

        assertEquals(4, result.size)
        assertTrue(result[0] is CatalogItem.Track)
        assertTrue(result[1] is CatalogItem.Album)
        assertTrue(result[2] is CatalogItem.Artist)
        assertTrue(result[3] is CatalogItem.Playlist)
        assertEquals(1, client.searches)
    }

    @Test
    fun emptyQueryFailsBeforeCallingClient() = runBlocking {
        val client = FakeClient(emptyList())

        val result = YouTubeMusicRepository(client).search("  ", CatalogFilter.ALL)

        assertTrue(result.isFailure)
        assertEquals(0, client.searches)
    }

    @Test
    fun emptyCatalogResponseMapsToEmptyResults() = runBlocking {
        val result = YouTubeMusicRepository(FakeClient(emptyList()))
            .search("unknown song", CatalogFilter.SONGS)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }
}
