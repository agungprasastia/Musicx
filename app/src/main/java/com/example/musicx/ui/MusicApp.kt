package com.example.musicx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.musicx.data.MusicSample
import com.example.musicx.data.MusicSamples

@Composable
fun MusicxApp() {
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.Home) }
    var selectedTrackId by rememberSaveable { mutableStateOf(MusicSamples.tracks.first().id) }
    var isNowPlaying by rememberSaveable { mutableStateOf(false) }
    var isDemoPlaying by rememberSaveable { mutableStateOf(false) }
    val selectedTrack = MusicSamples.tracks.first { it.id == selectedTrackId }
    val onTrackSelected: (MusicSample) -> Unit = {
        selectedTrackId = it.id
        isNowPlaying = true
    }

    if (isNowPlaying) {
        NowPlayingScreen(
            track = selectedTrack,
            isDemoPlaying = isDemoPlaying,
            onPlayPause = { isDemoPlaying = !isDemoPlaying },
            onBack = { isNowPlaying = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    Row(
                        Modifier.fillMaxWidth().height(62.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(role = Role.Button) { isNowPlaying = true }
                            .semantics { contentDescription = "Open Now Playing: ${selectedTrack.title}" }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MusicArtwork(selectedTrack, Modifier.height(44.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selectedTrack.title, color = MaterialTheme.colorScheme.onSurface)
                            Text("Demo · ${selectedTrack.artist}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (isDemoPlaying) "Ⅱ" else "▶",
                            Modifier.clickable(role = Role.Button) { isDemoPlaying = !isDemoPlaying }.padding(horizontal = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        MusicTab.entries.forEach { tab ->
                            Column(
                                Modifier.weight(1f).clickable(role = Role.Tab) { selectedTab = tab }.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(tab.name, color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                when (selectedTab) {
                    MusicTab.Home -> HomeScreen(onTrackSelected)
                    MusicTab.Search -> SearchScreen(onTrackSelected)
                    MusicTab.Library -> LibraryScreen(onTrackSelected)
                }
            }
        }
    }
}
