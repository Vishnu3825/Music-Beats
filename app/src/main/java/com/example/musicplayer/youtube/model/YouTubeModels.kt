package com.example.musicplayer.youtube.model

import com.example.musicplayer.data.local.YouTubeSavedVideoEntity

// ─── Raw API response models ──────────────────────────────────────────────────

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem> = emptyList(),
    val nextPageToken: String? = null,
    val pageInfo: PageInfo? = null
)

data class PageInfo(
    val totalResults: Int = 0,
    val resultsPerPage: Int = 0
)

data class YouTubeSearchItem(
    val id: VideoId,
    val snippet: VideoSnippet
)

data class VideoId(
    val videoId: String = ""
)

data class VideoSnippet(
    val title: String = "",
    val channelTitle: String = "",
    val description: String = "",
    val publishedAt: String = "",
    val thumbnails: Thumbnails = Thumbnails()
)

data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
    val maxres: Thumbnail? = null
)

data class Thumbnail(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0
)

// ─── App UI model ─────────────────────────────────────────────────────────────

data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val description: String = "",
    val publishedAt: String = "",
    val duration: String = "",       // "PT3M45S" format from API
    val viewCount: String = "",
    val isFavorite: Boolean = false
) {
    val embedUrl: String
        get() = "https://www.youtube.com/embed/$videoId" +
                "?autoplay=1&rel=0&showinfo=0&modestbranding=1"

    val watchUrl: String
        get() = "https://www.youtube.com/watch?v=$videoId"

    val thumbnailHigh: String
        get() = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    val thumbnailMax: String
        get() = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
}

fun YouTubeSearchItem.toYouTubeVideo() = YouTubeVideo(
    videoId      = id.videoId,
    title        = snippet.title
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'"),
    channelTitle = snippet.channelTitle,
    thumbnailUrl = snippet.thumbnails.high?.url
        ?: snippet.thumbnails.medium?.url
        ?: snippet.thumbnails.default?.url
        ?: "",
    description  = snippet.description,
    publishedAt  = snippet.publishedAt
)

fun YouTubeVideo.toSavedEntity() = YouTubeSavedVideoEntity(
    videoId = videoId,
    title = title,
    channelTitle = channelTitle,
    thumbnailUrl = thumbnailUrl,
    description = description,
    publishedAt = publishedAt,
    duration = duration,
    viewCount = viewCount
)

fun YouTubeSavedVideoEntity.toYouTubeVideo() = YouTubeVideo(
    videoId = videoId,
    title = title,
    channelTitle = channelTitle,
    thumbnailUrl = thumbnailUrl,
    description = description,
    publishedAt = publishedAt,
    duration = duration,
    viewCount = viewCount,
    isFavorite = true
)

data class TeluguMusicPerson(
    val id: String,
    val name: String,
    val role: String,
    val query: String,
    val imageUrl: String
)

private fun generatedPersonImage(name: String, background: String): String =
    "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}&background=$background&color=ffffff&size=256&bold=true"

val teluguMusicPeople = listOf(
    TeluguMusicPerson(
        id = "anirudh",
        name = "Anirudh",
        role = "Music Director",
        query = "Anirudh Ravichander Telugu movie songs",
        imageUrl = generatedPersonImage("Anirudh", "c0392b")
    ),
    TeluguMusicPerson(
        id = "thaman",
        name = "Thaman S",
        role = "Music Director",
        query = "Thaman S Telugu movie songs",
        imageUrl = generatedPersonImage("Thaman S", "1f3a93")
    ),
    TeluguMusicPerson(
        id = "devisri",
        name = "Devi Sri",
        role = "Music Director",
        query = "Devi Sri Prasad Telugu movie songs",
        imageUrl = generatedPersonImage("Devi Sri", "8e44ad")
    ),
    TeluguMusicPerson(
        id = "keeravani",
        name = "Keeravani",
        role = "Music Director",
        query = "MM Keeravani Telugu movie songs",
        imageUrl = generatedPersonImage("Keeravani", "d35400")
    ),
    TeluguMusicPerson(
        id = "arman",
        name = "Armaan Malik",
        role = "Singer",
        query = "Armaan Malik Telugu songs",
        imageUrl = generatedPersonImage("Armaan Malik", "16a085")
    ),
    TeluguMusicPerson(
        id = "sid",
        name = "Sid Sriram",
        role = "Singer",
        query = "Sid Sriram Telugu songs",
        imageUrl = generatedPersonImage("Sid Sriram", "2c3e50")
    ),
    TeluguMusicPerson(
        id = "shreya",
        name = "Shreya Ghoshal",
        role = "Singer",
        query = "Shreya Ghoshal Telugu songs",
        imageUrl = generatedPersonImage("Shreya Ghoshal", "ad1457")
    ),
    TeluguMusicPerson(
        id = "spb",
        name = "SP Balu",
        role = "Singer",
        query = "SP Balasubrahmanyam Telugu songs",
        imageUrl = generatedPersonImage("SP Balasubrahmanyam", "00695c")
    ),
    TeluguMusicPerson(
        id = "sunitha",
        name = "Sunitha",
        role = "Singer",
        query = "Sunitha Telugu songs",
        imageUrl = generatedPersonImage("Sunitha", "6a1b9a")
    ),
    TeluguMusicPerson(
        id = "edsheeran",
        name = "Ed Sheeran",
        role = "English Singer",
        query = "Ed Sheeran songs official audio",
        imageUrl = generatedPersonImage("Ed Sheeran", "1565c0")
    ),
    TeluguMusicPerson(
        id = "adele",
        name = "Adele",
        role = "English Singer",
        query = "Adele songs official audio",
        imageUrl = generatedPersonImage("Adele", "c2185b")
    ),
    TeluguMusicPerson(
        id = "taylorswift",
        name = "Taylor Swift",
        role = "English Singer",
        query = "Taylor Swift songs official audio",
        imageUrl = generatedPersonImage("Taylor Swift", "283593")
    )
)

