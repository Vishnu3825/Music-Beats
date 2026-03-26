package com.example.musicplayer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.local.PlaylistEntity
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.components.AlbumArtThumbnail
import com.example.musicplayer.ui.components.SongItem
import com.example.musicplayer.ui.theme.*

@Composable
fun LibraryScreen(
    favoriteSongIds: List<String>,
    localSongs: List<Song>,
    playlists: List<PlaylistEntity>,
    currentSongId: String?,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Favorites", "Playlists", "Albums")

    val favoriteSongs = remember(localSongs, favoriteSongIds) {
        localSongs.filter { favoriteSongIds.contains(it.id) }
    }
    val albums = remember(localSongs) {
        localSongs.groupBy { it.album }
            .map { (album, songs) ->
                AlbumGroup(album, songs.first().artist, songs.first().albumArtUri, songs)
            }
    }

    Column(modifier = modifier.fillMaxSize().background(Background)) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .background(SurfaceMid)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
        ) {
            Text(
                "Your Library",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceMid,
                contentColor = NeonGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonGreen,
                        height = 2.dp
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                tab,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) NeonGreen else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = Divider)

        // ── Tab Content ───────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> FavoritesTab(
                songs = favoriteSongs,
                currentSongId = currentSongId,
                favoriteIds = favoriteSongIds,
                onSongClick = { onSongClick(it, favoriteSongs) },
                onFavoriteClick = onFavoriteClick
            )
            1 -> PlaylistsTab(
                playlists = playlists,
                onCreatePlaylist = onCreatePlaylist
            )
            2 -> AlbumsTab(
                albums = albums,
                currentSongId = currentSongId,
                favoriteIds = favoriteSongIds,
                onSongClick = onSongClick,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}

// ─── Favorites Tab ────────────────────────────────────────────────────────────

@Composable
private fun FavoritesTab(
    songs: List<Song>,
    currentSongId: String?,
    favoriteIds: List<String>,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        EmptyTab(
            icon = Icons.Default.FavoriteBorder,
            title = "No Favorites Yet",
            subtitle = "Tap ♡ on any song to add it here"
        )
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
            item {
                Text(
                    "${songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
            items(songs, key = { it.id }) { song ->
                SongItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    isFavorite = favoriteIds.contains(song.id),
                    onSongClick = { onSongClick(it) },
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

// ─── Playlists Tab ────────────────────────────────────────────────────────────

@Composable
private fun PlaylistsTab(
    playlists: List<PlaylistEntity>,
    onCreatePlaylist: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column {
        // Create Playlist Button
        Surface(
            onClick = { showDialog = true },
            color = NeonGreen.copy(alpha = 0.10f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = NeonGreen, modifier = Modifier.size(22.dp))
                Text("New Playlist", style = MaterialTheme.typography.titleSmall, color = NeonGreen)
            }
        }

        if (playlists.isEmpty()) {
            EmptyTab(
                icon = Icons.Default.QueueMusic,
                title = "No Playlists",
                subtitle = "Create your first playlist to get started"
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistItem(playlist = playlist)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; newName = "" },
            title = { Text("New Playlist", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist name") },
                    placeholder = { Text("My awesome playlist", color = TextHint) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        focusedLabelColor = NeonGreen,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonGreen
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreatePlaylist(newName.trim())
                            newName = ""
                            showDialog = false
                        }
                    }
                ) { Text("Create", color = NeonGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; newName = "" }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceMid
        )
    }
}

@Composable
private fun PlaylistItem(playlist: PlaylistEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = SurfaceLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.QueueMusic, null, tint = NeonGreen, modifier = Modifier.size(26.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text("Playlist", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextHint)
    }
}

// ─── Albums Tab ───────────────────────────────────────────────────────────────

private data class AlbumGroup(
    val album: String,
    val artist: String,
    val artUri: String?,
    val songs: List<Song>
)

@Composable
private fun AlbumsTab(
    albums: List<AlbumGroup>,
    currentSongId: String?,
    favoriteIds: List<String>,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyTab(
            icon = Icons.Default.Album,
            title = "No Albums",
            subtitle = "Your local albums will appear here"
        )
        return
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
        items(albums, key = { it.album }) { group ->
            ExpandableAlbumItem(
                albumGroup = group,
                currentSongId = currentSongId,
                favoriteIds = favoriteIds,
                onSongClick = onSongClick,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}

@Composable
private fun ExpandableAlbumItem(
    albumGroup: AlbumGroup,
    currentSongId: String?,
    favoriteIds: List<String>,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Song) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AlbumArtThumbnail(
                artUri = albumGroup.artUri,
                modifier = Modifier.size(56.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    albumGroup.album,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    "${albumGroup.artist} • ${albumGroup.songs.size} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier.background(SurfaceDeep)
            ) {
                albumGroup.songs.forEach { song ->
                    SongItem(
                        song = song,
                        isPlaying = song.id == currentSongId,
                        isFavorite = favoriteIds.contains(song.id),
                        onSongClick = { onSongClick(it, albumGroup.songs) },
                        onFavoriteClick = onFavoriteClick,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            color = Divider.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ─── Shared Empty State ───────────────────────────────────────────────────────

@Composable
private fun EmptyTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(icon, null, tint = TextHint, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, color = TextSecondary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                color = TextHint,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
