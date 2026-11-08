package com.example.musicx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicx.spotify.match.MatchDecision
import com.example.musicx.spotify.match.TrackMatch
import com.example.musicx.spotify.model.SpotifyPlaylist
import com.example.musicx.spotify.model.SpotifyTrack
import com.example.musicx.core.model.CatalogItem

@Composable
fun SpotifyDisconnectedScreen(onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Spotify", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Connect your Spotify account to import playlists and Liked Songs. " +
                "Audio plays from YouTube Music — Spotify is used for metadata only.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onLogin, Modifier.fillMaxWidth()) { Text("Connect Spotify") }
        Text(
            "Your sp_dc cookie is stored encrypted on-device and never sent to any server except Spotify.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SpotifyExpiredScreen(onLogin: () -> Unit, onDisconnect: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Session expired", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Your Spotify session has expired. Sign in again to continue.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onLogin, Modifier.fillMaxWidth()) { Text("Reconnect") }
        OutlinedButton(onClick = onDisconnect, Modifier.fillMaxWidth()) { Text("Disconnect") }
    }
}

@Composable
fun SpotifyLibraryScreen(
    playlists: List<SpotifyPlaylist>,
    onPlaylistSelected: (SpotifyPlaylist) -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Spotify Library", style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = onDisconnect) { Text("Disconnect") }
        }
        if (playlists.isEmpty()) {
            Text("No playlists found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            playlists.forEach { playlist ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPlaylistSelected(playlist) }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        playlist.owner?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("→", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SpotifyTracksScreen(
    playlist: SpotifyPlaylist,
    tracks: List<SpotifyTrack>,
    onTrackSelected: (SpotifyTrack) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("← Back", Modifier.clickable(onClick = onBack), color = MaterialTheme.colorScheme.primary)
        Text(playlist.name, style = MaterialTheme.typography.headlineLarge)
        Text("${tracks.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tracks.isEmpty()) {
            Text("This playlist is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tracks.forEach { track ->
                Row(
                    Modifier.fillMaxWidth().clickable { onTrackSelected(track) }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artistLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("Match", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SpotifyMatchingScreen(track: SpotifyTrack, progress: String) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Matching", style = MaterialTheme.typography.headlineLarge)
        Text("${track.title} — ${track.artistLine}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        CircularProgressIndicator(Modifier.size(32.dp))
        Text(progress, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SpotifyMatchReviewScreen(
    track: SpotifyTrack,
    decision: MatchDecision,
    onPlay: (CatalogItem.Track) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("← Back", Modifier.clickable(onClick = onBack), color = MaterialTheme.colorScheme.primary)
        Text("Match results", style = MaterialTheme.typography.headlineLarge)
        Text("${track.title} — ${track.artistLine}", color = MaterialTheme.colorScheme.onSurfaceVariant)

        when (decision) {
            is MatchDecision.Matched -> {
                Text("✓ Confident match", color = MaterialTheme.colorScheme.primary)
                MatchCandidateRow(decision.best, onPlay)
            }
            is MatchDecision.Ambiguous -> {
                Text("⚠ Ambiguous — choose the correct match:", color = MaterialTheme.colorScheme.error)
                decision.candidates.forEach { match ->
                    MatchCandidateRow(match, onPlay)
                }
            }
            MatchDecision.NoMatch -> {
                Text("✗ No match found on YouTube Music.", color = MaterialTheme.colorScheme.error)
                Text("Try searching manually from the Search tab.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MatchCandidateRow(match: TrackMatch, onPlay: (CatalogItem.Track) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onPlay(match.candidate) }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(match.candidate.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(match.candidate.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Score: ${"%.0f".format(match.score * 100)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Play", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SpotifyErrorScreen(message: String, onRetry: () -> Unit, onDisconnect: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Spotify error", style = MaterialTheme.typography.headlineLarge)
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, Modifier.fillMaxWidth()) { Text("Retry") }
        OutlinedButton(onClick = onDisconnect, Modifier.fillMaxWidth()) { Text("Disconnect") }
    }
}

@Composable
fun SpotifyLoadingScreen() {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Loading Spotify…", style = MaterialTheme.typography.headlineLarge)
        CircularProgressIndicator(Modifier.size(32.dp))
    }
}
