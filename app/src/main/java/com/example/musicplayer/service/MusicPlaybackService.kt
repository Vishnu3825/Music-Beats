package com.example.musicplayer.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.example.musicplayer.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {
    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.musicplayer.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.musicplayer.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.musicplayer.action.PREVIOUS"
    }

    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        player.addListener(playerListener)

        startForeground(
            NotificationHelper.PLAYBACK_NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> if (player.isPlaying) player.pause() else player.play()
            ACTION_NEXT -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
            ACTION_PREVIOUS -> {
                if (player.currentPosition > 3_000L) {
                    player.seekTo(0)
                } else if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                }
            }
        }
        updateNotification()
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        player.removeListener(playerListener)
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private fun updateNotification() {
        NotificationManagerCompat.from(this).notify(
            NotificationHelper.PLAYBACK_NOTIFICATION_ID,
            buildNotification()
        )
    }

    private fun buildNotification(): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val previousIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val style = mediaSession?.let { session ->
            MediaStyle()
                .setMediaSession(session.sessionCompatToken)
                .setShowActionsInCompactView(0, 1, 2)
        }

        return NotificationCompat.Builder(this, NotificationHelper.PLAYBACK_CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon.takeIf { it != 0 } ?: R.mipmap.ic_launcher)
            .setContentTitle(player.mediaMetadata.title ?: getString(R.string.app_name))
            .setContentText(player.mediaMetadata.artist ?: getString(R.string.notification_playing_background))
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setWhen(0)
            .apply {
                style?.let { setStyle(it) }
            }
            .addAction(
                android.R.drawable.ic_media_previous,
                getString(R.string.previous),
                previousIntent
            )
            .addAction(
                if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (player.isPlaying) getString(R.string.pause) else getString(R.string.play),
                playPauseIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                getString(R.string.next),
                nextIntent
            )
            .setOngoing(player.isPlaying)
            .setAutoCancel(false)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateNotification()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateNotification()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateNotification()
        }
    }
}
