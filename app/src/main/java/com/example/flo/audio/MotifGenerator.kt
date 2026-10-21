package com.example.flo.audio

import com.example.flo.model.Complexity
import com.example.flo.model.Genre
import com.example.flo.model.KeyRoot
import com.example.flo.model.Mood
import com.example.flo.model.Note
import com.example.flo.model.Project
import com.example.flo.model.ScaleMode
import java.util.UUID

/**
 * Algorithmic motif and progression composer for M1/M2 backward compatibility.
 * Connects the user's selected Mood, Key, Genre, and Complexity
 * into musically coherent chord progressions and melodic lines on the piano roll.
 */
object MotifGenerator {

    fun generateMotif(project: Project): List<Note> {
        val rootOffset = project.keyRoot.pitchOffset
        val isMinor = project.scaleMode != ScaleMode.MAJOR && project.scaleMode != ScaleMode.MIXOLYDIAN

        // Base octave for chord roots: 48 (C3) or 36 (C2)
        val baseOctave = 48 + rootOffset
        val notes = mutableListOf<Note>()

        // Chord progression indices (scale degrees 0..6)
        val progressionDegrees = when (project.mood) {
            Mood.DARK -> if (isMinor) listOf(0, 5, 3, 6) else listOf(5, 3, 0, 4) // i - VI - iv - VII
            Mood.SAD -> if (isMinor) listOf(0, 3, 5, 4) else listOf(5, 0, 3, 4) // i - iv - VI - v
            Mood.MYSTERIOUS -> if (isMinor) listOf(0, 1, 0, 6) else listOf(0, 1, 5, 4) // Phrygian touch
            Mood.CALM -> if (isMinor) listOf(0, 2, 5, 2) else listOf(0, 4, 5, 3) // I - V - vi - IV
            Mood.EMOTIONAL -> if (isMinor) listOf(5, 0, 6, 2) else listOf(0, 5, 3, 4) // VI - i - VII - III
            Mood.HAPPY -> if (isMinor) listOf(2, 6, 0, 4) else listOf(0, 4, 5, 3) // III - VII - i - v
            Mood.EPIC -> if (isMinor) listOf(0, 0, 1, 6) else listOf(0, 1, 0, 6) // Rapid pulsing
            Mood.ROMANTIC -> if (isMinor) listOf(5, 2, 0, 6) else listOf(3, 4, 0, 5) // IV - V - I - vi
            Mood.ENERGETIC -> if (isMinor) listOf(0, 6, 5, 6) else listOf(0, 4, 0, 5)
        }

        val scaleIntervals = if (isMinor) {
            listOf(0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24)
        } else {
            listOf(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24)
        }

        fun degreeToPitch(degree: Int, octaveOffset: Int = 0): Int {
            val interval = scaleIntervals.getOrElse(degree) { degree }
            return baseOctave + interval + (octaveOffset * 12)
        }

        // 4 bars of 4 beats each = 16 beats total
        var currentBeat = 0f
        val beatsPerChord = 4f

        for ((chordIndex, degree) in progressionDegrees.withIndex()) {
            val chordRoot = degreeToPitch(degree, octaveOffset = -1) // Bass octave (C2 - B2)
            val third = degreeToPitch(degree + 2)                   // Mid octave (C3 - B3)
            val fifth = degreeToPitch(degree + 4)
            val seventh = degreeToPitch(degree + 6)
            val ninth = degreeToPitch(degree + 8)

            // 1. Sustained Bass Note
            notes.add(
                Note(
                    id = UUID.randomUUID().toString(),
                    pitch = chordRoot,
                    startBeat = currentBeat,
                    durationBeats = beatsPerChord,
                    velocity = 0.85f,
                    trackId = "track_bass"
                )
            )

            // 2. Chords / Arpeggio depending on complexity & genre
            when (project.complexity) {
                Complexity.SIMPLE -> {
                    // Block chords on beats 0 and 2
                    notes.add(
                        Note(
                            pitch = third,
                            startBeat = currentBeat,
                            durationBeats = 1.8f,
                            velocity = 0.70f,
                            trackId = "track_melody"
                        )
                    )
                    notes.add(
                        Note(
                            pitch = fifth,
                            startBeat = currentBeat,
                            durationBeats = 1.8f,
                            velocity = 0.75f,
                            trackId = "track_melody"
                        )
                    )
                    notes.add(
                        Note(
                            pitch = third,
                            startBeat = currentBeat + 2f,
                            durationBeats = 1.8f,
                            velocity = 0.65f,
                            trackId = "track_melody"
                        )
                    )
                    notes.add(
                        Note(
                            pitch = fifth,
                            startBeat = currentBeat + 2f,
                            durationBeats = 1.8f,
                            velocity = 0.70f,
                            trackId = "track_melody"
                        )
                    )
                }

                Complexity.MODERATE -> {
                    // Flowing arpeggio pattern: Root -> Fifth -> Third+Octave -> Ninth
                    val arpSteps = listOf(
                        Pair(0f, third),
                        Pair(1f, fifth),
                        Pair(2f, degreeToPitch(degree + 2, 1)), // Higher octave
                        Pair(3f, ninth)
                    )
                    for ((offset, pitch) in arpSteps) {
                        notes.add(
                            Note(
                                pitch = pitch,
                                startBeat = currentBeat + offset,
                                durationBeats = 0.95f,
                                velocity = 0.72f,
                                trackId = "track_melody"
                            )
                        )
                    }

                    // Gentle counter-melody note on beat 2.5
                    if (chordIndex % 2 == 1) {
                        notes.add(
                            Note(
                                pitch = degreeToPitch(degree + 4, 1),
                                startBeat = currentBeat + 2.5f,
                                durationBeats = 0.75f,
                                velocity = 0.68f,
                                trackId = "track_melody"
                            )
                        )
                    }
                }

                Complexity.COMPLEX -> {
                    // Rich cinematic polyphony with 8th notes
                    val pattern = listOf(
                        Pair(0f, third),
                        Pair(0.5f, fifth),
                        Pair(1.0f, degreeToPitch(degree + 2, 1)),
                        Pair(1.5f, seventh),
                        Pair(2.0f, ninth),
                        Pair(2.5f, degreeToPitch(degree + 4, 1)),
                        Pair(3.0f, degreeToPitch(degree + 2, 1)),
                        Pair(3.5f, fifth)
                    )
                    for ((offset, pitch) in pattern) {
                        notes.add(
                            Note(
                                pitch = pitch,
                                startBeat = currentBeat + offset,
                                durationBeats = 0.48f,
                                velocity = if (offset % 1f == 0f) 0.80f else 0.65f,
                                trackId = "track_melody"
                            )
                        )
                    }
                }
            }

            // Sustained Ambient Pad / Strings texture
            if (project.genre == Genre.AMBIENT || project.genre == Genre.CINEMATIC) {
                notes.add(
                    Note(
                        pitch = degreeToPitch(degree + 4, 1),
                        startBeat = currentBeat,
                        durationBeats = beatsPerChord,
                        velocity = 0.55f,
                        trackId = "track_chords"
                    )
                )
            }

            currentBeat += beatsPerChord
        }

        return notes
    }
}
