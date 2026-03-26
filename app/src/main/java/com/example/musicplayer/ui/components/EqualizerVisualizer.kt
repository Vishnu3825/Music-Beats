package com.example.musicplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.ElectricBlue
import com.example.musicplayer.ui.theme.NeonGreen

/**
 * Animated equalizer visualizer with horizontal motion.
 * Each segment grows and shrinks in width while staying level vertically.
 *
 * @param isPlaying   segments animate only when playing
 * @param barCount    number of horizontal segments
 * @param barWidth    minimum width of each segment
 * @param maxHeight   segment height
 * @param color       bar gradient start color
 */
@Composable
fun EqualizerVisualizer(
    isPlaying : Boolean,
    modifier  : Modifier = Modifier,
    barCount  : Int      = 28,
    barWidth  : Dp       = 4.dp,
    maxHeight : Dp       = 48.dp,
    color     : Color    = NeonGreen
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            AnimatedBar(
                isPlaying = isPlaying,
                index     = index,
                barCount  = barCount,
                barWidth  = barWidth,
                barHeight = maxHeight / 5,
                color     = color
            )
        }
    }
}

@Composable
private fun AnimatedBar(
    isPlaying : Boolean,
    index     : Int,
    barCount  : Int,
    barWidth  : Dp,
    barHeight : Dp,
    color     : Color
) {
    val phaseMs = (index * (900 / barCount)).toLong()
    val minFraction = 0.45f + (index % 4) * 0.06f
    val maxFraction = 1.2f + (index % 5) * 0.18f

    val infiniteTransition = rememberInfiniteTransition(label = "bar$index")
    val fraction by infiniteTransition.animateFloat(
        initialValue = minFraction,
        targetValue  = maxFraction,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 700 + (index % 5) * 80
                minFraction at 0
                maxFraction at durationMillis / 2
                minFraction at durationMillis
            },
            repeatMode          = RepeatMode.Restart,
            initialStartOffset  = StartOffset(phaseMs.toInt())
        ),
        label = "barFraction$index"
    )

    val widthMultiplier = if (isPlaying) fraction else 0.75f

    val gradient = Brush.horizontalGradient(
        colors = listOf(ElectricBlue.copy(alpha = 0.7f), color)
    )

    Box(
        modifier = Modifier
            .width(barWidth * widthMultiplier)
            .height(barHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(gradient)
    )
}

/**
 * Compact 5-bar version used in SongItem when that track is playing.
 */
@Composable
fun SmallEqualizerBars(
    isPlaying : Boolean,
    modifier  : Modifier = Modifier,
    color     : Color    = NeonGreen
) {
    EqualizerVisualizer(
        isPlaying = isPlaying,
        modifier  = modifier,
        barCount  = 5,
        barWidth  = 3.dp,
        maxHeight = 14.dp,
        color     = color
    )
}
