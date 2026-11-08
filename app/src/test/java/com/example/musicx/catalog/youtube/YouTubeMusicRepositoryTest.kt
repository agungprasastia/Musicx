package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogError
import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class YouTubeMusicRepositoryTest {
    private class FakeClient(
        private val items: List<CatalogDto> = emptyList(),
        private val searchResult: Result<List<CatalogDto>>? = null,
        private val browseResult: Result<BrowseDto>? = null,
    ) : YouTubeMusicClient {
        var searches = 0
        var browses = 0
        override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogDto>> {
            searches++
            return searchResult ?: Result.success(items)
        }

        override suspend fun browse(id: String): Result<BrowseDto> {
            browses++
            return browseResult ?: Result.success(BrowseDto(id = id, title = "Album", items = items))
        }
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

    @Test
    fun browseMapsPageAndBlankIdSkipsClient() = runBlocking {
        val client = FakeClient(
            items = listOf(
                CatalogDto.Track("track-1", "Track", "Artist", "Album", null, 1_000),
                CatalogDto.Track("", "Malformed", "Artist", null, null, null),
            )
        )
        val repository = YouTubeMusicRepository(client)

        val page = repository.browse("MPREb_album1").getOrThrow()
        assertEquals("MPREb_album1", page.id)
        assertEquals("Album", page.title)
        assertEquals(1, page.items.size)

        val blank = repository.browse("  ")
        assertTrue(blank.exceptionOrNull() is CatalogError.EmptyBrowseId)
        assertEquals(1, client.browses)
    }

    @Test
    fun browseMissingTitleFailsWithTypedError() = runBlocking {
        val client = FakeClient(
            browseResult = Result.success(BrowseDto(id = "MPREb_1", title = " ", items = emptyList()))
        )

        val result = YouTubeMusicRepository(client).browse("MPREb_1")

        assertTrue(result.exceptionOrNull() is CatalogError.InvalidResponse)
    }

    @Test
    fun typedFailureIsPreserved() = runBlocking {
        val failure = CatalogError.Http(429)
        val client = FakeClient(searchResult = Result.failure(failure))

        val result = YouTubeMusicRepository(client).search("m83", CatalogFilter.ALL)

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun cancellationPropagates() = runBlocking {
        val client = object : YouTubeMusicClient {
            override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogDto>> =
                throw CancellationException("cancelled")

            override suspend fun browse(id: String): Result<BrowseDto> =
                throw CancellationException("cancelled")
        }

        try {
            YouTubeMusicRepository(client).search("m83", CatalogFilter.ALL)
            fail("Expected cancellation to propagate")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }
}
