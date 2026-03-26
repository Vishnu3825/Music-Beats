package com.example.musicplayer.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.musicplayer.data.model.Song
import com.example.musicplayer.data.model.SongSource

object RingtoneUtils {
    fun canWriteSystemSettings(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)
    }

    fun createManageWriteSettingsIntent(context: Context): android.content.Intent {
        return android.content.Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun setAsRingtone(context: Context, song: Song): Result<Unit> = runCatching {
        require(song.source == SongSource.LOCAL) { "Only local audio files can be set as ringtone." }
        require(canWriteSystemSettings(context)) { "Allow modify system settings to set a ringtone." }

        val ringtoneUri = Uri.parse(song.uri)
        RingtoneManager.setActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE,
            ringtoneUri
        )
    }
}
