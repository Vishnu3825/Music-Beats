package com.example.musicplayer.ui.screens.youtube

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.*
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import coil.compose.AsyncImage
import com.example.musicplayer.youtube.model.YouTubeVideo
import com.example.musicplayer.youtube.viewmodel.YouTubeRepeatMode
import com.example.musicplayer.ui.theme.*
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerScreen(
    video           : YouTubeVideo,
    queue           : List<YouTubeVideo>,
    queueIndex      : Int,
    playbackCommandVersion: Long,
    resumeSeconds   : Float,
    shuffleEnabled  : Boolean,
    repeatMode      : YouTubeRepeatMode,
    relatedVideos   : List<YouTubeVideo>,
    isFavorite      : Boolean,
    isMinimized     : Boolean,
    onBack          : () -> Unit,
    onExpand        : () -> Unit,
    onMinimize      : () -> Unit,
    onClose         : () -> Unit,
    onPreviousVideo : () -> Unit,
    onToggleShuffle : () -> Unit,
    onToggleRepeat  : () -> Unit,
    onToggleFavorite: (YouTubeVideo) -> Unit,
    onRelatedClick  : (YouTubeVideo) -> Unit,
    onNextVideo     : () -> Unit,
    onVideoChanged  : (String, Int) -> Unit,
    onPlaybackProgress: (String, Float, Float) -> Unit,
    onQueueEnded    : () -> Unit,
    onPlayerError   : (Int) -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    modifier        : Modifier = Modifier
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val playerAssetUrl = "https://appassets.androidplatform.net/assets/youtube_player.html"
    val assetLoader = remember {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    var isFullscreen by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var playerPageReady by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }
    val queueJson = remember(queue) {
        queue.joinToString(prefix = "[", postfix = "]") { JSONObject.quote(it.videoId) }
    }

    fun createOrReuseWebView(ctx: android.content.Context): WebView {
        webView?.also { existing ->
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }

        return WebView(ctx).apply {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                javaScriptCanOpenWindowsAutomatically = true
                loadsImagesAutomatically = true
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) {
                    isFullscreen = true
                }
                override fun onHideCustomView() {
                    isFullscreen = false
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    playerPageReady = false
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    return assetLoader.shouldInterceptRequest(uri)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    url: String?
                ): WebResourceResponse? {
                    val uri = url?.let(android.net.Uri::parse) ?: return null
                    return assetLoader.shouldInterceptRequest(uri)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        onPlayerError(-1)
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true) {
                        onPlayerError(-1)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    playerPageReady = true
                }
            }

            addJavascriptInterface(object {
                @JavascriptInterface fun onPlayerReady() {}
                @JavascriptInterface fun onPlayerStateChange(state: Int) = Unit
                @JavascriptInterface fun onVideoChanged(videoId: String, index: Int) {
                    mainHandler.post { onVideoChanged(videoId, index) }
                }
                @JavascriptInterface fun onPlaybackProgress(videoId: String, currentTime: Double, duration: Double) {
                    mainHandler.post {
                        onPlaybackProgress(videoId, currentTime.toFloat(), duration.toFloat())
                    }
                }
                @JavascriptInterface fun onQueueEnded() {
                    mainHandler.post { onQueueEnded() }
                }
                @JavascriptInterface fun onPlayerError(code: Int) {
                    mainHandler.post { onPlayerError(code) }
                }
            }, "Android")

            loadUrl(playerAssetUrl)
            webView = this
        }
    }

    fun runPlayerCommand(command: String) {
        webView?.evaluateJavascript(command, null)
    }

    LaunchedEffect(playerPageReady, queueJson) {
        if (!playerPageReady) return@LaunchedEffect
        runPlayerCommand("setQueue($queueJson, $queueIndex);")
    }

    LaunchedEffect(playerPageReady, shuffleEnabled, repeatMode) {
        if (!playerPageReady) return@LaunchedEffect
        runPlayerCommand(
            "setPlaybackOptions(${if (shuffleEnabled) "true" else "false"}, ${JSONObject.quote(repeatMode.name.lowercase())});"
        )
    }

    LaunchedEffect(playerPageReady, playbackCommandVersion) {
        if (!playerPageReady || queue.isEmpty()) return@LaunchedEffect
        runPlayerCommand("setQueue($queueJson, $queueIndex);")
        runPlayerCommand("playAtIndex($queueIndex, ${resumeSeconds.coerceAtLeast(0f)}, true);")
    }

    DisposableEffect(Unit) {
        rootView.keepScreenOn = true
        onDispose {
            rootView.keepScreenOn = false
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    LaunchedEffect(isFullscreen) {
        onFullscreenChange(isFullscreen)
    }

    if (isMinimized) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                color = SurfaceDeep,
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 10.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .padding(end = 12.dp, bottom = 92.dp)
                    .width(220.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx -> createOrReuseWebView(ctx) },
                            update = { wv ->
                                webView = wv
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = onExpand)
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onExpand,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                            ) {
                                Icon(Icons.Default.OpenInFull, "Expand", tint = Color.White)
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                            ) {
                                Icon(Icons.Default.Close, "Close", tint = Color.White)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onExpand)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = video.channelTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Background)
        ) {
            // ── Video Player ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .then(
                        if (isFullscreen) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        }
                    )
                    .background(Color.Black)
            ) {
            // WebView with YouTube IFrame API
            AndroidView(
                factory = { ctx -> createOrReuseWebView(ctx) },
                update = { wv ->
                    webView = wv
                },
                modifier = Modifier.fillMaxSize()
            )

            // Back button overlay (top-left)
            IconButton(
                onClick = {
                    if (isFullscreen) isFullscreen = false else onBack()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }

            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
            ) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    "Fullscreen",
                    tint = Color.White
                )
            }

                if (!isFullscreen) {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.ExpandMore, "Minimize", tint = Color.White)
                    }
                }

            }

            if (!isFullscreen) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text     = video.title,
                                style    = MaterialTheme.typography.titleMedium,
                                color    = TextPrimary,
                                maxLines = if (showDescription) Int.MAX_VALUE else 2,
                                overflow = if (showDescription) TextOverflow.Clip else TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { showDescription = !showDescription }
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text  = video.channelTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (video.viewCount.isNotEmpty()) {
                                    Text("•", color = TextHint, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text  = video.viewCount,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextHint
                                    )
                                }
                            }

                            AnimatedVisibility(visible = showDescription && video.description.isNotEmpty()) {
                                Text(
                                    text     = video.description,
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = TextSecondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PlayerActionBtn(
                                    icon  = Icons.Default.SkipPrevious,
                                    label = "Prev",
                                    onClick = onPreviousVideo
                                )
                                PlayerActionBtn(
                                    icon  = Icons.Default.SkipNext,
                                    label = "Next",
                                    onClick = onNextVideo
                                )
                                PlayerActionBtn(
                                    icon  = Icons.Default.Shuffle,
                                    label = "Shuffle",
                                    tint  = if (shuffleEnabled) NeonGreen else TextSecondary,
                                    onClick = onToggleShuffle
                                )
                                PlayerActionBtn(
                                    icon  = if (repeatMode == YouTubeRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    label = "Repeat",
                                    tint  = if (repeatMode == YouTubeRepeatMode.OFF) TextSecondary else NeonGreen,
                                    onClick = onToggleRepeat
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PlayerActionBtn(
                                    icon  = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    label = if (isFavorite) "Saved" else "Save",
                                    tint  = if (isFavorite) AccentPink else TextSecondary,
                                    onClick = { onToggleFavorite(video) }
                                )
                                PlayerActionBtn(
                                    icon  = Icons.Default.Share,
                                    label = "Share",
                                    onClick = { shareVideo(context, video) }
                                )
                            }

                            HorizontalDivider(
                                color    = Divider,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Text(
                                "Up Next",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                    }

                    items(relatedVideos.filter { it.videoId != video.videoId }, key = { it.videoId }) { related ->
                        YouTubeVideoRow(
                            video      = related,
                            isFavorite = related.isFavorite,
                            onClick    = { onRelatedClick(related) },
                            onFavorite = { onToggleFavorite(related) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerActionBtn(
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    label  : String,
    tint   : Color = TextSecondary,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextHint)
    }
}

private fun shareVideo(context: android.content.Context, video: YouTubeVideo) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, video.title)
        putExtra(Intent.EXTRA_TEXT, "${video.title}\n${video.watchUrl}")
    }

    context.startActivity(
        Intent.createChooser(shareIntent, "Share video").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

// ─── Video row used in lists ──────────────────────────────────────────────────

@Composable
fun YouTubeVideoRow(
    video      : YouTubeVideo,
    isFavorite : Boolean,
    onClick    : () -> Unit,
    onFavorite : () -> Unit,
    modifier   : Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLight)
        ) {
            AsyncImage(
                model            = video.thumbnailHigh,
                contentDescription = video.title,
                contentScale     = ContentScale.Crop,
                modifier         = Modifier.fillMaxSize()
            )
            if (video.duration.isNotEmpty()) {
                Surface(
                    color    = Color.Black.copy(alpha = 0.75f),
                    shape    = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text  = video.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(
                Icons.Default.PlayCircle,
                null,
                tint     = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = video.title,
                style    = MaterialTheme.typography.bodyMedium,
                color    = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text  = video.channelTitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (video.viewCount.isNotEmpty()) {
                Text(
                    text  = video.viewCount,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
        }

        // Favorite
        IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint  = if (isFavorite) AccentPink else TextHint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
