package com.example.musicx.spotify.net

import com.example.musicx.spotify.model.SpotifyPlaylist
import com.example.musicx.spotify.model.SpotifyTrack
import com.example.musicx.spotify.model.SpotifyError
import com.example.musicx.spotify.auth.SpotifyTokenProvider
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

private val libraryJson = Json { ignoreUnknownKeys = true }

class SpotifyLibraryClient(
    private val tokens: SpotifyTokenProvider,
    private val transport: SpotifyHttpTransport = SpotifyHttpTransport(),
) {
    suspend fun playlists(): List<SpotifyPlaylist> {
        val headers = authHeaders()
        val result = mutableListOf<SpotifyPlaylist>()
        var offset = 0
        repeat(MAX_PAGES) {
            val root = query("libraryV3", LIBRARY_HASH, playlistVariables(offset), headers)
            val page = parsePlaylistPage(root)
            result += page.items
            if (page.rawCount == 0 || offset + page.rawCount >= page.total) return result
            offset += page.rawCount
        }
        return result
    }

    suspend fun playlistTracks(id: String): List<SpotifyTrack> {
        validatePlaylistId(id)
        val headers = authHeaders()
        val result = mutableListOf<SpotifyTrack>()
        var offset = 0
        repeat(MAX_PAGES) {
            val root = query("fetchPlaylist", PLAYLIST_HASH, playlistTrackVariables(id, offset), headers)
            val page = parseTrackPage(root, liked = false)
            result += page.items
            if (page.rawCount == 0 || offset + page.rawCount >= page.total) return result
            offset += page.rawCount
        }
        return result
    }

    suspend fun likedTracks(): List<SpotifyTrack> {
        val headers = authHeaders()
        val result = mutableListOf<SpotifyTrack>()
        var offset = 0
        repeat(MAX_PAGES) {
            val root = query("fetchLibraryTracks", LIKED_HASH, likedVariables(offset), headers)
            val page = parseTrackPage(root, liked = true)
            result += page.items
            if (page.rawCount == 0 || offset + page.rawCount >= page.total) return result
            offset += page.rawCount
        }
        return result
    }

    private suspend fun authHeaders(): Map<String, String> {
        val session = tokens.tokens()
        return mapOf(
            "Authorization" to "Bearer ${session.bearer}",
            "Client-Token" to session.clientToken,
            "app-platform" to "WebPlayer",
            "Origin" to SPOTIFY_ORIGIN,
            "Referer" to "$SPOTIFY_ORIGIN/",
        )
    }

    private suspend fun query(
        operation: String,
        hash: String,
        variables: JsonObject,
        headers: Map<String, String>,
    ): JsonObject {
        val body = buildJsonObject {
            put("operationName", operation)
            put("variables", variables)
            putJsonObject("extensions") {
                putJsonObject("persistedQuery") {
                    put("version", 1)
                    put("sha256Hash", hash)
                }
            }
        }
        val text = transport.post(GQL_URL, headers, body.toString())
        return try {
            val root = libraryJson.parseToJsonElement(text).jsonObject
            if (root.array("errors")?.isNotEmpty() == true) throw SpotifyError.ApiChanged
            root
        } catch (error: SpotifyError) {
            throw error
        } catch (_: Exception) {
            throw SpotifyError.InvalidResponse
        }
    }

    private fun playlistVariables(offset: Int) = buildJsonObject {
        putJsonArray("filters") { add("Playlists") }
        put("order", null as String?)
        put("textFilter", "")
        putJsonArray("features") {
            add("LIKED_SONGS")
            add("YOUR_EPISODES_V2")
            add("PRERELEASES")
            add("EVENTS")
        }
        put("limit", 50)
        put("offset", offset)
        put("flatten", true)
        putJsonArray("expandedFolders") {}
        put("folderUri", null as String?)
        put("includeFoldersWhenFlattening", false)
    }

    private fun playlistTrackVariables(id: String, offset: Int) = buildJsonObject {
        put("uri", "spotify:playlist:$id")
        put("offset", offset)
        put("limit", PAGE_SIZE)
        put("enableWatchFeedEntrypoint", false)
    }

    private fun likedVariables(offset: Int) = buildJsonObject {
        put("offset", offset)
        put("limit", PAGE_SIZE)
    }

    private fun validatePlaylistId(id: String) {
        if (id.length !in 1..128 || id.any { !(it.isLetterOrDigit() || it == '-' || it == '_') }) {
            throw SpotifyError.InvalidResponse
        }
    }

    private companion object {
        const val SPOTIFY_ORIGIN = "https://open.spotify.com"
        const val GQL_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
        // Spotify private persisted-query protocol constants; upstream may rotate hashes.
        const val LIBRARY_HASH = "973e511ca44261fda7eebac8b653155e7caee3675abb4fb110cc1b8c78b091c3"
        const val PLAYLIST_HASH = "346811f856fb0b7e4f6c59f8ebea78dd081c6e2fb01b77c954b26259d5fc6763"
        const val LIKED_HASH = "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240"
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 100
    }
}

fun parsePlaylists(json: String): List<SpotifyPlaylist> = try {
    val root = libraryJson.parseToJsonElement(json).jsonObject
    parsePlaylistPage(root).items
} catch (error: SpotifyError) {
    throw error
} catch (_: Exception) {
    throw SpotifyError.InvalidResponse
}

private data class Page<T>(val items: List<T>, val total: Int, val rawCount: Int)

