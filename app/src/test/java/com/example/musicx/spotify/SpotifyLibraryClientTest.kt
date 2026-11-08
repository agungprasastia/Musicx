package com.example.musicx.spotify

import com.example.musicx.spotify.model.SpotifyError
import com.example.musicx.spotify.net.parseLikedTracks
import com.example.musicx.spotify.net.parsePlaylists
import com.example.musicx.spotify.net.parsePlaylistTracks
import com.example.musicx.spotify.net.SpotifyHttpTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyLibraryClientTest {
    @Test
    fun transportRejectsNonHttpsUnknownHostsAndHostSuffixSpoofing() {
        listOf(
            "http://api-partner.spotify.com/pathfinder/v2/query",
            "https://evil.example/path",
            "https://api-partner.spotify.com.evil.example/path",
        ).forEach { url ->
            assertTrue(runCatching { SpotifyHttpTransport.validateSpotifyUrl(url) }.isFailure)
        }
        assertEquals(
            "https://api-partner.spotify.com/pathfinder/v2/query",
            SpotifyHttpTransport.validateSpotifyUrl("https://api-partner.spotify.com/pathfinder/v2/query"),
        )
    }

    @Test
    fun parsesPlaylistItemsAndDropsMalformedEntries() {
        val playlists = parsePlaylists("""{"data":{"me":{"libraryV3":{"totalCount":2,"items":[
          {"item":{"__typename":"PlaylistResponseWrapper","_uri":"spotify:playlist:abc","data":{"__typename":"Playlist","name":"Road","ownerV2":{"data":{"name":"Me"}}}}},
          {"item":{"__typename":"PlaylistResponseWrapper","_uri":"spotify:playlist:bad","data":{"__typename":"Playlist","name":" "}}}
        ]}}}}""")

        assertEquals(1, playlists.size)
        assertEquals("abc", playlists.single().id)
        assertEquals("Road", playlists.single().name)
        assertEquals("Me", playlists.single().owner)
    }

    @Test
    fun parsesPlaylistTracksWithArtistsAlbumAndDuration() {
        val tracks = parsePlaylistTracks("""{"data":{"playlistV2":{"content":{"totalCount":1,"items":[
          {"itemV2":{"data":{"uri":"spotify:track:t1","name":"Song","artists":{"items":[{"profile":{"name":"One"}},{"profile":{"name":"Two"}}]},"albumOfTrack":{"name":"Record"},"duration":{"totalMilliseconds":123000}}}}
        ]}}}}""")

        assertEquals(1, tracks.size)
        assertEquals("t1", tracks.single().id)
        assertEquals(listOf("One", "Two"), tracks.single().artists)
        assertEquals(123_000L, tracks.single().durationMs)
    }

    @Test
    fun parsesLikedTracksAndDropsMalformedEntries() {
        val tracks = parseLikedTracks("""{"data":{"me":{"library":{"tracks":{"items":[
          {"track":{"_uri":"spotify:track:liked1","data":{"name":"Liked","artists":{"items":[{"profile":{"name":"Artist"}}]}}}},
          {"track":{"data":{"name":"No ID"}}}
        ]}}}}}""")

        assertEquals(1, tracks.size)
        assertEquals("liked1", tracks.single().id)
        assertEquals(listOf("Artist"), tracks.single().artists)
    }

    @Test
    fun malformedOrUnexpectedResponsesHaveTypedError() {
        assertTrue(runCatching { parsePlaylists("not json") }.exceptionOrNull() is SpotifyError.InvalidResponse)
        assertTrue(runCatching { parsePlaylistTracks("{}") }.exceptionOrNull() is SpotifyError.ApiChanged)
    }
}
