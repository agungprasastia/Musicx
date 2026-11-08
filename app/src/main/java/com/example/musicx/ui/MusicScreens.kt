package com.example.musicx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicx.core.model.CatalogFilter
import com.example.musicx.core.model.CatalogItem
import com.example.musicx.data.MusicSamples
import kotlinx.coroutines.delay

enum class MusicTab { Home, Search, Library, Spotify }

private data class BrowseCollection(
    val title: String,
    val subtitle: String,
    val browseId: String,
    val gradient: List<Color>
)

private val homeCollections = listOf(
    BrowseCollection("Charts", "Top Hits", "FEmusic_charts", listOf(Color(0xFFDF8C64), Color(0xFF503448))),
    BrowseCollection("New releases", "Fresh Music", "FEmusic_new_releases", listOf(Color(0xFFA3AA7A), Color(0xFF354749))),
    BrowseCollection("Moods & genres", "Vibe Selector", "FEmusic_moods_and_genres", listOf(Color(0xFF4A6FA5), Color(0xFF16253D))),
    BrowseCollection("Trending", "Global Mix", "FEmusic_charts", listOf(Color(0xFF8E54E9), Color(0xFF4776E6))),
)

private val quickSampleTracks = MusicSamples.tracks.map { sample ->
    CatalogItem.Track(
        id = sample.id,
        title = sample.title,
        artist = sample.artist,
        album = sample.album,
        artworkUrl = null,
        durationMs = 240_000L
    )
}

@Composable
fun HomeScreen(
    onBrowseSelected: (String) -> Unit,
    onTrackSelected: (CatalogItem.Track) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Spotify Top Header: Greeting + Settings
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "GOOD EVENING",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Made for you",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onOpenSettings),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = MusicIcons.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Spotify 2x2 Quick Grid Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialGridCard(
                    title = homeCollections[0].title,
                    subtitle = homeCollections[0].subtitle,
                    gradientColors = homeCollections[0].gradient,
                    modifier = Modifier.weight(1f),
                    onClick = { onBrowseSelected(homeCollections[0].browseId) }
                )
                EditorialGridCard(
                    title = homeCollections[1].title,
                    subtitle = homeCollections[1].subtitle,
                    gradientColors = homeCollections[1].gradient,
                    modifier = Modifier.weight(1f),
                    onClick = { onBrowseSelected(homeCollections[1].browseId) }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialGridCard(
                    title = homeCollections[2].title,
                    subtitle = homeCollections[2].subtitle,
                    gradientColors = homeCollections[2].gradient,
                    modifier = Modifier.weight(1f),
                    onClick = { onBrowseSelected(homeCollections[2].browseId) }
                )
                EditorialGridCard(
                    title = homeCollections[3].title,
                    subtitle = homeCollections[3].subtitle,
                    gradientColors = homeCollections[3].gradient,
                    modifier = Modifier.weight(1f),
                    onClick = { onBrowseSelected(homeCollections[3].browseId) }
                )
            }
        }

        // Spotify "Quick Picks" / Instant Playable Tracks
        SectionHeading("Quick picks")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quickSampleTracks.forEach { track ->
                CatalogRow(track) { onTrackSelected(track) }
            }
        }

        SectionHeading("Made for your mood")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MoodRowCard(
                title = "After hours",
                subtitle = "Curated late night mix",
                gradientColors = listOf(Color(0xFF845EC2), Color(0xFFD65DB1)),
                onClick = { onBrowseSelected("FEmusic_moods_and_genres") }
            )
            MoodRowCard(
                title = "Deep focus",
                subtitle = "Instrumental soundscapes & lo-fi",
                gradientColors = listOf(Color(0xFF0089BA), Color(0xFF2C73D2)),
                onClick = { onBrowseSelected("FEmusic_moods_and_genres") }
            )
        }

        Text(
            "YouTube Music guest catalog access. No account required.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
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

    // Live Debounced Search: Stream results as user types!
    LaunchedEffect(query, filter) {
        if (query.isNotBlank()) {
            delay(350)
            onSearch(query.trim(), filter)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Spotify-style Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) onSearch("", filter)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search-input"),
            placeholder = { Text("Search songs, albums, artists") },
            leadingIcon = {
                Icon(
                    imageVector = MusicIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Text(
                        "✕",
                        Modifier
                            .clickable {
                                query = ""
                                onSearch("", filter)
                            }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { if (query.isNotBlank()) onSearch(query, filter) }
            )
        )

        // Spotify Category Filter Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogFilter.entries.forEach { entry ->
                FilterChip(
                    selected = filter == entry,
                    onClick = {
                        filter = entry
                        if (query.isNotBlank()) onSearch(query, entry)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    label = { Text(filterLabel(entry)) }
                )
            }
        }

        // Secondary Search Button (Kept for instant explicit submit / test compatibility)
        Button(
            onClick = { onSearch(query, filter) },
            enabled = query.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Search YouTube Music", fontWeight = FontWeight.SemiBold)
        }

        if (query.isBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Find your next favorite",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Type above to search millions of tracks, official artist pages, and community playlists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
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
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spotify Top Bar: Back Button
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onBack),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "← Back",
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Big Circular Play Button (Spotify-style) if tracks exist
            val firstTrack = (state as? CatalogUiState.Results)?.items?.filterIsInstance<CatalogItem.Track>()?.firstOrNull()
            if (firstTrack != null) {
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = { onTrackSelected(firstTrack) }),
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = MusicIcons.Play,
                            contentDescription = "Play all",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Text(
            "Browse",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

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
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            "Your Library",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        when {
            !hasPermission -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Local Audio Access",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Musicx needs permission to scan and play audio files stored on your device storage.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant access")
                        }
                    }
                }
            }
            isLoading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("Scanning local audio…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            tracks.isEmpty() -> {
                Text(
                    "No audio files found on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${tracks.size} tracks found",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tracks.forEach { track ->
                        CatalogRow(track) { onTrackSelected(track) }
                    }
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            CircularProgressIndicator(Modifier.size(24.dp))
            Text("Loading YouTube Music", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        is CatalogUiState.Empty -> Text(
            state.title?.let { "Nothing to show in $it" } ?: "No results found",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is CatalogUiState.Error -> Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry")
            }
        }

        is CatalogUiState.Results -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
