package com.example.musicplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Duration Formatting ─────────────────────────────────────────────────────

fun Long.toFormattedDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun Long.toMinuteSecond(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

fun Int.toFormattedSeconds(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

// ─── File Size Formatting ─────────────────────────────────────────────────────

fun Long.toReadableFileSize(): String {
    return when {
        this >= 1_000_000 -> "%.1f MB".format(this / 1_000_000.0)
        this >= 1_000     -> "%.1f KB".format(this / 1_000.0)
        else              -> "$this B"
    }
}

// ─── Palette Extraction ───────────────────────────────────────────────────────

suspend fun extractPaletteColors(
    context: Context,
    artUri: String?
): Pair<Color, Color> = withContext(Dispatchers.IO) {
    if (artUri == null) return@withContext Pair(
        Color(0xFF1A1A2E),
        Color(0xFF1DB954)
    )
    try {
        val bitmap: Bitmap? = context.contentResolver.openInputStream(
            Uri.parse(artUri)
        )?.use { BitmapFactory.decodeStream(it) }

        bitmap?.let { bmp ->
            val palette = Palette.from(bmp).generate()
            val dominantSwatch = palette.dominantSwatch
                ?: palette.darkMutedSwatch
                ?: palette.darkVibrantSwatch

            val bgColor = dominantSwatch?.rgb?.let { Color(it).copy(alpha = 1f) }
                ?: Color(0xFF1A1A2E)
            val accentColor = (palette.vibrantSwatch ?: palette.lightVibrantSwatch)
                ?.rgb?.let { Color(it) }
                ?: Color(0xFF1DB954)

            Pair(bgColor, accentColor)
        } ?: Pair(Color(0xFF1A1A2E), Color(0xFF1DB954))
    } catch (e: Exception) {
        Pair(Color(0xFF1A1A2E), Color(0xFF1DB954))
    }
}

// ─── String Utilities ─────────────────────────────────────────────────────────

fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }

fun String.truncate(maxLength: Int, ellipsis: String = "…"): String =
    if (length <= maxLength) this else take(maxLength) + ellipsis

// ─── Color Utilities ──────────────────────────────────────────────────────────

fun Color.darken(factor: Float = 0.4f): Color {
    return Color(
        red = (red * (1f - factor)).coerceIn(0f, 1f),
        green = (green * (1f - factor)).coerceIn(0f, 1f),
        blue = (blue * (1f - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

fun Color.lighten(factor: Float = 0.2f): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}
