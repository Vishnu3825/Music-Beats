package com.example.musicplayer.util

import org.junit.Assert.*
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `toFormattedDuration formats hours correctly`() {
        val ms = 3_661_000L   // 1h 1m 1s
        assertEquals("1:01:01", ms.toFormattedDuration())
    }

    @Test
    fun `toFormattedDuration formats minutes and seconds`() {
        val ms = 185_000L    // 3m 5s
        assertEquals("3:05", ms.toFormattedDuration())
    }

    @Test
    fun `toFormattedDuration formats zero`() {
        assertEquals("0:00", 0L.toFormattedDuration())
    }

    @Test
    fun `toMinuteSecond pads single-digit seconds`() {
        val ms = 61_000L
        assertEquals("1:01", ms.toMinuteSecond())
    }

    @Test
    fun `toReadableFileSize formats megabytes`() {
        val bytes = 3_200_000L
        assertTrue(3_200_000L.toReadableFileSize().contains("MB"))
    }

    @Test
    fun `toReadableFileSize formats kilobytes`() {
        val bytes = 512_000L
        assertTrue(bytes.toReadableFileSize().contains("KB"))
    }

    @Test
    fun `capitalizeWords capitalizes each word`() {
        assertEquals("Hello World", "hello world".capitalizeWords())
    }

    @Test
    fun `truncate shortens long strings`() {
        val long = "This is a very long string"
        val result = long.truncate(10)
        assertTrue(result.length <= 11) // 10 chars + ellipsis
        assertTrue(result.endsWith("…"))
    }

    @Test
    fun `truncate leaves short strings unchanged`() {
        val short = "Hi"
        assertEquals("Hi", short.truncate(10))
    }
}
