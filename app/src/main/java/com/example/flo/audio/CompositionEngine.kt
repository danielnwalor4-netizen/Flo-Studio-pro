package com.example.flo.audio

import com.example.flo.model.Complexity
import com.example.flo.model.KeyRoot
import com.example.flo.model.Mood
import com.example.flo.model.MusicalLayer
import com.example.flo.model.Note
import com.example.flo.model.Project
import com.example.flo.model.ScaleMode
import com.example.flo.model.TimeSignature
import java.util.Random
import java.util.UUID
import kotlin.math.abs

/**
 * M2: Automatic Procedural Music Composition Engine.
 * Generates deterministic, musically coherent arrangements across 4 discrete layers:
 * Melody, Chords, Bass, and Rhythm.
 */
object CompositionEngine {

    // General MIDI percussion pitch standards
    const val DRUM_KICK = 36       // Bass Drum
    const val DRUM_SNARE = 38      // Acoustic Snare
    const val DRUM_HIHAT_CLOSED = 42 // Closed Hi-Hat
    const val DRUM_HIHAT_OPEN = 46   // Open Hi-Hat

    /**
     * Generates a complete 4-layer composition arrangement.
     */
    fun generateComposition(project: Project): List<Note> {
        val random = Random(project.seed)
        val allNotes = mutableListOf<Note>()

        // 1. Generate Chord Progression
        val chordNotes = generateChords(project, random)
        allNotes.addAll(chordNotes)

        // 2. Generate Bassline supporting the progression
        val bassNotes = generateBass(project, random)
        allNotes.addAll(bassNotes)

        // 3. Generate Melodic Phrases
        val melodyNotes = generateMelody(project, random)
        allNotes.addAll(melodyNotes)

        // 4. Generate Rhythmic / Percussion Layer
        val rhythmNotes = generateRhythm(project, random)
        allNotes.addAll(rhythmNotes)

        return allNotes
    }

    /**
     * Regenerates a single specific musical layer while preserving all other layers.
     */
    fun regenerateLayer(project: Project, layer: MusicalLayer, newSeed: Long): List<Note> {
        val random = Random(newSeed)
        val targetTrackId = when (layer) {
            MusicalLayer.MELODY -> "track_melody"
            MusicalLayer.CHORDS -> "track_chords"
            MusicalLayer.BASS -> "track_bass"
            MusicalLayer.RHYTHM -> "track_rhythm"
        }

        // Keep notes from other tracks (including legacy track IDs)
        val preservedNotes = project.notes.filterNot {
            it.trackId == targetTrackId ||
            (layer == MusicalLayer.MELODY && (it.trackId == "track_piano" && it.pitch >= 55)) ||
            (layer == MusicalLayer.CHORDS && (it.trackId == "track_pad" || it.trackId == "track_strings")) ||
            (layer == MusicalLayer.BASS && it.trackId == "track_bass") ||
            (layer == MusicalLayer.RHYTHM && it.trackId == "track_rhythm")
        }

        val newLayerNotes = when (layer) {
            MusicalLayer.MELODY -> generateMelody(project.copy(seed = newSeed), random)
            MusicalLayer.CHORDS -> generateChords(project.copy(seed = newSeed), random)
            MusicalLayer.BASS -> generateBass(project.copy(seed = newSeed), random)
            MusicalLayer.RHYTHM -> generateRhythm(project.copy(seed = newSeed), random)
        }

        return preservedNotes + newLayerNotes
    }

    /**
     * Creates a musical variation of the project (melodic embellishment and rhythmic variation).
     */
    fun createVariation(project: Project, newSeed: Long): List<Note> {
        val random = Random(newSeed)
        val chordNotes = generateChords(project, random)
        val bassNotes = generateBass(project, random)
        val melodyNotes = generateMelody(project, random, isVariation = true)
        val rhythmNotes = generateRhythm(project, random)
        return chordNotes + bassNotes + melodyNotes + rhythmNotes
    }

    // ==========================================
    // 1. CHORD PROGRESSION GENERATOR
    // ==========================================

