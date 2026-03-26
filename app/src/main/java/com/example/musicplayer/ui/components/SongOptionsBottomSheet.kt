package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.model.SongSource
import com.example.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onViewAlbum: (Song) -> Unit,
    onSetAsRingtone: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceMid,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Song preview header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AlbumArtThumbnail(
                    artUri = song.albumArtUri,
                    modifier = Modifier.size(56.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = Divider,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Options list
            SongOptionItem(
                icon = Icons.Default.AddToQueue,
                label = "Add to Queue",
                onClick = {
                    onAddToQueue(song)
                    onDismiss()
                }
            )
            SongOptionItem(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (isFavorite) AccentPink else TextPrimary,
                onClick = {
                    onToggleFavorite(song)
                    onDismiss()
                }
            )
            SongOptionItem(
                icon = Icons.Default.PlaylistAdd,
                label = "Add to Playlist",
                onClick = {
                    onAddToPlaylist(song)
                    onDismiss()
                }
            )
            SongOptionItem(
                icon = Icons.Default.Album,
                label = "View Album",
                onClick = {
                    onViewAlbum(song)
                    onDismiss()
                }
            )
            if (song.source == SongSource.LOCAL) {
                SongOptionItem(
                    icon = Icons.Default.Notifications,
                    label = "Set as Ringtone",
                    onClick = {
                        onSetAsRingtone(song)
                        onDismiss()
                    }
                )
                SongOptionItem(
                    icon = Icons.Default.Delete,
                    label = "Delete from Device",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDeleteSong(song)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SongOptionItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
