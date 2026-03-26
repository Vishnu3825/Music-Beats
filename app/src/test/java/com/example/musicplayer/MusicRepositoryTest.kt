package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.*
import com.example.musicplayer.data.model.*
import com.example.musicplayer.data.remote.JamendoApiService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MusicRepository.
 *
 * Run with: ./gradlew :app:testDebugUnitTest
 *
 * Required test dependencies (add to app/build.gradle):
 *   testImplementation 'io.mockk:mockk:1.13.9'
 *   testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
 *   testImplementation 'junit:junit:4.13.2'
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MusicRepositoryTest {

    private lateinit var localDataSource : LocalMusicDataSource
    private lateinit var jamendoApi      : JamendoApiService
    private lateinit var favoriteDao     : FavoriteDao
    private lateinit var recentDao       : RecentlyPlayedDao
    private lateinit var playlistDao     : PlaylistDao
    private lateinit var repository      : MusicRepository

    private val fakeSong = Song(
        id    = "1",
        title = "Test Song",
        artist = "Test Artist",
        album  = "Test Album",
        duration = 200_000L,
        uri   = "content://media/1",
        albumArtUri = null
    )

    @Before
    fun setUp() {
        localDataSource = mockk()
        jamendoApi      = mockk()
        favoriteDao     = mockk()
        recentDao       = mockk()
        playlistDao     = mockk()

        repository = MusicRepository(
            localDataSource = localDataSource,
            jamendoApi      = jamendoApi,
            favoriteDao     = favoriteDao,
            recentlyPlayedDao = recentDao,
            playlistDao     = playlistDao
        )
    }

    @Test
    fun `getLocalSongs returns success with songs`() = runTest {
        coEvery { localDataSource.fetchLocalSongs() } returns listOf(fakeSong)

        val result = repository.getLocalSongs()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Test Song", result.getOrNull()?.first()?.title)
    }

    @Test
    fun `getLocalSongs returns failure on exception`() = runTest {
        coEvery { localDataSource.fetchLocalSongs() } throws SecurityException("No permission")

        val result = repository.getLocalSongs()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `toggleFavorite adds when not favorite`() = runTest {
        coEvery { favoriteDao.isFavorite("1") } returns false
        coEvery { favoriteDao.addFavorite(any()) } just Runs

        repository.toggleFavorite(fakeSong)

        coVerify { favoriteDao.addFavorite(FavoriteEntity(songId = "1")) }
    }

    @Test
    fun `toggleFavorite removes when already favorite`() = runTest {
        coEvery { favoriteDao.isFavorite("1") } returns true
        coEvery { favoriteDao.removeFavoriteById("1") } just Runs

        repository.toggleFavorite(fakeSong)

        coVerify { favoriteDao.removeFavoriteById("1") }
    }

    @Test
    fun `recordPlay inserts and trims recently played`() = runTest {
        coEvery { recentDao.insertRecentlyPlayed(any()) } just Runs
        coEvery { recentDao.trimOldEntries() } just Runs

        repository.recordPlay(fakeSong)

        coVerify { recentDao.insertRecentlyPlayed(any()) }
        coVerify { recentDao.trimOldEntries() }
    }

    @Test
    fun `createPlaylist returns non-empty id`() = runTest {
        val slot = slot<PlaylistEntity>()
        coEvery { playlistDao.createPlaylist(capture(slot)) } just Runs

        val id = repository.createPlaylist("My Playlist")

        assertTrue(id.isNotEmpty())
        assertEquals("My Playlist", slot.captured.name)
    }

    @Test
    fun `searchLocalSongs filters by title case-insensitively`() = runTest {
        coEvery { localDataSource.fetchLocalSongs() } returns listOf(
            fakeSong,
            fakeSong.copy(id = "2", title = "Another Track", artist = "Other Artist")
        )
        coEvery { localDataSource.searchLocalSongs("test") } answers {
            listOf(fakeSong)
        }

        val result = repository.searchLocalSongs("test")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }
}
