package com.example.flo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flo.model.CreatorPreset
import com.example.flo.model.Project
import com.example.flo.model.VideoExportRatio
import com.example.flo.viewmodel.ExportType
import java.io.File
import com.example.flo.ui.theme.FloBorder
import com.example.flo.ui.theme.FloBorderSubtle
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkBg
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloDarkSurfaceVariant
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloRedStop
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsDialog(
    projects: List<Project>,
    currentProjectId: String,
    onLoadProject: (Project) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = FloPurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOCAL PROJECTS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved projects yet",
                            color = FloTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(projects) { p ->
                            val isCurrent = p.id == currentProjectId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) FloDarkCard else FloDarkSurfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) FloPurplePrimary else FloBorderSubtle,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        onLoadProject(p)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrent) FloPurpleLight else FloTextPrimary
                                    )
                                    Text(
                                        text = "${p.mood.displayName} • ${p.bpm} BPM • ${p.notes.size} notes",
                                        fontSize = 10.sp,
                                        color = FloTextSecondary
                                    )
                                    Text(
                                        text = dateFormat.format(Date(p.lastModified)),
                                        fontSize = 9.sp,
                                        color = FloTextSecondary.copy(alpha = 0.6f)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteProject(p.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = FloRedStop,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = FloPurpleLight, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun NewCompositionConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Text(
                text = "Unsaved Changes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FloTextPrimary
            )
        },
        text = {
            Text(
                text = "You have unsaved changes in this composition. Creating a new composition will discard them unless saved. Continue?",
                fontSize = 13.sp,
                color = FloTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = FloPurplePrimary)
            ) {
                Text("DISCARD & CREATE NEW", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = FloTextSecondary)
            }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Text(
                text = "Rename Composition",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FloTextPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FloPurplePrimary,
                    unfocusedBorderColor = FloBorderSubtle,
                    focusedTextColor = FloTextPrimary,
                    unfocusedTextColor = FloTextPrimary,
                    cursorColor = FloCyanAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_input_field")
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(text) },
                colors = ButtonDefaults.buttonColors(containerColor = FloPurplePrimary)
            ) {
                Text("SAVE NAME", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = FloTextSecondary)
            }
        }
    )
}

