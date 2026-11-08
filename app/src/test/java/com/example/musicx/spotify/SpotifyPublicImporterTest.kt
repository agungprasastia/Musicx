package com.example.musicx.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyPublicImporterTest {

    private val importer = SpotifyPublicImporter()

    @Test
    fun `extractSpotifyResource extracts playlist and album ids correctly`() {
        assertEquals(
            Pair("playlist", "37i9dQZF1DXcBWIGoYBM5M"),
            SpotifyPublicImporter.extractSpotifyResource("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=12345")
        )
        assertEquals(
            Pair("playlist", "37i9dQZF1DXcBWIGoYBM5M"),
            SpotifyPublicImporter.extractSpotifyResource("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M")
        )
        assertEquals(
            Pair("album", "4aawyAB9vmqN3uQ7FjRGTy"),
            SpotifyPublicImporter.extractSpotifyResource("https://open.spotify.com/album/4aawyAB9vmqN3uQ7FjRGTy")
        )
        assertEquals(
            Pair("playlist", "37i9dQZF1DXcBWIGoYBM5M"),
            SpotifyPublicImporter.extractSpotifyResource("37i9dQZF1DXcBWIGoYBM5M")
        )
        assertNull(SpotifyPublicImporter.extractSpotifyResource("https://invalid-url.com"))
    }

    @Test
    fun `parseEmbedJson correctly parses playlist structure and tracks`() {
        val sampleJson = """
            {
                "props": {
                    "pageProps": {
                        "state": {
                            "data": {
                                "entity": {
                                    "name": "Today's Top Hits",
                                    "coverArt": {
                                        "sources": [
                                            { "url": "https://i.scdn.co/image/art123" }
                                        ]
                                    },
                                    "trackList": [
                                        {
                                            "title": "Birds of a Feather",
                                            "subtitle": "Billie Eilish",
                                            "duration": 190000,
                                            "uri": "spotify:track:6dOtVTDmmpTTemRI9xYs9J"
                                        },
                                        {
                                            "title": "Taste",
                                            "subtitle": "Sabrina Carpenter",
                                            "duration": 157000,
                                            "uri": "spotify:track:2eAvDnpXP5W0cVhuKJrwz8"
                                        }
                                    ]
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val result = importer.parseEmbedJson(sampleJson)
        assertEquals("Today's Top Hits", result.title)
        assertEquals("https://i.scdn.co/image/art123", result.artworkUrl)
        assertEquals(2, result.tracks.size)

        val first = result.tracks[0]
        assertEquals("6dOtVTDmmpTTemRI9xYs9J", first.id)
        assertEquals("Birds of a Feather", first.title)
        assertEquals("Billie Eilish", first.artist)
        assertEquals(190000L, first.durationMs)
        assertEquals("https://i.scdn.co/image/art123", first.artworkUrl)

        val second = result.tracks[1]
        assertEquals("2eAvDnpXP5W0cVhuKJrwz8", second.id)
        assertEquals("Taste", second.title)
        assertEquals("Sabrina Carpenter", second.artist)
    }
}
