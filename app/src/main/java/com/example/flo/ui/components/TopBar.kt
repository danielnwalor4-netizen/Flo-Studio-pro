package com.example.flo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flo.model.VideoExportRatio
import com.example.flo.ui.theme.FloBorderSubtle
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloDarkSurfaceVariant
import com.example.flo.ui.theme.FloGreenPlay
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloRedStop
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary

@Composable
fun TopBar(
    projectName: String,
    hasUnsavedChanges: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    videoExportRatio: VideoExportRatio,
    onVideoExportRatioSelected: (VideoExportRatio) -> Unit,
    onExportVideoClick: () -> Unit,
    onExportAudioClick: () -> Unit = onExportVideoClick,
    showSoundGenerator: Boolean,
    onToggleSoundGenerator: () -> Unit,
    showInspector: Boolean,
    onToggleInspector: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onNewProjectClick: () -> Unit,
    onSaveProjectClick: () -> Unit,
    onProjectsFolderClick: () -> Unit,
    onRenameClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRatioMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(FloDarkSurface)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Logo & Studio Indicator & Panel Toggles
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Flo Studio Logo",
                tint = FloPurplePrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FLO STUDIO",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                fontSize = 14.sp,
                color = FloTextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Sidebar toggle buttons
            IconButton(
                onClick = onToggleSoundGenerator,
                modifier = Modifier.size(32.dp).testTag("toggle_sound_generator_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Toggle Sound Generator",
                    tint = if (showSoundGenerator) FloPurpleLight else FloTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onToggleInspector,
                modifier = Modifier.size(32.dp).testTag("toggle_inspector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Toggle FX Inspector",
                    tint = if (showInspector) FloPurpleLight else FloTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Center: Composition Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onRenameClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("project_title_button")
        ) {
            Text(
                text = projectName + if (hasUnsavedChanges) " *" else "",
                color = if (hasUnsavedChanges) FloPurpleLight else FloTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename Project",
                tint = FloTextSecondary,
                modifier = Modifier.size(13.dp)
            )
        }

        // Center-Right: Virtual Recording Viewport & Video Export Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Viewport Mode Selector (9:16 Shorts / 16:9 YouTube / Off)
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (videoExportRatio != VideoExportRatio.NONE) FloDarkCard else FloDarkSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = when (videoExportRatio) {
                                VideoExportRatio.RATIO_9_16 -> FloRedStop
                                VideoExportRatio.RATIO_16_9 -> FloCyanAccent
                                else -> FloBorderSubtle
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { showRatioMenu = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("recording_viewport_selector")
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (videoExportRatio) {
                                    VideoExportRatio.RATIO_9_16 -> FloRedStop
                                    VideoExportRatio.RATIO_16_9 -> FloCyanAccent
                                    else -> FloTextSecondary.copy(alpha = 0.4f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (videoExportRatio) {
                            VideoExportRatio.RATIO_9_16 -> "VIEWPORT 9:16"
                            VideoExportRatio.RATIO_16_9 -> "VIEWPORT 16:9"
                            else -> "VIEWPORT: OFF"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FloTextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = FloTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showRatioMenu,
                    onDismissRequest = { showRatioMenu = false },
                    modifier = Modifier.background(FloDarkSurfaceVariant)
                ) {
                    VideoExportRatio.values().forEach { ratio ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${ratio.displayName} (${ratio.subtitle})",
                                    fontSize = 12.sp,
                                    color = if (ratio == videoExportRatio) FloCyanAccent else FloTextPrimary
                                )
                            },
                            onClick = {
                                onVideoExportRatioSelected(ratio)
                                showRatioMenu = false
                            }
                        )
                    }
                }
            }

            // Export Audio & MIDI Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(FloDarkCard)
                    .border(1.dp, FloCyanAccent, RoundedCornerShape(6.dp))
                    .clickable { onExportAudioClick() }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("export_audio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Export Audio & MIDI",
                    tint = FloCyanAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AUDIO / MIDI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FloCyanAccent
                )
            }

            // Export Video Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(FloPurplePrimary)
                    .clickable { onExportVideoClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("export_video_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Export Video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "EXPORT MP4",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Right Action Buttons: Undo, Redo, New, Save, Projects, Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            IconButton(
                onClick = onUndoClick,
                enabled = canUndo,
                modifier = Modifier.size(32.dp).testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) FloTextPrimary else FloTextSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onRedoClick,
                enabled = canRedo,
                modifier = Modifier.size(32.dp).testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) FloTextPrimary else FloTextSecondary.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onNewProjectClick,
                modifier = Modifier.size(32.dp).testTag("new_project_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Composition",
                    tint = FloTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onSaveProjectClick,
                modifier = Modifier.size(32.dp).testTag("save_project_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Project",
                    tint = if (hasUnsavedChanges) FloCyanAccent else FloTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onProjectsFolderClick,
                modifier = Modifier.size(32.dp).testTag("projects_folder_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Browse Projects",
                    tint = FloTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp).testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Studio Settings",
                    tint = FloTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
