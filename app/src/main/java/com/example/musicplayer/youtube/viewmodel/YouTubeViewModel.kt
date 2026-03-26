package com.example.musicplayer.youtube.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.youtube.api.YouTubeRepository
import com.example.musicplayer.youtube.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

enum class YouTubeRepeatMode {
    OFF, ALL, ONE
}

@OptIn(FlowPreview::class)
@HiltViewModel
class YouTubeViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {

    // ─── Currently playing ────────────────────────────────────────────────────
    private val _currentVideo = MutableStateFlow<YouTubeVideo?>(null)
    val currentVideo: StateFlow<YouTubeVideo?> = _currentVideo.asStateFlow()

    private val _showPlayer = MutableStateFlow(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    // ─── Trending / Home ──────────────────────────────────────────────────────
    private val _trendingVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val trendingVideos: StateFlow<List<YouTubeVideo>> = _trendingVideos.asStateFlow()

    private val _isLoadingTrending = MutableStateFlow(false)
    val isLoadingTrending: StateFlow<Boolean> = _isLoadingTrending.asStateFlow()

    // ─── Category videos ─────────────────────────────────────────────────────
    private val _categoryVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val categoryVideos: StateFlow<List<YouTubeVideo>> = _categoryVideos.asStateFlow()

    private val _selectedCategory = MutableStateFlow<MusicCategory?>(null)
    val selectedCategory: StateFlow<MusicCategory?> = _selectedCategory.asStateFlow()

    private val _isLoadingCategory = MutableStateFlow(false)
    val isLoadingCategory: StateFlow<Boolean> = _isLoadingCategory.asStateFlow()
    private val categoryRefreshCounts = mutableMapOf<String, Int>()

    // ─── Search ───────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val searchResults: StateFlow<List<YouTubeVideo>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _nextPageToken = MutableStateFlow<String?>(null)

    // ─── Favorites ────────────────────────────────────────────────────────────
    val favoriteIds: StateFlow<Set<String>> = repository.favoriteIds
    val savedVideos: StateFlow<List<YouTubeVideo>> = repository.savedVideos
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ─── Playlist (watch queue) ───────────────────────────────────────────────
    private val _queue = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val queue: StateFlow<List<YouTubeVideo>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _playbackPositionSeconds = MutableStateFlow(0f)
    val playbackPositionSeconds: StateFlow<Float> = _playbackPositionSeconds.asStateFlow()

    private val _playbackDurationSeconds = MutableStateFlow(0f)
    val playbackDurationSeconds: StateFlow<Float> = _playbackDurationSeconds.asStateFlow()

    private val _playbackCommandVersion = MutableStateFlow(0L)
    val playbackCommandVersion: StateFlow<Long> = _playbackCommandVersion.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(YouTubeRepeatMode.OFF)
    val repeatMode: StateFlow<YouTubeRepeatMode> = _repeatMode.asStateFlow()

    // ─── Error ────────────────────────────────────────────────────────────────
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSearchQuery()
    }

    // ─── Load trending ────────────────────────────────────────────────────────
    fun loadTrending() {
        viewModelScope.launch {
            _isLoadingTrending.value = true
            repository.getTrending().fold(
                onSuccess = { _trendingVideos.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoadingTrending.value = false
        }
    }

    fun playPersonMix(person: TeluguMusicPerson) {
        viewModelScope.launch {
            _isLoadingTrending.value = true
            repository.searchVideos(
                query = person.query,
                maxResults = 25
            ).fold(
                onSuccess = { (videos, _) ->
                    val queue = videos
                        .distinctBy { it.videoId }
                        .map { it.copy(isFavorite = repository.isFavorite(it.videoId)) }
                    _trendingVideos.value = queue
                    queue.firstOrNull()?.let { first ->
                        playVideo(first, queue)
                    }
                },
                onFailure = { _error.value = it.message }
            )
            _isLoadingTrending.value = false
        }
    }

    // ─── Load category ────────────────────────────────────────────────────────
    fun loadCategory(category: MusicCategory) {
        _selectedCategory.value = category
        _categoryVideos.value   = emptyList()
        viewModelScope.launch {
            _isLoadingCategory.value = true
            val refreshCount = categoryRefreshCounts[category.id] ?: 0
            val query = category.queryVariants[
                refreshCount % category.queryVariants.size
            ]
            categoryRefreshCounts[category.id] = refreshCount + 1

            repository.getCategoryVideos(query).fold(
                onSuccess = { videos ->
                    val shuffledVideos = videos
                        .distinctBy { it.videoId }
                        .shuffled()
                    _categoryVideos.value = shuffledVideos
                },
                onFailure = { _error.value = it.message }
            )
            _isLoadingCategory.value = false
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(450L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value  = emptyList()
                        _nextPageToken.value  = null
                        return@collect
                    }
                    performSearch(query, reset = true)
                }
        }
    }

    private fun performSearch(query: String, reset: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            repository.searchVideos(
                query     = query,
                pageToken = if (reset) null else _nextPageToken.value
            ).fold(
                onSuccess = { (videos, nextToken) ->
                    _searchResults.value = if (reset) videos
                                          else _searchResults.value + videos
                    _nextPageToken.value = nextToken
                },
                onFailure = { _error.value = it.message }
            )
            _isSearching.value = false
        }
    }

    fun loadMoreSearchResults() {
        val q = _searchQuery.value
        if (q.isNotBlank() && _nextPageToken.value != null) {
            performSearch(q, reset = false)
        }
    }

    fun clearSearch() {
        _searchQuery.value   = ""
        _searchResults.value = emptyList()
    }

