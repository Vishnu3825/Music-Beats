package com.example.musicplayer.data.model

data class LocalScanSummary(
    val previousSongs: Int,
    val totalSongs: Int,
    val newlyAddedSongs: Int,
    val scannedAtMillis: Long = System.currentTimeMillis()
)