private fun parsePlaylistPage(root: JsonObject): Page<SpotifyPlaylist> {
    val library = root.obj("data")?.obj("me")?.obj("libraryV3") ?: throw SpotifyError.ApiChanged
    val rawItems = library.array("items").orEmpty()
    val items = rawItems.mapNotNull { element ->
        val wrapper = element.obj("item") ?: return@mapNotNull null
        val data = wrapper.obj("data") ?: return@mapNotNull null
        if (data.string("__typename") != "Playlist") return@mapNotNull null
        val id = wrapper.string("_uri")?.takeIf { it.startsWith("spotify:playlist:") }
            ?.substringAfterLast(':')?.takeIf(::validId) ?: return@mapNotNull null
        val name = data.string("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        SpotifyPlaylist(
            id = id,
            name = name,
            owner = data.obj("ownerV2")?.obj("data")?.string("name"),
            imageUrl = largestImage(data.obj("images")),
            trackCount = null,
        )
    }
    return Page(items, library.int("totalCount") ?: rawItems.size, rawItems.size)
}

fun parsePlaylistTracks(json: String): List<SpotifyTrack> = parseTrackCollection(json, liked = false)

fun parseLikedTracks(json: String): List<SpotifyTrack> = try {
    val root = libraryJson.parseToJsonElement(json).jsonObject
    parseLikedPage(root).items
} catch (error: SpotifyError) {
    throw error
} catch (_: Exception) {
    throw SpotifyError.InvalidResponse
}

private fun parseLikedPage(root: JsonObject): Page<SpotifyTrack> {
    val tracks = root.obj("data")?.obj("me")?.obj("library")?.obj("tracks") ?: throw SpotifyError.ApiChanged
    val rawItems = tracks.array("items").orEmpty()
    val items = rawItems.mapNotNull { element ->
        val wrapper = element.obj("track") ?: return@mapNotNull null
        val data = wrapper.obj("data") ?: return@mapNotNull null
        parseTrack(data, wrapper.string("_uri") ?: data.string("uri"))
    }
    return Page(items, tracks.int("totalCount") ?: rawItems.size, rawItems.size)
}

private fun parseTrackCollection(json: String, liked: Boolean): List<SpotifyTrack> = try {
    val root = libraryJson.parseToJsonElement(json).jsonObject
    parseTrackPage(root, liked).items
} catch (error: SpotifyError) {
    throw error
} catch (_: Exception) {
    throw SpotifyError.InvalidResponse
}

private fun parseTrackPage(root: JsonObject, liked: Boolean): Page<SpotifyTrack> {
    val content = (if (liked) root.obj("data")?.obj("me")?.obj("library")?.obj("tracks")
    else root.obj("data")?.obj("playlistV2")?.obj("content")) ?: throw SpotifyError.ApiChanged
    val rawItems = content.array("items").orEmpty()
    val items = rawItems.mapNotNull { element ->
        if (liked) {
            val wrapper = element.obj("track") ?: return@mapNotNull null
            val data = wrapper.obj("data") ?: return@mapNotNull null
            return@mapNotNull parseTrack(data, wrapper.string("_uri") ?: data.string("uri"))
        }
        val data = element.obj("itemV2")?.obj("data") ?: return@mapNotNull null
        parseTrack(data, data.string("uri") ?: data.string("_uri"))
    }
    return Page(items, content.int("totalCount") ?: rawItems.size, rawItems.size)
}

private fun parseTrack(data: JsonObject, rawUri: String?): SpotifyTrack? {
    val id = rawUri?.takeIf { it.startsWith("spotify:track:") }
        ?.substringAfterLast(':')?.takeIf(::validId) ?: return null
    val title = data.string("name")?.takeIf { it.isNotBlank() } ?: return null
    val artists = data.obj("artists")?.array("items").orEmpty().mapNotNull { artist ->
        artist.obj("profile")?.string("name")?.takeIf { it.isNotBlank() }
    }
    val album = data.obj("albumOfTrack")
    return SpotifyTrack(
        id = id,
        title = title,
        artists = artists,
        album = album?.string("name"),
        durationMs = (data.obj("trackDuration") ?: data.obj("duration"))?.long("totalMilliseconds"),
        imageUrl = album?.obj("coverArt")?.array("sources")?.lastNotNullString("url"),
    )
}

private fun largestImage(images: JsonObject?): String? = images?.array("items")
    ?.firstOrNull()?.obj("sources")?.array("items")
    ?.mapNotNull { source -> source.string("url")?.let { it to (source.asObject()?.int("width") ?: 0) } }
    ?.maxByOrNull { it.second }?.first

private fun JsonArray.lastNotNullString(key: String): String? =
    mapNotNull { it.asObject()?.get(key).asString() }.lastOrNull()

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject
private fun JsonElement?.asArray(): JsonArray? = this as? JsonArray
private fun JsonElement?.asString(): String? = (this as? JsonPrimitive)?.contentOrNull
private fun JsonElement?.asLong(): Long? = (this as? JsonPrimitive)?.longOrNull
private fun JsonElement?.asInt(): Int? = (this as? JsonPrimitive)?.intOrNull
private fun JsonObject.obj(key: String): JsonObject? = this[key].asObject()
private fun JsonObject.array(key: String): JsonArray? = this[key].asArray()
private fun JsonObject.string(key: String): String? = this[key].asString()
private fun JsonObject.long(key: String): Long? = this[key].asLong()
private fun JsonObject.int(key: String): Int? = this[key].asInt()
private fun JsonElement.obj(key: String): JsonObject? = asObject()?.obj(key)
private fun JsonElement.array(key: String): JsonArray? = asObject()?.array(key)
private fun JsonElement.string(key: String): String? = asObject()?.string(key)

private fun validId(value: String): Boolean = value.length in 1..128 && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }
