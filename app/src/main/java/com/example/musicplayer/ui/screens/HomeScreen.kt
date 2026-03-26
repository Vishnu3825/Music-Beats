package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.local.RecentlyPlayedEntity
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.ui.theme.*

@Composable
fun HomeScreen(
    localSongs: List<Song>,
    featuredSongs: List<Song>,
    recentlyPlayed: List<RecentlyPlayedEntity>,
    currentSongId: String?,
    favoriteIds: List<String>,
    isLoadingLocal: Boolean,
    isLoadingFeatured: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit = {},
    onSetAsRingtone: (Song) -> Unit = {},
    onDeleteSong: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // ── Greeting Header ───────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SurfaceMid, Background)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp,
                        top = 24.dp, bottom = 20.dp
                    )
                ) {
                    Text(
                        text = "Good vibes,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = "Music Lover 🎵",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // ── Recently Played ───────────────────────────────────────────────────
        if (recentlyPlayed.isNotEmpty()) {
            item { SectionHeader("Recently Played") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentlyPlayed.take(10)) { recent ->
                        RecentCard(recent = recent)
                    }
                }
            }
        }

        // ── Featured Online ───────────────────────────────────────────────────
        if (isLoadingFeatured || featuredSongs.isNotEmpty()) {
            item { SectionHeader("Featured Online") }

            if (isLoadingFeatured) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            } else {
                // Horizontal scroll of featured cards
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(featuredSongs.take(10), key = { it.id }) { song ->
                            FeaturedSongCard(
                                title = song.title,
                                artist = song.artist,
                                artUri = song.albumArtUri,
                                onClick = { onSongClick(song, featuredSongs) }
                            )
                        }
                    }
                }
            }
        }

        // ── Your Library ──────────────────────────────────────────────────────
        item { SectionHeader("Your Library") }

        when {
            isLoadingLocal -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonGreen)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading your music...",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            localSongs.isEmpty() -> item {
                EmptyLibraryPlaceholder()
            }

            else -> {
                itemsIndexed(localSongs, key = { _, s -> s.id }) { _, song ->
                    SongItem(
                        song = song,
                        isPlaying = song.id == currentSongId,
                        isFavorite = favoriteIds.contains(song.id),
                        onSongClick = { onSongClick(it, localSongs) },
                        onFavoriteClick = onFavoriteClick,
                        onMoreClick = { selectedSong = it },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    selectedSong?.let { song ->
        SongOptionsBottomSheet(
            song = song,
            isFavorite = favoriteIds.contains(song.id),
            onDismiss = { selectedSong = null },
            onAddToQueue = {
                onAddToQueue(it)
                selectedSong = null
            },
            onToggleFavorite = {
                onFavoriteClick(it)
                selectedSong = null
            },
            onAddToPlaylist = {
                selectedSong = null
            },
            onViewAlbum = {
                selectedSong = null
            },
            onSetAsRingtone = {
                onSetAsRingtone(it)
                selectedSong = null
            },
            onDeleteSong = {
                onDeleteSong(it)
                selectedSong = null
            }
        )
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        modifier = Modifier.padding(
            start = 20.dp, end = 20.dp,
            top = 20.dp, bottom = 6.dp
        )
    )
}

@Composable
fun RecentCard(recent: RecentlyPlayedEntity) {
    Surface(
        color = SurfaceCard,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(148.dp)
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AlbumArtThumbnail(
                artUri = recent.albumArtUri,
                modifier = Modifier.size(40.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recent.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    recent.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = TextHint,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "No local music found",
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Add .mp3 / .flac / .wav files to your\ndevice storage to see them here.",
            color = TextHint,
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
