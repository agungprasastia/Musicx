package com.example.musicx

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.musicx.ui.HomeScreen
import com.example.musicx.ui.LibraryScreen
import com.example.musicx.ui.MusicxApp
import com.example.musicx.ui.SearchScreen
import org.junit.Rule
import org.junit.Test

class MusicNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeShowsFeaturedMusic() {
        compose.setContent { HomeScreen(onTrackSelected = {}) }

        compose.onNodeWithText("Made for you").assertIsDisplayed()
        compose.onNodeWithText("Recently played").assertIsDisplayed()
    }

    @Test
    fun searchShowsPromptBeforeQuery() {
        compose.setContent { SearchScreen(onTrackSelected = {}) }

        compose.onNodeWithText("Search songs, albums, artists").assertIsDisplayed()
        compose.onNodeWithText("Find your next favorite").assertIsDisplayed()
    }

    @Test
    fun libraryShowsMusicCategories() {
        compose.setContent { LibraryScreen(onTrackSelected = {}) }

        compose.onNodeWithText("Playlists").assertIsDisplayed()
        compose.onNodeWithText("Albums").assertIsDisplayed()
        compose.onNodeWithText("Artists").assertIsDisplayed()
    }

    @Test
    fun bottomNavigationKeepsMiniPlayerVisible() {
        compose.setContent { MusicxApp() }

        compose.onNodeWithText("Search").performClick()
        compose.onNodeWithText("Find your next favorite").assertIsDisplayed()
        compose.onNodeWithText("Demo · M83").assertIsDisplayed()
        compose.onNodeWithText("Library").performClick()
        compose.onNodeWithText("Your Library").assertIsDisplayed()
        compose.onNodeWithText("Demo · M83").assertIsDisplayed()
    }

    @Test
    fun miniPlayerOpensNowPlayingAndDemoControlChangesState() {
        compose.setContent { MusicxApp() }

        compose.onNodeWithContentDescription("Open Now Playing: Midnight City").performClick()
        compose.onNodeWithText("DEMO PREVIEW · NO AUDIO").assertIsDisplayed()
        compose.onNodeWithText("Demo timeline").assertIsDisplayed()
        compose.onNodeWithText("▶").performClick()
        compose.onNodeWithText("Ⅱ").assertIsDisplayed()
    }
}
