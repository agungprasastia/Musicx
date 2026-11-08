package com.example.musicx.catalog.youtube

import com.example.musicx.core.model.CatalogError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URI

/**
 * Parses guest InnerTube search/browse payloads into catalog DTOs.
 *
 * The renderer tree is walked depth-first instead of following one fixed path:
 * YouTube changes shelf containers regularly, while the row renderers are more
 * stable. Unsupported or malformed rows are dropped; an unreadable payload is a
 * typed [CatalogError.InvalidResponse].
 */
class InnerTubeParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseSearch(raw: String): List<CatalogDto> = parseItems(parseRoot(raw))

    fun parseBrowse(raw: String, browseId: String): BrowseDto {
        val root = parseRoot(raw)
        return BrowseDto(
            id = browseId,
            title = browseTitle(root) ?: "YouTube Music",
            items = parseItems(root),
        )
    }

    private fun parseRoot(raw: String): JsonObject {
        val element = try {
            json.parseToJsonElement(raw)
        } catch (error: Exception) {
            throw CatalogError.InvalidResponse
        }
        return element as? JsonObject ?: throw CatalogError.InvalidResponse
    }

    private fun parseItems(root: JsonObject): List<CatalogDto> {
        val seen = HashSet<String>()
        val items = mutableListOf<CatalogDto>()

        collect(root, "musicResponsiveListItemRenderer").forEach { renderer ->
            parseResponsiveRow(renderer)?.let { item ->
                if (seen.add("${item.javaClass.simpleName}:${item.id}")) items += item
            }
        }
        collect(root, "musicTwoRowItemRenderer").forEach { renderer ->
            parseTwoRowItem(renderer)?.let { item ->
                if (seen.add("${item.javaClass.simpleName}:${item.id}")) items += item
            }
        }
        return items
    }

    private fun parseResponsiveRow(renderer: JsonObject): CatalogDto? {
        val title = flexText(renderer, 0) ?: return null
        val subtitle = flexText(renderer, 1).orEmpty()
        val artwork = artworkUrl(renderer)
        val browse = renderer.obj("navigationEndpoint")?.obj("browseEndpoint")
        if (browse != null) {
            return browseItem(browse, title, subtitle, artwork)
        }
        val videoId = findWatchId(renderer) ?: return null
        if (!isSongRow(renderer)) return null
        return trackItem(videoId, title, subtitle, artwork)
    }

    private fun parseTwoRowItem(renderer: JsonObject): CatalogDto? {
        val title = renderer.obj("title")?.runsText() ?: return null
        val subtitle = renderer.obj("subtitle")?.runsText().orEmpty()
        val artwork = artworkUrl(renderer)
        val browse = renderer.obj("navigationEndpoint")?.obj("browseEndpoint")
        if (browse != null) {
            return browseItem(browse, title, subtitle, artwork)
        }
        val videoId = findWatchId(renderer) ?: return null
        if (!isSongRow(renderer)) return null
        return trackItem(videoId, title, subtitle, artwork)
    }

    private fun isSongRow(renderer: JsonObject): Boolean {
        val videoType = findMusicVideoType(renderer) ?: return true
        return videoType == SONG_VIDEO_TYPE
    }

    private fun browseItem(
        browse: JsonObject,
        title: String,
        subtitle: String,
        artwork: String?,
    ): CatalogDto? {
        val id = browse.text("browseId")?.takeIf { it.isNotBlank() } ?: return null
        val pageType = browse.obj("browseEndpointContextSupportedConfigs")
            ?.obj("browseEndpointContextMusicConfig")
            ?.text("pageType")
            .orEmpty()

        return when {
            pageType.contains("ALBUM", ignoreCase = true) || id.startsWith("MPRE") ->
                CatalogDto.Album(id, title, artistFrom(subtitle), artwork, trackCount(subtitle))
            pageType.contains("ARTIST", ignoreCase = true) || id.startsWith("UC") ->
                CatalogDto.Artist(id, title, artwork)
            pageType.contains("PLAYLIST", ignoreCase = true) || id.startsWith("VL") || id.startsWith("PL") ->
                CatalogDto.Playlist(id, title, artwork, trackCount(subtitle))
            else -> null
        }
    }

    private fun trackItem(
        id: String,
        title: String,
        subtitle: String,
        artwork: String?,
    ): CatalogDto.Track? {
        val metadata = subtitleTokens(subtitle).filterNot { it.lowercase() in TRACK_TYPE_LABELS }
        val durationIndex = metadata.indexOfLast { parseDurationMs(it) != null }
        val durationMs = metadata.getOrNull(durationIndex)?.let(::parseDurationMs)
        val remaining = metadata.filterIndexed { index, _ -> index != durationIndex }
            .filterNot { trackCount(it) != null || YEAR.matches(it) }
        val artist = remaining.firstOrNull() ?: return null
        val album = remaining.getOrNull(1)
        return CatalogDto.Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artwork,
            durationMs = durationMs,
        )
    }

    private fun artistFrom(subtitle: String): String? = subtitleTokens(subtitle)
        .filterNot {
            it.lowercase() in TYPE_LABELS ||
                parseDurationMs(it) != null ||
                trackCount(it) != null ||
                YEAR.matches(it)
        }
        .firstOrNull()

    private fun trackCount(subtitle: String): Int? {
        val match = TRACK_COUNT.find(subtitle) ?: return null
        return match.groupValues[1].replace(",", "").replace(".", "").toIntOrNull()
    }

    private fun parseDurationMs(value: String): Long? {
        val match = DURATION.matchEntire(value.trim()) ?: return null
        val hours = match.groupValues[1]
        val minutes = match.groupValues[2].toLongOrNull() ?: return null
        val seconds = match.groupValues[3].toLongOrNull() ?: return null
        if (seconds > 59) return null
        if (hours.isEmpty()) {
            return (minutes * 60 + seconds) * 1_000
        }
        val hourValue = hours.toLongOrNull() ?: return null
        if (minutes > 59) return null
        return (hourValue * 3_600 + minutes * 60 + seconds) * 1_000
    }

    private fun browseTitle(root: JsonObject): String? {
        HEADER_RENDERERS.forEach { name ->
            collect(root, name).firstNotNullOfOrNull { it.obj("title")?.runsText() }?.let { return it }
        }
        return root.obj("contents")
            ?.obj("singleColumnBrowseResultsRenderer")
            ?.arr("tabs")
            ?.firstOrNull()
            ?.obj("tabRenderer")
            ?.obj("title")
            ?.runsText()
    }

    private fun flexText(renderer: JsonObject, index: Int): String? =
        renderer.arr("flexColumns")
            ?.getOrNull(index)
            ?.obj("musicResponsiveListItemFlexColumnRenderer")
            ?.obj("text")
            ?.runsText()

    private fun artworkUrl(renderer: JsonObject): String? {
        val thumbnails = renderer.obj("thumbnail")
            ?.obj("musicThumbnailRenderer")
            ?.obj("thumbnail")
            ?.arr("thumbnails")
            ?: findFirstArray(renderer, "thumbnails")
        val best = thumbnails
            ?.mapNotNull { it as? JsonObject }
            ?.maxByOrNull { it.text("width")?.toIntOrNull() ?: 0 }
        return sanitizeArtworkUrl(best?.text("url"))
    }

    private fun sanitizeArtworkUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = URI(url)
            if (uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) url else null
        } catch (error: Exception) {
            null
        }
    }

    private fun findWatchId(element: JsonElement): String? = when (element) {
        is JsonObject -> element.obj("watchEndpoint")?.text("videoId")?.takeIf { it.isNotBlank() }
            ?: element.values.firstNotNullOfOrNull(::findWatchId)
        is JsonArray -> element.firstNotNullOfOrNull(::findWatchId)
        else -> null
    }

    private fun findMusicVideoType(element: JsonElement): String? = when (element) {
        is JsonObject -> element.text("musicVideoType")?.takeIf { it.isNotBlank() }
            ?: element.values.firstNotNullOfOrNull(::findMusicVideoType)
        is JsonArray -> element.firstNotNullOfOrNull(::findMusicVideoType)
        else -> null
    }

    private fun findFirstArray(element: JsonElement, name: String): JsonArray? = when (element) {
        is JsonObject -> (element[name] as? JsonArray) ?: element.values.firstNotNullOfOrNull { findFirstArray(it, name) }
        is JsonArray -> element.firstNotNullOfOrNull { findFirstArray(it, name) }
        else -> null
    }

    private fun collect(root: JsonElement, name: String): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    (node[name] as? JsonObject)?.let(result::add)
                    node.values.forEach(::walk)
                }
                is JsonArray -> node.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return result
    }

    private fun subtitleTokens(subtitle: String): List<String> =
        subtitle.split('•').map(String::trim).filter(String::isNotBlank)

    private fun JsonElement?.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject

    private fun JsonElement?.arr(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray

    private fun JsonElement?.text(key: String): String? = (this as? JsonObject)?.get(key).text()

    private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.runsText(): String? = arr("runs")
        ?.joinToString(separator = "") { run -> (run as? JsonObject)?.text("text").orEmpty() }
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private companion object {
        val DURATION = Regex("""^(?:(\d{1,2}):)?(\d{1,3}):(\d{2})$""")
        val YEAR = Regex("""^\d{4}$""")
        val TRACK_COUNT = Regex("""(\d[\d,.]*)\s+(?:songs?|tracks?)""", RegexOption.IGNORE_CASE)
        val HEADER_RENDERERS = listOf(
            "musicDetailHeaderRenderer",
            "musicImmersiveHeaderRenderer",
            "musicVisualHeaderRenderer",
        )
        val TRACK_TYPE_LABELS = setOf("song", "video")
        const val SONG_VIDEO_TYPE = "MUSIC_VIDEO_TYPE_ATV"
        val TYPE_LABELS = TRACK_TYPE_LABELS + setOf(
            "album",
            "single",
            "ep",
            "artist",
            "playlist",
            "public",
        )
    }
}
