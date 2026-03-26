package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.*
import com.example.musicplayer.data.model.*
import com.example.musicplayer.data.remote.JamendoApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val localDataSource: com.example.musicplayer.data.local.LocalMusicDataSource,
    private val jamendoApi: JamendoApiService,
    private val favoriteDao: FavoriteDao,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val playlistDao: PlaylistDao
) {
    // ─── Local Songs ─────────────────────────────────────────────────────────

    suspend fun getLocalSongs(): Result<List<Song>> = runCatching {
        localDataSource.fetchLocalSongs()
    }

    suspend fun scanLocalSongs(): Result<Pair<List<Song>, LocalScanSummary>> = runCatching {
        localDataSource.scanLocalSongs()
    }

    suspend fun searchLocalSongs(query: String): Result<List<Song>> = runCatching {
        localDataSource.searchLocalSongs(query)
    }

    // ─── Online Songs ─────────────────────────────────────────────────────────

    suspend fun searchOnlineSongs(query: String): Result<List<Song>> = runCatching {
        val response = jamendoApi.searchTracks(query = query)
        response.results.map { dto ->
            Song(
                id = "jamendo_${dto.id}",
                title = dto.name,
                artist = dto.artist_name,
                album = dto.album_name,
                duration = dto.duration * 1000L,
                uri = dto.audio,
                albumArtUri = dto.album_image ?: dto.image,
                source = SongSource.JAMENDO,
                genre = dto.musicinfo?.tags?.genres?.firstOrNull()
            )
        }
    }

    suspend fun getFeaturedSongs(): Result<List<Song>> = runCatching {
        val response = jamendoApi.getFeaturedTracks()
        response.results.map { dto ->
            Song(
                id = "jamendo_${dto.id}",
                title = dto.name,
                artist = dto.artist_name,
                album = dto.album_name,
                duration = dto.duration * 1000L,
                uri = dto.audio,
                albumArtUri = dto.album_image ?: dto.image,
                source = SongSource.JAMENDO
            )
        }
    }

    suspend fun getSongsByGenre(genre: String): Result<List<Song>> = runCatching {
        val response = jamendoApi.getTracksByGenre(genre = genre)
        response.results.map { dto ->
            Song(
                id = "jamendo_${dto.id}",
                title = dto.name,
                artist = dto.artist_name,
                album = dto.album_name,
                duration = dto.duration * 1000L,
                uri = dto.audio,
                albumArtUri = dto.album_image ?: dto.image,
                source = SongSource.JAMENDO,
                genre = genre
            )
        }
    }

    // ─── Favorites ────────────────────────────────────────────────────────────

    fun getFavoriteIds(): Flow<List<String>> =
        favoriteDao.getAllFavorites().map { list -> list.map { it.songId } }

    suspend fun isFavorite(songId: String): Boolean = favoriteDao.isFavorite(songId)

    suspend fun toggleFavorite(song: Song) {
        if (favoriteDao.isFavorite(song.id)) {
            favoriteDao.removeFavoriteById(song.id)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(songId = song.id))
        }
    }

    // ─── Recently Played ──────────────────────────────────────────────────────

    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>> =
        recentlyPlayedDao.getRecentlyPlayed()

    suspend fun recordPlay(song: Song) {
        recentlyPlayedDao.insertRecentlyPlayed(
            RecentlyPlayedEntity(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                albumArtUri = song.albumArtUri,
                uri = song.uri,
                duration = song.duration
            )
        )
        recentlyPlayedDao.trimOldEntries()
    }

    // ─── Playlists ────────────────────────────────────────────────────────────

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): String {
        val id = "playlist_${System.currentTimeMillis()}"
        playlistDao.createPlaylist(PlaylistEntity(id = id, name = name))
        return id
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = playlistDao.deletePlaylist(playlist)

    suspend fun addSongToPlaylist(playlistId: String, songId: String, position: Int) {
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = position)
        )
    }

    fun getPlaylistSongs(playlistId: String): Flow<List<PlaylistSongCrossRef>> =
        playlistDao.getPlaylistSongs(playlistId)
}
