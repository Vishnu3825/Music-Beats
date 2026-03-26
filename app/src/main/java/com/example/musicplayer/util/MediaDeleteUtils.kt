package com.example.musicplayer.util

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.musicplayer.data.model.Song

object MediaDeleteUtils {

    fun createDeleteIntentSender(context: Context, song: Song): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uri = song.uri.toUriOrNull() ?: return null
        return MediaStore.createDeleteRequest(
            context.contentResolver,
            listOf(uri)
        ).intentSender
    }

    fun deleteDirectly(context: Context, song: Song): Result<Boolean> = runCatching {
        val uri = song.uri.toUriOrNull() ?: error("Invalid media file")
        context.contentResolver.delete(uri, null, null) > 0
    }

    private fun String.toUriOrNull(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
}
