package com.example.musicplayer.ui.screens.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.ui.theme.Background
import com.example.musicplayer.ui.theme.Divider
import com.example.musicplayer.ui.theme.NeonGreen
import com.example.musicplayer.ui.theme.SurfaceDeep
import com.example.musicplayer.ui.theme.TextHint
import com.example.musicplayer.ui.theme.TextPrimary
import com.example.musicplayer.ui.theme.TextSecondary
import com.example.musicplayer.youtube.model.TeluguMusicPerson
import com.example.musicplayer.youtube.model.teluguMusicPeople

@Composable
fun YouTubeHomeScreen(
    isLoadingTrending: Boolean,
    onPersonTap: (TeluguMusicPerson) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111522), Background, Color(0xFF07090F))
                )
            ),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                Text(
                    text = "Music Directors & Singers",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Tap any tile to play that singer or director songs one by one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                teluguMusicPeople.chunked(3).forEach { rowPeople ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPeople.forEach { person ->
                            PersonTile(
                                person = person,
                                onClick = { onPersonTap(person) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowPeople.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (isLoadingTrending) {
            item { LoadingRow() }
        }
    }
}

@Composable
private fun PersonTile(
    person: TeluguMusicPerson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDeep.copy(alpha = 0.92f))
            .border(1.dp, Divider.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = person.imageUrl,
            contentDescription = person.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Text(
            text = person.name,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = person.role,
            style = MaterialTheme.typography.bodySmall,
            color = TextHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp)
    }
}
