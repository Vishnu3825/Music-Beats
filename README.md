# 🎵 Music Player — Android (Kotlin + Jetpack Compose)

A production-grade hybrid music player with offline playback and Jamendo streaming,
built with MVVM, Hilt, ExoPlayer/Media3, Room, and Material 3.

---

## Architecture

```
com.example.musicplayer
├── MainActivity.kt              ← App entry, navigation host, overlay management
├── MusicPlayerApp.kt            ← Hilt Application class, notification channels
│
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt        ← Greeting, recently played, genres, library list
│   │   ├── PlayerScreen.kt      ← Full-screen player with palette, tabs, controls
│   │   ├── SearchScreen.kt      ← Debounced search: local + Jamendo results
│   │   ├── LibraryScreen.kt     ← Favorites / Playlists / Albums tabs
│   │   ├── QueueScreen.kt       ← Current playback queue
│   │   ├── PermissionScreen.kt  ← Animated permission request
│   │   └── SettingsScreen.kt    ← Playback & streaming preferences
│   │
│   ├── components/
│   │   ├── SongItem.kt          ← List row: art, info, favorite, options
│   │   ├── MiniPlayer.kt        ← Persistent bottom bar with rotating art
│   │   ├── PlayerControls.kt    ← Seek bar + transport controls
│   │   ├── GenreCard.kt         ← Genre chips + featured song cards
│   │   ├── SongOptionsBottomSheet.kt ← Add to queue/playlist, share, etc.
│   │   └── SleepTimerDialog.kt  ← Sleep timer picker
│   │
│   └── theme/
│       ├── Color.kt             ← Dark palette (NeonGreen, SurfaceDeep…)
│       ├── Typography.kt        ← Type scale
│       └── Theme.kt             ← MaterialTheme wiring
│
├── data/
│   ├── model/Song.kt            ← Song, PlayerState, RepeatMode, DTOs
│   ├── local/
│   │   ├── MusicDatabase.kt     ← Room DB: favorites, recently played, playlists
│   │   ├── LocalMusicDataSource.kt ← MediaStore query
│   │   └── UserPreferencesDataStore.kt ← DataStore for settings persistence
│   └── remote/
│       └── JamendoApiService.kt ← Retrofit Jamendo API
│
├── data/repository/
│   └── MusicRepository.kt       ← Single source of truth
│
├── domain/usecase/
│   └── UseCases.kt              ← GetLocalSongs, Search, Toggle Favorite, etc.
│
├── player/
│   └── MusicPlayerManager.kt    ← ExoPlayer wrapper, StateFlow output
│
├── service/
│   └── MusicPlaybackService.kt  ← MediaSessionService for background playback
│
├── viewmodel/
│   └── MusicViewModel.kt        ← All StateFlows, user actions, network awareness
│
├── di/
│   └── AppModule.kt             ← Hilt providers: Retrofit, Room, ExoPlayer
│
└── util/
    ├── Extensions.kt            ← Duration formatting, palette, color utils
    ├── NetworkUtils.kt          ← Connectivity Flow
    ├── NotificationHelper.kt    ← Notification channel setup
    └── SleepTimerManager.kt     ← Countdown + auto-pause
```

---

## Setup

### 1. Clone & open in Android Studio

```bash
git clone <your-repo>
```

Open the `MusicPlayer/` folder in **Android Studio Hedgehog** or later.

### 2. Get a Jamendo API key (free)

1. Register at [https://developer.jamendo.com](https://developer.jamendo.com)
2. Create an app → copy the **Client ID**
3. Open `JamendoApiService.kt` and replace:

```kotlin
const val JAMENDO_CLIENT_ID = "YOUR_JAMENDO_CLIENT_ID"
```

### 3. Build & run

- Target: Android 8.0+ (API 26+)
- Connect a device or start an emulator
- Press **Run ▶**

---

## Permissions

| Permission | Purpose |
|---|---|
| `READ_MEDIA_AUDIO` (API 33+) | Access local audio files |
| `READ_EXTERNAL_STORAGE` (API ≤ 32) | Access local audio files |
| `INTERNET` | Stream from Jamendo |
| `FOREGROUND_SERVICE` | Background playback |
| `POST_NOTIFICATIONS` | Media notification (API 33+) |

---

## Key Libraries

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.02.00 | UI |
| Media3 ExoPlayer | 1.2.1 | Playback engine |
| Hilt | 2.50 | Dependency injection |
| Room | 2.6.1 | Local database |
| Retrofit | 2.9.0 | Jamendo HTTP API |
| Coil | 2.6.0 | Image loading |
| DataStore | 1.0.0 | Settings persistence |
| Palette | 1.0.0 | Dynamic album art colors |

---

## Features

- ✅ Local music playback via MediaStore
- ✅ ExoPlayer with background service & media session
- ✅ Jamendo API streaming (search + featured + genre)
- ✅ Full-screen player with dynamic palette colors
- ✅ Animated mini player with rotating album art
- ✅ Seek bar, skip, shuffle, repeat
- ✅ Favorites (Room DB)
- ✅ Recently played (Room DB)
- ✅ Playlists (Room DB)
- ✅ Albums tab with expandable lists
- ✅ Genre browsing
- ✅ Debounced search (local + online, sectioned)
- ✅ Queue screen
- ✅ Sleep timer
- ✅ Settings screen
- ✅ Network awareness (offline snackbar)
- ✅ Permission screen
- ✅ Lyrics tab (placeholder — connect Musixmatch/Genius)

---

## Lyrics Integration (Optional)

To add lyrics, integrate the **Musixmatch API** or **Genius API**:

1. Add a `LyricsRepository` that fetches by `"$artist $title"`
2. Wire it into `MusicViewModel` as `lyrics: StateFlow<String>`
3. Render inside `LyricsTab()` in `PlayerScreen.kt` using a `LazyColumn`
   of timestamped lines with scroll-sync to `playerState.currentPosition`
