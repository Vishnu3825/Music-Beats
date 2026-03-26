package com.example.musicplayer.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Favorite Songs ─────────────────────────────────────────────────────────

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "youtube_saved_videos")
data class YouTubeSavedVideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val description: String,
    val publishedAt: String,
    val duration: String,
    val viewCount: String,
    val addedAt: Long = System.currentTimeMillis()
)

// ─── Recently Played ────────────────────────────────────────────────────────

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val uri: String,
    val duration: Long,
    val playedAt: Long = System.currentTimeMillis()
)

// ─── Playlist Entities ──────────────────────────────────────────────────────

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

// ─── DAOs ────────────────────────────────────────────────────────────────────

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavoriteById(songId: String)
}

@Dao
interface YouTubeSavedVideoDao {
    @Query("SELECT * FROM youtube_saved_videos ORDER BY addedAt DESC")
    fun getAllSavedVideos(): Flow<List<YouTubeSavedVideoEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM youtube_saved_videos WHERE videoId = :videoId)")
    suspend fun isSaved(videoId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVideo(video: YouTubeSavedVideoEntity)

    @Query("DELETE FROM youtube_saved_videos WHERE videoId = :videoId")
    suspend fun removeSavedVideo(videoId: String)
}

@Dao
interface RecentlyPlayedDao {
    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(song: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played WHERE songId NOT IN (SELECT songId FROM recently_played ORDER BY playedAt DESC LIMIT 50)")
    suspend fun trimOldEntries()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getPlaylistSongs(playlistId: String): Flow<List<PlaylistSongCrossRef>>
}

// ─── Database ────────────────────────────────────────────────────────────────

@Database(
    entities = [
        FavoriteEntity::class,
        YouTubeSavedVideoEntity::class,
        RecentlyPlayedEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun youTubeSavedVideoDao(): YouTubeSavedVideoDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun playlistDao(): PlaylistDao
}
