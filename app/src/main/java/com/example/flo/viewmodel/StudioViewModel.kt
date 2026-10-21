package com.example.flo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flo.audio.AudioEngine
import com.example.flo.audio.MotifGenerator
import com.example.flo.audio.PlaybackState
import com.example.flo.model.Complexity
import com.example.flo.model.DurationPreset
import com.example.flo.model.Genre
import com.example.flo.model.InstrumentType
import com.example.flo.model.KeyRoot
import com.example.flo.model.Mood
import com.example.flo.model.Note
import com.example.flo.model.Project
import com.example.flo.model.ScaleMode
import com.example.flo.model.SoundParameters
import com.example.flo.model.VideoExportRatio
import com.example.flo.export.AudioExporter
import com.example.flo.export.MidiExporter
import com.example.flo.export.VideoExporter
import com.example.flo.storage.ProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Stack
import java.util.UUID

enum class ExportType(val displayName: String, val mimeType: String, val extension: String) {
    WAV("WAV Audio (44.1 kHz 16-bit)", "audio/wav", "wav"),
    MIDI("Standard MIDI File (Format 1)", "audio/midi", "mid"),
    VIDEO_9_16("9:16 Video (TikTok/Reels/Shorts)", "video/mp4", "mp4"),
    VIDEO_16_9("16:9 Video (YouTube)", "video/mp4", "mp4")
}

enum class NavTab(val title: String) {
    STUDIO("Studio"),
    GENERATE("Generate"),
    INSTRUMENTS("Instruments"),
    ARRANGEMENT("Arrangement"),
    PROJECTS("Projects")
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProjectRepository(application.applicationContext)
    val audioEngine = AudioEngine()

    private val _project = MutableStateFlow(repository.loadLastSessionProject())
    val project: StateFlow<Project> = _project.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioEngine.playbackState
    val currentBeat: StateFlow<Float> = audioEngine.currentBeat
    val vuMeterLevel: StateFlow<Float> = audioEngine.vuMeterLevel

    private val _selectedNoteId = MutableStateFlow<String?>(null)
    val selectedNoteId: StateFlow<String?> = _selectedNoteId.asStateFlow()

    private val _activeTab = MutableStateFlow(NavTab.STUDIO)
    val activeTab: StateFlow<NavTab> = _activeTab.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _showNewDialog = MutableStateFlow(false)
    val showNewDialog: StateFlow<Boolean> = _showNewDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Grid snap in beats: 0.25f (1/16), 0.5f (1/8), 1.0f (1/4)
    private val _snapBeats = MutableStateFlow(0.5f)
    val snapBeats: StateFlow<Float> = _snapBeats.asStateFlow()

    // Video Viewport & Export (Independent of application landscape workspace)
    private val _videoExportRatio = MutableStateFlow(VideoExportRatio.NONE)
    val videoExportRatio: StateFlow<VideoExportRatio> = _videoExportRatio.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // Backward-compatibility alias
    val isExportingVideo: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _currentExportType = MutableStateFlow<ExportType?>(null)
    val currentExportType: StateFlow<ExportType?> = _currentExportType.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportedFile = MutableStateFlow<java.io.File?>(null)
    val exportedFile: StateFlow<java.io.File?> = _exportedFile.asStateFlow()

    private val _exportedMimeType = MutableStateFlow("video/mp4")
    val exportedMimeType: StateFlow<String> = _exportedMimeType.asStateFlow()

    private var exportJob: Job? = null

    // M4: Preset Repository
    val presetRepository = com.example.flo.storage.CreatorPresetRepository(application.applicationContext)
    private val _availablePresets = MutableStateFlow(presetRepository.getAllPresets())
    val availablePresets: StateFlow<List<com.example.flo.model.CreatorPreset>> = _availablePresets.asStateFlow()

    private val _selectedPresetId = MutableStateFlow<String?>("preset_dark_cinematic")
    val selectedPresetId: StateFlow<String?> = _selectedPresetId.asStateFlow()

