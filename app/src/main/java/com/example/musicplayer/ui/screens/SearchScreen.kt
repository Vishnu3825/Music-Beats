package com.example.musicplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.model.SongSource
import com.example.musicplayer.ui.components.SongItem
import com.example.musicplayer.ui.theme.*

@Composable
fun SearchScreen(
    query: String,
    results: List<Song>,
    isSearching: Boolean,
    currentSongId: String?,
    favoriteIds: List<String>,
    onQueryChange: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .background(SurfaceMid)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
        ) {
            Text(
                "Search",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text("Songs, artists, albums...", color = TextHint)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = if (query.isNotEmpty()) NeonGreen else TextHint)
                },
                trailingIcon = {
                    AnimatedVisibility(visible = query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, null, tint = TextHint)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = Divider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonGreen
                ),
                singleLine = true
            )
        }

        // ── Results Area ──────────────────────────────────────────────────────
        AnimatedContent(
            targetState = when {
                isSearching         -> SearchState.LOADING
                query.isBlank()     -> SearchState.IDLE
                results.isEmpty()   -> SearchState.EMPTY
                else                -> SearchState.RESULTS
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "searchState",
            modifier = Modifier.weight(1f)
        ) { state ->
            when (state) {
                SearchState.IDLE -> SearchIdleView()
                SearchState.LOADING -> SearchLoadingView()
                SearchState.EMPTY -> SearchEmptyView(query)
                SearchState.RESULTS -> SearchResultsList(
                    results = results,
                    currentSongId = currentSongId,
                    favoriteIds = favoriteIds,
                    onSongClick = onSongClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}

private enum class SearchState { IDLE, LOADING, EMPTY, RESULTS }

@Composable
private fun SearchIdleView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.TravelExplore,
                null,
                tint = TextHint,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Discover Music",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Search your local library and stream\nnew music from Jamendo",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = NeonGreen,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(16.dp))
            Text("Searching...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SearchEmptyView(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = TextHint, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(14.dp))
            Text(
                "No results for",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            Text(
                "\"$query\"",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<Song>,
    currentSongId: String?,
    favoriteIds: List<String>,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit
) {
    val localResults = results.filter { it.source != com.example.musicplayer.data.model.SongSource.JAMENDO }
    val onlineResults = results.filter { it.source == com.example.musicplayer.data.model.SongSource.JAMENDO }

    LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
        if (localResults.isNotEmpty()) {
            item {
                ResultSectionLabel("On Device", localResults.size)
            }
            items(localResults, key = { it.id }) { song ->
                SongItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    isFavorite = favoriteIds.contains(song.id),
                    onSongClick = { onSongClick(it, buildSimilarSongQueue(it, localResults)) },
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        if (onlineResults.isNotEmpty()) {
            item {
                ResultSectionLabel("Online — Jamendo", onlineResults.size)
            }
            items(onlineResults, key = { it.id }) { song ->
                SongItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    isFavorite = favoriteIds.contains(song.id),
                    onSongClick = { onSongClick(it, buildSimilarSongQueue(it, onlineResults)) },
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private fun buildSimilarSongQueue(
    selected: Song,
    songs: List<Song>
): List<Song> {
    val selectedTokens = selected.title.lowercase()
        .split(Regex("\\W+"))
        .filter { it.length > 2 }
        .toSet()

    val ranked = songs
        .filter { it.id != selected.id }
        .sortedWith(
            compareByDescending<Song> { it.artist == selected.artist }
                .thenByDescending { it.album == selected.album }
                .thenByDescending { candidate ->
                    candidate.title.lowercase()
                        .split(Regex("\\W+"))
                        .count { it in selectedTokens }
                }
                .thenBy { it.title }
        )

    return listOf(selected) + ranked
}

@Composable
private fun ResultSectionLabel(label: String, count: Int) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Surface(
            color = NeonGreen.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                color = NeonGreen,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
