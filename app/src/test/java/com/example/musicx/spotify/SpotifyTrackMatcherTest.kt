package com.example.musicx.spotify

import com.example.musicx.core.model.CatalogItem
import com.example.musicx.spotify.match.MatchDecision
import com.example.musicx.spotify.match.SpotifyTrackMatcher
import com.example.musicx.spotify.model.SpotifyTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyTrackMatcherTest {
    private val matcher = SpotifyTrackMatcher()

    @Test
    fun exactMetadataIsMatchedWithFullScore() {
        val spotify = spotifyTrack("Midnight City", listOf("M83"), "Hurry Up", 243_000)
        val candidate = track("vid-1", "Midnight City", "M83", "Hurry Up", 243_000)

        val decision = matcher.decide(spotify, listOf(candidate))

        val matched = decision as MatchDecision.Matched
        assertEquals(candidate, matched.best.candidate)
        assertEquals(1.0, matched.best.score, 0.0001)
    }

    @Test
    fun featureAndPunctuationVariantsStillMatch() {
        val spotify = spotifyTrack(
            title = "Sunflower (Spider-Man: Into the Spider-Verse)",
            artists = listOf("Post Malone", "Swae Lee"),
        )
        val candidate = track("vid-2", "Sunflower", "Post Malone feat. Swae Lee")

        val decision = matcher.decide(spotify, listOf(candidate))

        assertTrue(decision is MatchDecision.Matched)
    }

    @Test
    fun diacriticsAndCaseAreNormalized() {
        val spotify = spotifyTrack("Déjà Vu", listOf("Beyoncé"))
        val candidate = track("vid-3", "DEJA VU", "BEYONCE")

        val decision = matcher.decide(spotify, listOf(candidate))

        val matched = decision as MatchDecision.Matched
        assertEquals(1.0, matched.best.score, 0.0001)
    }

    @Test
    fun remasterSuffixDoesNotBlockAMatch() {
        val spotify = spotifyTrack("Song", listOf("Artist"))
        val candidate = track("vid-4", "Song - Remastered 2011", "Artist")

        val decision = matcher.decide(spotify, listOf(candidate))

        assertTrue(decision is MatchDecision.Matched)
    }

    @Test
    fun wrongArtistFallsBelowThreshold() {
        val spotify = spotifyTrack("Hello", listOf("Adele"))
        val candidate = track("vid-5", "Hello", "Lionel Richie")

        assertEquals(MatchDecision.NoMatch, matcher.decide(spotify, listOf(candidate)))
    }

    @Test
    fun closeDurationRanksAheadOfDistantDuration() {
        val spotify = spotifyTrack("Song", listOf("Artist"), "Album", 180_000)
        val close = track("vid-6", "Song", "Artist", "Album", 181_000)
        val distant = track("vid-7", "Song", "Artist", "Album", 195_000)

        val ranked = matcher.rank(spotify, listOf(distant, close))

        assertEquals(close, ranked.first().candidate)
        assertTrue(ranked.first().score > ranked[1].score)
    }

    @Test
    fun nearIdenticalCandidatesAreAmbiguous() {
        val spotify = spotifyTrack("Song", listOf("Artist"), "Album", 180_000)
        val first = track("vid-8", "Song", "Artist", "Album", 180_000)
        val second = track("vid-9", "Song", "Artist", "Album", 180_000)

        val decision = matcher.decide(spotify, listOf(first, second))

        val ambiguous = decision as MatchDecision.Ambiguous
        assertEquals(setOf(first, second), ambiguous.candidates.map { it.candidate }.toSet())
    }

    @Test
    fun unrelatedCandidateIsNoMatch() {
        val spotify = spotifyTrack("Bohemian Rhapsody", listOf("Queen"))
        val candidate = track("vid-10", "Blinding Lights", "The Weeknd")

        assertEquals(MatchDecision.NoMatch, matcher.decide(spotify, listOf(candidate)))
    }

    @Test
    fun emptyAndBlankCandidatesAreIgnored() {
        assertEquals(MatchDecision.NoMatch, matcher.decide(spotifyTrack("Song", listOf("Artist")), emptyList()))
        assertEquals(
            MatchDecision.NoMatch,
            matcher.decide(
                spotifyTrack("Song", listOf("Artist")),
                listOf(track("blank", "   ", "Artist")),
            ),
        )
    }

    private fun spotifyTrack(
        title: String,
        artists: List<String>,
        album: String? = null,
        durationMs: Long? = null,
    ) = SpotifyTrack(
        id = "spotify-$title",
        title = title,
        artists = artists,
        album = album,
        durationMs = durationMs,
        imageUrl = null,
    )

    private fun track(
        id: String,
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null,
    ) = CatalogItem.Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = null,
        durationMs = durationMs,
    )
}
