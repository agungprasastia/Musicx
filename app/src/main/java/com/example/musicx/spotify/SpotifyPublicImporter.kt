package com.example.musicx.spotify

import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SpotifyImportResult(
    val title: String,
    val artworkUrl: String?,
    val tracks: List<CatalogItem.Track>,
)

// ponytail: scrapes public embed __NEXT_DATA__ without cookies or login. Add Web API OAuth if embed format deprecated.
class SpotifyPublicImporter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun importFromUrl(urlOrId: String): Result<SpotifyImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val (type, id) = extractSpotifyResource(urlOrId)
                ?: throw IllegalArgumentException("Invalid Spotify playlist or album URL/ID.")

            val embedUrl = "https://open.spotify.com/embed/$type/$id"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Spotify responded with HTTP ${response.code}")
            }
            val html = response.body?.string() ?: throw IOException("Empty response from Spotify")
            parseEmbedHtml(html)
        }
    }

    fun parseEmbedHtml(html: String): SpotifyImportResult {
        val marker = "id=\"__NEXT_DATA__\""
        val idx = html.indexOf(marker)
        if (idx == -1) {
            throw IllegalArgumentException("Could not find Spotify payload in page.")
        }
        val start = html.indexOf('>', idx) + 1
        val end = html.indexOf("</script>", start)
        if (start <= 0 || end <= start) {
            throw IllegalArgumentException("Malformed Spotify page structure.")
        }
        val jsonStr = html.substring(start, end)
        return parseEmbedJson(jsonStr)
    }

    fun parseEmbedJson(jsonStr: String): SpotifyImportResult {
        val root = Json.parseToJsonElement(jsonStr).jsonObject
        val props = root["props"]?.jsonObject ?: throw IllegalArgumentException("Missing props")
        val pageProps = props["pageProps"]?.jsonObject ?: throw IllegalArgumentException("Missing pageProps")
        val state = pageProps["state"]?.jsonObject ?: throw IllegalArgumentException("Missing state")
        val data = state["data"]?.jsonObject ?: throw IllegalArgumentException("Missing data")
        val entity = data["entity"]?.jsonObject ?: throw IllegalArgumentException("Missing entity")

        val title = entity["name"]?.jsonPrimitive?.contentOrNull
            ?: entity["title"]?.jsonPrimitive?.contentOrNull
            ?: "Spotify Playlist"

        val coverUrl = entity["coverArt"]?.jsonObject
            ?.get("sources")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.contentOrNull
            ?: entity["visualIdentity"]?.jsonObject
                ?.get("image")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull

        val trackList = entity["trackList"]?.jsonArray ?: throw IllegalArgumentException("No tracks found in playlist")
        val tracks = trackList.mapNotNull { itemElement ->
            val trackObj = itemElement.jsonObject
            val trackTitle = trackObj["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val trackArtist = trackObj["subtitle"]?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
            val uri = trackObj["uri"]?.jsonPrimitive?.contentOrNull
            val trackId = uri?.substringAfterLast(":") ?: UUID.randomUUID().toString()
            val durationMs = trackObj["duration"]?.jsonPrimitive?.longOrNull

            CatalogItem.Track(
                id = trackId,
                title = trackTitle,
                artist = trackArtist,
                album = title,
                artworkUrl = coverUrl,
                durationMs = durationMs
            )
        }

        if (tracks.isEmpty()) {
            throw IllegalArgumentException("Playlist contains 0 playable tracks.")
        }

        return SpotifyImportResult(
            title = title,
            artworkUrl = coverUrl,
            tracks = tracks
        )
    }

    companion object {
        private val RESOURCE_REGEX = Regex("""(?:spotify\.com\/(playlist|album)\/|spotify:(playlist|album):)([a-zA-Z0-9]+)""")

        fun extractSpotifyResource(input: String): Pair<String, String>? {
            val clean = input.trim()
            val match = RESOURCE_REGEX.find(clean)
            if (match != null) {
                val type = match.groupValues[1].ifEmpty { match.groupValues[2] }
                val id = match.groupValues[3]
                return Pair(type, id)
            }
            if (clean.matches(Regex("""^[a-zA-Z0-9]{22}$"""))) {
                return Pair("playlist", clean)
            }
            return null
        }
    }
}
