package com.example.flo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flo.model.MusicalPitch
import com.example.flo.model.Note
import com.example.flo.ui.theme.FloBorder
import com.example.flo.ui.theme.FloBorderSubtle
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkBg
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloDarkSurfaceVariant
import com.example.flo.ui.theme.FloNoteBorder
import com.example.flo.ui.theme.FloNotePrimary
import com.example.flo.ui.theme.FloNoteSelected
import com.example.flo.ui.theme.FloPianoBarLine
import com.example.flo.ui.theme.FloPianoBlackKey
import com.example.flo.ui.theme.FloPianoGridLine
import com.example.flo.ui.theme.FloPianoWhiteKey
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloRedStop
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary
import kotlin.math.roundToInt

@Composable
fun PianoRollView(
    notes: List<Note>,
    selectedNoteId: String?,
    currentBeat: Float,
    snapBeats: Float,
    videoExportRatio: com.example.flo.model.VideoExportRatio = com.example.flo.model.VideoExportRatio.NONE,
    onNoteAdd: (pitch: Int, startBeat: Float, durationBeats: Float) -> Unit,
    onNoteSelect: (String?) -> Unit,
    onNoteMove: (noteId: String, newPitch: Int, newStartBeat: Float) -> Unit,
    onNoteResize: (noteId: String, newDurationBeats: Float) -> Unit,
    onNoteDelete: (noteId: String) -> Unit,
    onKeyPreviewOn: (pitch: Int) -> Unit,
    onKeyPreviewOff: (pitch: Int) -> Unit,
    onScrubPlayhead: (beat: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pitch range: C6 (84) down to C2 (36) -> 49 rows
    val topPitch = 84
    val bottomPitch = 36
    val pitches = (topPitch downTo bottomPitch).toList()

    val totalBeats = maxOf(32f, (notes.maxOfOrNull { it.endBeat } ?: 16f) + 4f)

    var beatWidthDp by remember { mutableFloatStateOf(48f) } // horizontal zoom
    val rowHeightDp = 22f                                    // vertical row height
    val keyboardWidthDp = 52f

    val density = LocalDensity.current
    val beatWidthPx = with(density) { beatWidthDp.dp.toPx() }
    val rowHeightPx = with(density) { rowHeightDp.dp.toPx() }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState(
        // scroll around C4 (pitch 60) by default
        initial = with(density) { ((topPitch - 64) * rowHeightDp).dp.roundToPx() }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FloDarkSurface)
    ) {
        // TOP CONTROL STRIP (Zoom, Snap, Selected Note Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(FloDarkSurfaceVariant)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Grid Snap and Note Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PIANO ROLL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = FloTextPrimary
                )
                Text(
                    text = "${notes.size} notes",
                    fontSize = 10.sp,
                    color = FloTextSecondary
                )
            }

            // Right: Delete Note, Zoom In, Zoom Out
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectedNoteId != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FloRedStop.copy(alpha = 0.2f))
                            .clickable { onNoteDelete(selectedNoteId) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = FloRedStop,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DELETE NOTE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FloRedStop
                        )
                    }
                }

                // Zoom Out
                IconButton(
                    onClick = { beatWidthDp = maxOf(28f, beatWidthDp - 8f) },
                    modifier = Modifier.size(28.dp).testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out Timeline",
                        tint = FloTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Zoom In
                IconButton(
                    onClick = { beatWidthDp = minOf(90f, beatWidthDp + 8f) },
                    modifier = Modifier.size(28.dp).testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In Timeline",
                        tint = FloTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // TIMELINE HEADER (Ruler with Bar numbers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(FloDarkCard)
        ) {
            // Empty space above piano keys
            Box(
                modifier = Modifier
                    .width(keyboardWidthDp.dp)
                    .fillMaxHeight()
                    .background(FloDarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Text("KEY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FloTextSecondary)
            }

            // Scrollable Timeline Ruler
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .pointerInput(totalBeats, beatWidthPx) {
                        detectTapGestures { offset ->
                            val clickedBeat = (offset.x / beatWidthPx).coerceAtLeast(0f)
                            onScrubPlayhead(clickedBeat)
                        }
                    }
            ) {
                val totalBars = (totalBeats / 4).toInt() + 1
                for (bar in 0 until totalBars) {
                    val barWidth = beatWidthDp * 4
                    Box(
                        modifier = Modifier
                            .width(barWidth.dp)
                            .fillMaxHeight()
                            .border(width = 0.5.dp, color = FloBorderSubtle),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "BAR ${bar + 1}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FloTextSecondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // MAIN WORKSPACE: Vertical Piano Keyboard on Left + Interactive Grid on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(verticalScrollState)
        ) {
            // LEFT: Vertical Piano Keyboard
            Column(
                modifier = Modifier
                    .width(keyboardWidthDp.dp)
                    .background(FloDarkSurface)
            ) {
                for (pitch in pitches) {
                    val isBlack = MusicalPitch.isBlackKey(pitch)
                    val pitchName = MusicalPitch.getPitchName(pitch)
                    val isC = pitch % 12 == 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeightDp.dp)
                            .background(if (isBlack) FloPianoBlackKey else FloPianoWhiteKey)
                            .border(width = 0.5.dp, color = FloBorderSubtle)
                            .pointerInput(pitch) {
                                detectTapGestures(
                                    onPress = {
                                        onKeyPreviewOn(pitch)
                                        tryAwaitRelease()
                                        onKeyPreviewOff(pitch)
                                    }
                                )
                            }
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (isC || isBlack) {
                            Text(
                                text = pitchName,
                                fontSize = if (isC) 10.sp else 8.sp,
                                fontWeight = if (isC) FontWeight.Bold else FontWeight.Normal,
                                color = if (isC) FloCyanAccent else FloTextSecondary
                            )
                        }
                    }
                }
            }

            // RIGHT: Scrollable Note Grid + Notes + Playhead
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                val totalGridWidthDp = totalBeats * beatWidthDp
                val totalGridHeightDp = pitches.size * rowHeightDp

                // Dragging / Resizing State tracking
                var draggingNoteId by remember { mutableStateOf<String?>(null) }
                var isResizing by remember { mutableStateOf(false) }
                var dragStartBeat by remember { mutableFloatStateOf(0f) }
                var dragStartPitch by remember { mutableStateOf(0) }

                Box(
                    modifier = Modifier
                        .width(totalGridWidthDp.dp)
                        .height(totalGridHeightDp.dp)
                        .pointerInput(notes, beatWidthPx, rowHeightPx, snapBeats) {
                            detectTapGestures { offset ->
                                val tappedBeat = offset.x / beatWidthPx
                                val rowIndex = (offset.y / rowHeightPx).toInt().coerceIn(0, pitches.size - 1)
                                val tappedPitch = pitches[rowIndex]

                                // Check if tapped an existing note
                                val hitNote = notes.firstOrNull { note ->
                                    note.pitch == tappedPitch &&
                                            tappedBeat >= note.startBeat &&
                                            tappedBeat <= note.endBeat
                                }

                                if (hitNote != null) {
                                    onNoteSelect(hitNote.id)
                                } else {
                                    // Add new note at snapped position
                                    val snappedBeat = (kotlin.math.round(tappedBeat / snapBeats) * snapBeats).coerceAtLeast(0f)
                                    onNoteAdd(tappedPitch, snappedBeat, 1.0f)
                                }
                            }
                        }
                        .pointerInput(notes, beatWidthPx, rowHeightPx, snapBeats) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val startBeat = offset.x / beatWidthPx
                                    val rowIndex = (offset.y / rowHeightPx).toInt().coerceIn(0, pitches.size - 1)
                                    val pitch = pitches[rowIndex]

                                    val hitNote = notes.firstOrNull { note ->
                                        note.pitch == pitch &&
                                                startBeat >= note.startBeat &&
                                                startBeat <= note.endBeat
                                    }

                                    if (hitNote != null) {
                                        onNoteSelect(hitNote.id)
                                        draggingNoteId = hitNote.id
                                        dragStartBeat = hitNote.startBeat
                                        dragStartPitch = hitNote.pitch

                                        // If dragging within 14px of right edge of note, trigger resize mode
                                        val noteEndPx = hitNote.endBeat * beatWidthPx
                                        isResizing = (noteEndPx - offset.x) < 16f
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val noteId = draggingNoteId ?: return@detectDragGestures
                                    val note = notes.firstOrNull { it.id == noteId } ?: return@detectDragGestures

                                    if (isResizing) {
                                        val deltaBeats = dragAmount.x / beatWidthPx
                                        val newDuration = maxOf(snapBeats, note.durationBeats + deltaBeats)
                                        onNoteResize(noteId, newDuration)
                                    } else {
                                        val deltaBeats = dragAmount.x / beatWidthPx
                                        val deltaRows = (dragAmount.y / rowHeightPx).roundToInt()
                                        val newStartBeat = maxOf(0f, note.startBeat + deltaBeats)
                                        val currentPitchIndex = pitches.indexOf(note.pitch)
                                        val newPitchIndex = (currentPitchIndex + deltaRows).coerceIn(0, pitches.size - 1)
                                        val newPitch = pitches[newPitchIndex]
                                        onNoteMove(noteId, newPitch, newStartBeat)
                                    }
                                },
                                onDragEnd = {
                                    draggingNoteId = null
                                    isResizing = false
                                },
                                onDragCancel = {
                                    draggingNoteId = null
                                    isResizing = false
                                }
                            )
                        }
                ) {
                    // Canvas Grid Lines and Background
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Draw Pitch Row backgrounds (white vs black keys)
                        for ((index, pitch) in pitches.withIndex()) {
                            val y = index * rowHeightPx
                            val isBlack = MusicalPitch.isBlackKey(pitch)
                            val rowColor = if (isBlack) FloDarkBg else FloDarkCard.copy(alpha = 0.4f)
                            drawRect(
                                color = rowColor,
                                topLeft = Offset(0f, y),
                                size = Size(canvasWidth, rowHeightPx)
                            )
                            // Row horizontal separator
                            drawLine(
                                color = FloPianoGridLine,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Vertical Beat & Bar Lines
                        val totalGridBeats = (canvasWidth / beatWidthPx).toInt() + 1
                        for (beat in 0..totalGridBeats) {
                            val x = beat * beatWidthPx
                            val isBar = beat % 4 == 0
                            val lineColor = if (isBar) FloPianoBarLine else FloPianoGridLine
                            val lineWidth = if (isBar) 2f else 1f
                            drawLine(
                                color = lineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = lineWidth
                            )
                        }

                        // Draw Notes
                        for (note in notes) {
                            val rowIndex = pitches.indexOf(note.pitch)
                            if (rowIndex != -1) {
                                val noteX = note.startBeat * beatWidthPx
                                val noteY = rowIndex * rowHeightPx + 2f
                                val noteWidth = maxOf(12f, note.durationBeats * beatWidthPx - 2f)
                                val noteHeight = rowHeightPx - 4f
                                val isSelected = note.id == selectedNoteId

                                // Note Body
                                drawRoundRect(
                                    color = if (isSelected) FloNoteSelected else FloNotePrimary,
                                    topLeft = Offset(noteX, noteY),
                                    size = Size(noteWidth, noteHeight),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )

                                // Highlight / Selected Border
                                if (isSelected) {
                                    drawRoundRect(
                                        color = FloNoteBorder,
                                        topLeft = Offset(noteX, noteY),
                                        size = Size(noteWidth, noteHeight),
                                        cornerRadius = CornerRadius(6f, 6f),
                                        style = Stroke(width = 2.5f)
                                    )
                                    // Right resize handle bar
                                    drawRect(
                                        color = Color.White,
                                        topLeft = Offset(noteX + noteWidth - 4f, noteY + 2f),
                                        size = Size(3f, noteHeight - 4f)
                                    )
                                }
                            }
                        }

                        // Draw Playhead Line
                        val playheadX = currentBeat * beatWidthPx
                        drawLine(
                            color = FloCyanAccent,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, canvasHeight),
                            strokeWidth = 2.5f
                        )

                        // Playhead triangular cursor at top
                        val trianglePath = Path().apply {
                            moveTo(playheadX - 6f, 0f)
                            lineTo(playheadX + 6f, 0f)
                            lineTo(playheadX, 10f)
                            close()
                        }
                        drawPath(trianglePath, color = FloCyanAccent)

                        // VIRTUAL RECORDING VIEWPORT OVERLAY (9:16 or 16:9)
                        if (videoExportRatio == com.example.flo.model.VideoExportRatio.RATIO_9_16) {
                            val frameHeight = canvasHeight
                            val frameWidth = frameHeight * (9f / 16f)
                            // Smoothly follow the moving playhead horizontally
                            val frameLeft = maxOf(0f, playheadX - frameWidth * 0.45f)
                            val frameRight = frameLeft + frameWidth

                            // Dim areas outside the 9:16 portrait recording viewport
                            if (frameLeft > 0f) {
                                drawRect(
                                    color = Color(0xAA080912),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(frameLeft, canvasHeight)
                                )
                            }
                            if (frameRight < canvasWidth) {
                                drawRect(
                                    color = Color(0xAA080912),
                                    topLeft = Offset(frameRight, 0f),
                                    size = Size(canvasWidth - frameRight, canvasHeight)
                                )
                            }

                            // Glowing frame border
                            drawRoundRect(
                                color = Color(0xFFEF4444),
                                topLeft = Offset(frameLeft, 2f),
                                size = Size(frameWidth, canvasHeight - 4f),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 2.5f)
                            )

                            // Viewport corner brackets
                            val bracket = 24f
                            // Top-left
                            drawLine(Color.White, Offset(frameLeft, 2f), Offset(frameLeft + bracket, 2f), strokeWidth = 3f)
                            drawLine(Color.White, Offset(frameLeft, 2f), Offset(frameLeft, 2f + bracket), strokeWidth = 3f)
                            // Top-right
                            drawLine(Color.White, Offset(frameRight - bracket, 2f), Offset(frameRight, 2f), strokeWidth = 3f)
                            drawLine(Color.White, Offset(frameRight, 2f), Offset(frameRight, 2f + bracket), strokeWidth = 3f)
                            // Bottom-left
                            drawLine(Color.White, Offset(frameLeft, canvasHeight - 2f), Offset(frameLeft + bracket, canvasHeight - 2f), strokeWidth = 3f)
                            drawLine(Color.White, Offset(frameLeft, canvasHeight - 2f - bracket), Offset(frameLeft, canvasHeight - 2f), strokeWidth = 3f)
                            // Bottom-right
                            drawLine(Color.White, Offset(frameRight - bracket, canvasHeight - 2f), Offset(frameRight, canvasHeight - 2f), strokeWidth = 3f)
                            drawLine(Color.White, Offset(frameRight, canvasHeight - 2f - bracket), Offset(frameRight, canvasHeight - 2f), strokeWidth = 3f)
                        } else if (videoExportRatio == com.example.flo.model.VideoExportRatio.RATIO_16_9) {
                            val frameHeight = canvasHeight
                            val frameWidth = minOf(canvasWidth, frameHeight * (16f / 9f))
                            val frameLeft = 0f
                            val frameRight = frameLeft + frameWidth

                            if (frameRight < canvasWidth) {
                                drawRect(
                                    color = Color(0x99080912),
                                    topLeft = Offset(frameRight, 0f),
                                    size = Size(canvasWidth - frameRight, canvasHeight)
                                )
                            }

                            drawRoundRect(
                                color = FloCyanAccent,
                                topLeft = Offset(frameLeft, 2f),
                                size = Size(frameWidth, canvasHeight - 4f),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 2.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
