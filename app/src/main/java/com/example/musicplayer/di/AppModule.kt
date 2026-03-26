package com.example.musicplayer.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import com.example.musicplayer.data.local.FavoriteDao
import com.example.musicplayer.data.local.MusicDatabase
import com.example.musicplayer.data.local.PlaylistDao
import com.example.musicplayer.data.local.RecentlyPlayedDao
import com.example.musicplayer.data.local.YouTubeSavedVideoDao
import com.example.musicplayer.data.remote.JamendoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Network ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(JamendoApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideJamendoApiService(retrofit: Retrofit): JamendoApiService =
        retrofit.create(JamendoApiService::class.java)

    // ─── Database ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase =
        Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_player_db"
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideFavoriteDao(db: MusicDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideYouTubeSavedVideoDao(db: MusicDatabase): YouTubeSavedVideoDao = db.youTubeSavedVideoDao()

    @Provides
    fun provideRecentlyPlayedDao(db: MusicDatabase): RecentlyPlayedDao = db.recentlyPlayedDao()

    @Provides
    fun providePlaylistDao(db: MusicDatabase): PlaylistDao = db.playlistDao()

    // ─── YouTube API ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideYouTubeApiService(okHttpClient: OkHttpClient): com.example.musicplayer.youtube.api.YouTubeApiService {
        val youtubeRetrofit = Retrofit.Builder()
            .baseUrl(com.example.musicplayer.youtube.api.YouTubeApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return youtubeRetrofit.create(com.example.musicplayer.youtube.api.YouTubeApiService::class.java)
    }

    // ─── ExoPlayer ────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
