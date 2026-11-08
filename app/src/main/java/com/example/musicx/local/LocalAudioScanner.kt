package com.example.musicx.local

import android.content.Context
import android.provider.MediaStore
import com.example.musicx.core.model.CatalogItem

/**
 * Scans MediaStore for music files. Caller must hold READ_MEDIA_AUDIO
 * (API 33+) or READ_EXTERNAL_STORAGE (API 32 and below).
 */
object LocalAudioScanner {

    const val MEDIA_URI_PREFIX = "content://media/external/audio/media/"
    private const val ALBUM_ART_URI_PREFIX = "content://media/external/audio/albumart/"

    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
    )

    fun scan(context: Context): List<CatalogItem.Track> {
        val tracks = mutableListOf<CatalogItem.Track>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                mapRow(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol),
                    artist = cursor.getString(artistCol),
                    album = cursor.getString(albumCol),
                    duration = cursor.getLong(durationCol),
                    albumId = cursor.getLong(albumIdCol),
                )?.let(tracks::add)
            }
        }
        return tracks
    }

    fun mapRow(
        id: Long,
        title: String?,
        artist: String?,
        album: String?,
        duration: Long,
        albumId: Long = 0L,
    ): CatalogItem.Track? {
        if (id <= 0 || title.isNullOrBlank()) return null
        return CatalogItem.Track(
            id = "$MEDIA_URI_PREFIX$id",
            title = title,
            artist = artist?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING } ?: "Unknown artist",
            album = album?.takeIf { it.isNotBlank() },
            artworkUrl = if (albumId > 0) "$ALBUM_ART_URI_PREFIX$albumId" else null,
            durationMs = duration.takeIf { it > 0 },
        )
    }

    fun isLocal(trackId: String): Boolean = trackId.startsWith(MEDIA_URI_PREFIX)
}