    private fun generateChords(project: Project, random: Random): List<Note> {
        val notes = mutableListOf<Note>()
        val totalBars = project.bars
        val beatsPerBar = project.beatsPerBar
        val rootOffset = project.keyRoot.pitchOffset
        val baseOctave = 48 + rootOffset // C3 register for chords

        val scaleIntervals = getScaleIntervals(project.scaleMode)

        // Select chord progression degrees (0..6) based on ScaleMode and Mood
        val progressionDegrees = selectChordProgression(project.scaleMode, project.mood, random)
        val beatsPerChord = beatsPerBar // 1 chord per bar

        for (bar in 0 until totalBars) {
            val degreeIndex = bar % progressionDegrees.size
            val rootDegree = progressionDegrees[degreeIndex]
            val currentBeat = bar * beatsPerBar

            // Build Triad or 7th/9th chord
            val rootPitch = baseOctave + scaleIntervals[rootDegree % 7]
            val thirdPitch = baseOctave + scaleIntervals[(rootDegree + 2) % 7] + (if ((rootDegree + 2) >= 7) 12 else 0)
            val fifthPitch = baseOctave + scaleIntervals[(rootDegree + 4) % 7] + (if ((rootDegree + 4) >= 7) 12 else 0)
            val seventhPitch = baseOctave + scaleIntervals[(rootDegree + 6) % 7] + (if ((rootDegree + 6) >= 7) 12 else 0)

            val chordPitches = when (project.complexity) {
                Complexity.SIMPLE -> listOf(rootPitch, thirdPitch, fifthPitch)
                Complexity.MODERATE -> listOf(rootPitch, thirdPitch, fifthPitch, seventhPitch)
                Complexity.COMPLEX -> {
                    val ninth = baseOctave + 12 + scaleIntervals[(rootDegree + 1) % 7]
                    listOf(rootPitch, thirdPitch, fifthPitch, ninth)
                }
            }

            // Duration: leave small breath before next bar
            val duration = beatsPerBar - 0.15f
            val chordVelocity = when (project.mood) {
                Mood.CALM, Mood.SAD, Mood.ROMANTIC -> 0.65f
                Mood.EPIC, Mood.ENERGETIC -> 0.85f
                else -> 0.72f
            }

            for (pitch in chordPitches) {
                notes.add(
                    Note(
                        id = UUID.randomUUID().toString(),
                        pitch = pitch.coerceIn(40, 76),
                        startBeat = currentBeat,
                        durationBeats = duration,
                        velocity = chordVelocity,
                        trackId = "track_chords"
                    )
                )
            }
        }
        return notes
    }

    // ==========================================
    // 2. BASSLINE GENERATOR
    // ==========================================

