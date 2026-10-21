package com.example.flo.audio

import com.example.flo.model.ArrangementSection
import com.example.flo.model.AutomationPoint
import com.example.flo.model.Note
import com.example.flo.model.Project
import com.example.flo.model.SectionType
import com.example.flo.model.TrackAutomation
import java.util.Random
import java.util.UUID

/**
 * M5: Automatic Arrangement Engine.
 * Structures musical ideas into complete compositions with sections,
 * layer entries/exits, dynamics, and volume automation.
 */
object ArrangementEngine {

    enum class ArrangementStructure(val displayName: String, val recommendedBars: Int) {
        SHORT_LOOP("Short Loop", 4),
        STANDARD_8("8-Bar Loop & Chorus", 8),
        STANDARD_SONG("16-Bar Standard Song", 16),
        COMPLETE_ARRANGEMENT("32-Bar Full Arrangement", 32),
        CINEMATIC_BUILD("32-Bar Cinematic Build", 32)
    }

    /**
     * Plans sections for the requested structure and total bars.
     */
    fun planSections(structure: ArrangementStructure, totalBars: Int): List<ArrangementSection> {
        return when (structure) {
            ArrangementStructure.SHORT_LOOP -> listOf(
                ArrangementSection(
                    name = "Loop",
                    sectionType = SectionType.LOOP,
                    startBar = 1,
                    lengthBars = totalBars,
                    energy = 0.85f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass", "track_rhythm")
                )
            )
            ArrangementStructure.STANDARD_8 -> listOf(
                ArrangementSection(
                    name = "Intro",
                    sectionType = SectionType.INTRO,
                    startBar = 1,
                    lengthBars = 4,
                    energy = 0.6f,
                    activeTrackIds = listOf("track_chords", "track_melody")
                ),
                ArrangementSection(
                    name = "Chorus",
                    sectionType = SectionType.CHORUS,
                    startBar = 5,
                    lengthBars = 4,
                    energy = 0.95f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass", "track_rhythm")
                )
            )
            ArrangementStructure.STANDARD_SONG -> listOf(
                ArrangementSection(
                    name = "Intro",
                    sectionType = SectionType.INTRO,
                    startBar = 1,
                    lengthBars = 4,
                    energy = 0.5f,
                    activeTrackIds = listOf("track_chords", "track_melody")
                ),
                ArrangementSection(
                    name = "Verse",
                    sectionType = SectionType.VERSE,
                    startBar = 5,
                    lengthBars = 4,
                    energy = 0.7f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass")
                ),
                ArrangementSection(
                    name = "Chorus",
                    sectionType = SectionType.CHORUS,
                    startBar = 9,
                    lengthBars = 4,
                    energy = 1.0f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass", "track_rhythm")
                ),
                ArrangementSection(
                    name = "Outro",
                    sectionType = SectionType.OUTRO,
                    startBar = 13,
                    lengthBars = 4,
                    energy = 0.45f,
                    activeTrackIds = listOf("track_chords", "track_melody")
                )
            )
            ArrangementStructure.COMPLETE_ARRANGEMENT -> listOf(
                ArrangementSection(
                    name = "Intro",
                    sectionType = SectionType.INTRO,
                    startBar = 1,
                    lengthBars = 4,
                    energy = 0.45f,
                    activeTrackIds = listOf("track_chords", "track_melody")
                ),
                ArrangementSection(
                    name = "Verse 1",
                    sectionType = SectionType.VERSE,
                    startBar = 5,
                    lengthBars = 8,
                    energy = 0.70f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass")
                ),
                ArrangementSection(
                    name = "Chorus 1",
                    sectionType = SectionType.CHORUS,
                    startBar = 13,
                    lengthBars = 8,
                    energy = 0.95f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass", "track_rhythm")
                ),
                ArrangementSection(
                    name = "Breakdown",
                    sectionType = SectionType.BREAKDOWN,
                    startBar = 21,
                    lengthBars = 4,
                    energy = 0.40f,
                    activeTrackIds = listOf("track_chords")
                ),
                ArrangementSection(
                    name = "Outro",
                    sectionType = SectionType.OUTRO,
                    startBar = 25,
                    lengthBars = 8,
                    energy = 0.50f,
                    activeTrackIds = listOf("track_chords", "track_melody")
                )
            )
            ArrangementStructure.CINEMATIC_BUILD -> listOf(
                ArrangementSection(
                    name = "Theme Swell",
                    sectionType = SectionType.INTRO,
                    startBar = 1,
                    lengthBars = 8,
                    energy = 0.4f,
                    activeTrackIds = listOf("track_chords")
                ),
                ArrangementSection(
                    name = "Build-Up",
                    sectionType = SectionType.BUILDUP,
                    startBar = 9,
                    lengthBars = 8,
                    energy = 0.75f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass")
                ),
                ArrangementSection(
                    name = "Climax",
                    sectionType = SectionType.CHORUS,
                    startBar = 17,
                    lengthBars = 12,
                    energy = 1.0f,
                    activeTrackIds = listOf("track_melody", "track_chords", "track_bass", "track_rhythm")
                ),
                ArrangementSection(
                    name = "Resolution",
                    sectionType = SectionType.OUTRO,
                    startBar = 29,
                    lengthBars = 4,
                    energy = 0.35f,
                    activeTrackIds = listOf("track_chords", "track_melody")
                )
            )
        }
    }

    /**
     * Arranges the composition notes according to the planned sections,
     * gating instruments appropriately per section and adjusting velocities and dynamics.
     */
    fun arrangeComposition(project: Project, sections: List<ArrangementSection>): Pair<List<Note>, List<TrackAutomation>> {
        val rawNotes = CompositionEngine.generateComposition(project)
        val arrangedNotes = mutableListOf<Note>()
        val beatsPerBar = project.beatsPerBar

        for (section in sections) {
            val sectionStartBeat = (section.startBar - 1) * beatsPerBar
            val sectionEndBeat = (section.startBar - 1 + section.lengthBars) * beatsPerBar

            // Filter raw notes falling within this section's time window
            val sectionNotes = rawNotes.filter { note ->
                note.startBeat >= sectionStartBeat && note.startBeat < sectionEndBeat
            }

            for (note in sectionNotes) {
                // Check if this note's track is active in this section
                val isTrackActive = section.activeTrackIds.contains(note.trackId) ||
                        (note.trackId == "track_piano" && section.activeTrackIds.contains("track_melody")) ||
                        (note.trackId == "track_pad" && section.activeTrackIds.contains("track_chords"))

                if (isTrackActive) {
                    // Scale velocity according to section energy
                    val scaledVelocity = (note.velocity * section.energy).coerceIn(0.2f, 1.0f)
                    arrangedNotes.add(note.copy(velocity = scaledVelocity))
                }
            }
        }

        // Generate Volume Automation curves for smooth transitions
        val automations = mutableListOf<TrackAutomation>()
        val trackIds = listOf("track_melody", "track_chords", "track_bass", "track_rhythm")

        for (trackId in trackIds) {
            val points = mutableListOf<AutomationPoint>()
            for (section in sections) {
                val startBeat = (section.startBar - 1) * beatsPerBar
                val endBeat = (section.startBar - 1 + section.lengthBars) * beatsPerBar

                val isActive = section.activeTrackIds.contains(trackId)
                val targetVol = if (isActive) section.energy else 0.0f

                points.add(AutomationPoint(startBeat, targetVol))
                points.add(AutomationPoint(endBeat - 0.2f, targetVol))
            }
            automations.add(TrackAutomation(trackId, points))
        }

        return Pair(arrangedNotes, automations)
    }
}
