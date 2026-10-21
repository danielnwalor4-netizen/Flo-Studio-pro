package com.example.flo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flo.audio.PlaybackState
import com.example.flo.ui.theme.FloBorder
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloGreenPlay
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloRedStop
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary

@Composable
fun TransportBar(
    playbackState: PlaybackState,
    currentBeat: Float,
    bpm: Int,
    vuMeterLevel: Float,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onMasterVolumeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var masterVol by remember { mutableFloatStateOf(0.9f) }

    // Time calculations
    // 1 beat = 60.0 / bpm seconds
    val secondsElapsed = currentBeat * (60.0 / bpm)
    val minutes = (secondsElapsed / 60).toInt()
    val seconds = (secondsElapsed % 60).toInt()
    val millis = ((secondsElapsed - secondsElapsed.toInt()) * 1000).toInt()
    val timecode = String.format("%02d:%02d:%03d", minutes, seconds, millis)

    val currentBar = (currentBeat / 4f).toInt() + 1
    val currentQuarterInBar = (currentBeat % 4f).toInt() + 1

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(FloDarkSurface)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Transport Action Buttons (Play / Pause / Stop)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play / Pause Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FloPurplePrimary)
                    .clickable {
                        if (playbackState == PlaybackState.PLAYING) onPause() else onPlay()
                    }
                    .testTag("transport_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState == PlaybackState.PLAYING) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Stop Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FloDarkCard)
                    .clickable { onStop() }
                    .testTag("transport_stop_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = if (playbackState != PlaybackState.STOPPED) FloRedStop else FloTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Timecode & Bar readout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(FloDarkCard)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Digital Timecode
            Text(
                text = timecode,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = FloCyanAccent
            )
            // Bar / Beat counter
            Text(
                text = "BAR $currentBar.$currentQuarterInBar",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FloTextSecondary
            )
        }

        // Tempo & Signature Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "$bpm BPM",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FloTextPrimary
            )
            Text(
                text = "4/4",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FloTextSecondary
            )
        }

        // Master Volume & VU Meter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(180.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Master Volume",
                tint = FloTextSecondary,
                modifier = Modifier.size(16.dp)
            )

            Slider(
                value = masterVol,
                onValueChange = {
                    masterVol = it
                    onMasterVolumeChanged(it)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = FloPurplePrimary,
                    activeTrackColor = FloPurplePrimary,
                    inactiveTrackColor = FloDarkCard
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("master_volume_slider")
            )

            // Multi-segment LED VU Meter
            VuMeter(level = vuMeterLevel)
        }
    }
}

@Composable
fun VuMeter(level: Float, modifier: Modifier = Modifier) {
    // 6-segment LED level meter
    val segmentCount = 6
    Row(
        modifier = modifier
            .width(28.dp)
            .height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until segmentCount) {
            val threshold = (i + 1).toFloat() / segmentCount
            val isActive = level >= (threshold - 0.1f)
            val segmentColor = when {
                i >= 5 -> if (isActive) Color(0xFFEF4444) else Color(0xFF451A1A) // Red (clipping)
                i >= 4 -> if (isActive) Color(0xFFF59E0B) else Color(0xFF45301A) // Yellow
                else -> if (isActive) Color(0xFF22C55E) else Color(0xFF133820)   // Green
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (i >= 5) 14.dp else 12.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(segmentColor)
            )
        }
    }
}
