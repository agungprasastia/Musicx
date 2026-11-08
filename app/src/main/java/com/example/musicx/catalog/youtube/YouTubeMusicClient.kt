package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogError
import com.example.musicx.core.model.CatalogFilter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// Guest-only InnerTube web endpoint client. Unofficial endpoint; isolate for replacement.

sealed interface CatalogDto {
    val id: String
    val title: String
    val artworkUrl: String?

    data class Track(
        override val id: String,
        override val title: String,
        val artist: String,
        val album: String?,
        override val artworkUrl: String?,
        val durationMs: Long?,
    ) : CatalogDto

    data class Album(
        override val id: String,
        override val title: String,
        val artist: String?,
        override val artworkUrl: String?,
        val trackCount: Int?,
    ) : CatalogDto

    data class Artist(
        override val id: String,
        override val title: String,
        override val artworkUrl: String?,
    ) : CatalogDto

    data class Playlist(
        override val id: String,
        override val title: String,
        override val artworkUrl: String?,
        val trackCount: Int?,
    ) : CatalogDto
}

data class BrowseDto(val id: String, val title: String, val items: List<CatalogDto>)

interface YouTubeMusicClient {
    suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogDto>>
    suspend fun browse(id: String): Result<BrowseDto>
}

interface InnerTubeTransport {
    suspend fun post(endpoint: String, jsonBody: String): Result<String>
}

class InnerTubeYouTubeMusicClient(
    private val transport: InnerTubeTransport,
    private val parser: InnerTubeParser = InnerTubeParser(),
) : YouTubeMusicClient {
    override suspend fun search(query: String, filter: CatalogFilter): Result<List<CatalogDto>> {
        if (query.isBlank()) return Result.failure(CatalogError.EmptyQuery)
        return transport.post(ENDPOINT_SEARCH, searchBody(query.trim(), filter)).fold(
            onSuccess = { raw -> parseCatalog { parser.parseSearch(raw).filterBy(filter) } },
            onFailure = { Result.failure(it.asCatalogFailure()) },
        )
    }

    override suspend fun browse(id: String): Result<BrowseDto> {
        if (id.isBlank()) return Result.failure(CatalogError.EmptyBrowseId)
        val browseId = id.trim()
        return transport.post(ENDPOINT_BROWSE, browseBody(browseId)).fold(
            onSuccess = { raw -> parseCatalog { parser.parseBrowse(raw, browseId) } },
            onFailure = { Result.failure(it.asCatalogFailure()) },
        )
    }

    private fun searchBody(query: String, filter: CatalogFilter): String = buildJsonObject {
        putGuestContext()
        put("query", query)
        SEARCH_PARAMS[filter]?.let { put("params", it) }
    }.toString()

    private fun browseBody(browseId: String): String = buildJsonObject {
        putGuestContext()
        put("browseId", browseId)
    }.toString()

    private fun kotlinx.serialization.json.JsonObjectBuilder.putGuestContext() {
        putJsonObject("context") {
            putJsonObject("client") {
                put("clientName", "WEB_REMIX")
                put("clientVersion", CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
            }
            putJsonObject("user") {
                put("lockedSafetyMode", false)
            }
            putJsonObject("request") {
                put("useSsl", true)
            }
        }
    }

    private fun List<CatalogDto>.filterBy(filter: CatalogFilter): List<CatalogDto> = when (filter) {
        CatalogFilter.ALL -> this
        CatalogFilter.SONGS -> filterIsInstance<CatalogDto.Track>()
        CatalogFilter.ALBUMS -> filterIsInstance<CatalogDto.Album>()
        CatalogFilter.ARTISTS -> filterIsInstance<CatalogDto.Artist>()
        CatalogFilter.PLAYLISTS -> filterIsInstance<CatalogDto.Playlist>()
    }

    private inline fun <T> parseCatalog(parse: () -> T): Result<T> = try {
        Result.success(parse())
    } catch (error: CancellationException) {
        throw error
    } catch (error: CatalogError) {
        Result.failure(error)
    } catch (error: Exception) {
        Result.failure(CatalogError.InvalidResponse)
    }

    private fun Throwable.asCatalogFailure(): Throwable {
        if (this is CancellationException) throw this
        return this as? CatalogError ?: CatalogError.Network(this)
    }

    private companion object {
        const val ENDPOINT_SEARCH = "search"
        const val ENDPOINT_BROWSE = "browse"
        const val CLIENT_VERSION = "1.20250101.01.00"

        // Documented guest WEB_REMIX search-tab protocol values, not stream access data.
        val SEARCH_PARAMS = mapOf(
            CatalogFilter.SONGS to "EgWKAQIIAWoKEAkQChAFEAMQBA==",
            CatalogFilter.ALBUMS to "EgWKAQIYAWoKEAkQChAFEAMQBA==",
            CatalogFilter.ARTISTS to "EgWKAQIgAWoKEAkQChAFEAMQBA==",
            CatalogFilter.PLAYLISTS to "EgWKAQIoAWoKEAkQChAFEAMQBA==",
        )
    }
}
