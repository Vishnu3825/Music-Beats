package com.example.musicplayer.data.local

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.musicplayer.data.model.LocalScanSummary
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.model.SongSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocalMusicDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences(SCAN_PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun fetchLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        querySongs()
    }

    suspend fun scanLocalSongs(): Pair<List<Song>, LocalScanSummary> = withContext(Dispatchers.IO) {
        refreshMediaStore()

        val songs = querySongs()
        val previousCount = prefs.getInt(KEY_PREVIOUS_COUNT, 0)
        val lastScanTimestampSeconds = prefs.getLong(KEY_LAST_SCAN_SECONDS, 0L)
        val totalSongs = songs.size
        val newlyAddedSongs = if (lastScanTimestampSeconds > 0L) {
            querySongs(addedAfterSeconds = lastScanTimestampSeconds).size
        } else {
            (totalSongs - previousCount).coerceAtLeast(0)
        }

        prefs.edit()
            .putInt(KEY_PREVIOUS_COUNT, totalSongs)
            .putLong(KEY_LAST_SCAN_SECONDS, System.currentTimeMillis() / 1000L)
            .apply()

        songs to LocalScanSummary(
            previousSongs = previousCount,
            totalSongs = totalSongs,
            newlyAddedSongs = newlyAddedSongs
        )
    }

    suspend fun searchLocalSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = querySongs()
        val q = query.lowercase()
        allSongs.filter { song ->
            song.title.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                song.album.lowercase().contains(q)
        }
    }

    private fun querySongs(addedAfterSeconds: Long? = null): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.TRACK
        )
        val selection = buildString {
            append("${MediaStore.Audio.Media.SIZE} > 0")
            if (addedAfterSeconds != null) {
                append(" AND ${MediaStore.Audio.Media.DATE_ADDED} > ?")
            }
        }
        val selectionArgs = buildList {
            addedAfterSeconds?.let { add(it.toString()) }
        }.toTypedArray()
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val query: Cursor? = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val genreColumn = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown" }
                val artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Unknown Artist" }
                val album = cursor.getString(albumColumn).orEmpty().ifBlank { "Unknown Album" }
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val genre = if (genreColumn >= 0) cursor.getString(genreColumn) else null
                val track = if (trackColumn >= 0) cursor.getInt(trackColumn) else 0

                songs.add(
                    Song(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        albumArtUri = getAlbumArtUri(albumId)?.toString(),
                        source = SongSource.LOCAL,
                        size = size,
                        dateAdded = dateAdded * 1000L,
                        genre = genre,
                        trackNumber = track
                    )
                )
            }
        }

        return songs
    }

    private suspend fun refreshMediaStore() {
        val scanPaths = buildScanPaths()
        if (scanPaths.isEmpty()) return

        suspendCancellableCoroutine { continuation ->
            var remaining = scanPaths.size
            MediaScannerConnection.scanFile(context, scanPaths.toTypedArray(), null) { _, _ ->
                remaining -= 1
                if (remaining <= 0 && continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    private fun buildScanPaths(): List<String> {
        val roots = mutableListOf<File>()
        val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicMusicDir?.exists() == true) roots += publicMusicDir
        if (publicDownloadsDir?.exists() == true) roots += publicDownloadsDir

        context.getExternalFilesDirs(null)
            .mapNotNull { dir -> dir?.parentFile?.parentFile?.parentFile?.parentFile }
            .filter { it.exists() && it.isDirectory }
            .forEach { roots += it }

        return roots
            .distinctBy { it.absolutePath }
            .flatMap { root ->
                root.walkTopDown()
                    .maxDepth(3)
                    .filter { file -> file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS }
                    .map(File::getAbsolutePath)
                    .toList()
            }
            .distinct()
    }

    private fun getAlbumArtUri(albumId: Long): Uri? {
        return try {
            val albumArtUri = Uri.parse("content://media/external/audio/albumart")
            ContentUris.withAppendedId(albumArtUri, albumId)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        private const val SCAN_PREFS_NAME = "local_scan_prefs"
        private const val KEY_PREVIOUS_COUNT = "previous_song_count"
        private const val KEY_LAST_SCAN_SECONDS = "last_scan_seconds"
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "aac", "m4a", "wav", "flac", "ogg", "opus", "amr", "3gp", "mid", "midi"
        )
    }
}
