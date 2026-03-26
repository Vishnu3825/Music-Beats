package com.example.musicplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.model.PlayerState
import com.example.musicplayer.data.model.RepeatMode
import com.example.musicplayer.ui.theme.*

@Composable
fun PlayerControls(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Seek Bar ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Slider(
                value = playerState.progress.coerceIn(0f, 1f),
                onValueChange = onSeek,
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = SurfaceLight
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playerState.currentPosition.toMinuteSecond(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
                Text(
                    text = playerState.duration.toMinuteSecond(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Transport Controls ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playerState.shuffleEnabled) NeonGreen else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Previous
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = TextPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Play / Pause (primary action)
            if (playerState.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = NeonGreen,
                    strokeWidth = 3.dp
                )
            } else {
                Surface(
                    onClick = onPlayPause,
                    color = NeonGreen,
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playerState.isPlaying)
                                Icons.Default.Pause
                            else
                                Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            // Next
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Repeat
            IconButton(onClick = onToggleRepeat) {
                Icon(
                    imageVector = when (playerState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = when (playerState.repeatMode) {
                        RepeatMode.OFF -> TextSecondary
                        else -> NeonGreen
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