    private fun generateBass(project: Project, random: Random): List<Note> {
        val notes = mutableListOf<Note>()
        val totalBars = project.bars
        val beatsPerBar = project.beatsPerBar
        val rootOffset = project.keyRoot.pitchOffset
        val baseOctave = 24 + rootOffset // C1 register (24 to 48)

        val scaleIntervals = getScaleIntervals(project.scaleMode)
        val progressionDegrees = selectChordProgression(project.scaleMode, project.mood, random)

        for (bar in 0 until totalBars) {
            val rootDegree = progressionDegrees[bar % progressionDegrees.size]
            val currentBeat = bar * beatsPerBar
            val rootPitch = (baseOctave + scaleIntervals[rootDegree % 7]).coerceIn(24, 45)
            val fifthPitch = (rootPitch + 7).coerceIn(28, 50)

            when (project.complexity) {
                Complexity.SIMPLE -> {
                    // Sustained root note spanning the bar
                    notes.add(
                        Note(
                            pitch = rootPitch,
                            startBeat = currentBeat,
                            durationBeats = beatsPerBar - 0.2f,
                            velocity = 0.82f,
                            trackId = "track_bass"
                        )
                    )
                }
                Complexity.MODERATE -> {
                    // Root on beat 1, Fifth or octaves on beat 3
                    notes.add(
                        Note(
                            pitch = rootPitch,
                            startBeat = currentBeat,
                            durationBeats = 1.8f,
                            velocity = 0.85f,
                            trackId = "track_bass"
                        )
                    )
                    if (beatsPerBar >= 3f) {
                        val secondaryPitch = if (random.nextBoolean()) fifthPitch else rootPitch + 12
                        notes.add(
                            Note(
                                pitch = secondaryPitch,
                                startBeat = currentBeat + (if (beatsPerBar == 3f) 2f else 2f),
                                durationBeats = (beatsPerBar - 2f) - 0.2f,
                                velocity = 0.78f,
                                trackId = "track_bass"
                            )
                        )
                    }
                }
                Complexity.COMPLEX -> {
                    // Rhythmic bass pulse: driving 8th-note or syncopated groove
                    val stepCount = if (project.timeSignature == TimeSignature.TIME_6_8) 6 else (beatsPerBar * 2).toInt()
                    val stepSize = beatsPerBar / stepCount

                    for (step in 0 until stepCount) {
                        // Pattern filter: skip some off-beats for groove
                        if (step == 0 || step == 2 || (step == 3 && random.nextBoolean()) || step == 4) {
                            val p = if (step == 0) rootPitch else if (step % 2 == 0) fifthPitch else rootPitch
                            notes.add(
                                Note(
                                    pitch = p,
                                    startBeat = currentBeat + (step * stepSize),
                                    durationBeats = stepSize * 0.85f,
                                    velocity = if (step == 0) 0.90f else 0.75f,
                                    trackId = "track_bass"
                                )
                            )
                        }
                    }
                }
            }
        }
        return notes
    }

    // ==========================================
    // 3. MELODY GENERATOR
    // ==========================================

    private fun generateMelody(project: Project, random: Random, isVariation: Boolean = false): List<Note> {
        val notes = mutableListOf<Note>()
        val totalBars = project.bars
        val beatsPerBar = project.beatsPerBar
        val rootOffset = project.keyRoot.pitchOffset
        val melodyBaseOctave = 60 + rootOffset // C4 register (60 to 84)

        val scaleIntervals = getScaleIntervals(project.scaleMode)
        val progressionDegrees = selectChordProgression(project.scaleMode, project.mood, random)

        fun getScalePitch(degree: Int): Int {
            val oct = degree / 7
            val degInOct = (degree % 7 + 7) % 7
            return melodyBaseOctave + scaleIntervals[degInOct] + (oct * 12)
        }

        // Phrase length: 2 bars or 4 bars
        var currentDegree = random.nextInt(5) // Start around root to fifth
        val stepDiv = when (project.complexity) {
            Complexity.SIMPLE -> 1.0f  // Quarter notes
            Complexity.MODERATE -> 0.5f // 8th notes
            Complexity.COMPLEX -> 0.25f // 16th notes / syncopations
        }

        val totalBeats = totalBars * beatsPerBar
        var beat = 0f

        while (beat < totalBeats) {
            val bar = (beat / beatsPerBar).toInt()
            val beatInBar = beat % beatsPerBar
            val chordRootDegree = progressionDegrees[bar % progressionDegrees.size]

            // Rest probability depending on mood
            val restProb = when (project.mood) {
                Mood.CALM -> 0.40f
                Mood.SAD -> 0.35f
                Mood.ENERGETIC -> 0.15f
                else -> 0.25f
            }

            if (random.nextFloat() < restProb && beatInBar != 0f) {
                beat += stepDiv * (if (random.nextBoolean()) 2 else 1)
                continue
            }

            // Note pitch decision:
            // Strong beat -> prefer chord tones (root, 3rd, 5th, 7th)
            val isStrongBeat = (beatInBar == 0f || beatInBar == 2f)
            val targetDegree = if (isStrongBeat) {
                val chordToneOffsets = listOf(0, 2, 4, 6)
                chordRootDegree + chordToneOffsets[random.nextInt(chordToneOffsets.size)]
            } else {
                // Step-wise motion or passing note
                val step = if (random.nextBoolean()) 1 else -1
                (currentDegree + step).coerceIn(0, 14)
            }

            currentDegree = targetDegree
            var pitch = getScalePitch(currentDegree)

            // Embellish if variation mode
            if (isVariation && random.nextFloat() < 0.3f) {
                pitch += if (random.nextBoolean()) 2 else -2
            }

            pitch = pitch.coerceIn(55, 84)

            // Duration: note length
            val duration = when (project.complexity) {
                Complexity.SIMPLE -> (stepDiv * (1 + random.nextInt(2))).coerceAtMost(beatsPerBar - beatInBar) - 0.08f
                Complexity.MODERATE -> (stepDiv * (1 + random.nextInt(3))).coerceAtMost(2f) - 0.06f
                Complexity.COMPLEX -> (stepDiv * (1 + random.nextInt(2))).coerceAtMost(1.5f) - 0.04f
            }

            val velocity = (0.65f + random.nextFloat() * 0.25f).coerceIn(0.5f, 0.95f)

            notes.add(
                Note(
                    id = UUID.randomUUID().toString(),
                    pitch = pitch,
                    startBeat = beat,
                    durationBeats = maxOf(0.25f, duration),
                    velocity = velocity,
                    trackId = "track_melody"
                )
            )

            beat += maxOf(stepDiv, duration)
        }

        return notes
    }

