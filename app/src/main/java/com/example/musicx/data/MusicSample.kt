package com.example.musicx.data

data class MusicSample(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationLabel: String,
    val artworkColors: List<Long>,
    val artworkUrl: String? = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
)

object MusicSamples {
    private val flyMe = MusicSample(
        id = "ZEcqHA7dbwM",
        title = "Fly Me To The Moon",
        artist = "Frank Sinatra",
        album = "It Might as Well Be Swing",
        durationLabel = "2:27",
        artworkColors = listOf(0xFF2C3E50, 0xFFBDC3C7)
    )
    private val hereComesSun = MusicSample(
        id = "xUNqsfFUwhY",
        title = "Here Comes The Sun",
        artist = "The Beatles",
        album = "Abbey Road",
        durationLabel = "3:05",
        artworkColors = listOf(0xFFF39C12, 0xFFD35400)
    )
    private val bohemian = MusicSample(
        id = "BSTsnWoslP4",
        title = "Bohemian Rhapsody",
        artist = "Queen",
        album = "A Night at the Opera",
        durationLabel = "5:54",
        artworkColors = listOf(0xFF1B1B1B, 0xFF8E44AD)
    )
    private val billieJean = MusicSample(
        id = "7CTJcHjkq0E",
        title = "Billie Jean",
        artist = "Michael Jackson",
        album = "Thriller",
        durationLabel = "4:54",
        artworkColors = listOf(0xFF34495E, 0xFFE74C3C)
    )
    private val blindingLights = MusicSample(
        id = "J7p4bzqLvCw",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        durationLabel = "3:20",
        artworkColors = listOf(0xFF9D263D, 0xFF391D36, 0xFF120F18)
    )
    private val antiHero = MusicSample(
        id = "3YgtjHZyCIQ",
        title = "Anti-Hero",
        artist = "Taylor Swift",
        album = "Midnights",
        durationLabel = "3:20",
        artworkColors = listOf(0xFF2C3E50, 0xFF3498DB)
    )
    private val espresso = MusicSample(
        id = "kIft-LUHHVA",
        title = "Espresso",
        artist = "Sabrina Carpenter",
        album = "Short n' Sweet",
        durationLabel = "2:55",
        artworkColors = listOf(0xFFE67E22, 0xFFF1C40F)
    )
    private val cantHelp = MusicSample(
        id = "h4R4aSIAG0A",
        title = "Can’t Help Falling In Love",
        artist = "Chris Isaak",
        album = "Beyond the Sun",
        durationLabel = "3:15",
        artworkColors = listOf(0xFF7F8C8D, 0xFFBDC3C7)
    )
    private val letDown = MusicSample(
        id = "ZVgHPSyEIqk",
        title = "Let Down",
        artist = "Radiohead",
        album = "OK Computer",
        durationLabel = "4:59",
        artworkColors = listOf(0xFF16A085, 0xFF2ECC71)
    )
    private val dynamite = MusicSample(
        id = "HaEYUJ2aRHs",
        title = "Dynamite",
        artist = "BTS",
        album = "BE",
        durationLabel = "3:19",
        artworkColors = listOf(0xFF9B59B6, 0xFFE91E63)
    )
    private val loseYourself = MusicSample(
        id = "4wOLVrGHiIU",
        title = "Lose Yourself",
        artist = "Eminem",
        album = "8 Mile",
        durationLabel = "5:26",
        artworkColors = listOf(0xFF2C3E50, 0xFF1A1A1A)
    )
    private val humble = MusicSample(
        id = "H4RELGc9su8",
        title = "HUMBLE.",
        artist = "Kendrick Lamar",
        album = "DAMN.",
        durationLabel = "2:57",
        artworkColors = listOf(0xFFC0392B, 0xFF2C3E50)
    )
    private val midnight = MusicSample(
        id = "dX3k_QDnzHE",
        title = "Midnight City",
        artist = "M83",
        album = "Hurry Up, We're Dreaming",
        durationLabel = "4:03",
        artworkColors = listOf(0xFFDB8C68, 0xFF68436D, 0xFF293F55)
    )
    private val afterHours = MusicSample(
        id = "ygTZZpVHNys",
        title = "After Hours",
        artist = "The Weeknd",
        album = "After Hours",
        durationLabel = "6:01",
        artworkColors = listOf(0xFF9D263D, 0xFF391D36, 0xFF120F18)
    )
    private val sunset = MusicSample(
        id = "uzS3WG6__G4",
        title = "Pink + White",
        artist = "Frank Ocean",
        album = "Blonde",
        durationLabel = "3:04",
        artworkColors = listOf(0xFFE5A777, 0xFF927A81, 0xFF3A5254)
    )

    val tracks = listOf(
        blindingLights,
        espresso,
        antiHero,
        flyMe,
        hereComesSun,
        bohemian,
        billieJean,
        cantHelp,
        letDown,
        dynamite,
        loseYourself,
        humble,
        midnight,
        afterHours,
        sunset,
    )
    val artists = tracks
    val albums = tracks
    val playlists = listOf(
        midnight.copy(id = "FEmusic_charts", title = "Top Hits", album = "Global charts"),
        afterHours.copy(id = "FEmusic_new_releases", title = "Fresh Music", album = "New releases"),
        sunset.copy(id = "FEmusic_moods_and_genres", title = "Vibe Selector", album = "Moods & genres")
    )
}
