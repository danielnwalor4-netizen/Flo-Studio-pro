package com.example.flo.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.flo.model.VideoExportRatio
import com.example.flo.ui.components.ArrangementView
import com.example.flo.ui.components.GeneratePanel
import com.example.flo.ui.components.ExportProgressDialog
import com.example.flo.ui.components.ExportStudioDialog
import com.example.flo.ui.components.ExportSuccessDialog
import com.example.flo.ui.components.InspectorPanel
import com.example.flo.ui.components.NewCompositionConfirmDialog
import com.example.flo.ui.components.PianoRollView
import com.example.flo.ui.components.PresetsDialog
import com.example.flo.ui.components.ProjectsDialog
import com.example.flo.ui.components.RenameDialog
import com.example.flo.ui.components.SavePresetDialog
import com.example.flo.ui.components.SettingsDialog
import com.example.flo.ui.components.TopBar
import com.example.flo.ui.components.TransportBar
import com.example.flo.ui.theme.FloBorderSubtle
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkBg
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloGreenPlay
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary
import com.example.flo.viewmodel.StudioViewModel
import java.io.File

@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentBeat by viewModel.currentBeat.collectAsState()
    val vuMeterLevel by viewModel.vuMeterLevel.collectAsState()
    val selectedNoteId by viewModel.selectedNoteId.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val snapBeats by viewModel.snapBeats.collectAsState()

    val videoExportRatio by viewModel.videoExportRatio.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val currentExportType by viewModel.currentExportType.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()
    val exportedMimeType by viewModel.exportedMimeType.collectAsState()

    val showSoundGenerator by viewModel.showSoundGenerator.collectAsState()
    val showInspector by viewModel.showInspector.collectAsState()

    val showNewDialog by viewModel.showNewDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val availablePresets by viewModel.availablePresets.collectAsState()
    val selectedPresetId by viewModel.selectedPresetId.collectAsState()

    var showProjectsDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var selectedTrackId by remember { mutableStateOf("track_melody") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FloDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                projectName = project.name,
                hasUnsavedChanges = hasUnsavedChanges,
                canUndo = viewModel.canUndo,
                canRedo = viewModel.canRedo,
                videoExportRatio = videoExportRatio,
                onVideoExportRatioSelected = { viewModel.setVideoExportRatio(it) },
                onExportVideoClick = {
                    viewModel.setShowExportDialog(true)
                },
                showSoundGenerator = showSoundGenerator,
                onToggleSoundGenerator = { viewModel.toggleSoundGenerator() },
                showInspector = showInspector,
                onToggleInspector = { viewModel.toggleInspector() },
                onUndoClick = { viewModel.undo() },
                onRedoClick = { viewModel.redo() },
                onNewProjectClick = { viewModel.requestNewComposition() },
                onSaveProjectClick = { viewModel.saveProject() },
                onProjectsFolderClick = { showProjectsDialog = true },
                onRenameClick = { viewModel.setShowRenameDialog(true) },
                onSettingsClick = { viewModel.setShowSettingsDialog(true) }
            )
        },
        bottomBar = {
            TransportBar(
                playbackState = playbackState,
                currentBeat = currentBeat,
                bpm = project.bpm,
                vuMeterLevel = vuMeterLevel,
                onPlay = { viewModel.play() },
                onPause = { viewModel.pause() },
                onStop = { viewModel.stop() },
                onMasterVolumeChanged = { viewModel.setMasterVolume(it) }
            )
        }
    ) { innerPadding ->
        // MAIN APPLICATION WORKSPACE: Always standard 16:9 Landscape DAW
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val totalW = maxWidth
            // Proportional sizing for 16:9 landscape layout
            val soundPanelWidth = if (totalW >= 960.dp) 270.dp else 230.dp
            val inspectorPanelWidth = if (totalW >= 960.dp) 240.dp else 200.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT: Sound Generator Panel (collapsible via TopBar toggle icon)
                if (showSoundGenerator) {
                    GeneratePanel(
                        selectedMood = project.mood,
                        onMoodSelected = { viewModel.setMood(it) },
                        bpm = project.bpm,
                        onBpmChanged = { viewModel.setBpm(it) },
                        keyRoot = project.keyRoot,
                        onKeyRootSelected = { viewModel.setKeyRoot(it) },
                        scaleMode = project.scaleMode,
                        onScaleModeSelected = { viewModel.setScaleMode(it) },
                        timeSignature = project.timeSignature,
                        onTimeSignatureSelected = { viewModel.setTimeSignature(it) },
                        bars = project.bars,
                        onBarsSelected = { viewModel.setBars(it) },
                        complexity = project.complexity,
                        onComplexitySelected = { viewModel.setComplexity(it) },
                        seed = project.seed,
                        onRandomizeSeed = { viewModel.randomizeSeed() },
                        onBrowsePresetsClick = { showPresetsDialog = true },
                        onRegenerateLayer = { viewModel.regenerateLayer(it) },
                        onCreateVariation = { viewModel.createVariation() },
                        onGenerateClick = { viewModel.generateComposition() },
                        onStructureSelected = { viewModel.generateArrangement(it, project.bars) },
                        modifier = Modifier
                            .width(soundPanelWidth)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = FloBorderSubtle)
                    )
                }

                // CENTER: Full WIDE Piano Roll (top) + Arrangement Timeline (bottom)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Wide Horizontal Piano Roll Editor with Virtual Recording Viewport Overlay
                    PianoRollView(
                        notes = project.notes,
                        selectedNoteId = selectedNoteId,
                        currentBeat = currentBeat,
                        snapBeats = snapBeats,
                        videoExportRatio = videoExportRatio,
                        onNoteAdd = { pitch, startBeat, duration ->
                            viewModel.addNote(pitch, startBeat, duration)
                        },
                        onNoteSelect = { viewModel.selectNote(it) },
                        onNoteMove = { id, pitch, start ->
                            viewModel.moveNote(id, pitch, start)
                        },
                        onNoteResize = { id, dur ->
                            viewModel.resizeNote(id, dur)
                        },
                        onNoteDelete = { viewModel.deleteNote(it) },
                        onKeyPreviewOn = { viewModel.previewPitchOn(it) },
                        onKeyPreviewOff = { viewModel.previewPitchOff(it) },
                        onScrubPlayhead = { viewModel.seekToBeat(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.70f)
                    )

                    // Multi-track Arrangement
                    ArrangementView(
                        tracks = project.tracks,
                        notes = project.notes,
                        currentBeat = currentBeat,
                        selectedTrackId = selectedTrackId,
                        onTrackSelect = { selectedTrackId = it },
                        onTrackMuteToggle = { viewModel.toggleTrackMute(it) },
                        onTrackSoloToggle = { viewModel.toggleTrackSolo(it) },
                        onTrackVolumeChanged = { id, vol -> viewModel.setTrackVolume(id, vol) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.30f)
                    )
                }

                // RIGHT: Instrument & FX Inspector Panel (collapsible via TopBar toggle icon)
                if (showInspector) {
                    InspectorPanel(
                        tracks = project.tracks,
                        selectedTrackId = selectedTrackId,
                        onTrackSelected = { selectedTrackId = it },
                        onTrackInstrumentChanged = { trackId, inst ->
                            viewModel.setTrackInstrument(trackId, inst)
                        },
                        soundParameters = project.soundParameters,
                        onSoundParametersChanged = { viewModel.setSoundParameters(it) },
                        modifier = Modifier
                            .width(inspectorPanelWidth)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = FloBorderSubtle)
                    )
                }
            }
        }
    }

    // DIALOGS
    if (showProjectsDialog) {
        ProjectsDialog(
            projects = viewModel.getSavedProjects(),
            currentProjectId = project.id,
            onLoadProject = { viewModel.loadProject(it) },
            onDeleteProject = { viewModel.deleteProject(it) },
            onDismiss = { showProjectsDialog = false }
        )
    }

    if (showPresetsDialog) {
        PresetsDialog(
            presets = availablePresets,
            selectedPresetId = selectedPresetId,
            onApplyPreset = { viewModel.applyPreset(it) },
            onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) },
            onOpenSavePreset = { showSavePresetDialog = true },
            onDismiss = { showPresetsDialog = false }
        )
    }

    if (showSavePresetDialog) {
        SavePresetDialog(
            onSave = { name, desc -> viewModel.saveCustomPreset(name, desc) },
            onDismiss = { showSavePresetDialog = false }
        )
    }

    if (showNewDialog) {
        NewCompositionConfirmDialog(
            onConfirm = { viewModel.confirmNewComposition() },
            onDismiss = { viewModel.dismissNewDialog() }
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = project.name,
            onRename = { viewModel.renameProject(it) },
            onDismiss = { viewModel.setShowRenameDialog(false) }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            snapBeats = snapBeats,
            onSnapBeatsChanged = { viewModel.setSnapBeats(it) },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }

    // EXPORT SELECTION DIALOG (Audio WAV, MIDI, Video MP4)
    if (showExportDialog) {
        ExportStudioDialog(
            project = project,
            onExportWav = { customName ->
                viewModel.startAudioExportWav(context, customName)
            },
            onExportMidi = { customName ->
                viewModel.startMidiExport(context, customName)
            },
            onExportVideo = { ratio ->
                viewModel.startVideoExport(context, ratio)
            },
            onDismiss = { viewModel.setShowExportDialog(false) }
        )
    }

    // EXPORT PROGRESS MODAL (Cancelable background rendering)
    if (isExporting) {
        ExportProgressDialog(
            exportType = currentExportType,
            progress = exportProgress,
            onCancel = { viewModel.cancelCurrentExport() }
        )
    }

    // EXPORT SUCCESS / SHARING MODAL
    exportedFile?.let { file ->
        ExportSuccessDialog(
            file = file,
            mimeType = exportedMimeType,
            onShare = {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = exportedMimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${file.name}"))
                } catch (e: Exception) {
                    // Fallback
                }
            },
            onOpen = {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, exportedMimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(openIntent, "Open with"))
                } catch (e: Exception) {
                    // Fallback
                }
            },
            onDismiss = { viewModel.clearExportedFile() }
        )
    }
}