    // ==========================================
    // 4. RHYTHM / DRUM GENERATOR
    // ==========================================

    private fun generateRhythm(project: Project, random: Random): List<Note> {
        val notes = mutableListOf<Note>()
        val totalBars = project.bars
        val beatsPerBar = project.beatsPerBar

        for (bar in 0 until totalBars) {
            val barStartBeat = bar * beatsPerBar

            when (project.timeSignature) {
                TimeSignature.TIME_4_4 -> {
                    // Kick on beat 0 and beat 2 (or 2.5)
                    notes.add(Note(pitch = DRUM_KICK, startBeat = barStartBeat, durationBeats = 0.25f, velocity = 0.92f, trackId = "track_rhythm"))
                    notes.add(Note(pitch = DRUM_KICK, startBeat = barStartBeat + 2f, durationBeats = 0.25f, velocity = 0.88f, trackId = "track_rhythm"))
                    if (project.complexity != Complexity.SIMPLE && random.nextBoolean()) {
                        notes.add(Note(pitch = DRUM_KICK, startBeat = barStartBeat + 2.5f, durationBeats = 0.25f, velocity = 0.80f, trackId = "track_rhythm"))
                    }

                    // Snare on beat 1 and beat 3 (backbeat)
                    notes.add(Note(pitch = DRUM_SNARE, startBeat = barStartBeat + 1f, durationBeats = 0.25f, velocity = 0.90f, trackId = "track_rhythm"))
                    notes.add(Note(pitch = DRUM_SNARE, startBeat = barStartBeat + 3f, durationBeats = 0.25f, velocity = 0.90f, trackId = "track_rhythm"))

                    // Hi-Hats: 8th notes (every 0.5 beat)
                    val hatCount = 8
                    for (h in 0 until hatCount) {
                        val hatBeat = barStartBeat + (h * 0.5f)
                        val isOpen = (h == 7 && random.nextBoolean())
                        val hatPitch = if (isOpen) DRUM_HIHAT_OPEN else DRUM_HIHAT_CLOSED
                        val hatVel = if (h % 2 == 0) 0.75f else 0.58f
                        notes.add(Note(pitch = hatPitch, startBeat = hatBeat, durationBeats = 0.2f, velocity = hatVel, trackId = "track_rhythm"))
                    }
                }
                TimeSignature.TIME_3_4 -> {
                    // 3/4 Waltz / Ballad rhythm
                    notes.add(Note(pitch = DRUM_KICK, startBeat = barStartBeat, durationBeats = 0.3f, velocity = 0.90f, trackId = "track_rhythm"))
                    notes.add(Note(pitch = DRUM_SNARE, startBeat = barStartBeat + 1f, durationBeats = 0.3f, velocity = 0.75f, trackId = "track_rhythm"))
                    notes.add(Note(pitch = DRUM_SNARE, startBeat = barStartBeat + 2f, durationBeats = 0.3f, velocity = 0.75f, trackId = "track_rhythm"))
                    for (h in 0 until 6) {
                        notes.add(Note(pitch = DRUM_HIHAT_CLOSED, startBeat = barStartBeat + (h * 0.5f), durationBeats = 0.2f, velocity = 0.60f, trackId = "track_rhythm"))
                    }
                }
                TimeSignature.TIME_6_8 -> {
                    // 6/8 Slow Compound Meter
                    notes.add(Note(pitch = DRUM_KICK, startBeat = barStartBeat, durationBeats = 0.3f, velocity = 0.92f, trackId = "track_rhythm"))
                    notes.add(Note(pitch = DRUM_SNARE, startBeat = barStartBeat + 1.5f, durationBeats = 0.3f, velocity = 0.88f, trackId = "track_rhythm"))
                    for (h in 0 until 6) {
                        notes.add(Note(pitch = DRUM_HIHAT_CLOSED, startBeat = barStartBeat + (h * 0.5f), durationBeats = 0.2f, velocity = 0.65f, trackId = "track_rhythm"))
                    }
                }
            }
        }
        return notes
    }

