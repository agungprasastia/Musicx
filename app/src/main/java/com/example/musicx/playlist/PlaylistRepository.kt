package com.example.musicx.playlist

import android.content.Context
import com.example.musicx.core.model.CatalogItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID

data class UserPlaylist(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tracks: List<CatalogItem.Track> = emptyList(),
)

class PlaylistRepository(private val baseDir: File) {

    constructor(context: Context) : this(context.filesDir)

    private val storageFile: File
        get() = File(baseDir, "user_playlists.json")

    private val _likedTracks = MutableStateFlow<List<CatalogItem.Track>>(emptyList())
    val likedTracks: StateFlow<List<CatalogItem.Track>> = _likedTracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    init {
        loadFromDisk()
    }

    @Synchronized
    fun isLiked(trackId: String): Boolean {
        return _likedTracks.value.any { it.id == trackId }
    }

    @Synchronized
    fun toggleLike(track: CatalogItem.Track): Boolean {
        val current = _likedTracks.value.toMutableList()
        val index = current.indexOfFirst { it.id == track.id }
        val nowLiked: Boolean
        if (index >= 0) {
            current.removeAt(index)
            nowLiked = false
        } else {
            current.add(0, track)
            nowLiked = true
        }
        _likedTracks.value = current
        saveToDisk()
        return nowLiked
    }

    @Synchronized
    fun createPlaylist(name: String): UserPlaylist {
        val cleanName = name.trim().ifEmpty { "My Playlist #${_playlists.value.size + 1}" }
        val newPlaylist = UserPlaylist(
            id = "pl_${UUID.randomUUID()}",
            name = cleanName,
            createdAt = System.currentTimeMillis(),
            tracks = emptyList(),
        )
        _playlists.value = listOf(newPlaylist) + _playlists.value
        saveToDisk()
        return newPlaylist
    }

    @Synchronized
    fun importPlaylist(name: String, tracks: List<CatalogItem.Track>): UserPlaylist {
        val cleanName = name.trim().ifEmpty { "Imported Playlist" }
        val newPlaylist = UserPlaylist(
            id = "pl_${UUID.randomUUID()}",
            name = cleanName,
            createdAt = System.currentTimeMillis(),
            tracks = tracks,
        )
        _playlists.value = listOf(newPlaylist) + _playlists.value
        saveToDisk()
        return newPlaylist
    }

    @Synchronized
    fun deletePlaylist(playlistId: String) {
        _playlists.value = _playlists.value.filter { it.id != playlistId }
        saveToDisk()
    }

    @Synchronized
    fun addTrackToPlaylist(playlistId: String, track: CatalogItem.Track): Boolean {
        var added = false
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.tracks.none { it.id == track.id }) {
                    added = true
                    pl.copy(tracks = pl.tracks + track)
                } else pl
            } else pl
        }
        if (added) saveToDisk()
        return added
    }

    @Synchronized
    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(tracks = pl.tracks.filter { it.id != trackId })
            } else pl
        }
        saveToDisk()
    }

    @Synchronized
    private fun loadFromDisk() {
        if (!storageFile.exists()) return
        try {
            val jsonStr = storageFile.readText()
            val root = Json.parseToJsonElement(jsonStr).jsonObject

            val likedArr = root["liked"]?.jsonArray ?: JsonArray(emptyList())
            val loadedLiked = likedArr.mapNotNull { it.jsonObject.toTrack() }
            _likedTracks.value = loadedLiked

            val plArr = root["playlists"]?.jsonArray ?: JsonArray(emptyList())
            val loadedPls = plArr.mapNotNull { it.jsonObject.toPlaylist() }
            _playlists.value = loadedPls
        } catch (_: Exception) {
            // Keep default empty state if file corrupt
        }
    }

    @Synchronized
    private fun saveToDisk() {
        try {
            baseDir.mkdirs()
            val root = buildJsonObject {
                put("liked", buildJsonArray {
                    _likedTracks.value.forEach { add(it.toJson()) }
                })
                put("playlists", buildJsonArray {
                    _playlists.value.forEach { pl ->
                        add(buildJsonObject {
                            put("id", pl.id)
                            put("name", pl.name)
                            put("createdAt", pl.createdAt)
                            put("tracks", buildJsonArray {
                                pl.tracks.forEach { add(it.toJson()) }
                            })
                        })
                    }
                })
            }

            val tempFile = File(baseDir, "user_playlists.json.tmp")
            tempFile.writeText(root.toString())
            if (storageFile.exists()) storageFile.delete()
            tempFile.renameTo(storageFile)
        } catch (_: Exception) {
            // Best effort disk persistence
        }
    }

    private fun CatalogItem.Track.toJson(): JsonObject {
        return buildJsonObject {
            put("id", id)
            put("title", title)
            put("artist", artist)
            album?.let { put("album", it) }
            artworkUrl?.let { put("artworkUrl", it) }
            durationMs?.let { put("durationMs", it) }
        }
    }

    private fun JsonObject.toTrack(): CatalogItem.Track? {
        val id = this["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = this["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val artist = this["artist"]?.jsonPrimitive?.contentOrNull ?: "Unknown artist"
        val album = this["album"]?.jsonPrimitive?.contentOrNull
        val artworkUrl = this["artworkUrl"]?.jsonPrimitive?.contentOrNull
        val durationMs = this["durationMs"]?.jsonPrimitive?.longOrNull
        return CatalogItem.Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artworkUrl,
            durationMs = durationMs
        )
    }

    private fun JsonObject.toPlaylist(): UserPlaylist? {
        val id = this["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = this["name"]?.jsonPrimitive?.contentOrNull ?: "Playlist"
        val createdAt = this["createdAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        val tracks = this["tracks"]?.jsonArray?.mapNotNull { it.jsonObject.toTrack() } ?: emptyList()
        return UserPlaylist(id = id, name = name, createdAt = createdAt, tracks = tracks)
    }

    companion object {
        const val LIKED_PLAYLIST_ID = "liked_songs"
    }
}
