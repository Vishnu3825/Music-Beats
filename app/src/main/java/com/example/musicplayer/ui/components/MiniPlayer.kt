package com.example.musicplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.data.model.PlayerState
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.theme.*

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onPlayerClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong ?: return

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onPlayerClick),
                color = SurfaceMid,
                shadowElevation = 16.dp,
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box {
                    // Progress background strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(playerState.progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .align(Alignment.BottomStart)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NeonGreen, ElectricBlue)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Rotating album art
                        RotatingAlbumArt(
                            artUri = song.albumArtUri,
                            isPlaying = playerState.isPlaying,
                            size = 46
                        )

                        // Track info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Buffering or play/pause
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = NeonGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = onPlayPause,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying)
                                        Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Skip next
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Skip next",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RotatingAlbumArt(
    artUri: String?,
    isPlaying: Boolean,
    size: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "albumRotation"
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .rotate(if (isPlaying) rotation else 0f)
            .clip(androidx.compose.foundation.shape.CircleShape)
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
                tint = NeonGreen,
                modifier = Modifier.size((size * 0.5f).dp)
            )
        }

        // Center dot for vinyl effect
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(SurfaceDeep)
        )
    }
}