    // ==========================================
    // HARMONIC & SCALE UTILITIES
    // ==========================================

    private fun getScaleIntervals(scaleMode: ScaleMode): List<Int> {
        val base = scaleMode.intervals
        // Extended two octaves
        return base + base.map { it + 12 } + base.map { it + 24 }
    }

    private fun selectChordProgression(scaleMode: ScaleMode, mood: Mood, random: Random): List<Int> {
        return when (scaleMode) {
            ScaleMode.MAJOR -> when (mood) {
                Mood.HAPPY, Mood.ENERGETIC -> listOf(0, 4, 5, 3)     // I - V - vi - IV
                Mood.CALM, Mood.ROMANTIC -> listOf(0, 3, 4, 0)       // I - IV - V - I
                Mood.EMOTIONAL -> listOf(5, 3, 0, 4)                 // vi - IV - I - V
                Mood.EPIC -> listOf(0, 5, 3, 4)                      // I - vi - IV - V
                else -> listOf(0, 2, 4, 3)                           // I - iii - V - IV
            }
            ScaleMode.NATURAL_MINOR -> when (mood) {
                Mood.DARK -> listOf(0, 5, 3, 6)                      // i - VI - iv - VII
                Mood.SAD -> listOf(0, 3, 5, 4)                       // i - iv - VI - v
                Mood.EMOTIONAL -> listOf(5, 6, 0, 4)                 // VI - VII - i - v
                Mood.EPIC, Mood.ENERGETIC -> listOf(0, 6, 5, 6)      // i - VII - VI - VII
                else -> listOf(0, 5, 2, 6)                           // i - VI - III - VII
            }
            ScaleMode.HARMONIC_MINOR -> when (mood) {
                Mood.DARK, Mood.MYSTERIOUS -> listOf(0, 3, 4, 0)     // i - iv - V - i
                Mood.EPIC -> listOf(0, 5, 3, 4)                      // i - VI - iv - V
                else -> listOf(0, 3, 6, 4)                           // i - iv - vii° - V
            }
            ScaleMode.DORIAN -> when (mood) {
                Mood.MYSTERIOUS, Mood.CALM -> listOf(0, 3, 6, 0)     // i - IV - VII - i
                else -> listOf(0, 1, 3, 0)                           // i - ii - IV - i
            }
            ScaleMode.MIXOLYDIAN -> when (mood) {
                Mood.HAPPY, Mood.ENERGETIC -> listOf(0, 6, 3, 0)     // I - bVII - IV - I
                else -> listOf(0, 4, 6, 3)                           // I - v - bVII - IV
            }
            ScaleMode.PHRYGIAN -> when (mood) {
                Mood.DARK, Mood.MYSTERIOUS -> listOf(0, 1, 6, 0)     // i - bII - vii - i
                else -> listOf(0, 1, 0, 6)
            }
        }
    }
}
