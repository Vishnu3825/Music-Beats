package com.example.musicplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.local.PlaylistEntity
import com.example.musicplayer.data.local.RecentlyPlayedEntity
import com.example.musicplayer.data.model.*
import com.example.musicplayer.data.repository.MusicRepository
import com.example.musicplayer.domain.usecase.*
import com.example.musicplayer.player.MusicPlayerManager
import com.example.musicplayer.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val getLocalSongs: GetLocalSongs,
    private val scanLocalSongs: ScanLocalSongs,
    private val searchOnlineSongs: SearchOnlineSongs,
    private val searchLocalSongs: SearchLocalSongs,
    private val getFeaturedSongs: GetFeaturedSongs,
    private val toggleFavorite: ToggleFavorite,
    private val recordPlay: RecordPlay,
    private val getSongsByGenre: GetSongsByGenre,
    private val repository: MusicRepository,
    val playerManager: MusicPlayerManager,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    // ── Player State ──────────────────────────────────────────────────────────

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val playerError: StateFlow<String?> = playerManager.playerError

    // ── Local Songs ───────────────────────────────────────────────────────────

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()

    private val _isLoadingLocal = MutableStateFlow(false)
    val isLoadingLocal: StateFlow<Boolean> = _isLoadingLocal.asStateFlow()

    private val _lastLocalScanSummary = MutableStateFlow<LocalScanSummary?>(null)
    val lastLocalScanSummary: StateFlow<LocalScanSummary?> = _lastLocalScanSummary.asStateFlow()

    // ── Featured ──────────────────────────────────────────────────────────────

    private val _featuredSongs = MutableStateFlow<List<Song>>(emptyList())
    val featuredSongs: StateFlow<List<Song>> = _featuredSongs.asStateFlow()

    private val _isLoadingFeatured = MutableStateFlow(false)
    val isLoadingFeatured: StateFlow<Boolean> = _isLoadingFeatured.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ── Favorites ─────────────────────────────────────────────────────────────

    val favoriteIds: StateFlow<List<String>> = repository.getFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Recently Played ───────────────────────────────────────────────────────

    val recentlyPlayed: StateFlow<List<RecentlyPlayedEntity>> =
        repository.getRecentlyPlayed()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Playlists ─────────────────────────────────────────────────────────────

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Genre ─────────────────────────────────────────────────────────────────

    private val _genreSongs = MutableStateFlow<List<Song>>(emptyList())
    val genreSongs: StateFlow<List<Song>> = _genreSongs.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    // ── Network ───────────────────────────────────────────────────────────────

    val isOnline: StateFlow<Boolean> = networkUtils.observeConnectivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), networkUtils.isConnected)

    // ── UI / Error ────────────────────────────────────────────────────────────

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private var searchJob: Job? = null

    init {
        playerManager.initPlayer()
        observeSearchQuery()
        observePlayerErrors()
    }

    // ── Init / Permission ─────────────────────────────────────────────────────

    fun onPermissionGranted() {
        _permissionGranted.value = true
        loadLocalSongs()
        loadFeaturedSongs()
    }

    fun onPermissionDenied() {
        _permissionGranted.value = false
        _errorMessage.value = "Media permission is required to load local audio"
    }

    fun loadLocalSongs() {
        viewModelScope.launch {
            _isLoadingLocal.value = true
            getLocalSongs().fold(
                onSuccess = { songs ->
                    _localSongs.value = songs
                    _isLoadingLocal.value = false
                    val activeQuery = _searchQuery.value.trim()
                    if (activeQuery.isNotEmpty()) {
                        performSearch(activeQuery)
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                    _isLoadingLocal.value = false
                }
            )
        }
    }

    fun scanDeviceMedia() {
        viewModelScope.launch {
            _isLoadingLocal.value = true
            scanLocalSongs().fold(
                onSuccess = { (songs, summary) ->
                    _localSongs.value = songs
                    _lastLocalScanSummary.value = summary
                    _isLoadingLocal.value = false
                    val activeQuery = _searchQuery.value.trim()
                    if (activeQuery.isNotEmpty()) {
                        performSearch(activeQuery)
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Unable to scan device media"
                    _isLoadingLocal.value = false
                }
            )
        }
    }

    fun loadFeaturedSongs() {
        if (!networkUtils.isConnected) return
        viewModelScope.launch {
            _isLoadingFeatured.value = true
            getFeaturedSongs().fold(
                onSuccess = { songs ->
                    _featuredSongs.value = songs
                    _isLoadingFeatured.value = false
                },
                onFailure = { _isLoadingFeatured.value = false }
            )
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(400L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        return@collect
                    }
                    performSearch(query)
                }
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val localResult = searchLocalSongs(query)
            val onlineResult = if (networkUtils.isConnected) searchOnlineSongs(query)
                               else Result.success(emptyList())
            val combined = buildList {
                localResult.getOrNull()?.let { addAll(it) }
                onlineResult.getOrNull()?.let { addAll(it) }
            }
            _searchResults.value = combined
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val q = queue.ifEmpty { listOf(song) }
        val index = q.indexOf(song).coerceAtLeast(0)
        playerManager.playSong(song, q, index)
        viewModelScope.launch { recordPlay(song) }
    }

    fun playOrPause() = playerManager.playOrPause()
    fun skipToNext() = playerManager.skipToNext()
    fun skipToPrevious() = playerManager.skipToPrevious()
    fun seekTo(position: Long) = playerManager.seekTo(position)
    fun seekToFraction(fraction: Float) = playerManager.seekToFraction(fraction)

    fun toggleRepeat() {
        val next = when (playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerManager.setRepeatMode(next)
    }

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun playFromQueueIndex(index: Int) {
        val queue = playerState.value.queue
        if (index in queue.indices) playSong(queue[index], queue)
    }

    // ── Favorites ─────────────────────────────────────────────────────────────

    fun onToggleFavorite(song: Song) {
        viewModelScope.launch { toggleFavorite(song) }
    }

    fun isFavorite(songId: String): Boolean = favoriteIds.value.contains(songId)

    // ── Playlists ─────────────────────────────────────────────────────────────

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            val currentSize = repository.getPlaylistSongs(playlistId).first().size
            repository.addSongToPlaylist(playlistId, song.id, currentSize)
        }
    }

    // ── Genre ─────────────────────────────────────────────────────────────────

    fun loadGenreSongs(genre: String) {
        if (_selectedGenre.value == genre || !networkUtils.isConnected) return
        _selectedGenre.value = genre
        viewModelScope.launch {
            getSongsByGenre(genre).fold(
                onSuccess = { _genreSongs.value = it },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    // ── Error Handling ────────────────────────────────────────────────────────

    fun clearError() {
        _errorMessage.value = null
    }

    private fun observePlayerErrors() {
        viewModelScope.launch {
            playerManager.playerError.collect { error ->
                if (error != null) _errorMessage.value = error
            }
        }
    }

    // ── Queue management ──────────────────────────────────────────────────────

    fun removeFromQueue(index: Int) {
        val current = playerState.value
        val newQueue = current.queue.toMutableList().apply { removeAt(index) }
        // Rebuild ExoPlayer media items for updated queue
        if (newQueue.isEmpty()) return
        val newIndex = when {
            index < current.currentIndex -> current.currentIndex - 1
            index == current.currentIndex -> minOf(index, newQueue.lastIndex)
            else -> current.currentIndex
        }.coerceAtLeast(0)
        playerManager.playSong(newQueue[newIndex], newQueue, newIndex)
    }

    fun addToQueue(song: Song) {
        val current = playerState.value
        val newQueue = current.queue + song
        // Keep playing from current position — just update queue reference in player
        playerManager.playSong(
            song  = current.currentSong ?: song,
            queue = newQueue,
            startIndex = current.currentIndex
        )
    }

    fun onSongDeleted(song: Song) {
        playerManager.removeSongById(song.id)
        val activeQuery = _searchQuery.value.trim()
        if (activeQuery.isNotEmpty()) {
            performSearch(activeQuery)
        }
    }

    // ── Sleep timer ───────────────────────────────────────────────────────────

    private val _sleepTimerRemaining = MutableStateFlow<Int?>(null)
    val sleepTimerRemaining: StateFlow<Int?> = _sleepTimerRemaining.asStateFlow()

    private var sleepTimerJob: Job? = null

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60
            _sleepTimerRemaining.value = remaining
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                _sleepTimerRemaining.value = remaining
            }
            playerManager.playOrPause()
            _sleepTimerRemaining.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.releasePlayer()
    }
}
