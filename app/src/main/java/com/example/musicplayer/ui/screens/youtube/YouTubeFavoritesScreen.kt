package com.example.musicplayer.ui.screens.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.youtube.model.YouTubeVideo
import com.example.musicplayer.ui.theme.*

@Composable
fun YouTubeFavoritesScreen(
    videos         : List<YouTubeVideo>,
    onVideoClick   : (YouTubeVideo) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    modifier       : Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Background)) {
        // Header
        Column(
            modifier = Modifier
                .background(SurfaceMid)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
        ) {
            Text("Saved Videos", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            if (videos.isNotEmpty()) {
                Text(
                    "${videos.size} videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        HorizontalDivider(color = Divider)

        if (videos.isEmpty()) {
            // Empty state
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = TextHint, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("No Saved Videos", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap ♡ on any video while browsing\nto save it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // 2-column grid
            LazyVerticalGrid(
                columns        = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                modifier       = Modifier.fillMaxSize()
            ) {
                items(videos, key = { it.videoId }) { video ->
                    FavoriteVideoCard(
                        video    = video,
                        onClick  = { onVideoClick(video) },
                        onRemove = { onRemoveFavorite(video.videoId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteVideoCard(
    video   : YouTubeVideo,
    onClick : () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(SurfaceLight)
            ) {
                AsyncImage(
                    model            = video.thumbnailHigh,
                    contentDescription = video.title,
                    contentScale     = ContentScale.Crop,
                    modifier         = Modifier.fillMaxSize()
                )
                Icon(
                    Icons.Default.PlayCircle,
                    null,
                    tint     = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp).align(Alignment.Center)
                )
                // Remove button
                IconButton(
                    onClick  = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        tint     = AccentPink,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    video.title,
                    style    = MaterialTheme.typography.labelMedium,
                    color    = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    video.channelTitle,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
