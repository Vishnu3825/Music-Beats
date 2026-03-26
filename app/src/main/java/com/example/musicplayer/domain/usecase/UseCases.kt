package com.example.musicplayer.domain.usecase

import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.model.LocalScanSummary
import com.example.musicplayer.data.repository.MusicRepository
import javax.inject.Inject

class GetLocalSongs @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(): Result<List<Song>> = repository.getLocalSongs()
}

class ScanLocalSongs @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(): Result<Pair<List<Song>, LocalScanSummary>> =
        repository.scanLocalSongs()
}

class SearchOnlineSongs @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String): Result<List<Song>> =
        repository.searchOnlineSongs(query)
}

class SearchLocalSongs @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String): Result<List<Song>> =
        repository.searchLocalSongs(query)
}

class GetFeaturedSongs @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(): Result<List<Song>> = repository.getFeaturedSongs()
}

class ToggleFavorite @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(song: Song) = repository.toggleFavorite(song)
}

class RecordPlay @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(song: Song) = repository.recordPlay(song)
}

class GetSongsByGenre @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(genre: String): Result<List<Song>> =
        repository.getSongsByGenre(genre)
}
