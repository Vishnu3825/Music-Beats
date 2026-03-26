package com.example.musicplayer.util

import com.example.musicplayer.player.MusicPlayerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor(
    private val playerManager: MusicPlayerManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()

    val isRunning: Boolean get() = timerJob?.isActive == true

    /**
     * Start a sleep timer. After [minutes] minutes, playback will pause.
     */
    fun start(minutes: Int) {
        timerJob?.cancel()
        timerJob = scope.launch {
            var remaining = minutes * 60
            _remainingSeconds.value = remaining
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                _remainingSeconds.value = remaining
            }
            playerManager.playOrPause()   // pause when timer fires
            _remainingSeconds.value = null
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = null
    }

    /** Formatted MM:SS string for display */
    fun format(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
