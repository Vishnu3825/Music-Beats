package com.example.musicplayer.youtube.api

import com.example.musicplayer.BuildConfig
import com.example.musicplayer.youtube.model.YouTubeSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * YouTube Data API v3
 *
 * HOW TO GET A FREE API KEY:
 * 1. Go to https://console.cloud.google.com
 * 2. Create a new project (e.g. "MusicPlayerApp")
 * 3. Enable "YouTube Data API v3" in APIs & Services
 * 4. Go to Credentials → Create Credentials → API Key
 * 5. (Optional) Restrict key to Android apps for security
 * 6. Add YOUTUBE_API_KEY to your Gradle properties instead of committing it
 *
 * FREE QUOTA: 10,000 units/day
 *   - search.list costs 100 units per call → ~100 searches/day free
 *   - videos.list costs 1 unit per call    → ~10,000 calls/day free
 */
interface YouTubeApiService {

    /**
     * Search for music videos.
     * Used for: keyword search, category browsing, trending.
     */
    @GET("search")
    suspend fun searchVideos(
        @Query("key")        apiKey    : String  = YOUTUBE_API_KEY,
        @Query("part")       part      : String  = "snippet",
        @Query("type")       type      : String  = "video",
        @Query("videoCategoryId") categoryId: String = "10",   // 10 = Music
        @Query("videoEmbeddable") embeddable: String = "true",
        @Query("q")          query     : String,
        @Query("maxResults") maxResults: Int     = 20,
        @Query("pageToken")  pageToken : String? = null,
        @Query("order")      order     : String  = "relevance",
        @Query("regionCode") region    : String  = "IN",       // India default
        @Query("relevanceLanguage") lang: String = "hi"
    ): YouTubeSearchResponse

    /**
     * Get trending music videos.
     */
    @GET("videos")
    suspend fun getTrendingVideos(
        @Query("key")        apiKey     : String = YOUTUBE_API_KEY,
        @Query("part")       part       : String = "snippet,statistics,status,contentDetails",
        @Query("chart")      chart      : String = "mostPopular",
        @Query("videoCategoryId") catId : String = "10",
        @Query("regionCode") region     : String = "IN",
        @Query("maxResults") maxResults : Int    = 20
    ): YouTubeVideosResponse

    /**
     * Get video details (duration, views) for a list of IDs.
     */
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("key")    apiKey : String = YOUTUBE_API_KEY,
        @Query("part")   part   : String = "snippet,contentDetails,statistics",
        @Query("id")     ids    : String
    ): YouTubeVideosResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
        const val YOUTUBE_API_KEY = BuildConfig.YOUTUBE_API_KEY
    }
}

// ─── Videos endpoint response ─────────────────────────────────────────────────

data class YouTubeVideosResponse(
    val items         : List<YouTubeVideoItem> = emptyList(),
    val nextPageToken : String?                = null
)

data class YouTubeVideoItem(
    val id              : String,
    val snippet         : com.example.musicplayer.youtube.model.VideoSnippet,
    val contentDetails  : ContentDetails? = null,
    val statistics      : VideoStatistics? = null,
    val status          : VideoStatus? = null
)

data class ContentDetails(
    val duration: String = ""   // ISO 8601: "PT3M45S"
)

data class VideoStatistics(
    val viewCount   : String = "0",
    val likeCount   : String = "0",
    val commentCount: String = "0"
)

data class VideoStatus(
    val embeddable: Boolean = true
)

// ─── Duration parser ──────────────────────────────────────────────────────────

fun parseDuration(iso: String): String {
    if (iso.isBlank()) return ""
    val h = Regex("(\\d+)H").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val m = Regex("(\\d+)M").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val s = Regex("(\\d+)S").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatViewCount(count: String): String {
    val n = count.toLongOrNull() ?: return ""
    return when {
        n >= 1_000_000_000 -> "%.1fB views".format(n / 1_000_000_000.0)
        n >= 1_000_000     -> "%.1fM views".format(n / 1_000_000.0)
        n >= 1_000         -> "%.1fK views".format(n / 1_000.0)
        else               -> "$n views"
    }
}
