package com.example.musicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.*

@Composable
fun SleepTimerDialog(
    remainingSeconds: Int?,
    isRunning: Boolean,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 20, 30, 45, 60)
    var selectedMinutes by remember { mutableIntStateOf(15) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceMid,
        icon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text("Sleep Timer", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isRunning && remainingSeconds != null) {
                    // Active timer display
                    Surface(
                        color = NeonGreen.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Stops in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                formatTime(remainingSeconds),
                                style = MaterialTheme.typography.headlineSmall,
                                color = NeonGreen
                            )
                        }
                    }
                }

                Text(
                    "Stop playback after:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // Minute selector chips
                val rows = options.chunked(4)
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { minutes ->
                            FilterChip(
                                selected = selectedMinutes == minutes,
                                onClick  = { selectedMinutes = minutes },
                                label    = { Text("${minutes}m") },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor     = NeonGreen,
                                    selectedLabelColor         = androidx.compose.ui.graphics.Color.Black,
                                    containerColor             = SurfaceLight,
                                    labelColor                 = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(selectedMinutes); onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor   = androidx.compose.ui.graphics.Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Start Timer")
            }
        },
        dismissButton = {
            if (isRunning) {
                TextButton(onClick = { onCancel(); onDismiss() }) {
                    Icon(
                        Icons.Default.TimerOff,
                        null,
                        tint = AccentPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel Timer", color = AccentPink)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            }
        }
    )
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
