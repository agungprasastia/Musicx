package com.example.musicx.spotify.match

import com.example.musicx.core.model.CatalogItem
import com.example.musicx.spotify.model.SpotifyTrack
import java.text.Normalizer
import kotlin.math.abs

data class TrackMatch(
    val candidate: CatalogItem.Track,
    val score: Double,
    val titleScore: Double,
    val artistScore: Double,
    val albumScore: Double?,
    val durationScore: Double?,
)

sealed interface MatchDecision {
    data class Matched(val best: TrackMatch) : MatchDecision
    data class Ambiguous(val candidates: List<TrackMatch>) : MatchDecision
    data object NoMatch : MatchDecision
}

/**
 * Deterministic metadata matcher between a Spotify track and YouTube Music
 * search results. It never streams or resolves audio; it only ranks candidate
 * catalog entries so the UI can ask the user before playback when the result
 * is uncertain.
 */
class SpotifyTrackMatcher(
    private val threshold: Double = DEFAULT_THRESHOLD,
    private val ambiguityMargin: Double = DEFAULT_AMBIGUITY_MARGIN,
) {
    fun rank(track: SpotifyTrack, candidates: List<CatalogItem.Track>): List<TrackMatch> {
        val title = normalizeTokens(track.title)
        if (title.isEmpty()) return emptyList()

        return candidates
            .mapNotNull { candidate -> score(track, title, candidate) }
            .sortedWith(
                compareByDescending<TrackMatch> { it.score }
                    .thenBy { it.candidate.title }
                    .thenBy { it.candidate.id }
            )
    }

    fun decide(track: SpotifyTrack, candidates: List<CatalogItem.Track>): MatchDecision {
        val ranked = rank(track, candidates)
        val best = ranked.firstOrNull() ?: return MatchDecision.NoMatch
        if (best.score < threshold) return MatchDecision.NoMatch

        val close = ranked.filter { it.score >= threshold && best.score - it.score <= ambiguityMargin }
        return if (close.size <= 1) {
            MatchDecision.Matched(best)
        } else {
            MatchDecision.Ambiguous(close)
        }
    }

    private fun score(
        track: SpotifyTrack,
        normalizedTitle: Set<String>,
        candidate: CatalogItem.Track,
    ): TrackMatch? {
        val candidateTitle = normalizeTokens(candidate.title)
        if (candidateTitle.isEmpty()) return null

        val titleScore = titleScore(normalizedTitle, candidateTitle)
        val artistScore = overlapScore(
            normalizeTokens(track.artists.joinToString(" ")),
            normalizeTokens(candidate.artist),
        )
        val albumScore = if (track.album != null && candidate.album != null) {
            overlapScore(normalizeTokens(track.album), normalizeTokens(candidate.album))
        } else {
            null
        }
        val durationScore = if (track.durationMs != null && candidate.durationMs != null &&
            track.durationMs > 0 && candidate.durationMs > 0
        ) {
            durationScore(track.durationMs, candidate.durationMs)
        } else {
            null
        }

        var weighted = titleScore * TITLE_WEIGHT + artistScore * ARTIST_WEIGHT
        var totalWeight = TITLE_WEIGHT + ARTIST_WEIGHT
        if (albumScore != null) {
            weighted += albumScore * ALBUM_WEIGHT
            totalWeight += ALBUM_WEIGHT
        }
        if (durationScore != null) {
            weighted += durationScore * DURATION_WEIGHT
            totalWeight += DURATION_WEIGHT
        }

        return TrackMatch(
            candidate = candidate,
            score = weighted / totalWeight,
            titleScore = titleScore,
            artistScore = artistScore,
            albumScore = albumScore,
            durationScore = durationScore,
        )
    }

    private fun titleScore(left: Set<String>, right: Set<String>): Double {
        val shared = left.count { it in right }
        if (shared == 0) return 0.0

        val coverage = shared.toDouble() / minOf(left.size, right.size)
        val extraCandidateTokens = right.count { it !in left }
        val penalty = (1.0 - EXTRA_TOKEN_PENALTY * extraCandidateTokens)
            .coerceAtLeast(MIN_TITLE_FACTOR)
        return coverage * penalty
    }

    private fun overlapScore(left: Set<String>, right: Set<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val shared = left.count { it in right }
        if (shared == 0) return 0.0

        val union = left.size + right.size - shared
        val jaccard = shared.toDouble() / union
        val coverage = shared.toDouble() / minOf(left.size, right.size)
        return maxOf(jaccard, coverage)
    }

    private fun durationScore(left: Long, right: Long): Double {
        val difference = abs(left - right).toDouble()
        return (1.0 - difference / MAX_DURATION_DIFFERENCE_MS).coerceIn(0.0, 1.0)
    }

    companion object {
        const val DEFAULT_THRESHOLD = 0.72
        const val DEFAULT_AMBIGUITY_MARGIN = 0.08

        private const val TITLE_WEIGHT = 0.58
        private const val ARTIST_WEIGHT = 0.27
        private const val ALBUM_WEIGHT = 0.08
        private const val DURATION_WEIGHT = 0.07

        private const val EXTRA_TOKEN_PENALTY = 0.10
        private const val MIN_TITLE_FACTOR = 0.40
        private const val MAX_DURATION_DIFFERENCE_MS = 30_000.0

        private val NOISE_TOKENS = setOf(
            "feat", "ft", "featuring",
            "remaster", "remastered",
            "official", "video", "audio", "lyric", "lyrics",
            "hd", "hq", "explicit", "clean",
        )

        private val COMBINING_MARKS = Regex("\\p{M}+")
        private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

        internal fun normalizeTokens(raw: String?): Set<String> {
            if (raw.isNullOrBlank()) return emptySet()
            val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
            val withoutMarks = decomposed.replace(COMBINING_MARKS, "")
            val folded = withoutMarks
                .lowercase()
                .replace('&', ' ')
                .replace('\'', ' ')
                .replace('\u2019', ' ')
            return folded
                .split(NON_WORD)
                .filter { it.isNotEmpty() && it !in NOISE_TOKENS }
                .toSet()
        }
    }
}
