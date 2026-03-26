package com.example.musicplayer.ui.screens.youtube

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.youtube.model.YouTubeVideo
import com.example.musicplayer.ui.theme.*

@Composable
fun YouTubeSearchScreen(
    query          : String,
    results        : List<YouTubeVideo>,
    isSearching    : Boolean,
    favoriteIds    : Set<String>,
    onQueryChange  : (String) -> Unit,
    onVideoClick   : (YouTubeVideo, List<YouTubeVideo>) -> Unit,
    onFavoriteClick: (YouTubeVideo) -> Unit,
    onLoadMore     : () -> Unit,
    modifier       : Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Trigger load-more when near the end
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo    = listState.layoutInfo
            val lastVisible   = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total         = layoutInfo.totalItemsCount
            lastVisible >= total - 4 && total > 0
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) onLoadMore()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Search bar header ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .background(SurfaceMid)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)
        ) {
            Text(
                "Search Music",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value           = query,
                onValueChange   = onQueryChange,
                placeholder     = { Text("Bollywood, Telugu, Artists…", color = TextHint) },
                leadingIcon     = {
                    Icon(
                        Icons.Default.Search, null,
                        tint = if (query.isNotEmpty()) NeonGreen else TextHint
                    )
                },
                trailingIcon    = {
                    AnimatedVisibility(visible = query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, null, tint = TextHint)
                        }
                    }
                },
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor      = NeonGreen,
                    unfocusedBorderColor    = Divider,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = NeonGreen
                ),
                singleLine = true
            )

            // Quick suggestion chips
            if (query.isBlank()) {
                Spacer(Modifier.height(10.dp))
                QuickSearchSuggestions(onQueryChange)
            }
        }

        // ── Results ───────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = when {
                isSearching && results.isEmpty() -> YouTubeSearchState.LOADING
                query.isBlank()                  -> YouTubeSearchState.IDLE
                results.isEmpty()                -> YouTubeSearchState.EMPTY
                else                             -> YouTubeSearchState.RESULTS
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label          = "ytSearch",
            modifier       = Modifier.weight(1f)
        ) { state ->
            when (state) {
                YouTubeSearchState.IDLE    -> SearchIdleHint()
                YouTubeSearchState.LOADING -> SearchLoading()
                YouTubeSearchState.EMPTY   -> SearchEmpty(query)
                YouTubeSearchState.RESULTS -> LazyColumn(
                    state          = listState,
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Text(
                            "About ${results.size}+ results",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = TextHint,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(results, key = { it.videoId }) { video ->
                        YouTubeVideoRow(
                            video      = video,
                            isFavorite = favoriteIds.contains(video.videoId),
                            onClick    = { onVideoClick(video, buildSimilarQueue(video, results)) },
                            onFavorite = { onFavoriteClick(video) }
                        )
                    }
                    if (isSearching) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildSimilarQueue(
    selected: YouTubeVideo,
    results: List<YouTubeVideo>
): List<YouTubeVideo> {
    if (results.isEmpty()) return listOf(selected)

    val selectedTokens = selected.title.lowercase()
        .split(Regex("\\W+"))
        .filter { it.length > 2 }
        .toSet()

    val others = results.filter { it.videoId != selected.videoId }
    val ranked = others.sortedWith(
        compareByDescending<YouTubeVideo> { it.channelTitle == selected.channelTitle }
            .thenByDescending { candidate ->
                candidate.title.lowercase()
                    .split(Regex("\\W+"))
                    .count { it in selectedTokens }
            }
            .thenBy { it.title }
    )

    return listOf(selected) + ranked
}

private enum class YouTubeSearchState { IDLE, LOADING, EMPTY, RESULTS }

@Composable
private fun QuickSearchSuggestions(onQuery: (String) -> Unit) {
    val suggestions = listOf(
        "Arijit Singh", "AR Rahman", "Allu Arjun", "Trending 2024",
        "Lofi Study", "Bhajans", "Workout Music", "90s Hindi"
    )
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionChip(
                onClick = { onQuery(suggestion) },
                label   = { Text(suggestion, style = MaterialTheme.typography.labelMedium, color = TextSecondary) },
                icon    = {
                    Icon(Icons.Default.Search, null, tint = TextHint, modifier = Modifier.size(14.dp))
                },
                colors  = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = SurfaceLight
                ),
                border  = SuggestionChipDefaults.suggestionChipBorder(
                    enabled     = true,
                    borderColor = Divider
                )
            )
        }
    }
}

@Composable
private fun SearchIdleHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.MusicNote, null, tint = TextHint, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Search millions of songs", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Text(
                "Bollywood, Telugu, Punjabi,\nDevotional, Lo-Fi and more",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SearchLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NeonGreen)
            Spacer(Modifier.height(16.dp))
            Text("Searching YouTube…", color = TextSecondary)
        }
    }
}

@Composable
private fun SearchEmpty(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.SearchOff, null, tint = TextHint, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(14.dp))
            Text("No results for", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            Text("\"$query\"", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
    }
}
