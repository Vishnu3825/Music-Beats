package com.example.musicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.musicplayer.data.model.LocalScanSummary
import com.example.musicplayer.ui.theme.*

@Composable
fun SettingsScreen(
    onScanDeviceMedia: () -> Unit = {},
    isScanningMedia: Boolean = false,
    scanSummary: LocalScanSummary? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .background(SurfaceMid)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        HorizontalDivider(color = Divider)

        // ── Playback ──────────────────────────────────────────────────────────
        SettingsSectionTitle("Playback")

        var crossfadeEnabled by remember { mutableStateOf(false) }
        var gaplessEnabled   by remember { mutableStateOf(true) }
        var loudnessNorm     by remember { mutableStateOf(true) }

        SettingsToggleItem(
            icon    = Icons.Default.Tune,
            title   = "Crossfade",
            subtitle = "Fade between tracks",
            checked = crossfadeEnabled,
            onCheckedChange = { crossfadeEnabled = it }
        )
        SettingsToggleItem(
            icon    = Icons.Default.GraphicEq,
            title   = "Gapless Playback",
            subtitle = "Remove silence between tracks",
            checked = gaplessEnabled,
            onCheckedChange = { gaplessEnabled = it }
        )
        SettingsToggleItem(
            icon    = Icons.Default.VolumeUp,
            title   = "Loudness Normalization",
            subtitle = "Balance volume across tracks",
            checked = loudnessNorm,
            onCheckedChange = { loudnessNorm = it }
        )

        // ── Streaming ─────────────────────────────────────────────────────────
        SettingsSectionTitle("Streaming")

        var streamOnWifiOnly by remember { mutableStateOf(false) }
        var highQualityStream by remember { mutableStateOf(true) }

        SettingsToggleItem(
            icon    = Icons.Default.Wifi,
            title   = "Stream on Wi-Fi Only",
            subtitle = "Disable streaming on mobile data",
            checked = streamOnWifiOnly,
            onCheckedChange = { streamOnWifiOnly = it }
        )
        SettingsToggleItem(
            icon    = Icons.Default.HighQuality,
            title   = "High Quality Streaming",
            subtitle = "320kbps when available",
            checked = highQualityStream,
            onCheckedChange = { highQualityStream = it }
        )

        // ── Library ───────────────────────────────────────────────────────────
        SettingsSectionTitle("Library")

        SettingsActionItem(
            icon = Icons.Default.Sync,
            title = "Scan Device Media",
            subtitle = "Refresh local songs from this device",
            actionLabel = if (isScanningMedia) "Scanning..." else "Scan",
            onClick = onScanDeviceMedia
        )
        scanSummary?.let { summary ->
            ScanSummaryCard(summary = summary)
        }

        // ── About ─────────────────────────────────────────────────────────────
        SettingsSectionTitle("About")

        SettingsInfoItem(
            icon  = Icons.Default.Info,
            title = "Version",
            value = "1.0.0"
        )
        SettingsInfoItem(
            icon  = Icons.Default.MusicNote,
            title = "Online Music",
            value = "Powered by Jamendo"
        )
        SettingsInfoItem(
            icon  = Icons.Default.Code,
            title = "Playback Engine",
            value = "ExoPlayer / Media3"
        )

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ScanSummaryCard(summary: LocalScanSummary) {
    Surface(
        color = SurfaceLight,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Last Scan", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text("Previous Songs: ${summary.previousSongs}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Text("Total Songs: ${summary.totalSongs}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Text("Newly Added Songs: ${summary.newlyAddedSongs}", color = NeonGreen, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ─── Shared Setting Item Components ──────────────────────────────────────────

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text    = title.uppercase(),
        style   = MaterialTheme.typography.labelSmall,
        color   = NeonGreen,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color  = SurfaceLight,
            shape  = RoundedCornerShape(10.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = androidx.compose.ui.graphics.Color.Black,
                checkedTrackColor  = NeonGreen,
                uncheckedThumbColor = TextHint,
                uncheckedTrackColor = SurfaceLight
            )
        )
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color  = SurfaceLight,
            shape  = RoundedCornerShape(10.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = SurfaceLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        FilledTonalButton(onClick = onClick) {
            Text(actionLabel)
        }
    }
}
