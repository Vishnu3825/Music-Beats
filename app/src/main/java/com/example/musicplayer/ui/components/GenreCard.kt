package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.*

data class Genre(
    val name: String,
    val displayName: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val icon: ImageVector
)

val predefinedGenres = listOf(
    Genre("pop",        "Pop",       Color(0xFFE040FB), Color(0xFF7B1FA2), Icons.Default.Star),
    Genre("rock",       "Rock",      Color(0xFFFF5722), Color(0xFFBF360C), Icons.Default.Bolt),
    Genre("jazz",       "Jazz",      Color(0xFF00BCD4), Color(0xFF006064), Icons.Default.MusicNote),
    Genre("electronic", "Electronic",Color(0xFF1DE9B6), Color(0xFF004D40), Icons.Default.Waves),
    Genre("classical",  "Classical", Color(0xFFFFD54F), Color(0xFFE65100), Icons.Default.Piano),
    Genre("hiphop",     "Hip-Hop",   Color(0xFF69F0AE), Color(0xFF1B5E20), Icons.Default.Mic),
    Genre("ambient",    "Ambient",   Color(0xFF80D8FF), Color(0xFF01579B), Icons.Default.Air),
    Genre("folk",       "Folk",      Color(0xFFD4E157), Color(0xFF33691E), Icons.Default.NaturePeople),
)

@Composable
fun GenreSection(
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Browse by Genre",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(predefinedGenres) { genre ->
                GenreCard(
                    genre = genre,
                    onClick = { onGenreClick(genre.name) }
                )
            }
        }
    }
}

@Composable
fun GenreCard(
    genre: Genre,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(110.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(genre.gradientStart, genre.gradientEnd)
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        // Decorative icon (top-right, large, semi-transparent)
        Icon(
            imageVector = genre.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        )

        Text(
            text = genre.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier.padding(10.dp)
        )
    }
}

// ─── Featured Song Card (horizontal scroll) ───────────────────────────────────

@Composable
fun FeaturedSongCard(
    title: String,
    artist: String,
    artUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(160.dp)
            .height(190.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard
    ) {
        Column {
            // Album art area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(SurfaceLight),
                contentAlignment = Alignment.Center
            ) {
                if (artUri != null) {
                    coil.compose.AsyncImage(
                        model = artUri,
                        contentDescription = "Album Art",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        null,
                        tint = TextHint,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            // Info
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
