package com.example.musicx

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.musicx.catalog.youtube.CatalogRepository
import com.example.musicx.core.model.BrowsePage
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.ui.CatalogUiState
import com.example.musicx.ui.HomeScreen
import com.example.musicx.ui.LibraryScreen
import com.example.musicx.ui.MusicViewModel
import com.example.musicx.ui.MusicxApp
import com.example.musicx.ui.SearchScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MusicNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private class FakeRepository : CatalogRepository {
        override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogItem>> =
            Result.success(emptyList())

        override suspend fun browse(id: String): Result<BrowsePage> =
            Result.success(BrowsePage(id, "Page", emptyList()))
    }

    @Test
    fun homeShowsBrowseCollections() {
        compose.setContent { HomeScreen(onBrowseSelected = {}) }

        compose.onNodeWithText("Made for you").assertIsDisplayed()
        compose.onNodeWithText("Charts").assertIsDisplayed()
        compose.onNodeWithText("New releases").assertIsDisplayed()
    }

    @Test
    fun searchShowsPromptBeforeQuery() {
        compose.setContent {
            SearchScreen(
                state = CatalogUiState.Idle,
                onSearch = { _, _ -> },
                onTrackSelected = {},
                onBrowseSelected = {},
                onRetry = {}
            )
        }

        compose.onNodeWithText("Search songs, albums, artists").assertIsDisplayed()
        compose.onNodeWithText("Find your next favorite").assertIsDisplayed()
    }

    @Test
    fun searchTypingRevealsResultsAndSelectsTrack() {
        val track = CatalogItem.Track("vid-1", "Midnight City", "M83", "Hurry Up", null, 243_000)
        var selected: CatalogItem.Track? = null
        compose.setContent {
            SearchScreen(
                state = CatalogUiState.Results(null, listOf(track)),
                onSearch = { _, _ -> },
                onTrackSelected = { selected = it },
                onBrowseSelected = {},
                onRetry = {}
            )
        }

        compose.onNodeWithTag("search-input").performTextInput("midnight")
        compose.onNodeWithText("Midnight City").assertIsDisplayed().performClick()

        compose.runOnIdle { assertTrue(selected?.id == "vid-1") }
    }

    @Test
    fun searchShowsLoadingAndEmptyStates() {
        compose.setContent {
            SearchScreen(
                state = CatalogUiState.Loading,
                onSearch = { _, _ -> },
                onTrackSelected = {},
                onBrowseSelected = {},
                onRetry = {}
            )
        }

        compose.onNodeWithTag("search-input").performTextInput("midnight")
        compose.onNodeWithText("Loading YouTube Music").assertIsDisplayed()
    }

    @Test
    fun searchShowsEmptyState() {
        compose.setContent {
            SearchScreen(
                state = CatalogUiState.Empty(null),
                onSearch = { _, _ -> },
                onTrackSelected = {},
                onBrowseSelected = {},
                onRetry = {}
            )
        }

        compose.onNodeWithTag("search-input").performTextInput("nothing")
        compose.onNodeWithText("No results found").assertIsDisplayed()
    }

    @Test
    fun searchShowsRetryableError() {
        var retried = false
        compose.setContent {
            SearchScreen(
                state = CatalogUiState.Error("Could not reach YouTube Music. Check your connection and try again."),
                onSearch = { _, _ -> },
                onTrackSelected = {},
                onBrowseSelected = {},
                onRetry = { retried = true }
            )
        }

        compose.onNodeWithTag("search-input").performTextInput("m83")
        compose.onNodeWithText("Could not reach YouTube Music. Check your connection and try again.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        compose.runOnIdle { assertTrue(retried) }
    }

    @Test
    fun libraryAsksForAudioPermission() {
        var requested = false
        compose.setContent {
            LibraryScreen(
                tracks = emptyList(),
                isLoading = false,
                hasPermission = false,
                onRequestPermission = { requested = true },
                onTrackSelected = {},
            )
        }

        compose.onNodeWithText("Your Library").assertIsDisplayed()
        compose.onNodeWithText("Grant access").performClick()
        compose.runOnIdle { assertTrue(requested) }
    }

    @Test
    fun bottomNavigationKeepsPlayerPlaceholderVisible() {
        val model = MusicViewModel(FakeRepository())
        compose.setContent { MusicxApp(viewModel = model) }

        compose.onNodeWithContentDescription("Nothing playing").assertIsDisplayed()
        compose.onNodeWithText("Search").performClick()
        compose.onNodeWithText("Find your next favorite").assertIsDisplayed()
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Your Library").assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Charts").assertIsDisplayed()
    }
}
