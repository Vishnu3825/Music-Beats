package com.example.musicplayer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,           // in milliseconds
    val uri: String,              // content:// or stream URL
    val albumArtUri: String?,
    val source: SongSource = SongSource.LOCAL,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val size: Long = 0L,          // file size in bytes (local only)
    val genre: String? = null,
    val trackNumber: Int = 0
)

enum class SongSource {
    LOCAL,
    JAMENDO,
    STREAM
}

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val coverArt: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,           // 0f..1f
    val currentPosition: Long = 0L,     // ms
    val duration: Long = 0L,            // ms
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isBuffering: Boolean = false
)

enum class RepeatMode {
    OFF, ONE, ALL
}

data class JamendoTrack(
    val id: String,
    val name: String,
    val artist_name: String,
    val album_name: String,
    val duration: Int,
    val audio: String,
    val image: String,
    val album_image: String?
)

data class JamendoResponse(
    val results: List<JamendoTrack>
)

fun JamendoTrack.toSong(): Song = Song(
    id = "jamendo_$id",
    title = name,
    artist = artist_name,
    album = album_name,
    duration = duration * 1000L,
    uri = audio,
    albumArtUri = album_image ?: image,
    source = SongSource.JAMENDO
)
