package com.example.musicplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.data.model.PlayerState
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.model.SongSource
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.ui.theme.*
import com.example.musicplayer.util.extractPaletteColors
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onSetAsRingtone: (Song) -> Unit = {},
    onDeleteSong: (Song) -> Unit = {},
    onQueueClick: () -> Unit = {},
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    sleepTimerRemaining: Int? = null,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dynamic palette from album art
    var dominantColor by remember { mutableStateOf(PlayerGradientTop) }
    var accentColor by remember { mutableStateOf(NeonGreen) }

    LaunchedEffect(song?.albumArtUri) {
        scope.launch {
            val (bg, accent) = extractPaletteColors(context, song?.albumArtUri)
            dominantColor = bg
            accentColor = accent
        }
    }

    // Tab state: 0 = Player, 1 = Lyrics
    var selectedTab by remember { mutableIntStateOf(0) }

    // Options bottom sheet
    var showOptions by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Blurred album art background
        if (song?.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
            )
        }

        // Multi-stop gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to dominantColor.copy(alpha = 0.55f),
                            0.35f to Background.copy(alpha = 0.80f),
                            1f to Background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (song?.source == SongSource.JAMENDO) "STREAMING" else "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Text(
                        text = song?.album ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onQueueClick) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = TextPrimary
                    )
                }
            }

            // ── Tab Switcher ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Player", "Lyrics").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Surface(
                        onClick = { selectedTab = index },
                        color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) accentColor else TextHint,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Tab Content ──────────────────────────────────────────────────
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    else
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                },
                label = "tabContent",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    0 -> PlayerTab(
                        playerState         = playerState,
                        isFavorite          = isFavorite,
                        accentColor         = accentColor,
                        sleepTimerRemaining = sleepTimerRemaining,
                        onPlayPause         = onPlayPause,
                        onSkipNext          = onSkipNext,
                        onSkipPrevious      = onSkipPrevious,
                        onToggleShuffle     = onToggleShuffle,
                        onToggleRepeat      = onToggleRepeat,
                        onSeek              = onSeek,
                        onToggleFavorite    = onToggleFavorite,
                        onSleepTimerStart   = onStartSleepTimer,
                        onSleepTimerCancel  = onCancelSleepTimer,
                        onMoreOptions       = { showOptions = true }
                    )
                    1 -> LyricsTab()
                }
            }
        }
    }

    // More options bottom sheet
    if (showOptions && song != null) {
        SongOptionsBottomSheet(
            song = song,
            isFavorite = isFavorite,
            onDismiss = { showOptions = false },
            onAddToQueue = { showOptions = false },
            onToggleFavorite = { onToggleFavorite(); showOptions = false },
            onAddToPlaylist = { showOptions = false },
            onViewAlbum = { showOptions = false },
            onSetAsRingtone = onSetAsRingtone,
            onDeleteSong = onDeleteSong
        )
    }
}

// ─── Player Tab ───────────────────────────────────────────────────────────────

@Composable
private fun PlayerTab(
    playerState: PlayerState,
    isFavorite: Boolean,
    accentColor: Color,
    sleepTimerRemaining: Int?,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleFavorite: () -> Unit,
    onSleepTimerStart: (Int) -> Unit,
    onSleepTimerCancel: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val song = playerState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Large Album Art ──────────────────────────────────────────────────
        val artScale by animateFloatAsState(
            targetValue = if (playerState.isPlaying) 1f else 0.88f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "artScale"
        )

        Box(
            modifier = Modifier
                .size(288.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceLight)
                .then(
                    Modifier.then(
                        if (playerState.isPlaying)
                            Modifier.border(2.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                        else
                            Modifier
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (song?.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = TextHint,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Song Info Row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = song?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = song?.artist ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AccentPink else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = onMoreOptions) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Controls ─────────────────────────────────────────────────────────
        PlayerControls(
            playerState = playerState,
            onPlayPause = onPlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ── Equalizer visualizer ─────────────────────────────────────────────
        com.example.musicplayer.ui.components.EqualizerVisualizer(
            isPlaying = playerState.isPlaying,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            barCount  = 32,
            barWidth  = 5.dp,
            maxHeight = 40.dp,
            color     = accentColor
        )

        Spacer(Modifier.height(8.dp))

        SleepTimerSection(
            sleepTimerRemaining = sleepTimerRemaining,
            onStart  = onSleepTimerStart,
            onCancel = onSleepTimerCancel
        )
    }
}

// ─── Lyrics Tab ───────────────────────────────────────────────────────────────

@Composable
private fun LyricsTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.MusicNote,
                null,
                tint = TextHint,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Lyrics Not Available",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect a lyrics API like Musixmatch or Genius to display synchronized lyrics here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Sleep Timer Wire-up (called from ExtraActionButton in PlayerTab) ─────────
// PlayerScreen exposes showSleepTimer via a state hoisted inside PlayerScreen.
// The ExtraActionButton for "Sleep Timer" sets it to true.
// This extension composable is appended here for co-location.

@Composable
internal fun SleepTimerSection(
    sleepTimerRemaining : Int?,
    onStart             : (Int) -> Unit,
    onCancel            : () -> Unit
) {
    var show by remember { mutableStateOf(false) }

    // Display remaining time on button label
    val label = if (sleepTimerRemaining != null) {
        val m = sleepTimerRemaining / 60
        val s = sleepTimerRemaining % 60
        "%d:%02d".format(m, s)
    } else "Sleep Timer"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { show = true }) {
            Icon(
                Icons.Default.Timer,
                contentDescription = label,
                tint = if (sleepTimerRemaining != null) NeonGreen else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextHint)
    }

    if (show) {
        com.example.musicplayer.ui.components.SleepTimerDialog(
            remainingSeconds = sleepTimerRemaining,
            isRunning        = sleepTimerRemaining != null,
            onStart          = onStart,
            onCancel         = onCancel,
            onDismiss        = { show = false }
        )
    }
}