// ─── Categories ───────────────────────────────────────────────────────────────

data class MusicCategory(
    val id: String,
    val displayName: String,
    val queryVariants: List<String>,
    val emoji: String,
    val gradientColors: List<Long>   // Compose Color longs for gradient
)

val musicCategories = listOf(
    MusicCategory(
        id = "trending",
        displayName = "Trending",
        queryVariants = listOf(
            "trending music india latest",
            "viral songs india latest",
            "top music hits india",
            "new trending songs india"
        ),
        emoji = "🔥",
        gradientColors = listOf(0xFFFF6B35, 0xFFF7C59F)
    ),
    MusicCategory(
        id = "bollywood",
        displayName = "Bollywood",
        queryVariants = listOf(
            "bollywood latest songs",
            "bollywood new releases",
            "bollywood trending songs",
            "bollywood hit songs latest"
        ),
        emoji = "🎬",
        gradientColors = listOf(0xFFE040FB, 0xFFFF80AB)
    ),
    MusicCategory(
        id = "telugu",
        displayName = "Telugu",
        queryVariants = listOf(
            "telugu latest songs",
            "telugu new releases",
            "telugu trending songs",
            "telugu hit songs latest"
        ),
        emoji = "🌟",
        gradientColors = listOf(0xFF00BCD4, 0xFF1DE9B6)
    ),
    MusicCategory(
        id = "devotional",
        displayName = "Devotional",
        queryVariants = listOf(
            "devotional songs latest",
            "telugu devotional songs latest",
            "bhakti songs new devotional",
            "devotional bhajans latest"
        ),
        emoji = "🙏",
        gradientColors = listOf(0xFFFFD54F, 0xFFFF8F00)
    ),
    MusicCategory(
        id = "tamil",
        displayName = "Tamil",
        queryVariants = listOf(
            "tamil latest songs",
            "tamil new releases",
            "tamil trending songs",
            "tamil hit songs latest"
        ),
        emoji = "🎵",
        gradientColors = listOf(0xFF69F0AE, 0xFF1B5E20)
    ),
    MusicCategory(
        id = "hiphop",
        displayName = "Hip-Hop",
        queryVariants = listOf(
            "hip hop rap latest",
            "trending hip hop music",
            "rap music new releases",
            "best hip hop songs latest"
        ),
        emoji = "🎤",
        gradientColors = listOf(0xFF212121, 0xFF757575)
    ),
    MusicCategory(
        id = "lofi",
        displayName = "Lo-Fi",
        queryVariants = listOf(
            "lofi chill beats study music",
            "lofi songs chill mix",
            "lofi indian beats",
            "late night lofi music"
        ),
        emoji = "☕",
        gradientColors = listOf(0xFF80CBC4, 0xFF4DB6AC)
    ),
    MusicCategory(
        id = "romantic",
        displayName = "Romantic",
        queryVariants = listOf(
            "romantic love songs latest",
            "romantic melody songs",
            "love songs hindi latest",
            "romantic hits playlist"
        ),
        emoji = "❤️",
        gradientColors = listOf(0xFFEF9A9A, 0xFFC62828)
    ),
    MusicCategory(
        id = "punjabi",
        displayName = "Punjabi",
        queryVariants = listOf(
            "punjabi latest songs",
            "punjabi new releases",
            "punjabi trending songs",
            "punjabi hit songs latest"
        ),
        emoji = "💃",
        gradientColors = listOf(0xFFFFCC02, 0xFFFF6D00)
    ),
    MusicCategory(
        id = "english",
        displayName = "English",
        queryVariants = listOf(
            "english pop songs latest",
            "english trending songs",
            "international hit songs latest",
            "english new music releases"
        ),
        emoji = "🎸",
        gradientColors = listOf(0xFF42A5F5, 0xFF1565C0)
    ),
    MusicCategory(
        id = "instrumental",
        displayName = "Instrumental",
        queryVariants = listOf(
            "instrumental music relaxing",
            "instrumental background music",
            "peaceful instrumental songs",
            "instrumental music latest"
        ),
        emoji = "🎻",
        gradientColors = listOf(0xFFCE93D8, 0xFF6A1B9A)
    ),
    MusicCategory(
        id = "workout",
        displayName = "Workout",
        queryVariants = listOf(
            "gym workout motivation music",
            "workout songs latest",
            "fitness music playlist",
            "high energy gym songs"
        ),
        emoji = "💪",
        gradientColors = listOf(0xFF80D8FF, 0xFF0091EA)
    ),
)
