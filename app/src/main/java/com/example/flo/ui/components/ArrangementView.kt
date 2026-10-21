package com.example.flo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flo.model.Note
import com.example.flo.model.Track
import com.example.flo.ui.theme.FloBorder
import com.example.flo.ui.theme.FloBorderSubtle
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloDarkSurfaceVariant
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloRedStop
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary

@Composable
fun ArrangementView(
    tracks: List<Track>,
    notes: List<Note>,
    currentBeat: Float,
    selectedTrackId: String,
    onTrackSelect: (String) -> Unit,
    onTrackMuteToggle: (String) -> Unit,
    onTrackSoloToggle: (String) -> Unit,
    onTrackVolumeChanged: (String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val totalBeats = maxOf(32f, (notes.maxOfOrNull { it.endBeat } ?: 16f) + 4f)
    val beatWidthDp = 24f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FloDarkSurface)
            .border(width = 1.dp, color = FloBorderSubtle)
    ) {
        // Arrangement Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(FloDarkSurfaceVariant)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = FloPurplePrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ARRANGEMENT & TRACKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = FloTextPrimary
            )
        }

        // Track Lanes
        for (track in tracks) {
            val trackNotes = notes.filter { it.trackId == track.id }
            val isSelected = track.id == selectedTrackId

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(if (isSelected) FloDarkCard else FloDarkSurface)
                    .border(width = 0.5.dp, color = FloBorderSubtle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Track Header (Name, Mute, Solo, Volume)
                Row(
                    modifier = Modifier
                        .width(180.dp)
                        .fillMaxHeight()
                        .background(FloDarkSurfaceVariant)
                        .clickable { onTrackSelect(track.id) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(track.colorHex))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = track.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) FloPurpleLight else FloTextPrimary,
                            maxLines = 1
                        )
                    }

                    // Mute / Solo Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MUTE
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (track.isMuted) FloRedStop else FloDarkCard)
                                .clickable { onTrackMuteToggle(track.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "M",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (track.isMuted) Color.White else FloTextSecondary
                            )
                        }

                        // SOLO
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (track.isSolo) FloCyanAccent else FloDarkCard)
                                .clickable { onTrackSoloToggle(track.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (track.isSolo) FloDarkSurface else FloTextSecondary
                            )
                        }
                    }
                }

                // Right: Timeline Clips
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                ) {
                    val trackLaneWidthDp = totalBeats * beatWidthDp

                    Canvas(
                        modifier = Modifier
                            .width(trackLaneWidthDp.dp)
                            .fillMaxHeight()
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Draw Bar Grid lines
                        val totalBars = (totalBeats / 4).toInt() + 1
                        for (bar in 0..totalBars) {
                            val x = bar * 4 * (canvasWidth / totalBeats)
                            drawLine(
                                color = FloBorderSubtle,
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Note clips in track color
                        val beatPx = canvasWidth / totalBeats
                        for (note in trackNotes) {
                            val clipX = note.startBeat * beatPx
                            val clipW = maxOf(4f, note.durationBeats * beatPx)
                            drawRoundRect(
                                color = Color(track.colorHex).copy(alpha = if (track.isMuted) 0.25f else 0.85f),
                                topLeft = Offset(clipX, 6f),
                                size = Size(clipW, canvasHeight - 12f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }

                        // Draw Playhead
                        val playheadX = currentBeat * beatPx
                        drawLine(
                            color = FloCyanAccent,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, canvasHeight),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }
    }
}
