package com.example.musicx.ui

import com.example.musicx.catalog.youtube.CatalogRepository
import com.example.musicx.core.model.BrowsePage
import com.example.musicx.core.model.CatalogError
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MusicViewModelTest {
    private class FakeRepository(
        var searchResult: Result<List<CatalogItem>> = Result.success(emptyList()),
        var browseResult: Result<BrowsePage> = Result.success(BrowsePage("id", "Page", emptyList())),
        var searchGate: CompletableDeferred<Unit>? = null,
    ) : CatalogRepository {
        var searches = 0
        var browses = 0
        var lastFilter: CatalogFilter? = null
        var lastQuery: String? = null
        var firstSearchCancelled = false

        override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogItem>> {
            searches++
            lastQuery = query
            lastFilter = filter
            if (searches == 1 && searchGate != null) {
                try {
                    searchGate!!.await()
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    firstSearchCancelled = true
                    throw cancelled
                }
            }
            return searchResult
        }

        override suspend fun browse(id: String): Result<BrowsePage> {
            browses++
            return browseResult
        }
    }

    private fun viewModel(
        repository: FakeRepository,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
    ) = MusicViewModel(repository, scope)

    @Test
    fun blankQueryStaysIdleAndSkipsRepository() = runBlocking {
        val repository = FakeRepository()
        val model = viewModel(repository)

        model.search("   ", CatalogFilter.ALL)

        assertEquals(CatalogUiState.Idle, model.uiState.value)
        assertEquals(0, repository.searches)
    }

    @Test
    fun searchEmitsLoadingThenResults() = runBlocking {
        val repository = FakeRepository(
            searchResult = Result.success(listOf(track("vid-1", "Midnight City"))),
            searchGate = CompletableDeferred(),
        )
        val model = viewModel(repository)

        model.search("midnight", CatalogFilter.SONGS)

        assertEquals(CatalogUiState.Loading, model.uiState.value)
        repository.searchGate!!.complete(Unit)

        val results = model.uiState.value as CatalogUiState.Results
        assertEquals("midnight", repository.lastQuery)
        assertEquals(CatalogFilter.SONGS, repository.lastFilter)
        assertEquals(1, results.items.size)
        assertEquals("Midnight City", results.items[0].title)
    }

    @Test
    fun emptySearchResultsEmitEmptyState() = runBlocking {
        val repository = FakeRepository(searchResult = Result.success(emptyList()))
        val model = viewModel(repository)

        model.search("nothing", CatalogFilter.ALL)

        assertTrue(model.uiState.value is CatalogUiState.Empty)
    }

    @Test
    fun failedSearchEmitsGenericErrorWithoutLeakingUrl() = runBlocking {
        val repository = FakeRepository(
            searchResult = Result.failure(
                CatalogError.Network(IOException("https://rr1.googlevideo.com/videoplayback?token=secret"))
            )
        )
        val model = viewModel(repository)

        model.search("m83", CatalogFilter.ALL)

        val error = model.uiState.value as CatalogUiState.Error
        assertFalse(error.message.contains("googlevideo.com"))
        assertFalse(error.message.contains("secret"))
    }

    @Test
    fun retryRepeatsLastSearch() = runBlocking {
        val repository = FakeRepository(searchResult = Result.failure(CatalogError.InvalidResponse))
        val model = viewModel(repository)
        model.search("m83", CatalogFilter.ARTISTS)
        assertTrue(model.uiState.value is CatalogUiState.Error)

        repository.searchResult = Result.success(listOf(artist("UC1", "M83")))
        model.retry()

        assertEquals(2, repository.searches)
        assertEquals(CatalogFilter.ARTISTS, repository.lastFilter)
        assertTrue(model.uiState.value is CatalogUiState.Results)
    }

    @Test
    fun browseUsesPageTitle() = runBlocking {
        val repository = FakeRepository(
            browseResult = Result.success(
                BrowsePage(
                    id = "MPREb_1",
                    title = "Hurry Up, We're Dreaming",
                    items = listOf(track("vid-1", "Midnight City")),
                )
            )
        )
        val model = viewModel(repository)

        model.browse("MPREb_1")

        assertEquals(1, repository.browses)
        val results = model.uiState.value as CatalogUiState.Results
        assertEquals("Hurry Up, We're Dreaming", results.title)
        assertEquals(1, results.items.size)
    }

    @Test
    fun newSearchCancelsPreviousInFlightSelection() = runBlocking {
        val repository = FakeRepository(
            searchResult = Result.success(listOf(track("vid-2", "After Hours"))),
            searchGate = CompletableDeferred(),
        )
        val model = viewModel(repository)

        model.search("first", CatalogFilter.ALL)
        model.search("second", CatalogFilter.SONGS)

        assertTrue(repository.firstSearchCancelled)
        val results = model.uiState.value as CatalogUiState.Results
        assertEquals("After Hours", results.items[0].title)
    }

    private fun track(id: String, title: String) = CatalogItem.Track(
        id = id,
        title = title,
        artist = "Artist",
        album = null,
        artworkUrl = null,
        durationMs = null,
    )

    private fun artist(id: String, title: String) = CatalogItem.Artist(
        id = id,
        title = title,
        artworkUrl = null,
    )
}
