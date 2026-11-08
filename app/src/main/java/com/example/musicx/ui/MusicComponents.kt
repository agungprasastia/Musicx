package com.example.musicx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.data.MusicSample

@Composable
fun MusicArtwork(track: MusicSample, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(track.artworkColors.map(::Color)))
            .semantics { contentDescription = "Artwork: ${track.album}" }
    )
}

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun TrackRow(track: MusicSample, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(76.dp).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicArtwork(track, Modifier.height(60.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("${track.artist} · ${track.album}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("•••", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CatalogArtwork(item: CatalogItem, modifier: Modifier = Modifier) {
    val hue = (item.id.hashCode() and 0x7FFFFFFF) % 360
    val colors = listOf(
        Color.hsl(hue.toFloat(), 0.45f, 0.38f),
        Color.hsl(((hue + 48) % 360).toFloat(), 0.50f, 0.22f)
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(colors))
            .semantics { contentDescription = "Artwork: ${item.title}" }
    )
}

@Composable
fun CatalogRow(item: CatalogItem, onClick: () -> Unit) {
    val type = when (item) {
        is CatalogItem.Track -> "Song"
        is CatalogItem.Album -> "Album"
        is CatalogItem.Artist -> "Artist"
        is CatalogItem.Playlist -> "Playlist"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CatalogArtwork(item, Modifier.size(60.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(catalogSubtitle(item), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun catalogSubtitle(item: CatalogItem): String = when (item) {
    is CatalogItem.Track -> listOfNotNull(item.artist, item.album).joinToString(" · ")
    is CatalogItem.Album -> listOfNotNull(
        item.artist,
        item.trackCount?.let { "$it tracks" }
    ).joinToString(" · ").ifBlank { "Album" }
    is CatalogItem.Artist -> "Artist"
    is CatalogItem.Playlist -> item.trackCount?.let { "$it tracks" } ?: "Playlist"
}