    // Workspace panel visibility (for landscape DAW optimization)
    private val _showSoundGenerator = MutableStateFlow(true)
    val showSoundGenerator: StateFlow<Boolean> = _showSoundGenerator.asStateFlow()

    private val _showInspector = MutableStateFlow(false)
    val showInspector: StateFlow<Boolean> = _showInspector.asStateFlow()

    // Undo / Redo history stacks
    private val undoStack = Stack<List<Note>>()
    private val redoStack = Stack<List<Note>>()

    init {
        syncAudioEngineWithProject()
    }

    private fun pushUndoState() {
        undoStack.push(_project.value.notes.toList())
        redoStack.clear()
        _hasUnsavedChanges.value = true
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_project.value.notes.toList())
            val previousNotes = undoStack.pop()
            _project.value = _project.value.copy(notes = previousNotes)
            syncAudioEngineWithProject()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_project.value.notes.toList())
            val nextNotes = redoStack.pop()
            _project.value = _project.value.copy(notes = nextNotes)
            syncAudioEngineWithProject()
        }
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    private fun syncAudioEngineWithProject() {
        val p = _project.value
        audioEngine.updateComposition(
            notes = p.notes,
            tracks = p.tracks,
            automations = p.automations,
            bpm = p.bpm,
            soundParams = p.soundParameters,
            instrument = p.instrument,
            totalCompositionBeats = p.totalBeats
        )
    }

    // ==========================================
    // Note Editing Operations
    // ==========================================

    fun addNote(pitch: Int, startBeat: Float, durationBeats: Float = 1.0f): Note {
        pushUndoState()
        val snappedStart = snapToGrid(startBeat)
        val note = Note(
            id = UUID.randomUUID().toString(),
            pitch = pitch,
            startBeat = snappedStart,
            durationBeats = maxOf(snapBeats.value, durationBeats),
            velocity = 0.8f,
            trackId = "track_piano"
        )
        val updatedNotes = _project.value.notes + note
        _project.value = _project.value.copy(notes = updatedNotes)
        _selectedNoteId.value = note.id
        syncAudioEngineWithProject()
        audioEngine.previewNoteOn(pitch, 0.8f)
        return note
    }

    fun selectNote(noteId: String?) {
        _selectedNoteId.value = noteId
        noteId?.let { id ->
            _project.value.notes.firstOrNull { it.id == id }?.let {
                audioEngine.previewNoteOn(it.pitch, it.velocity)
            }
        }
    }

    fun moveNote(noteId: String, newPitch: Int, newStartBeat: Float) {
        val currentNotes = _project.value.notes
        val noteIndex = currentNotes.indexOfFirst { it.id == noteId }
        if (noteIndex != -1) {
            pushUndoState()
            val existing = currentNotes[noteIndex]
            val snappedStart = maxOf(0f, snapToGrid(newStartBeat))
            val clampedPitch = newPitch.coerceIn(24, 84)
            val updatedNote = existing.copy(pitch = clampedPitch, startBeat = snappedStart)
            val newNotes = currentNotes.toMutableList()
            newNotes[noteIndex] = updatedNote
            _project.value = _project.value.copy(notes = newNotes)
            syncAudioEngineWithProject()
            audioEngine.previewNoteOn(clampedPitch, updatedNote.velocity)
        }
    }

    fun resizeNote(noteId: String, newDurationBeats: Float) {
        val currentNotes = _project.value.notes
        val noteIndex = currentNotes.indexOfFirst { it.id == noteId }
        if (noteIndex != -1) {
            pushUndoState()
            val existing = currentNotes[noteIndex]
            val clampedDuration = maxOf(snapBeats.value, snapToGrid(newDurationBeats))
            val updatedNote = existing.copy(durationBeats = clampedDuration)
            val newNotes = currentNotes.toMutableList()
            newNotes[noteIndex] = updatedNote
            _project.value = _project.value.copy(notes = newNotes)
            syncAudioEngineWithProject()
        }
    }

    fun deleteNote(noteId: String) {
        pushUndoState()
        val updated = _project.value.notes.filterNot { it.id == noteId }
        _project.value = _project.value.copy(notes = updated)
        if (_selectedNoteId.value == noteId) {
            _selectedNoteId.value = null
        }
        syncAudioEngineWithProject()
    }

    fun deleteSelectedNote() {
        _selectedNoteId.value?.let { deleteNote(it) }
    }

    fun snapToGrid(beat: Float): Float {
        val snap = _snapBeats.value
        return (kotlin.math.round(beat / snap) * snap)
    }

    fun setSnapBeats(snap: Float) {
        _snapBeats.value = snap
    }

    // ==========================================
    // Real-Time Note Auditioning
    // ==========================================

    fun previewPitchOn(pitch: Int, velocity: Float = 0.8f) {
        audioEngine.previewNoteOn(pitch, velocity)
    }

    fun previewPitchOff(pitch: Int) {
        audioEngine.previewNoteOff(pitch)
    }

    // ==========================================
    // Transport & Playback
    // ==========================================

    fun play() {
        syncAudioEngineWithProject()
        audioEngine.play()
    }

    fun pause() {
        audioEngine.pause()
    }

    fun stop() {
        audioEngine.stop()
    }

    fun seekToBeat(beat: Float) {
        audioEngine.seekToBeat(beat)
    }

    fun setMasterVolume(vol: Float) {
        audioEngine.setMasterVolume(vol)
    }

    // ==========================================
    // Generation Settings & Motif Generator
    // ==========================================

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(40, 200)
        _project.value = _project.value.copy(bpm = clamped)
        _hasUnsavedChanges.value = true
        syncAudioEngineWithProject()
    }

    fun setMood(mood: Mood) {
        _project.value = _project.value.copy(mood = mood)
        _hasUnsavedChanges.value = true
    }

    fun setGenre(genre: Genre) {
        _project.value = _project.value.copy(genre = genre)
        _hasUnsavedChanges.value = true
    }

    fun setInstrument(instrument: InstrumentType) {
        _project.value = _project.value.copy(instrument = instrument)
        _hasUnsavedChanges.value = true
        syncAudioEngineWithProject()
    }

    fun setKeyRoot(keyRoot: KeyRoot) {
        _project.value = _project.value.copy(keyRoot = keyRoot)
        _hasUnsavedChanges.value = true
    }

    fun setScaleMode(mode: ScaleMode) {
        _project.value = _project.value.copy(scaleMode = mode)
        _hasUnsavedChanges.value = true
    }

    fun setDurationPreset(preset: DurationPreset) {
        _project.value = _project.value.copy(durationPreset = preset)
        _hasUnsavedChanges.value = true
    }

    fun setComplexity(complexity: Complexity) {
        _project.value = _project.value.copy(complexity = complexity)
        _hasUnsavedChanges.value = true
    }

    fun setSoundParameters(params: SoundParameters) {
        _project.value = _project.value.copy(soundParameters = params)
        _hasUnsavedChanges.value = true
        syncAudioEngineWithProject()
    }

    // ==========================================
    // M2: Procedural Composition Engine
    // ==========================================

    fun setSeed(seed: Long) {
        _project.value = _project.value.copy(seed = seed)
        _hasUnsavedChanges.value = true
    }

    fun randomizeSeed() {
        val newSeed = kotlin.random.Random.nextLong(10000, 999999)
        setSeed(newSeed)
        _statusMessage.value = "New Seed: $newSeed"
    }

    fun setBars(bars: Int) {
        val clamped = bars.coerceIn(4, 64)
        _project.value = _project.value.copy(bars = clamped)
        _hasUnsavedChanges.value = true
        syncAudioEngineWithProject()
    }

    fun setTimeSignature(ts: com.example.flo.model.TimeSignature) {
        _project.value = _project.value.copy(
            timeSignatureNumerator = ts.numerator,
            timeSignatureDenominator = ts.denominator
        )
        _hasUnsavedChanges.value = true
        syncAudioEngineWithProject()
    }

    fun generateComposition() {
        pushUndoState()
        val newNotes = com.example.flo.audio.CompositionEngine.generateComposition(_project.value)
        _project.value = _project.value.copy(notes = newNotes)
        syncAudioEngineWithProject()
        audioEngine.stop()
        audioEngine.play()
        _statusMessage.value = "Generated ${_project.value.mood.displayName} composition in ${_project.value.keyRoot.displayName} ${_project.value.scaleMode.displayName}"
    }

    fun regenerateLayer(layer: com.example.flo.model.MusicalLayer) {
        pushUndoState()
        val layerSeed = _project.value.seed + layer.ordinal * 1013L + System.currentTimeMillis() % 1000L
        val updatedNotes = com.example.flo.audio.CompositionEngine.regenerateLayer(_project.value, layer, layerSeed)
        _project.value = _project.value.copy(notes = updatedNotes)
        syncAudioEngineWithProject()
        audioEngine.stop()
        audioEngine.play()
        _statusMessage.value = "Regenerated ${layer.displayName} layer"
    }

    fun createVariation() {
        pushUndoState()
        val varSeed = _project.value.seed + 777L + System.currentTimeMillis() % 1000L
        val variedNotes = com.example.flo.audio.CompositionEngine.createVariation(_project.value, varSeed)
        _project.value = _project.value.copy(seed = varSeed, notes = variedNotes)
        syncAudioEngineWithProject()
        audioEngine.stop()
        audioEngine.play()
        _statusMessage.value = "Created musical variation"
    }

    // ==========================================
    // M3: Multi-Instrument Track Controls
    // ==========================================

    fun setTrackInstrument(trackId: String, instrument: InstrumentType) {
        val updatedTracks = _project.value.tracks.map { track ->
            if (track.id == trackId) track.copy(instrumentType = instrument) else track
        }
        _project.value = _project.value.copy(tracks = updatedTracks)
        _hasUnsavedChanges.value = true
        syncAudioEngineWithProject()
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        val updatedTracks = _project.value.tracks.map { track ->
            if (track.id == trackId) track.copy(volume = volume.coerceIn(0f, 1f)) else track
        }
        _project.value = _project.value.copy(tracks = updatedTracks)
        syncAudioEngineWithProject()
    }

    fun toggleTrackMute(trackId: String) {
        val updatedTracks = _project.value.tracks.map { track ->
            if (track.id == trackId) track.copy(isMuted = !track.isMuted) else track
        }
        _project.value = _project.value.copy(tracks = updatedTracks)
        syncAudioEngineWithProject()
    }

    fun toggleTrackSolo(trackId: String) {
        val updatedTracks = _project.value.tracks.map { track ->
            if (track.id == trackId) track.copy(isSolo = !track.isSolo) else track
        }
        _project.value = _project.value.copy(tracks = updatedTracks)
        syncAudioEngineWithProject()
    }

    fun previewInstrument(instrument: InstrumentType) {
        audioEngine.previewInstrument(instrument)
    }

    // ==========================================
    // M4: Creator Presets
    // ==========================================

    fun applyPreset(preset: com.example.flo.model.CreatorPreset, replaceNotes: Boolean = true) {
        if (replaceNotes) pushUndoState()

        val updatedTracks = _project.value.tracks.map { track ->
            val inst = preset.trackInstruments[track.id] ?: track.instrumentType
            track.copy(instrumentType = inst)
        }

        val updatedProject = _project.value.copy(
            keyRoot = preset.recommendedKey,
            scaleMode = preset.recommendedScale,
            bpm = preset.recommendedBpm,
            timeSignatureNumerator = preset.recommendedTimeSignature.numerator,
            timeSignatureDenominator = preset.recommendedTimeSignature.denominator,
            mood = preset.mood,
            complexity = preset.complexity,
            tracks = updatedTracks
        )

        val finalProject = if (replaceNotes) {
            val generatedNotes = com.example.flo.audio.CompositionEngine.generateComposition(updatedProject)
            updatedProject.copy(notes = generatedNotes)
        } else {
            updatedProject
        }

        _project.value = finalProject
        _selectedPresetId.value = preset.id
        _hasUnsavedChanges.value = true
        syncAudioEngineWithProject()
        if (replaceNotes) {
            audioEngine.stop()
            audioEngine.play()
        }
        _statusMessage.value = "Applied preset '${preset.name}'"
    }

    fun saveCustomPreset(name: String, description: String) {
        val current = _project.value
        val trackMap = current.tracks.associate { it.id to it.instrumentType }
        val customPreset = com.example.flo.model.CreatorPreset(
            id = "custom_${System.currentTimeMillis()}",
            name = name.ifBlank { "My Custom Preset" },
            category = "My Presets",
            description = description.ifBlank { "Custom user composition preset" },
            recommendedKey = current.keyRoot,
            recommendedScale = current.scaleMode,
            recommendedBpm = current.bpm,
            recommendedTimeSignature = current.timeSignature,
            mood = current.mood,
            complexity = current.complexity,
            trackInstruments = trackMap,
            isUserCustom = true
        )
        val success = presetRepository.saveCustomPreset(customPreset)
        if (success) {
            _availablePresets.value = presetRepository.getAllPresets()
            _statusMessage.value = "Saved custom preset '${customPreset.name}'"
        }
    }

    fun deleteCustomPreset(presetId: String) {
        val success = presetRepository.deleteCustomPreset(presetId)
        if (success) {
            _availablePresets.value = presetRepository.getAllPresets()
            _statusMessage.value = "Preset deleted"
        }
    }

    // ==========================================
    // M5: Automatic Arrangement Engine
    // ==========================================

    fun generateArrangement(structure: com.example.flo.audio.ArrangementEngine.ArrangementStructure, bars: Int = 16) {
        pushUndoState()
        val plannedSections = com.example.flo.audio.ArrangementEngine.planSections(structure, bars)
        val projectWithBars = _project.value.copy(bars = bars, sections = plannedSections)
        val (arrangedNotes, automations) = com.example.flo.audio.ArrangementEngine.arrangeComposition(projectWithBars, plannedSections)

        _project.value = projectWithBars.copy(
            notes = arrangedNotes,
            sections = plannedSections,
            automations = automations
        )
        syncAudioEngineWithProject()
        audioEngine.stop()
        audioEngine.play()
        _statusMessage.value = "Arranged '${structure.displayName}' (${bars} bars)"
    }

    // ==========================================
    // Project Persistence & Management
    // ==========================================

    fun saveProject() {
        val current = _project.value.copy(lastModified = System.currentTimeMillis())
        _project.value = current
        val success = repository.saveCurrentProject(current)
        if (success) {
            _hasUnsavedChanges.value = false
            _statusMessage.value = "Project '${current.name}' saved"
        } else {
            _statusMessage.value = "Failed to save project"
        }
    }

    fun requestNewComposition() {
        if (_hasUnsavedChanges.value) {
            _showNewDialog.value = true
        } else {
            createNewComposition()
        }
    }

    fun confirmNewComposition() {
        _showNewDialog.value = false
        createNewComposition()
    }

    fun dismissNewDialog() {
        _showNewDialog.value = false
    }

    private fun createNewComposition() {
        audioEngine.stop()
        val newProj = Project(
            name = "New Composition",
            bpm = _project.value.bpm,
            mood = _project.value.mood,
            genre = _project.value.genre,
            instrument = _project.value.instrument,
            keyRoot = _project.value.keyRoot,
            scaleMode = _project.value.scaleMode,
            notes = emptyList()
        )
        undoStack.clear()
        redoStack.clear()
        _project.value = newProj
        _selectedNoteId.value = null
        _hasUnsavedChanges.value = false
        syncAudioEngineWithProject()
        repository.saveCurrentProject(newProj)
        _statusMessage.value = "Created new composition"
    }

    fun loadProject(loadedProject: Project) {
        audioEngine.stop()
        undoStack.clear()
        redoStack.clear()
        _project.value = loadedProject
        _selectedNoteId.value = null
        _hasUnsavedChanges.value = false
        syncAudioEngineWithProject()
        repository.saveCurrentProject(loadedProject)
        _statusMessage.value = "Loaded '${loadedProject.name}'"
        _activeTab.value = NavTab.STUDIO
    }

    fun renameProject(newName: String) {
        if (newName.isNotBlank()) {
            _project.value = _project.value.copy(name = newName.trim())
            _hasUnsavedChanges.value = true
            _showRenameDialog.value = false
            saveProject()
        }
    }

    fun getSavedProjects(): List<Project> = repository.getAllSavedProjects()

    fun deleteProject(projectId: String) {
        repository.deleteProject(projectId)
    }

    fun setActiveTab(tab: NavTab) {
        _activeTab.value = tab
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setShowRenameDialog(show: Boolean) {
        _showRenameDialog.value = show
    }

    fun setVideoExportRatio(ratio: VideoExportRatio) {
        _videoExportRatio.value = ratio
        if (ratio != VideoExportRatio.NONE) {
            _statusMessage.value = "Active Recording Viewport: ${ratio.displayName}"
        }
    }

    fun toggleSoundGenerator() {
        _showSoundGenerator.value = !_showSoundGenerator.value
    }

    fun toggleInspector() {
        _showInspector.value = !_showInspector.value
    }

    fun setShowExportDialog(show: Boolean) {
        _showExportDialog.value = show
    }

    fun startAudioExportWav(context: android.content.Context, customName: String? = null) {
        if (_isExporting.value) return
        _isExporting.value = true
        _currentExportType.value = ExportType.WAV
        _exportProgress.value = 0f
        _statusMessage.value = "Synthesizing and rendering 44.1 kHz WAV audio..."

        exportJob = viewModelScope.launch {
            try {
                val file = AudioExporter.exportWav(
                    context = context,
                    project = _project.value,
                    customFileName = customName,
                    onProgress = { progress -> _exportProgress.value = progress }
                )
                if (file != null) {
                    _exportedFile.value = file
                    _exportedMimeType.value = "audio/wav"
                    _statusMessage.value = "WAV exported: ${file.name}"
                } else {
                    _statusMessage.value = "WAV audio export failed or cancelled"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Audio export error: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun startMidiExport(context: android.content.Context, customName: String? = null) {
        if (_isExporting.value) return
        _isExporting.value = true
        _currentExportType.value = ExportType.MIDI
        _exportProgress.value = 0f
        _statusMessage.value = "Exporting Standard MIDI File (SMF Format 1)..."

        exportJob = viewModelScope.launch {
            try {
                val file = MidiExporter.exportMidi(
                    context = context,
                    project = _project.value,
                    customFileName = customName
                )
                if (file != null) {
                    _exportedFile.value = file
                    _exportedMimeType.value = "audio/midi"
                    _exportProgress.value = 1.0f
                    _statusMessage.value = "MIDI exported: ${file.name}"
                } else {
                    _statusMessage.value = "MIDI export failed"
                }
            } catch (e: Exception) {
                _statusMessage.value = "MIDI export error: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun startVideoExport(context: android.content.Context, ratio: VideoExportRatio) {
        if (_isExporting.value) return
        _isExporting.value = true
        _currentExportType.value = if (ratio == VideoExportRatio.RATIO_9_16) ExportType.VIDEO_9_16 else ExportType.VIDEO_16_9
        _exportProgress.value = 0f
        _statusMessage.value = "Rendering ${ratio.displayName} video with synchronized audio..."

        exportJob = viewModelScope.launch {
            try {
                val file = VideoExporter.exportVideo(
                    context = context,
                    project = _project.value,
                    ratio = ratio,
                    onProgress = { progress -> _exportProgress.value = progress }
                )
                if (file != null) {
                    _exportedFile.value = file
                    _exportedMimeType.value = "video/mp4"
                    _statusMessage.value = "Video exported: ${file.name}"
                } else {
                    _statusMessage.value = "Video export failed or cancelled"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Export error: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun cancelCurrentExport() {
        exportJob?.cancel()
        _isExporting.value = false
        _currentExportType.value = null
        _statusMessage.value = "Export cancelled by user"
    }

    fun clearExportedFile() {
        _exportedFile.value = null
        _currentExportType.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
