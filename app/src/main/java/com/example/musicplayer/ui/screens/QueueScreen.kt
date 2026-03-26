package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.components.AlbumArtThumbnail
import com.example.musicplayer.ui.components.toMinuteSecond
import com.example.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queue             : List<Song>,
    currentIndex      : Int,
    onSongClick       : (Int) -> Unit,
    onRemoveFromQueue : (Int) -> Unit,
    onClose           : () -> Unit,
    modifier          : Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current track when screen opens
    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        "Up Next",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (queue.isNotEmpty()) {
                        Text(
                            "${queue.size} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close queue",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            actions = {
                if (queue.isNotEmpty()) {
                    TextButton(onClick = { /* clear all except current */ }) {
                        Text("Clear", color = AccentPink, style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
        )

        HorizontalDivider(color = Divider)

        if (queue.isEmpty()) {
            // ── Empty state ───────────────────────────────────────────────────
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QueueMusic,
                        null,
                        tint = TextHint,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Queue is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Play a song or album to fill it up",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                }
            }
        } else {
            // ── Currently playing header ───────────────────────────────────────
            if (currentIndex in queue.indices) {
                val currentSong = queue[currentIndex]
                Surface(
                    color = NeonGreen.copy(alpha = 0.07f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Equalizer,
                            null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Now Playing • ${currentSong.title}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Queue list ────────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                itemsIndexed(queue, key = { idx, _ -> idx }) { index, song ->
                    val isCurrent = index == currentIndex
                    QueueSongRow(
                        song        = song,
                        index       = index,
                        isCurrent   = isCurrent,
                        onClick     = { onSongClick(index) },
                        onRemove    = { onRemoveFromQueue(index) }
                    )
                }
            }
        }
    }
}

// ─── Queue row ────────────────────────────────────────────────────────────────

@Composable
private fun QueueSongRow(
    song      : Song,
    index     : Int,
    isCurrent : Boolean,
    onClick   : () -> Unit,
    onRemove  : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) SurfaceLight else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Position / playing indicator
        Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                Icon(Icons.Default.Equalizer, null, tint = NeonGreen,
                    modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text  = "${index + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }
        }

        // Album art
        AlbumArtThumbnail(
            artUri    = song.albumArtUri,
            isPlaying = isCurrent,
            modifier  = Modifier.size(46.dp)
        )

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) NeonGreen else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text  = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text  = song.duration.toMinuteSecond(),
            style = MaterialTheme.typography.bodySmall,
            color = TextHint
        )

        // Remove button (hidden for currently playing)
        if (!isCurrent) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove",
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Drag handle (visual)
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Reorder",
            tint = TextHint,
            modifier = Modifier.size(18.dp)
        )
    }

    HorizontalDivider(
        color    = Divider.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
