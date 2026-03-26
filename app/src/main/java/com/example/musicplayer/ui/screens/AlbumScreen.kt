package com.example.musicplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.components.SongItem
import com.example.musicplayer.ui.components.toMinuteSecond
import com.example.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumName    : String,
    artistName   : String,
    songs        : List<Song>,
    currentSongId: String?,
    favoriteIds  : List<String>,
    onBack       : () -> Unit,
    onSongClick  : (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onPlayAll    : () -> Unit,
    onShuffleAll : () -> Unit,
    modifier     : Modifier = Modifier
) {
    val albumArtUri = songs.firstOrNull()?.albumArtUri
    val totalDuration = songs.sumOf { it.duration }
    val totalMinutes  = totalDuration / 60_000

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        // ── Blurred header art ────────────────────────────────────────────────
        if (albumArtUri != null) {
            AsyncImage(
                model = albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .blur(60.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f   to Background.copy(alpha = 0.3f),
                            0.6f to Background.copy(alpha = 0.85f),
                            1f   to Background
                        )
                    )
                )
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Hero header ───────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 56.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album art
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceLight)
                    ) {
                        if (albumArtUri != null) {
                            AsyncImage(
                                model = albumArtUri,
                                contentDescription = "Album cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.Album, null, tint = TextHint,
                                modifier = Modifier.size(72.dp).align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        albumName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        artistName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${songs.size} songs  •  ${totalMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )

                    Spacer(Modifier.height(20.dp))

                    // Play / Shuffle row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPlayAll,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor   = androidx.compose.ui.graphics.Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play All", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = onShuffleAll,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                HorizontalDivider(color = Divider)
            }

            // ── Track list ────────────────────────────────────────────────────
            itemsIndexed(songs, key = { _, s -> s.id }) { _, song ->
                SongItem(
                    song          = song,
                    isPlaying     = song.id == currentSongId,
                    isFavorite    = favoriteIds.contains(song.id),
                    onSongClick   = { onSongClick(it, songs) },
                    onFavoriteClick = onFavoriteClick,
                    modifier      = Modifier.padding(horizontal = 8.dp)
                )
            }

            item { Spacer(Modifier.height(140.dp)) }
        }

        // ── Back button floating ──────────────────────────────────────────────
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .background(
                    SurfaceMid.copy(alpha = 0.7f),
                    RoundedCornerShape(50)
                )
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
        }
    }
}