@Composable
fun SettingsDialog(
    snapBeats: Float,
    onSnapBeatsChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Text(
                text = "STUDIO SETTINGS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FloTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "GRID QUANTIZATION & SNAPPING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloTextSecondary,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val snapOptions = listOf(
                        Pair(0.25f, "1/16 Beat"),
                        Pair(0.5f, "1/8 Beat"),
                        Pair(1.0f, "1/4 Beat")
                    )
                    snapOptions.forEach { (snap, label) ->
                        PillTag(
                            text = label,
                            isSelected = snap == snapBeats,
                            onClick = { onSnapBeatsChanged(snap) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "AUDIO ENGINE STATUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloTextSecondary,
                    letterSpacing = 1.sp
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(FloDarkCard)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Synthesis Engine:", fontSize = 11.sp, color = FloTextSecondary)
                        Text("Real-time PCM 44.1 kHz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FloCyanAccent)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Latency Buffer:", fontSize = 11.sp, color = FloTextSecondary)
                        Text("1024 frames (~23ms)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FloTextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Polyphony:", fontSize = 11.sp, color = FloTextSecondary)
                        Text("24 Voice Dynamic Pool", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FloTextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Studio Storage:", fontSize = 11.sp, color = FloTextSecondary)
                        Text("Local Device JSON (M1)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FloTextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = FloPurpleLight, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PresetsDialog(
    presets: List<CreatorPreset>,
    selectedPresetId: String?,
    onApplyPreset: (CreatorPreset) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onOpenSavePreset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Chill & Lo-fi", "Atmospheric", "Cinematic & Film", "Upbeat & Energetic", "Mystery & Psychology", "My Presets")

    val filteredPresets = if (selectedCategory == "All") {
        presets
    } else if (selectedCategory == "My Presets") {
        presets.filter { it.isUserCustom }
    } else {
        presets.filter { it.category.contains(selectedCategory, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CREATOR PRESETS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloTextPrimary
                )
                Button(
                    onClick = onOpenSavePreset,
                    colors = ButtonDefaults.buttonColors(containerColor = FloDarkCard),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.border(1.dp, FloCyanAccent, RoundedCornerShape(6.dp))
                ) {
                    Text("+ SAVE AS PRESET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FloCyanAccent)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Category Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        PillTag(
                            text = if (cat == "Cinematic & Film") "Cinematic" else cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.drop(4).forEach { cat ->
                        PillTag(
                            text = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Presets List
                if (filteredPresets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No presets in this category",
                            color = FloTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPresets) { preset ->
                            val isSelected = preset.id == selectedPresetId
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) FloDarkCard else FloDarkSurfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) FloPurplePrimary else FloBorderSubtle,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = preset.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FloTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(FloDarkBg)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = preset.category,
                                                fontSize = 9.sp,
                                                color = FloCyanAccent
                                            )
                                        }
                                    }
                                    if (preset.isUserCustom) {
                                        IconButton(
                                            onClick = { onDeleteCustomPreset(preset.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Preset",
                                                tint = FloRedStop,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = preset.description,
                                    fontSize = 11.sp,
                                    color = FloTextSecondary,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${preset.recommendedKey.displayName} ${preset.recommendedScale.displayName} • ${preset.recommendedBpm} BPM • ${preset.recommendedTimeSignature.displayName}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = FloPurpleLight
                                    )
                                    Button(
                                        onClick = {
                                            onApplyPreset(preset)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FloPurplePrimary),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("LOAD & COMPOSE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = FloPurpleLight, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SavePresetDialog(
    onSave: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Text(
                text = "Save Current Sound as Preset",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FloTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name", color = FloTextSecondary, fontSize = 11.sp) },
                    placeholder = { Text("e.g. My Dark Lofi Vibe", color = FloTextSecondary.copy(alpha = 0.5f), fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FloPurplePrimary,
                        unfocusedBorderColor = FloBorderSubtle,
                        focusedTextColor = FloTextPrimary,
                        unfocusedTextColor = FloTextPrimary,
                        cursorColor = FloCyanAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = FloTextSecondary, fontSize = 11.sp) },
                    placeholder = { Text("Describe mood or video usage", color = FloTextSecondary.copy(alpha = 0.5f), fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FloPurplePrimary,
                        unfocusedBorderColor = FloBorderSubtle,
                        focusedTextColor = FloTextPrimary,
                        unfocusedTextColor = FloTextPrimary,
                        cursorColor = FloCyanAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, description)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FloPurplePrimary)
            ) {
                Text("SAVE PRESET", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = FloTextSecondary)
            }
        }
    )
}

@Composable
fun ExportStudioDialog(
    project: Project,
    onExportWav: (fileName: String) -> Unit,
    onExportMidi: (fileName: String) -> Unit,
    onExportVideo: (ratio: VideoExportRatio) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Audio (WAV), 1: MIDI, 2: Video (MP4)
    var wavFileName by remember { mutableStateOf("FloStudio_${project.name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")}") }
    var midiFileName by remember { mutableStateOf("FloStudio_${project.name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")}") }
    var selectedVideoRatio by remember { mutableStateOf(VideoExportRatio.RATIO_9_16) }

    val durationSec = (project.totalBeats * (60.0 / project.bpm)).toInt()
    val mins = durationSec / 60
    val secs = durationSec % 60
    val durationFormatted = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    val approxWavMb = ((durationSec + 1.5) * 44100 * 4) / (1024 * 1024)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Column {
                Text(
                    text = "EXPORT COMPOSITION",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    fontSize = 15.sp,
                    color = FloTextPrimary
                )
                Text(
                    text = "${project.name} • $durationFormatted • ${project.bpm} BPM",
                    fontSize = 11.sp,
                    color = FloTextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Selector: Audio | MIDI | Video
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FloDarkCard)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("AUDIO (WAV)", "MIDI (.MID)", "VIDEO (MP4)")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) FloPurplePrimary else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FloTextSecondary
                            )
                        }
                    }
                }

                // TAB 0: AUDIO (WAV)
                if (selectedTab == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(FloDarkBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = FloCyanAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("44.1 kHz • 16-Bit Stereo Uncompressed PCM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FloTextPrimary)
                                Text("Estimated size: ~${String.format(java.util.Locale.US, "%.1f", approxWavMb)} MB • All tracks, automations & reverb", fontSize = 11.sp, color = FloTextSecondary)
                            }
                        }

                        OutlinedTextField(
                            value = wavFileName,
                            onValueChange = { wavFileName = it },
                            label = { Text("Output Filename") },
                            suffix = { Text(".wav", color = FloCyanAccent) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FloCyanAccent,
                                unfocusedBorderColor = FloBorderSubtle,
                                focusedTextColor = FloTextPrimary,
                                unfocusedTextColor = FloTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("wav_filename_input")
                        )
                    }
                }

                // TAB 1: MIDI
                if (selectedTab == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(FloDarkBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = FloPurpleLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Standard MIDI File (SMF Format 1)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FloTextPrimary)
                                Text("480 PPQN • Separate tracks: Melody, Chords, Bass, Drums", fontSize = 11.sp, color = FloTextSecondary)
                            }
                        }

                        OutlinedTextField(
                            value = midiFileName,
                            onValueChange = { midiFileName = it },
                            label = { Text("Output Filename") },
                            suffix = { Text(".mid", color = FloPurpleLight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FloPurpleLight,
                                unfocusedBorderColor = FloBorderSubtle,
                                focusedTextColor = FloTextPrimary,
                                unfocusedTextColor = FloTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("midi_filename_input")
                        )
                    }
                }

                // TAB 2: VIDEO (MP4)
                if (selectedTab == 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(FloDarkBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = FloRedStop, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("H.264 Video + Synchronized AAC Audio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FloTextPrimary)
                                Text("Real-time playhead animation across the piano roll", fontSize = 11.sp, color = FloTextSecondary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val is916 = selectedVideoRatio == VideoExportRatio.RATIO_9_16
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (is916) FloDarkCard else FloDarkSurfaceVariant)
                                    .border(1.dp, if (is916) FloRedStop else FloBorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { selectedVideoRatio = VideoExportRatio.RATIO_9_16 }
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("9:16 VERTICAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (is916) FloRedStop else FloTextPrimary)
                                    Text("720x1280 • TikTok, Reels, Shorts", fontSize = 10.sp, color = FloTextSecondary)
                                }
                            }

                            val is169 = selectedVideoRatio == VideoExportRatio.RATIO_16_9
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (is169) FloDarkCard else FloDarkSurfaceVariant)
                                    .border(1.dp, if (is169) FloCyanAccent else FloBorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { selectedVideoRatio = VideoExportRatio.RATIO_16_9 }
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("16:9 LANDSCAPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (is169) FloCyanAccent else FloTextPrimary)
                                    Text("1280x720 • YouTube Landscape", fontSize = 10.sp, color = FloTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> onExportWav(wavFileName)
                        1 -> onExportMidi(midiFileName)
                        2 -> onExportVideo(selectedVideoRatio)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedTab) {
                        0 -> FloCyanAccent
                        1 -> FloPurplePrimary
                        else -> FloRedStop
                    }
                ),
                modifier = Modifier.testTag("confirm_export_button")
            ) {
                Icon(
                    imageVector = when (selectedTab) {
                        0 -> Icons.Default.FileDownload
                        1 -> Icons.Default.MusicNote
                        else -> Icons.Default.Videocam
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selectedTab == 0) FloDarkBg else Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (selectedTab) {
                        0 -> "START WAV EXPORT"
                        1 -> "START MIDI EXPORT"
                        else -> "START MP4 EXPORT"
                    },
                    color = if (selectedTab == 0) FloDarkBg else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = FloTextSecondary)
            }
        }
    )
}

@Composable
fun ExportProgressDialog(
    exportType: ExportType?,
    progress: Float,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = FloDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (exportType) {
                        ExportType.WAV -> Icons.Default.GraphicEq
                        ExportType.MIDI -> Icons.Default.MusicNote
                        else -> Icons.Default.Videocam
                    },
                    contentDescription = null,
                    tint = FloPurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exporting ${exportType?.displayName ?: "Project"}...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (exportType) {
                        ExportType.WAV -> "Synthesizing procedural audio & rendering uncompressed PCM: ${(progress * 100).toInt()}%"
                        ExportType.MIDI -> "Converting note events and multi-track structure: ${(progress * 100).toInt()}%"
                        else -> "Rendering piano-roll frames and encoding AAC audio: ${(progress * 100).toInt()}%"
                    },
                    fontSize = 12.sp,
                    color = FloTextSecondary
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = FloPurplePrimary,
                    trackColor = FloDarkCard
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("cancel_export_button")) {
                Text("CANCEL EXPORT", color = FloRedStop, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ExportSuccessDialog(
    file: File,
    mimeType: String,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FloDarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = FloCyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Export Complete!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloTextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Your exported file is ready:",
                    fontSize = 13.sp,
                    color = FloTextSecondary
                )
                Text(
                    text = file.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloCyanAccent
                )
                Text(
                    text = "Size: ${file.length() / 1024} KB • Location: ${file.parentFile?.name ?: "Storage"}",
                    fontSize = 11.sp,
                    color = FloTextSecondary
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = FloPurplePrimary),
                    modifier = Modifier.testTag("share_export_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SHARE", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = FloDarkCard),
                    modifier = Modifier.testTag("open_export_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = FloCyanAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN", color = FloCyanAccent, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = FloTextSecondary)
            }
        }
    )
}


