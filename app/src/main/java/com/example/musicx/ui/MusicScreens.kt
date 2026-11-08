package com.example.musicx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicx.data.MusicSample
import com.example.musicx.data.MusicSamples

enum class MusicTab { Home, Search, Library }

@Composable
fun HomeScreen(onTrackSelected: (MusicSample) -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("GOOD EVENING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Made for you", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(MusicSamples.playlists) { item ->
                Column(Modifier.size(220.dp)) {
                    MusicArtwork(item)
                    Text(item.title, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onBackground)
                    Text(item.album, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SectionHeading("Recently played")
        MusicSamples.tracks.forEach { TrackRow(it) { onTrackSelected(it) } }
        SectionHeading("Made for your mood")
        MusicSamples.albums.forEach { TrackRow(it) { onTrackSelected(it) } }
    }
}

@Composable
fun SearchScreen(onTrackSelected: (MusicSample) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Search music", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, albums, artists") },
            singleLine = true
        )
        if (query.isBlank()) {
            Text("Find your next favorite", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionHeading("Browse all")
            listOf("Playlists", "Albums", "Artists", "Songs").forEach { label ->
                Text(label, Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(18.dp), color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            MusicSamples.tracks.filter { "${it.title} ${it.artist} ${it.album}".contains(query, ignoreCase = true) }
                .forEach { TrackRow(it) { onTrackSelected(it) } }
            if (MusicSamples.tracks.none { "${it.title} ${it.artist} ${it.album}".contains(query, ignoreCase = true) }) {
                Text("No matches in this demo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LibraryScreen(onTrackSelected: (MusicSample) -> Unit) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Your Library", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        listOf("Playlists" to MusicSamples.playlists, "Albums" to MusicSamples.albums, "Artists" to MusicSamples.artists).forEach { (label, samples) ->
            SectionHeading(label)
            samples.forEach { TrackRow(it) { onTrackSelected(it) } }
        }
        SectionHeading("Songs")
        MusicSamples.tracks.forEach { TrackRow(it) { onTrackSelected(it) } }
    }
}
