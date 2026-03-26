package com.example.musicplayer.player

/**
 * Audio focus and noisy-audio handling notes.
 *
 * ExoPlayer / Media3 handles both automatically when built with:
 *
 *   ExoPlayer.Builder(context)
 *       .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
 *       .setHandleAudioBecomingNoisy(true)  // pause on headphone unplug
 *       .build()
 *
 * These flags are already set in AppModule.provideExoPlayer().
 *
 * Additional manual handling that may be required for complex scenarios:
 *
 * 1. Phone call interruption – Media3 SessionService handles this via
 *    AudioFocusRequest with AUDIOFOCUS_GAIN when the call ends.
 *
 * 2. Volume ducking – ExoPlayer reduces volume to 20% automatically during
 *    transient audio focus loss (e.g. navigation voice prompts) and
 *    restores it when focus returns.
 *
 * 3. Bluetooth A2DP / SCO – handled via system AudioManager; no extra code
 *    needed when using Media3.
 */

// ─── Equalizer / AudioEffect Setup (optional) ────────────────────────────────
//
// To attach a real hardware equalizer to the ExoPlayer audio session:
//
//   val audioSessionId = exoPlayer.audioSessionId
//   val equalizer = android.media.audiofx.Equalizer(0, audioSessionId)
//   equalizer.enabled = true
//
// Band indices and centre frequencies depend on device hardware.
// Query with: equalizer.numberOfBands, equalizer.getCenterFreq(band)
//
// See android.media.audiofx.Equalizer for full API documentation.
