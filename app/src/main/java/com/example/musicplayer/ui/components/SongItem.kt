package com.example.musicplayer.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.model.SongSource
import com.example.musicplayer.ui.theme.*

@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean = false,
    isFavorite: Boolean = false,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit = {},
    onMoreClick: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPlaying) SurfaceLight else Color.Transparent
    val titleColor = if (isPlaying) NeonGreen else TextPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onSongClick(song) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Album Art
        AlbumArtThumbnail(
            artUri = song.albumArtUri,
            isPlaying = isPlaying,
            modifier = Modifier.size(52.dp)
        )

        // Song Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (song.source == SongSource.JAMENDO) {
                    Surface(
                        color = NeonGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "ONLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration
        Text(
            text = song.duration.toMinuteSecond(),
            style = MaterialTheme.typography.bodySmall,
            color = TextHint
        )

        // Favorite Button
        IconButton(
            onClick = { onFavoriteClick(song) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) AccentPink else TextHint,
                modifier = Modifier.size(18.dp)
            )
        }

        // More Options
        IconButton(
            onClick = { onMoreClick(song) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = TextHint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AlbumArtThumbnail(
    artUri: String?,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceLight),
        contentAlignment = Alignment.Center
    ) {
        if (artUri != null) {
            AsyncImage(
                model = artUri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(24.dp)
            )
        }

        // Playing indicator overlay
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonGreen.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
            PlayingWaveIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun PlayingWaveIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "wave$i")
            val height by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = 14f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.keyframes {
                        durationMillis = 800
                        4f at 0
                        14f at 400
                        4f at 800
                    },
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(i * 130)
                ),
                label = "waveHeight$i"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(NeonGreen, RoundedCornerShape(2.dp))
            )
        }
    }
}

fun Long.toMinuteSecond(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
