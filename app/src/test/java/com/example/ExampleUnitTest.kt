package com.example

import com.example.flo.audio.ArrangementEngine
import com.example.flo.audio.CompositionEngine
import com.example.flo.audio.MotifGenerator
import com.example.flo.audio.SynthesizerVoice
import com.example.flo.model.ArrangementSection
import com.example.flo.model.Complexity
import com.example.flo.model.CreatorPreset
import com.example.flo.model.Genre
import com.example.flo.model.InstrumentType
import com.example.flo.model.KeyRoot
import com.example.flo.model.Mood
import com.example.flo.model.MusicalLayer
import com.example.flo.model.MusicalPitch
import com.example.flo.model.Note
import com.example.flo.model.Project
import com.example.flo.model.ScaleMode
import com.example.flo.model.SectionType
import com.example.flo.model.SoundParameters
import com.example.flo.model.TimeSignature
import com.example.flo.model.VideoExportRatio
import com.example.flo.export.AudioExporter
import com.example.flo.export.MidiExporter
import com.example.flo.export.VideoExporter
import com.example.flo.model.Track
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

  @Test
  fun testMusicalPitchCalculations() {
    // 69 is A4 = 440.0 Hz
    assertEquals(440.0, MusicalPitch.pitchToFrequency(69), 0.01)
    // 60 is C4
    assertEquals("C4", MusicalPitch.getPitchName(60))
    // 61 is C#4, which is a black key
    assertEquals("C#4", MusicalPitch.getPitchName(61))
    assertTrue(MusicalPitch.isBlackKey(61))
    // 60 is C4, white key
    assertFalse(MusicalPitch.isBlackKey(60))
  }

  @Test
  fun testNoteAndProjectSerialization() {
    val note1 = Note(pitch = 60, startBeat = 0f, durationBeats = 1f, velocity = 0.8f, trackId = "track_melody")
    val note2 = Note(pitch = 64, startBeat = 1f, durationBeats = 2f, velocity = 0.9f, trackId = "track_chords")
    val project = Project(
      name = "Cinematic Flow",
      bpm = 85,
      mood = Mood.DARK,
      genre = Genre.CINEMATIC,
      timeSignatureNumerator = 4,
      timeSignatureDenominator = 4,
      bars = 16,
      notes = listOf(note1, note2)
    )

    val jsonString = project.toJson()
    val restored = Project.fromJson(jsonString)

    assertEquals(project.name, restored.name)
    assertEquals(project.bpm, restored.bpm)
    assertEquals(project.mood, restored.mood)
    assertEquals(2, restored.notes.size)
    assertEquals(60, restored.notes[0].pitch)
    assertEquals(64, restored.notes[1].pitch)
    assertEquals("track_melody", restored.notes[0].trackId)
  }

  @Test
  fun testCompositionEngineM2ProceduralLayers() {
    val project = Project(
      mood = Mood.DARK,
      keyRoot = KeyRoot.A,
      scaleMode = ScaleMode.NATURAL_MINOR,
      bars = 8,
      seed = 42L,
      complexity = Complexity.MODERATE
    )

    val notes = CompositionEngine.generateComposition(project)
    assertTrue("Should generate notes for full composition", notes.isNotEmpty())

    // Must generate all 4 discrete layers
    val melodyNotes = notes.filter { it.trackId == "track_melody" }
    val chordNotes = notes.filter { it.trackId == "track_chords" }
    val bassNotes = notes.filter { it.trackId == "track_bass" }
    val rhythmNotes = notes.filter { it.trackId == "track_rhythm" }

    assertTrue("Melody layer must have notes", melodyNotes.isNotEmpty())
    assertTrue("Chords layer must have notes", chordNotes.isNotEmpty())
    assertTrue("Bass layer must have notes", bassNotes.isNotEmpty())
    assertTrue("Rhythm layer must have notes", rhythmNotes.isNotEmpty())

    // Determinism test: same project settings and seed must yield identical notes
    val notesSecondRun = CompositionEngine.generateComposition(project)
    assertEquals(notes.size, notesSecondRun.size)
    for (i in notes.indices) {
      assertEquals(notes[i].pitch, notesSecondRun[i].pitch)
      assertEquals(notes[i].startBeat, notesSecondRun[i].startBeat, 0.001f)
      assertEquals(notes[i].trackId, notesSecondRun[i].trackId)
    }
  }

  @Test
  fun testLayerRegenerationM2() {
    val project = Project(
      mood = Mood.EPIC,
      keyRoot = KeyRoot.D,
      scaleMode = ScaleMode.DORIAN,
      bars = 8,
      seed = 999L
    )
    val initialNotes = CompositionEngine.generateComposition(project)
    val projectWithNotes = project.copy(notes = initialNotes)

    // Regenerate Melody only
    val updatedNotes = CompositionEngine.regenerateLayer(projectWithNotes, MusicalLayer.MELODY, newSeed = 888L)

    val oldBass = initialNotes.filter { it.trackId == "track_bass" }
    val newBass = updatedNotes.filter { it.trackId == "track_bass" }
    assertEquals("Bass layer should be preserved when regenerating melody", oldBass.size, newBass.size)

    val oldRhythm = initialNotes.filter { it.trackId == "track_rhythm" }
    val newRhythm = updatedNotes.filter { it.trackId == "track_rhythm" }
    assertEquals("Rhythm layer should be preserved when regenerating melody", oldRhythm.size, newRhythm.size)
  }

  @Test
  fun testSynthesizerVoiceRenderingM3() {
    // Test multiple instrument families: Grand Piano, Warm Pad, Strings, Drums
    val instrumentsToTest = listOf(
      InstrumentType.PIANO_ACOUSTIC,
      InstrumentType.PAD_WARM,
      InstrumentType.STRINGS_ORCHESTRAL,
      InstrumentType.PERCUSSION
    )

    for (inst in instrumentsToTest) {
      val pitch = if (inst == InstrumentType.PERCUSSION) CompositionEngine.DRUM_KICK else 60
      val voice = SynthesizerVoice(
        pitch = pitch,
        velocity = 0.8f,
        instrument = inst,
        soundParams = SoundParameters(volume = 1f, reverb = 10f, attack = 2f, release = 2f)
      )

      var nonZeroFound = false
      for (i in 0 until 1000) {
        val sample = voice.renderSample(44100)
        assertFalse("Sample must not be NaN", sample.isNaN())
        assertFalse("Sample must not be Infinite", sample.isInfinite())
        if (sample != 0f) nonZeroFound = true
      }
      assertTrue("Voice for $inst should produce non-zero audio output", nonZeroFound)
    }
  }

  @Test
  fun testCreatorPresetsM4() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val repo = com.example.flo.storage.CreatorPresetRepository(context)

    val presets = repo.getAllPresets()
    assertTrue("Should have at least 17 built-in creator presets", presets.size >= 17)

    // Check curated categories
    val categories = presets.map { it.category }.toSet()
    assertTrue(categories.any { it.contains("Chill", ignoreCase = true) || it.contains("Lo-fi", ignoreCase = true) })
    assertTrue(categories.any { it.contains("Cinematic", ignoreCase = true) })
    assertTrue(categories.any { it.contains("Atmospheric", ignoreCase = true) })

    // Test creating custom preset
    val custom = CreatorPreset(
      id = "test_custom_1",
      name = "My Custom Preset",
      category = "My Presets",
      description = "Test description",
      recommendedKey = KeyRoot.C,
      recommendedScale = ScaleMode.MAJOR,
      recommendedBpm = 120,
      recommendedTimeSignature = TimeSignature.TIME_4_4,
      mood = Mood.HAPPY,
      complexity = Complexity.SIMPLE,
      trackInstruments = mapOf("track_melody" to InstrumentType.PIANO_ACOUSTIC),
      isUserCustom = true
    )
    repo.saveCustomPreset(custom)
    val afterSave = repo.getAllPresets()
    assertTrue(afterSave.any { it.id == "test_custom_1" })

    // Test deleting custom preset
    repo.deleteCustomPreset("test_custom_1")
    val afterDelete = repo.getAllPresets()
    assertFalse(afterDelete.any { it.id == "test_custom_1" })
  }

  @Test
  fun testArrangementEngineM5() {
    val project = Project(
      bars = 16,
      timeSignatureNumerator = 4,
      timeSignatureDenominator = 4,
      mood = Mood.EPIC
    )

    val sections = ArrangementEngine.planSections(ArrangementEngine.ArrangementStructure.STANDARD_SONG, 16)
    assertEquals(4, sections.size)
    assertEquals(SectionType.INTRO, sections[0].sectionType)
    assertEquals(SectionType.VERSE, sections[1].sectionType)
    assertEquals(SectionType.CHORUS, sections[2].sectionType)
    assertEquals(SectionType.OUTRO, sections[3].sectionType)

    val (arrangedNotes, automations) = ArrangementEngine.arrangeComposition(project, sections)
    assertTrue("Arranged notes must be generated", arrangedNotes.isNotEmpty())
    assertTrue("Volume automations must be generated for tracks", automations.isNotEmpty())
    assertEquals(4, automations.size)
  }

  @Test
  fun testVideoExportRatioAndTrackingCalculations() {
    val ratio916 = VideoExportRatio.RATIO_9_16
    val ratio169 = VideoExportRatio.RATIO_16_9

    assertEquals(9, ratio916.aspectWidth)
    assertEquals(16, ratio916.aspectHeight)
    assertEquals(16, ratio169.aspectWidth)
    assertEquals(9, ratio169.aspectHeight)

    // Verify 9:16 tracking calculation
    val canvasHeight = 600f
    val frameWidth = canvasHeight * (9f / 16f) // 337.5f
    val playheadX = 500f
    val frameLeft = maxOf(0f, playheadX - frameWidth * 0.45f)
    val frameRight = frameLeft + frameWidth

    assertTrue("Playhead must be framed within the 9:16 window", playheadX >= frameLeft && playheadX <= frameRight)
  }

  @Test
  fun testMilestone6WavHeaderAndPcmRendering() = runBlocking {
    val project = Project(
      name = "Test Audio Export",
      bpm = 120,
      bars = 4,
      notes = listOf(
        Note(pitch = 60, startBeat = 0f, durationBeats = 1f, velocity = 0.8f, trackId = "track_melody"),
        Note(pitch = 48, startBeat = 0f, durationBeats = 2f, velocity = 0.7f, trackId = "track_bass")
      )
    )

    // 1. Test offline PCM rendering
    val pcmBytes = AudioExporter.renderPcmBytes(project, maxDurationSec = 1.0)
    assertTrue("PCM bytes must not be empty", pcmBytes.isNotEmpty())
    val expectedBytes = (1.0 * 44100 * 4).toInt() // 44.1kHz, 2 channels, 16-bit
    assertEquals(expectedBytes, pcmBytes.size)

    // 2. Test standard 44-byte WAV header generation
    val tempFile = File.createTempFile("test_flo", ".wav")
    try {
      tempFile.writeBytes(ByteArray(44) + pcmBytes)
      AudioExporter.writeWavHeader(tempFile, pcmBytes.size.toLong())

      val headerBytes = ByteArray(44)
      tempFile.inputStream().use { it.read(headerBytes) }
      val bb = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

      // RIFF signature
      assertEquals('R'.code.toByte(), headerBytes[0])
      assertEquals('I'.code.toByte(), headerBytes[1])
      assertEquals('F'.code.toByte(), headerBytes[2])
      assertEquals('F'.code.toByte(), headerBytes[3])

      // Total file size - 8 = pcmBytes.size + 36
      assertEquals(pcmBytes.size + 36, bb.getInt(4))

      // WAVE format
      assertEquals('W'.code.toByte(), headerBytes[8])
      assertEquals('A'.code.toByte(), headerBytes[9])
      assertEquals('V'.code.toByte(), headerBytes[10])
      assertEquals('E'.code.toByte(), headerBytes[11])

      // fmt chunk
      assertEquals(16, bb.getInt(16)) // PCM format subchunk size
      assertEquals(1.toShort(), bb.getShort(20)) // 1 = PCM
      assertEquals(2.toShort(), bb.getShort(22)) // 2 channels (stereo)
      assertEquals(44100, bb.getInt(24)) // 44.1 kHz sample rate
      assertEquals(44100 * 2 * 2, bb.getInt(28)) // Byte rate
      assertEquals(4.toShort(), bb.getShort(32)) // Block align
      assertEquals(16.toShort(), bb.getShort(34)) // 16 bits per sample

      // data chunk
      assertEquals('d'.code.toByte(), headerBytes[36])
      assertEquals('a'.code.toByte(), headerBytes[37])
      assertEquals('t'.code.toByte(), headerBytes[38])
      assertEquals('a'.code.toByte(), headerBytes[39])
      assertEquals(pcmBytes.size, bb.getInt(40)) // Subchunk2Size
    } finally {
      tempFile.delete()
    }
  }

  @Test
  fun testMilestone6MidiExportStandardFormat1() {
    val project = Project(
      name = "MIDI Export Test",
      bpm = 90,
      timeSignatureNumerator = 4,
      timeSignatureDenominator = 4,
      bars = 8,
      notes = listOf(
        Note(pitch = 60, startBeat = 0f, durationBeats = 1f, velocity = 0.8f, trackId = "track_melody"),
        Note(pitch = 64, startBeat = 1f, durationBeats = 1f, velocity = 0.8f, trackId = "track_melody"),
        Note(pitch = 48, startBeat = 0f, durationBeats = 4f, velocity = 0.9f, trackId = "track_bass"),
        Note(pitch = 36, startBeat = 0f, durationBeats = 0.5f, velocity = 0.95f, trackId = "track_rhythm")
      )
    )

    val midiBytes = MidiExporter.generateMidiBytes(project)
    assertTrue("MIDI byte array must not be empty", midiBytes.isNotEmpty())

    // 1. Header chunk 'MThd'
    assertEquals('M'.code.toByte(), midiBytes[0])
    assertEquals('T'.code.toByte(), midiBytes[1])
    assertEquals('h'.code.toByte(), midiBytes[2])
    assertEquals('d'.code.toByte(), midiBytes[3])

    val bb = ByteBuffer.wrap(midiBytes).order(ByteOrder.BIG_ENDIAN)
    val headerLength = bb.getInt(4)
    assertEquals(6, headerLength)

    val format = bb.getShort(8).toInt()
    assertEquals(1, format) // SMF Format 1: Multi-track synchronous

    val numTracks = bb.getShort(10).toInt()
    // 1 conductor track + 4 default tracks = 5 tracks
    assertEquals(5, numTracks)

    val ppqn = bb.getShort(12).toInt()
    assertEquals(480, ppqn)

    // 2. Track 0 must be 'MTrk'
    assertEquals('M'.code.toByte(), midiBytes[14])
    assertEquals('T'.code.toByte(), midiBytes[15])
    assertEquals('r'.code.toByte(), midiBytes[16])
    assertEquals('k'.code.toByte(), midiBytes[17])
  }

  @Test
  fun testMilestone7ViewportCalculationsAndPlayheadBounds() {
    val exportBeats = 32f
    val beatW = 40f
    val keyboardW = 100f
    val width916 = 720f
    val visibleTimelineW = width916 - keyboardW

    // Test across all beats from start to end
    for (beatInt in 0..32) {
      val currentBeat = beatInt.toFloat()
      val playheadWorldX = currentBeat * beatW
      val targetCamX = playheadWorldX - (visibleTimelineW * 0.40f)
      val maxCamX = maxOf(0f, (exportBeats + 2) * beatW - visibleTimelineW)
      val cameraOffsetX = targetCamX.coerceIn(0f, maxCamX)

      val playheadScreenX = keyboardW + (currentBeat * beatW) - cameraOffsetX

      // Playhead screen coordinate must ALWAYS be framed in visible canvas
      assertTrue(
        "Playhead must be visible on screen at beat $currentBeat (screenX=$playheadScreenX)",
        playheadScreenX >= keyboardW && playheadScreenX <= width916
      )
      assertTrue("Camera offset must never be negative", cameraOffsetX >= 0f)
      assertTrue("Camera offset must never exceed maxCamX", cameraOffsetX <= maxCamX)
    }
  }

  @Test
  fun testMilestone8OptimizationAndScalability() {
    // Test performance & determinism across 4, 8, 16, and 32 bars
    val barLengths = listOf(4, 8, 16, 32)
    for (bars in barLengths) {
      val p = Project(bars = bars, seed = 999L)
      val notes = CompositionEngine.generateComposition(p)
      assertTrue("Notes generated for $bars bars", notes.isNotEmpty())

      // Deterministic seed: repeating with same seed produces identical notes
      val notesRepeat = CompositionEngine.generateComposition(p)
      assertEquals("Notes count must match identical seed", notes.size, notesRepeat.size)
      for (i in notes.indices) {
        assertEquals("Pitch at $i must match", notes[i].pitch, notesRepeat[i].pitch)
        assertEquals("Start beat at $i must match", notes[i].startBeat, notesRepeat[i].startBeat, 0.001f)
      }
    }

    // Test selective layer regeneration (preserving other layers)
    val projectBase = Project(bars = 8, seed = 1234L)
    val initialNotes = CompositionEngine.generateComposition(projectBase)
    val projectWithNotes = projectBase.copy(notes = initialNotes)

    val chordNotesBefore = initialNotes.filter { it.trackId == "track_chords" }
    val bassNotesBefore = initialNotes.filter { it.trackId == "track_bass" }

    // Regenerate ONLY melody
    val regeneratedNotes = CompositionEngine.regenerateLayer(projectWithNotes, MusicalLayer.MELODY, newSeed = 5678L)
    val chordNotesAfter = regeneratedNotes.filter { it.trackId == "track_chords" }
    val bassNotesAfter = regeneratedNotes.filter { it.trackId == "track_bass" }

    assertEquals("Chords must be fully preserved when melody is regenerated", chordNotesBefore.size, chordNotesAfter.size)
    assertEquals("Bass must be fully preserved when melody is regenerated", bassNotesBefore.size, bassNotesAfter.size)
  }

  @Test
  fun testVideoExporterPendingSampleBuffering() {
    val sampleVideo = VideoExporter.PendingSample(
      isVideo = true,
      data = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x67),
      size = 5,
      presentationTimeUs = 33333L,
      flags = 1 // Key frame
    )

    val sampleAudio = VideoExporter.PendingSample(
      isVideo = false,
      data = byteArrayOf(0x21, 0x10, 0x05),
      size = 3,
      presentationTimeUs = 23219L,
      flags = 0
    )

    assertTrue("Sample must be identified as video", sampleVideo.isVideo)
    assertFalse("Sample must be identified as audio", sampleAudio.isVideo)
    assertEquals(5, sampleVideo.size)
    assertEquals(3, sampleAudio.size)
    assertEquals(33333L, sampleVideo.presentationTimeUs)
    assertEquals(23219L, sampleAudio.presentationTimeUs)
    assertEquals(1, sampleVideo.flags)
    assertEquals(0, sampleAudio.flags)

    // Verify byte preservation
    assertEquals(0x67.toByte(), sampleVideo.data[4])
    assertEquals(0x21.toByte(), sampleAudio.data[0])

    // Verify ordering and track index separation
    val pendingList = mutableListOf(sampleVideo, sampleAudio)
    val videoSamples = pendingList.filter { it.isVideo }
    val audioSamples = pendingList.filter { !it.isVideo }

    assertEquals(1, videoSamples.size)
    assertEquals(1, audioSamples.size)
    assertEquals(33333L, videoSamples.first().presentationTimeUs)
    assertEquals(23219L, audioSamples.first().presentationTimeUs)
  }
}

