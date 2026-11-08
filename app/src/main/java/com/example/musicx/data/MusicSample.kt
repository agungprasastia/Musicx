package com.example.musicx.data

data class MusicSample(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationLabel: String,
    val artworkColors: List<Long>
)

object MusicSamples {
    private val midnight = MusicSample(
        id = "midnight-city",
        title = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        durationLabel = "4:03",
        artworkColors = listOf(0xFFDB8C68, 0xFF68436D, 0xFF293F55)
    )
    private val afterHours = MusicSample(
        id = "after-hours",
        title = "After Hours",
        artist = "The Weeknd",
        album = "After Hours",
        durationLabel = "6:01",
        artworkColors = listOf(0xFF9D263D, 0xFF391D36, 0xFF120F18)
    )
    private val sunset = MusicSample(
        id = "pink-plus-white",
        title = "Pink + White",
        artist = "Frank Ocean",
        album = "Blonde",
        durationLabel = "3:04",
        artworkColors = listOf(0xFFE5A777, 0xFF927A81, 0xFF3A5254)
    )

    val tracks = listOf(midnight, afterHours, sunset)
    val artists = listOf(midnight, afterHours, sunset)
    val albums = listOf(midnight, afterHours, sunset)
    val playlists = listOf(
        midnight.copy(id = "daily-mix", title = "Daily Mix", album = "A mix made for you"),
        afterHours.copy(id = "after-dark", title = "After Dark", album = "Late night favorites")
    )
}
