package com.example.musicx.ui

import com.example.musicx.catalog.youtube.CatalogRepository
import com.example.musicx.core.model.BrowsePage
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.spotify.SpotifyRepository
import com.example.musicx.spotify.auth.InMemorySpotifySecretStore
import com.example.musicx.spotify.auth.SpotifyClientToken
import com.example.musicx.spotify.auth.SpotifyTokenProvider
import com.example.musicx.spotify.auth.SpotifyWebToken
import com.example.musicx.spotify.model.SpotifyPlaylist
import com.example.musicx.spotify.model.SpotifySessionState
import com.example.musicx.spotify.model.SpotifyTrack
import com.example.musicx.spotify.net.SpotifyHttpTransport
import com.example.musicx.spotify.net.SpotifyLibraryClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyViewModelTest {

    private class FakeCatalogRepository : CatalogRepository {
        override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogItem>> =
            Result.success(emptyList())

        override suspend fun browse(id: String): Result<BrowsePage> =
            Result.success(BrowsePage("id", "Page", emptyList()))
    }

    private class FakeSpotifyRepository(
        var playlistsResult: List<SpotifyPlaylist> = emptyList(),
        var tracksResult: List<SpotifyTrack> = emptyList(),
    ) : SpotifyRepository(
        InMemorySpotifySecretStore(),
        SpotifyTokenProvider(
            InMemorySpotifySecretStore(),
            { SpotifyWebToken("bearer", "cid", Long.MAX_VALUE, false) },
            { SpotifyClientToken("client", 3600) },
        ),
        SpotifyLibraryClient(
            SpotifyTokenProvider(
                InMemorySpotifySecretStore(),
                { SpotifyWebToken("bearer", "cid", Long.MAX_VALUE, false) },
                { SpotifyClientToken("client", 3600) },
            ),
            SpotifyHttpTransport(),
        ),
    ) {
        private val _fakeSession = MutableStateFlow<SpotifySessionState>(SpotifySessionState.Disconnected)
        override val session: StateFlow<SpotifySessionState> = _fakeSession.asStateFlow()

        override suspend fun playlists(): List<SpotifyPlaylist> = playlistsResult
        override suspend fun playlistTracks(id: String): List<SpotifyTrack> = tracksResult
        override suspend fun likedTracks(): List<SpotifyTrack> = tracksResult
    }

    private val samplePlaylist = SpotifyPlaylist(
        id = "pl-1",
        name = "My Jams",
        owner = "Agung",
        imageUrl = "https://example.com/cover.jpg",
        trackCount = 10,
    )

    private val sampleTrack = SpotifyTrack(
        id = "tr-1",
        title = "Midnight City",
        artists = listOf("M83"),
        album = "Hurry Up, We're Dreaming",
        durationMs = 240_000,
        imageUrl = "https://example.com/art.jpg",
    )

    @Test
    fun initialStateIsDisconnected() {
        val repo = FakeSpotifyRepository()
        val viewModel = SpotifyViewModel(
            repository = repo,
            catalogRepository = FakeCatalogRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        assertEquals(SpotifyUiState.Disconnected, viewModel.ui.value)
    }

    @Test
    fun loadLibrarySetsLibraryStateAndCachesPlaylists() = runBlocking {
        val repo = FakeSpotifyRepository(playlistsResult = listOf(samplePlaylist))
        val viewModel = SpotifyViewModel(
            repository = repo,
            catalogRepository = FakeCatalogRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        viewModel.loadLibrary()

        val state = viewModel.ui.value
        assertTrue(state is SpotifyUiState.Library)
        val library = state as SpotifyUiState.Library
        assertEquals(listOf(samplePlaylist), library.playlists)
    }

    @Test
    fun loadPlaylistTracksSetsTracksState() = runBlocking {
        val repo = FakeSpotifyRepository(
            playlistsResult = listOf(samplePlaylist),
            tracksResult = listOf(sampleTrack),
        )
        val viewModel = SpotifyViewModel(
            repository = repo,
            catalogRepository = FakeCatalogRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        viewModel.loadLibrary()
        viewModel.loadPlaylistTracks(samplePlaylist)

        val state = viewModel.ui.value
        assertTrue(state is SpotifyUiState.Tracks)
        val tracksState = state as SpotifyUiState.Tracks
        assertEquals(samplePlaylist.id, tracksState.playlist.id)
        assertEquals(1, tracksState.tracks.size)
        assertEquals(sampleTrack.id, tracksState.tracks.first().id)
    }

    @Test
    fun goBackFromTracksReturnsToLibrary() = runBlocking {
        val repo = FakeSpotifyRepository(
            playlistsResult = listOf(samplePlaylist),
            tracksResult = listOf(sampleTrack),
        )
        val viewModel = SpotifyViewModel(
            repository = repo,
            catalogRepository = FakeCatalogRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        viewModel.loadLibrary()
        viewModel.loadPlaylistTracks(samplePlaylist)

        val handled = viewModel.goBack()

        assertTrue(handled)
        assertTrue(viewModel.ui.value is SpotifyUiState.Library)
        assertEquals(
            listOf(samplePlaylist),
            (viewModel.ui.value as SpotifyUiState.Library).playlists,
        )
    }

    @Test
    fun goBackFromLibraryReturnsFalseToExit() = runBlocking {
        val repo = FakeSpotifyRepository(playlistsResult = listOf(samplePlaylist))
        val viewModel = SpotifyViewModel(
            repository = repo,
            catalogRepository = FakeCatalogRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        viewModel.loadLibrary()

        val handled = viewModel.goBack()

        assertFalse(handled)
        assertTrue(viewModel.ui.value is SpotifyUiState.Library)
    }
}
