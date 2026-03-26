package com.example.musicplayer.youtube.api

import com.example.musicplayer.data.local.YouTubeSavedVideoDao
import com.example.musicplayer.youtube.model.YouTubeVideo
import com.example.musicplayer.youtube.model.toSavedEntity
import com.example.musicplayer.youtube.model.toYouTubeVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepository @Inject constructor(
    private val api: YouTubeApiService,
    private val savedVideoDao: YouTubeSavedVideoDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()
    val savedVideos = savedVideoDao.getAllSavedVideos().map { videos ->
        videos.map { it.toYouTubeVideo() }
    }

    init {
        scope.launch {
            savedVideoDao.getAllSavedVideos().collect { videos ->
                _favoriteIds.value = videos.map { it.videoId }.toSet()
            }
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    suspend fun searchVideos(
        query     : String,
        maxResults: Int    = 20,
        pageToken : String? = null
    ): Result<Pair<List<YouTubeVideo>, String?>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.searchVideos(
                query      = query,
                maxResults = maxResults,
                pageToken  = pageToken
            )
            val videos = response.items
                .filter { it.id.videoId.isNotBlank() }
                .map { it.toYouTubeVideo() }
                .map { it.copy(isFavorite = _favoriteIds.value.contains(it.videoId)) }
            Pair(videos, response.nextPageToken)
        }
    }

    // ─── Category / Genre search ──────────────────────────────────────────────

    suspend fun getCategoryVideos(query: String): Result<List<YouTubeVideo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.searchVideos(query = query, maxResults = 20, order = "date")
                response.items
                    .filter { it.id.videoId.isNotBlank() }
                    .map { it.toYouTubeVideo() }
                    .map { it.copy(isFavorite = _favoriteIds.value.contains(it.videoId)) }
            }
        }

    // ─── Stream feed ──────────────────────────────────────────────────────────

    suspend fun getTrending(): Result<List<YouTubeVideo>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.searchVideos(
                query = "latest telugu movie songs",
                maxResults = 25,
                order = "date",
                lang = "te"
            )
            response.items
                .filter { it.id.videoId.isNotBlank() }
                .map { it.toYouTubeVideo() }
                .map { it.copy(isFavorite = _favoriteIds.value.contains(it.videoId)) }
        }
    }

    // ─── Video details ────────────────────────────────────────────────────────

    suspend fun getVideoDetails(videoIds: List<String>): Result<List<YouTubeVideo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.getVideoDetails(ids = videoIds.joinToString(","))
                response.items.map { item ->
                    YouTubeVideo(
                        videoId      = item.id,
                        title        = item.snippet.title,
                        channelTitle = item.snippet.channelTitle,
                        thumbnailUrl = item.snippet.thumbnails.high?.url ?: "",
                        duration     = parseDuration(item.contentDetails?.duration ?: ""),
                        viewCount    = formatViewCount(item.statistics?.viewCount ?: "0"),
                        isFavorite   = _favoriteIds.value.contains(item.id)
                    )
                }
            }
        }

    // ─── Favorites (in-memory) ────────────────────────────────────────────────

    suspend fun toggleFavorite(video: YouTubeVideo) {
        if (savedVideoDao.isSaved(video.videoId)) {
            savedVideoDao.removeSavedVideo(video.videoId)
        } else {
            savedVideoDao.saveVideo(video.toSavedEntity())
        }
    }

    fun isFavorite(videoId: String) = _favoriteIds.value.contains(videoId)
}