    // ─── Playback ─────────────────────────────────────────────────────────────
    fun playVideo(video: YouTubeVideo, queue: List<YouTubeVideo> = emptyList()) {
        val q     = queue.ifEmpty { listOf(video) }
        val index = q.indexOfFirst { it.videoId == video.videoId }.coerceAtLeast(0)
        _queue.value      = q
        _queueIndex.value = index
        _currentVideo.value = video
        _playbackPositionSeconds.value = 0f
        _playbackDurationSeconds.value = 0f
        _playbackCommandVersion.value += 1
        _isMinimized.value = false
        _showPlayer.value   = true
    }

    fun playNext() {
        resolveNextIndex()?.let { next ->
            _queueIndex.value = next
            _currentVideo.value = _queue.value.getOrNull(next)
            _playbackPositionSeconds.value = 0f
            _playbackCommandVersion.value += 1
        }
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        val prev = when {
            _repeatMode.value == YouTubeRepeatMode.ONE -> _queueIndex.value
            _queueIndex.value > 0 -> _queueIndex.value - 1
            _repeatMode.value == YouTubeRepeatMode.ALL -> q.lastIndex
            else -> return
        }

        _queueIndex.value = prev
        _currentVideo.value = q[prev]
        _playbackPositionSeconds.value = 0f
        _playbackCommandVersion.value += 1
    }

    fun closePlayer() {
        _isMinimized.value = false
        _showPlayer.value = false
    }

    fun minimizePlayer() {
        if (_currentVideo.value != null) {
            _isMinimized.value = true
            _showPlayer.value = true
        }
    }

    fun expandPlayer() {
        if (_currentVideo.value != null) {
            _isMinimized.value = false
            _showPlayer.value = true
        }
    }

    fun addToQueue(video: YouTubeVideo) {
        _queue.value = _queue.value + video
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            YouTubeRepeatMode.OFF -> YouTubeRepeatMode.ALL
            YouTubeRepeatMode.ALL -> YouTubeRepeatMode.ONE
            YouTubeRepeatMode.ONE -> YouTubeRepeatMode.OFF
        }
    }

    fun onPlaybackProgress(videoId: String, currentTime: Float, duration: Float) {
        if (_currentVideo.value?.videoId != videoId) return
        _playbackPositionSeconds.value = currentTime
        if (duration > 0f) {
            _playbackDurationSeconds.value = duration
        }
    }

    fun onVideoChanged(videoId: String, queueIndex: Int) {
        val q = _queue.value
        val resolvedIndex = when {
            queueIndex in q.indices && q[queueIndex].videoId == videoId -> queueIndex
            else -> q.indexOfFirst { it.videoId == videoId }
        }
        if (resolvedIndex !in q.indices) return

        _queueIndex.value = resolvedIndex
        _currentVideo.value = q[resolvedIndex]
        _playbackPositionSeconds.value = 0f
        _playbackDurationSeconds.value = 0f
        _showPlayer.value = true
    }

    fun onQueueEnded() {
        if (_repeatMode.value == YouTubeRepeatMode.OFF) {
            _playbackPositionSeconds.value = 0f
        }
    }

    // ─── Favorites ────────────────────────────────────────────────────────────
    fun toggleFavorite(video: YouTubeVideo) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
            refreshFavoriteFlags()
        }
    }

    fun removeFavorite(videoId: String) {
        val knownVideo = sequenceOf(
            savedVideos.value,
            _trendingVideos.value,
            _categoryVideos.value,
            _searchResults.value,
            _queue.value
        ).flatten().firstOrNull { it.videoId == videoId } ?: return

        viewModelScope.launch {
            repository.toggleFavorite(knownVideo)
            refreshFavoriteFlags()
        }
    }

    fun clearError() { _error.value = null }

    private fun refreshFavoriteFlags() {
        _trendingVideos.value = _trendingVideos.value.map {
            it.copy(isFavorite = repository.isFavorite(it.videoId))
        }
        _categoryVideos.value = _categoryVideos.value.map {
            it.copy(isFavorite = repository.isFavorite(it.videoId))
        }
        _searchResults.value = _searchResults.value.map {
            it.copy(isFavorite = repository.isFavorite(it.videoId))
        }
        _queue.value = _queue.value.map {
            it.copy(isFavorite = repository.isFavorite(it.videoId))
        }
        _currentVideo.value = _currentVideo.value?.copy(
            isFavorite = _currentVideo.value?.let { repository.isFavorite(it.videoId) } == true
        )
    }

    fun reportPlaybackError(code: Int) {
        _error.value = when (code) {
            -3 -> "YouTube player API did not initialize in time. Using fallback player."
            -2 -> "The embedded YouTube page failed to initialize."
            -1 -> "The embedded YouTube page failed to load."
            2 -> "This YouTube video has an invalid playback ID."
            5 -> "This YouTube video can't be played in the embedded player."
            100 -> "This YouTube video is unavailable."
            101, 150 -> "This YouTube video doesn't allow embedded playback."
            else -> "Unable to play this YouTube video right now."
        }
    }

    private fun resolveNextIndex(): Int? {
        val q = _queue.value
        if (q.isEmpty()) return null

        if (_repeatMode.value == YouTubeRepeatMode.ONE) {
            return _queueIndex.value.coerceIn(q.indices)
        }

        if (_shuffleEnabled.value && q.size > 1) {
            val current = _queueIndex.value.coerceIn(q.indices)
            val candidates = q.indices.filter { it != current }
            if (candidates.isNotEmpty()) {
                return candidates[Random.nextInt(candidates.size)]
            }
        }

        val next = _queueIndex.value + 1
        return when {
            next < q.size -> next
            _repeatMode.value == YouTubeRepeatMode.ALL -> 0
            else -> null
        }
    }
}
