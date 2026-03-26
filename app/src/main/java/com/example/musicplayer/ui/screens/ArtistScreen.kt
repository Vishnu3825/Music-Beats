package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.components.SongItem
import com.example.musicplayer.ui.theme.*

data class ArtistAlbum(val name: String, val artUri: String?, val songs: List<Song>)

@Composable
fun ArtistScreen(
    artistName   : String,
    topSongs     : List<Song>,
    albums       : List<ArtistAlbum>,
    artUri       : String?,
    currentSongId: String?,
    favoriteIds  : List<String>,
    onBack       : () -> Unit,
    onSongClick  : (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onAlbumClick : (ArtistAlbum) -> Unit,
    modifier     : Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ── Artist header ─────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceMid)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(SurfaceLight)
                ) {
                    if (artUri != null) {
                        AsyncImage(
                            model = artUri,
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person, null, tint = TextHint,
                            modifier = Modifier.size(60.dp).align(Alignment.Center)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    artistName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    "${albums.size} albums  •  ${topSongs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = Divider)
        }

        // ── Top songs ─────────────────────────────────────────────────────────
        if (topSongs.isNotEmpty()) {
            item {
                Text(
                    "Popular Songs",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(topSongs.take(5), key = { it.id }) { song ->
                SongItem(
                    song          = song,
                    isPlaying     = song.id == currentSongId,
                    isFavorite    = favoriteIds.contains(song.id),
                    onSongClick   = { onSongClick(it, topSongs) },
                    onFavoriteClick = onFavoriteClick,
                    modifier      = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // ── Albums ────────────────────────────────────────────────────────────
        if (albums.isNotEmpty()) {
            item {
                Text(
                    "Albums",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
                )
            }
            items(albums, key = { it.name }) { album ->
                ArtistAlbumRow(album = album, onClick = { onAlbumClick(album) })
            }
        }
    }
}

@Composable
private fun ArtistAlbumRow(album: ArtistAlbum, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color   = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceLight)
            ) {
                if (album.artUri != null) {
                    AsyncImage(
                        model = album.artUri,
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Album, null, tint = TextHint,
                        modifier = Modifier.size(28.dp).align(Alignment.Center)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    album.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${album.songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextHint)
        }
    }
    HorizontalDivider(
        color    = Divider.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
