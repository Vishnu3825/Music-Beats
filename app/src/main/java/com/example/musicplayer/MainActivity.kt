package com.example.musicplayer

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.ui.components.MiniPlayer
import com.example.musicplayer.ui.screens.*
import com.example.musicplayer.ui.screens.youtube.*
import com.example.musicplayer.util.MediaDeleteUtils
import com.example.musicplayer.ui.theme.*
import com.example.musicplayer.util.RingtoneUtils
import com.example.musicplayer.viewmodel.MusicViewModel
import com.example.musicplayer.youtube.viewmodel.YouTubeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val pipModeState = mutableStateOf(false)

    private val viewModel       : MusicViewModel   by viewModels()
    private val youTubeViewModel: YouTubeViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grants[Manifest.permission.READ_MEDIA_AUDIO] == true || hasAudioPermission()
        } else {
            grants[Manifest.permission.READ_EXTERNAL_STORAGE] == true || hasAudioPermission()
        }

        if (audioGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        syncAudioPermissionState()
        setContent {
            MusicPlayerTheme {
                MusicPlayerApp(
                    viewModel        = viewModel,
                    youTubeViewModel = youTubeViewModel,
                    onRequestPermission = ::requestAudioPermission,
                    isInPictureInPictureMode = pipModeState.value
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncAudioPermissionState()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && youTubeViewModel.showPlayer.value) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipModeState.value = isInPictureInPictureMode
    }

    private fun requestAudioPermission() {
        if (hasAudioPermission()) {
            viewModel.onPermissionGranted()
            return
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    private fun syncAudioPermissionState() {
        if (hasAudioPermission()) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    private fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

// ─── Nav Destinations ─────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home      : Screen("home",      "Local",     Icons.Default.LibraryMusic)
    object YouTube   : Screen("youtube",   "Stream",    Icons.Default.PlayCircleFilled)
    object Search    : Screen("search",    "Search",    Icons.Default.Search)
    object Library   : Screen("library",   "Library",   Icons.Default.Favorite)
    object Settings  : Screen("settings",  "Settings",  Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home, Screen.YouTube, Screen.Search, Screen.Library, Screen.Settings
)

// ─── Root composable ──────────────────────────────────────────────────────────

@Composable
fun MusicPlayerApp(
    viewModel           : MusicViewModel,
    youTubeViewModel    : YouTubeViewModel,
    onRequestPermission : () -> Unit = {},
    isInPictureInPictureMode: Boolean = false
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val navController     = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    val currentRoute      by navController.currentBackStackEntryAsState()

    // ── Local music state ─────────────────────────────────────────────────────
    val playerState       by viewModel.playerState.collectAsState()
    val localSongs        by viewModel.localSongs.collectAsState()
    val featuredSongs     by viewModel.featuredSongs.collectAsState()
    val recentlyPlayed    by viewModel.recentlyPlayed.collectAsState()
    val favoriteIds       by viewModel.favoriteIds.collectAsState()
    val isLoadingLocal    by viewModel.isLoadingLocal.collectAsState()
    val isLoadingFeatured by viewModel.isLoadingFeatured.collectAsState()
    val errorMessage      by viewModel.errorMessage.collectAsState()
    val lastLocalScanSummary by viewModel.lastLocalScanSummary.collectAsState()
    val permissionGranted by viewModel.permissionGranted.collectAsState()
    val isOnline          by viewModel.isOnline.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()

    // ── YouTube state ─────────────────────────────────────────────────────────
    val ytCurrentVideo    by youTubeViewModel.currentVideo.collectAsState()
    val ytShowPlayer      by youTubeViewModel.showPlayer.collectAsState()
    val ytSearchQuery     by youTubeViewModel.searchQuery.collectAsState()
    val ytSearchResults   by youTubeViewModel.searchResults.collectAsState()
    val ytIsSearching     by youTubeViewModel.isSearching.collectAsState()
    val ytFavoriteIds     by youTubeViewModel.favoriteIds.collectAsState()
    val ytSavedVideos     by youTubeViewModel.savedVideos.collectAsState()
    val ytIsLoadingTrend  by youTubeViewModel.isLoadingTrending.collectAsState()
    val ytError           by youTubeViewModel.error.collectAsState()
    val ytQueue           by youTubeViewModel.queue.collectAsState()
    val ytIsMinimized     by youTubeViewModel.isMinimized.collectAsState()
    val ytQueueIndex      by youTubeViewModel.queueIndex.collectAsState()
    val ytPlaybackCommandVersion by youTubeViewModel.playbackCommandVersion.collectAsState()
    val ytPlaybackPositionSeconds by youTubeViewModel.playbackPositionSeconds.collectAsState()
    val ytShuffleEnabled  by youTubeViewModel.shuffleEnabled.collectAsState()
    val ytRepeatMode      by youTubeViewModel.repeatMode.collectAsState()

    // Overlay flags (local music player)
    var showPlayerScreen  by remember { mutableStateOf(false) }
    var showQueueScreen   by remember { mutableStateOf(false) }
    var ytPlayerFullscreen by remember { mutableStateOf(false) }
    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }
    var showOpeningScreen by rememberSaveable { mutableStateOf(true) }
    val shouldHideBottomBar = isInPictureInPictureMode || ytPlayerFullscreen ||
        (ytShowPlayer && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    val startRoute = Screen.YouTube.route

    LaunchedEffect(Unit) {
        delay(1800)
        showOpeningScreen = false
    }

    fun finishSongDeletion(song: Song, deleted: Boolean) {
        pendingDeleteSong = null
        scope.launch {
            if (deleted) {
                val wasOnlySongInQueue =
                    playerState.currentSong?.id == song.id && playerState.queue.size <= 1
                viewModel.onSongDeleted(song)
                viewModel.loadLocalSongs()
                if (wasOnlySongInQueue) {
                    showPlayerScreen = false
                    showQueueScreen = false
                }
                snackbarHostState.showSnackbar("Deleted from device.")
            } else {
                snackbarHostState.showSnackbar("Unable to delete song.")
            }
        }
    }

    val deleteSongLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val song = pendingDeleteSong ?: return@rememberLauncherForActivityResult
        finishSongDeletion(song, result.resultCode == Activity.RESULT_OK)
    }

    BackHandler(
        enabled = showQueueScreen || showPlayerScreen || ytShowPlayer ||
            (currentRoute?.destination?.route != null && currentRoute?.destination?.route != startRoute)
    ) {
        when {
            showQueueScreen -> {
                showQueueScreen = false
                showPlayerScreen = playerState.currentSong != null
            }

            showPlayerScreen -> {
                showPlayerScreen = false
            }

            ytShowPlayer -> {
                ytPlayerFullscreen = false
                if (ytIsMinimized) {
                    youTubeViewModel.closePlayer()
                } else {
                    youTubeViewModel.minimizePlayer()
                }
            }

            currentRoute?.destination?.route != startRoute -> {
                navController.popBackStack()
            }
        }
    }

    // Snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                viewModel.clearError()
            }
        }
    }
    LaunchedEffect(ytError) {
        ytError?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                youTubeViewModel.clearError()
            }
        }
    }
    LaunchedEffect(isOnline) {
        if (!isOnline) scope.launch {
            snackbarHostState.showSnackbar("You're offline", duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(lastLocalScanSummary?.scannedAtMillis) {
        val summary = lastLocalScanSummary ?: return@LaunchedEffect
        scope.launch {
            snackbarHostState.showSnackbar(
                "Previous Songs: ${summary.previousSongs}  Total Songs: ${summary.totalSongs}  Newly Added Songs: ${summary.newlyAddedSongs}",
                duration = SnackbarDuration.Short
            )
        }
    }

    if (showOpeningScreen) {
        OpeningScreen()
        return
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = SurfaceLight,
                    contentColor   = TextPrimary,
                    actionColor    = NeonGreen,
                    shape          = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !shouldHideBottomBar,
                enter   = slideInVertically { it } + fadeIn(tween(200)),
                exit    = slideOutVertically { it } + fadeOut(tween(160))
            ) {
                Column {
                    // Local music mini-player
                    AnimatedVisibility(
                        visible = playerState.currentSong != null && !showPlayerScreen && !showQueueScreen && !ytShowPlayer,
                        enter   = slideInVertically { it } + fadeIn(tween(200)),
                        exit    = slideOutVertically { it } + fadeOut(tween(160))
                    ) {
                        MiniPlayer(
                            playerState   = playerState,
                            onPlayerClick = { showPlayerScreen = true },
                            onPlayPause   = viewModel::playOrPause,
                            onSkipNext    = viewModel::skipToNext
                        )
                    }

                    // Navigation bar
                    NavigationBar(containerColor = SurfaceDeep, tonalElevation = 0.dp) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute?.destination?.route == screen.route
                            NavigationBarItem(
                                icon     = { Icon(screen.icon, screen.label) },
                                label    = { Text(screen.label) },
                                selected = selected,
                                onClick  = {
                                    showPlayerScreen = false
                                    showQueueScreen = false
                                    ytPlayerFullscreen = false
                                    if (ytShowPlayer) {
                                        youTubeViewModel.minimizePlayer()
                                    }
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = NeonGreen,
                                    selectedTextColor   = NeonGreen,
                                    unselectedIconColor = TextHint,
                                    unselectedTextColor = TextHint,
                                    indicatorColor      = NeonGreen.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Permission gate
            if (!permissionGranted &&
                currentRoute?.destination?.route != Screen.YouTube.route &&
                currentRoute?.destination?.route != Screen.Search.route) {
                PermissionScreen(onRequestPermission = onRequestPermission)
            } else {
                NavHost(
                    navController      = navController,
                    startDestination   = Screen.YouTube.route,
                    enterTransition    = { fadeIn(tween(220)) },
                    exitTransition     = { fadeOut(tween(180)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition  = { fadeOut(tween(180)) }
                ) {
                    // ── Local music home ──────────────────────────────────────
                    composable(Screen.Home.route) {
                        HomeScreen(
                            localSongs        = localSongs,
                            featuredSongs     = featuredSongs,
                            recentlyPlayed    = recentlyPlayed,
                            currentSongId     = playerState.currentSong?.id,
                            favoriteIds       = favoriteIds,
                            isLoadingLocal    = isLoadingLocal,
                            isLoadingFeatured = isLoadingFeatured,
                            onSongClick       = { song, queue -> viewModel.playSong(song, queue) },
                            onFavoriteClick   = viewModel::onToggleFavorite,
                            onAddToQueue      = viewModel::addToQueue,
                            onSetAsRingtone   = { song ->
                                if (!RingtoneUtils.canWriteSystemSettings(context)) {
                                    context.startActivity(RingtoneUtils.createManageWriteSettingsIntent(context))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Allow modify system settings, then try again.")
                                    }
                                } else {
                                    val result = RingtoneUtils.setAsRingtone(context, song)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            result.fold(
                                                onSuccess = { "Ringtone updated." },
                                                onFailure = { it.message ?: "Unable to set ringtone." }
                                            )
                                        )
                                    }
                                }
                            },
                            onDeleteSong      = { song ->
                                pendingDeleteSong = song
                                val deleteIntentSender = MediaDeleteUtils.createDeleteIntentSender(context, song)
                                if (deleteIntentSender != null) {
                                    deleteSongLauncher.launch(
                                        IntentSenderRequest.Builder(deleteIntentSender).build()
                                    )
                                } else {
                                    val deleted = MediaDeleteUtils.deleteDirectly(context, song).getOrDefault(false)
                                    finishSongDeletion(song, deleted)
                                }
                            }
                        )
                    }

                    // ── YouTube streaming ─────────────────────────────────────
                    composable(Screen.YouTube.route) {
                        YouTubeHomeScreen(
                            isLoadingTrending = ytIsLoadingTrend,
                            onPersonTap       = youTubeViewModel::playPersonMix
                        )
                    }

                    // ── Combined search (local + YouTube) ─────────────────────
                    composable(Screen.Search.route) {
                        YouTubeSearchScreen(
                            query           = ytSearchQuery,
                            results         = ytSearchResults,
                            isSearching     = ytIsSearching,
                            favoriteIds     = ytFavoriteIds,
                            onQueryChange   = youTubeViewModel::onSearchQueryChange,
                            onVideoClick    = { video, queue -> youTubeViewModel.playVideo(video, queue) },
                            onFavoriteClick = youTubeViewModel::toggleFavorite,
                            onLoadMore      = youTubeViewModel::loadMoreSearchResults
                        )
                    }

                    // ── Video Library ─────────────────────────────────────────
                    composable(Screen.Library.route) {
                        YouTubeFavoritesScreen(
                            videos = ytSavedVideos,
                            onVideoClick = { video -> youTubeViewModel.playVideo(video, ytSavedVideos) },
                            onRemoveFavorite = youTubeViewModel::removeFavorite
                        )
                    }

                    // ── Settings ──────────────────────────────────────────────
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onScanDeviceMedia = {
                                viewModel.scanDeviceMedia()
                            },
                            isScanningMedia = isLoadingLocal,
                            scanSummary = lastLocalScanSummary
                        )
                    }
                }
            }

            // ── Local music full-screen player overlay ────────────────────────
            AnimatedVisibility(
                visible = showPlayerScreen,
                enter   = slideInVertically(initialOffsetY = { it }, animationSpec = tween(340, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                exit    = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(260, easing = FastOutSlowInEasing)) + fadeOut(tween(160))
            ) {
                PlayerScreen(
                    playerState          = playerState,
                    isFavorite           = playerState.currentSong?.let { favoriteIds.contains(it.id) } ?: false,
                    onBack               = { showPlayerScreen = false },
                    onPlayPause          = viewModel::playOrPause,
                    onSkipNext           = viewModel::skipToNext,
                    onSkipPrevious       = viewModel::skipToPrevious,
                    onToggleShuffle      = viewModel::toggleShuffle,
                    onToggleRepeat       = viewModel::toggleRepeat,
                    onSeek               = viewModel::seekToFraction,
                    onToggleFavorite     = { playerState.currentSong?.let { viewModel.onToggleFavorite(it) } },
                    onSetAsRingtone      = { song ->
                        if (!RingtoneUtils.canWriteSystemSettings(context)) {
                            context.startActivity(RingtoneUtils.createManageWriteSettingsIntent(context))
                            scope.launch {
                                snackbarHostState.showSnackbar("Allow modify system settings, then try again.")
                            }
                        } else {
                            val result = RingtoneUtils.setAsRingtone(context, song)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    result.fold(
                                        onSuccess = { "Ringtone updated." },
                                        onFailure = { it.message ?: "Unable to set ringtone." }
                                    )
                                )
                            }
                        }
                    },
                    onDeleteSong         = { song ->
                        pendingDeleteSong = song
                        val deleteIntentSender = MediaDeleteUtils.createDeleteIntentSender(context, song)
                        if (deleteIntentSender != null) {
                            deleteSongLauncher.launch(
                                IntentSenderRequest.Builder(deleteIntentSender).build()
                            )
                        } else {
                            val deleted = MediaDeleteUtils.deleteDirectly(context, song).getOrDefault(false)
                            finishSongDeletion(song, deleted)
                        }
                    },
                    onQueueClick         = { showPlayerScreen = false; showQueueScreen = true },
                    sleepTimerRemaining  = sleepTimerRemaining,
                    onStartSleepTimer    = viewModel::startSleepTimer,
                    onCancelSleepTimer   = viewModel::cancelSleepTimer
                )
            }

            // ── Queue overlay ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showQueueScreen,
                enter   = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(),
                exit    = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut()
            ) {
                QueueScreen(
                    queue             = playerState.queue,
                    currentIndex      = playerState.currentIndex,
                    onSongClick       = { index -> viewModel.playFromQueueIndex(index); showQueueScreen = false; showPlayerScreen = true },
                    onRemoveFromQueue = viewModel::removeFromQueue,
                    onClose           = { showQueueScreen = false; showPlayerScreen = true }
                )
            }

            // ── YouTube full-screen player overlay ────────────────────────────
            AnimatedVisibility(
                visible = ytShowPlayer && ytCurrentVideo != null,
                enter   = slideInVertically(initialOffsetY = { it }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(),
                exit    = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(260, easing = FastOutSlowInEasing)) + fadeOut()
            ) {
                ytCurrentVideo?.let { video ->
                    YouTubePlayerScreen(
                        video            = video,
                        queue            = ytQueue,
                        queueIndex       = ytQueueIndex,
                        playbackCommandVersion = ytPlaybackCommandVersion,
                        resumeSeconds    = ytPlaybackPositionSeconds,
                        shuffleEnabled   = ytShuffleEnabled,
                        repeatMode       = ytRepeatMode,
                        relatedVideos    = ytQueue,
                        isFavorite       = ytFavoriteIds.contains(video.videoId),
                        isMinimized      = ytIsMinimized,
                        onBack           = { youTubeViewModel.minimizePlayer() },
                        onExpand         = youTubeViewModel::expandPlayer,
                        onMinimize       = youTubeViewModel::minimizePlayer,
                        onClose          = youTubeViewModel::closePlayer,
                        onPreviousVideo  = youTubeViewModel::playPrevious,
                        onToggleShuffle  = youTubeViewModel::toggleShuffle,
                        onToggleRepeat   = youTubeViewModel::toggleRepeat,
                        onToggleFavorite = youTubeViewModel::toggleFavorite,
                        onRelatedClick   = { related -> youTubeViewModel.playVideo(related, ytQueue) },
                        onNextVideo      = youTubeViewModel::playNext,
                        onVideoChanged   = youTubeViewModel::onVideoChanged,
                        onPlaybackProgress = youTubeViewModel::onPlaybackProgress,
                        onQueueEnded     = youTubeViewModel::onQueueEnded,
                        onPlayerError    = youTubeViewModel::reportPlaybackError,
                        onFullscreenChange = { ytPlayerFullscreen = it }
                    )
                }
            }

        }
    }
}
