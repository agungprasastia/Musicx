package com.example.musicx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem

enum class MusicTab { Home, Search, Library, Spotify }

private val browseShortcuts = listOf(
    "Charts" to "FEmusic_charts",
    "New releases" to "FEmusic_new_releases",
    "Moods & genres" to "FEmusic_moods_and_genres",
)

@Composable
fun HomeScreen(
    onBrowseSelected: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GOOD EVENING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Settings",
                Modifier.clickable(role = Role.Button, onClick = onOpenSettings).padding(8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text("Made for you", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Browse public YouTube Music collections. Guest access only; no account required.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        browseShortcuts.forEach { (label, id) ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable(role = Role.Button) { onBrowseSelected(id) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Browse", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            "YouTube Music integration is unofficial and may stop working when the service changes.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SearchScreen(
    state: CatalogUiState,
    onSearch: (String, CatalogFilter) -> Unit,
    onTrackSelected: (CatalogItem.Track) -> Unit,
    onBrowseSelected: (CatalogItem) -> Unit,
    onRetry: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(CatalogFilter.ALL) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Search music", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) onSearch("", filter)
            },
            modifier = Modifier.fillMaxWidth().testTag("search-input"),
            placeholder = { Text("Search songs, albums, artists") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { if (query.isNotBlank()) onSearch(query, filter) }
            )
        )
        Button(
            onClick = { onSearch(query, filter) },
            enabled = query.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search YouTube Music")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogFilter.entries.forEach { entry ->
                FilterChip(
                    selected = filter == entry,
                    onClick = {
                        filter = entry
                        if (query.isNotBlank()) onSearch(query, entry)
                    },
                    label = { Text(filterLabel(entry)) }
                )
            }
        }
        if (query.isBlank()) {
            Text("Find your next favorite", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CatalogStateContent(
                state = state,
                onRetry = onRetry,
                onItemSelected = { item ->
                    if (item is CatalogItem.Track) onTrackSelected(item) else onBrowseSelected(item)
                }
            )
        }
    }
}

@Composable
fun BrowseScreen(
    state: CatalogUiState,
    onBack: () -> Unit,
    onTrackSelected: (CatalogItem.Track) -> Unit,
    onBrowseSelected: (CatalogItem) -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("← Back", Modifier.clickable(onClick = onBack), color = MaterialTheme.colorScheme.primary)
        Text("Browse", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        CatalogStateContent(
            state = state,
            onRetry = onRetry,
            onItemSelected = { item ->
                if (item is CatalogItem.Track) onTrackSelected(item) else onBrowseSelected(item)
            },
            idleText = "Choose a collection to browse."
        )
    }
}

@Composable
fun LibraryScreen(
    tracks: List<CatalogItem.Track>,
    isLoading: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onTrackSelected: (CatalogItem.Track) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Your Library", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        when {
            !hasPermission -> {
                Text("Musicx needs permission to access your audio files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRequestPermission) { Text("Grant access") }
            }
            isLoading -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp))
                    Text("Scanning local audio…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            tracks.isEmpty() -> {
                Text("No audio files found on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                Text("${tracks.size} tracks", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                tracks.forEach { track ->
                    CatalogRow(track) { onTrackSelected(track) }
                }
            }
        }
    }
}

@Composable
private fun CatalogStateContent(
    state: CatalogUiState,
    onRetry: () -> Unit,
    onItemSelected: (CatalogItem) -> Unit,
    idleText: String = "",
) {
    when (state) {
        CatalogUiState.Idle -> if (idleText.isNotBlank()) {
            Text(idleText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        CatalogUiState.Loading -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(Modifier.size(22.dp))
            Text("Loading YouTube Music", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        is CatalogUiState.Empty -> Text(
            state.title?.let { "Nothing to show in $it" } ?: "No results found",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is CatalogUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) { Text("Retry") }
        }

        is CatalogUiState.Results -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.title?.let { SectionHeading(it) }
            state.items.forEach { item ->
                CatalogRow(item) { onItemSelected(item) }
            }
        }
    }
}

private fun filterLabel(filter: CatalogFilter): String = when (filter) {
    CatalogFilter.ALL -> "All"
    CatalogFilter.SONGS -> "Songs"
    CatalogFilter.ALBUMS -> "Albums"
    CatalogFilter.ARTISTS -> "Artists"
    CatalogFilter.PLAYLISTS -> "Playlists"
}
