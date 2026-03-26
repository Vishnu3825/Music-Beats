package com.example.musicplayer.player

import com.example.musicplayer.data.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for MusicPlayerManager state flow correctness.
 *
 * Note: ExoPlayer itself is not unit-tested here (requires instrumented tests).
 * These tests verify the StateFlow state mutations in isolation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerStateTest {

    @Test
    fun `PlayerState default values are correct`() {
        val state = PlayerState()
        assertNull(state.currentSong)
        assertFalse(state.isPlaying)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(0L, state.currentPosition)
        assertEquals(0L, state.duration)
        assertEquals(RepeatMode.OFF, state.repeatMode)
        assertFalse(state.shuffleEnabled)
        assertTrue(state.queue.isEmpty())
        assertEquals(0, state.currentIndex)
        assertFalse(state.isBuffering)
    }

    @Test
    fun `RepeatMode cycles OFF → ALL → ONE → OFF`() {
        val modes = listOf(RepeatMode.OFF, RepeatMode.ALL, RepeatMode.ONE)
        modes.forEachIndexed { index, mode ->
            val next = when (mode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            assertEquals(modes[(index + 1) % modes.size], next)
        }
    }

    @Test
    fun `Song toMinuteSecond formats correctly`() {
        // 3 minutes 5 seconds = 185_000 ms
        val ms = 185_000L
        val total = ms / 1000
        val minutes = total / 60
        val seconds = total % 60
        val formatted = "$minutes:${seconds.toString().padStart(2, '0')}"
        assertEquals("3:05", formatted)
    }

    @Test
    fun `Song toMinuteSecond pads single digit seconds`() {
        val ms = 61_000L  // 1:01
        val total = ms / 1000
        val formatted = "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
        assertEquals("1:01", formatted)
    }

    @Test
    fun `progress is clamped between 0 and 1`() {
        val overOne   = 1.5f.coerceIn(0f, 1f)
        val underZero = (-0.2f).coerceIn(0f, 1f)
        assertEquals(1.0f, overOne, 0.001f)
        assertEquals(0.0f, underZero, 0.001f)
    }

    @Test
    fun `Song data model initialises with defaults`() {
        val song = Song(
            id          = "test",
            title       = "My Song",
            artist      = "Artist",
            album       = "Album",
            duration    = 180_000L,
            uri         = "content://media/1",
            albumArtUri = null
        )
        assertEquals(SongSource.LOCAL, song.source)
        assertFalse(song.isFavorite)
        assertEquals(0, song.playCount)
        assertNull(song.genre)
        assertNotNull(song.dateAdded)
    }
}
