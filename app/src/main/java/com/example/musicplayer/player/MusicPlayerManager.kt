package com.example.musicplayer.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicplayer.data.model.PlayerState
import com.example.musicplayer.data.model.RepeatMode
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.service.MusicPlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    // ─── Player Initialization ────────────────────────────────────────────────

    fun initPlayer() {
        exoPlayer.removeListener(playerListener)
        exoPlayer.addListener(playerListener)
    }

    fun releasePlayer() {
        progressJob?.cancel()
        exoPlayer.removeListener(playerListener)
    }

    // ─── Playback Controls ────────────────────────────────────────────────────

    fun playSong(song: Song, queue: List<Song> = emptyList(), startIndex: Int = 0) {
        val exo = exoPlayer
        val actualQueue = if (queue.isEmpty()) listOf(song) else queue

        val mediaItems = actualQueue.map { s ->
            MediaItem.Builder()
                .setUri(Uri.parse(s.uri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(s.albumArtUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }

        exo.setMediaItems(mediaItems, startIndex, 0L)
        exo.prepare()
        exo.play()
        ensurePlaybackServiceRunning()

        _playerState.update {
            it.copy(
                currentSong = song,
                queue = actualQueue,
                currentIndex = startIndex,
                isPlaying = true,
                isBuffering = true
            )
        }

        startProgressTracking()
    }

    fun playOrPause() {
        val exo = exoPlayer
        if (exo.isPlaying) {
            exo.pause()
        } else {
            exo.play()
            ensurePlaybackServiceRunning()
        }
        _playerState.update { it.copy(isPlaying = exo.isPlaying) }
    }

    fun skipToNext() {
        val exo = exoPlayer
        if (exo.hasNextMediaItem()) {
            exo.seekToNextMediaItem()
            updateCurrentSongFromPlayer()
        } else if (_playerState.value.repeatMode == RepeatMode.ALL) {
            exo.seekToDefaultPosition(0)
            updateCurrentSongFromPlayer()
        }
    }

    fun skipToPrevious() {
        val exo = exoPlayer
        if (exo.currentPosition > 3000L) {
            exo.seekTo(0)
        } else if (exo.hasPreviousMediaItem()) {
            exo.seekToPreviousMediaItem()
            updateCurrentSongFromPlayer()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _playerState.update { it.copy(currentPosition = position) }
    }

    fun seekToFraction(fraction: Float) {
        val duration = exoPlayer.duration
        if (duration > 0) seekTo((fraction * duration).toLong())
    }

    fun setRepeatMode(mode: RepeatMode) {
        val exo = exoPlayer
        when (mode) {
            RepeatMode.OFF -> exo.repeatMode = Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> exo.repeatMode = Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> exo.repeatMode = Player.REPEAT_MODE_ALL
        }
        _playerState.update { it.copy(repeatMode = mode) }
    }

    fun toggleShuffle() {
        val exo = exoPlayer
        val newShuffle = !_playerState.value.shuffleEnabled
        exo.shuffleModeEnabled = newShuffle
        _playerState.update { it.copy(shuffleEnabled = newShuffle) }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playSong(songs[startIndex], songs, startIndex)
    }

    fun removeSongById(songId: String) {
        val queue = _playerState.value.queue
        val index = queue.indexOfFirst { it.id == songId }
        if (index >= 0) {
            removeSongAt(index)
        }
    }

    private fun removeSongAt(index: Int) {
        val currentState = _playerState.value
        val currentQueue = currentState.queue
        if (index !in currentQueue.indices) return

        val updatedQueue = currentQueue.toMutableList().apply { removeAt(index) }
        exoPlayer.removeMediaItem(index)

        if (updatedQueue.isEmpty()) {
            stopPlayback()
            return
        }

        val playerIndex = exoPlayer.currentMediaItemIndex.coerceIn(0, updatedQueue.lastIndex)
        val duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        val position = exoPlayer.currentPosition.coerceAtLeast(0L)
        val progress = if (duration > 0) {
            (position.toFloat() / duration).coerceIn(0f, 1f)
        } else {
            0f
        }

        _playerState.update {
            it.copy(
                currentSong = updatedQueue.getOrNull(playerIndex),
                currentIndex = playerIndex,
                queue = updatedQueue,
                currentPosition = position,
                duration = duration,
                progress = progress,
                isPlaying = exoPlayer.isPlaying,
                isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
            )
        }
    }

    // ─── Progress Tracking ────────────────────────────────────────────────────

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val exo = exoPlayer
                val position = exo.currentPosition
                val duration = exo.duration.coerceAtLeast(1L)
                val progress = (position.toFloat() / duration).coerceIn(0f, 1f)

                _playerState.update {
                    it.copy(
                        currentPosition = position,
                        duration = duration,
                        progress = progress
                    )
                }
                delay(500L)
            }
        }
    }

    private fun updateCurrentSongFromPlayer() {
        val exo = exoPlayer
        val index = exo.currentMediaItemIndex
        val queue = _playerState.value.queue
        if (index < queue.size) {
            _playerState.update {
                it.copy(
                    currentSong = queue[index],
                    currentIndex = index
                )
            }
        }
    }

    private fun ensurePlaybackServiceRunning() {
        val intent = Intent(context, MusicPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopPlayback() {
        progressJob?.cancel()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playerState.update {
            it.copy(
                currentSong = null,
                isPlaying = false,
                progress = 0f,
                currentPosition = 0L,
                duration = 0L,
                queue = emptyList(),
                currentIndex = 0,
                isBuffering = false
            )
        }
    }

    // ─── Player Listener ──────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startProgressTracking() else progressJob?.cancel()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSongFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _playerState.update { it.copy(isBuffering = true) }
                Player.STATE_READY -> _playerState.update { it.copy(isBuffering = false) }
                Player.STATE_ENDED -> {
                    _playerState.update { it.copy(isPlaying = false, progress = 0f) }
                    progressJob?.cancel()
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerError.value = error.message ?: "Playback error occurred"
            _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
        }
    }
}
