package com.example.musicplayer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "music_player_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LAST_SONG_ID      = stringPreferencesKey("last_song_id")
        val LAST_POSITION     = longPreferencesKey("last_position_ms")
        val SHUFFLE_ENABLED   = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE       = intPreferencesKey("repeat_mode")       // 0=OFF, 1=ALL, 2=ONE
        val VOLUME_LEVEL      = floatPreferencesKey("volume_level")
        val SLEEP_TIMER_MINS  = intPreferencesKey("sleep_timer_minutes")
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    val lastSongId: Flow<String?> = context.dataStore.data
        .catchIOException()
        .map { it[Keys.LAST_SONG_ID] }

    val lastPosition: Flow<Long> = context.dataStore.data
        .catchIOException()
        .map { it[Keys.LAST_POSITION] ?: 0L }

    val shuffleEnabled: Flow<Boolean> = context.dataStore.data
        .catchIOException()
        .map { it[Keys.SHUFFLE_ENABLED] ?: false }

    val repeatMode: Flow<Int> = context.dataStore.data
        .catchIOException()
        .map { it[Keys.REPEAT_MODE] ?: 0 }

    val volumeLevel: Flow<Float> = context.dataStore.data
        .catchIOException()
        .map { it[Keys.VOLUME_LEVEL] ?: 1.0f }

    // ─── Write ────────────────────────────────────────────────────────────────

    suspend fun saveLastSong(songId: String, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SONG_ID]  = songId
            prefs[Keys.LAST_POSITION] = positionMs
        }
    }

    suspend fun saveShuffle(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }

    suspend fun saveRepeatMode(mode: Int) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode }
    }

    suspend fun saveVolumeLevel(level: Float) {
        context.dataStore.edit { it[Keys.VOLUME_LEVEL] = level }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun <T> Flow<T>.catchIOException(): Flow<T> =
        catch { /* Emit nothing on IO errors — values will default */ }
}
