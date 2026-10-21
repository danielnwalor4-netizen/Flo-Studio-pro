package com.example.flo.storage

import android.content.Context
import android.util.Log
import com.example.flo.model.Complexity
import com.example.flo.model.CreatorPreset
import com.example.flo.model.InstrumentType
import com.example.flo.model.KeyRoot
import com.example.flo.model.Mood
import com.example.flo.model.ScaleMode
import com.example.flo.model.TimeSignature
import org.json.JSONArray
import java.io.File

/**
 * M4: Repository for Creator Presets and Custom User Presets.
 */
class CreatorPresetRepository(private val context: Context) {
    private val TAG = "CreatorPresetRepo"
    private val customPresetsFile: File
        get() = File(context.filesDir, "custom_presets.json")

    /**
     * 17 Curated Creator Presets covering content creation needs.
     */
    val builtInPresets: List<CreatorPreset> = listOf(
        CreatorPreset(
            id = "preset_lofi",
            name = "Lo-fi Chill",
            category = "Chill & Lo-fi",
            description = "Dusty Rhodes chords, laid-back beat, and warm nostalgic keys",
            recommendedKey = KeyRoot.E,
            recommendedScale = ScaleMode.DORIAN,
            recommendedBpm = 78,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.CALM,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ELECTRIC,
                "track_chords" to InstrumentType.PAD_WARM,
                "track_bass" to InstrumentType.BASS_PLUCKED,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_ambient",
            name = "Ambient Cosmos",
            category = "Atmospheric",
            description = "Expansive floating pads, celestial bells, and serene meditation",
            recommendedKey = KeyRoot.D,
            recommendedScale = ScaleMode.MAJOR,
            recommendedBpm = 60,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.CALM,
            complexity = Complexity.SIMPLE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.BELL_SOFT,
                "track_chords" to InstrumentType.PAD_AMBIENT,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_cinematic",
            name = "Cinematic Story",
            category = "Cinematic & Film",
            description = "Dramatic string arrangements and expressive piano for documentary narrative",
            recommendedKey = KeyRoot.A,
            recommendedScale = ScaleMode.NATURAL_MINOR,
            recommendedBpm = 75,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.EMOTIONAL,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ACOUSTIC,
                "track_chords" to InstrumentType.STRINGS_ORCHESTRAL,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_dark_cinematic",
            name = "Dark Cinematic",
            category = "Cinematic & Film",
            description = "Ominous minor harmony, deep low end, and tense psychology themes",
            recommendedKey = KeyRoot.C,
            recommendedScale = ScaleMode.NATURAL_MINOR,
            recommendedBpm = 70,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.DARK,
            complexity = Complexity.COMPLEX,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ACOUSTIC,
                "track_chords" to InstrumentType.PAD_DARK,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_emotional_piano",
            name = "Emotional Piano",
            category = "Acoustic & Piano",
            description = "Intimate grand piano chords, expressive dynamics, and poignant lyrical melody",
            recommendedKey = KeyRoot.F,
            recommendedScale = ScaleMode.MAJOR,
            recommendedBpm = 68,
            recommendedTimeSignature = TimeSignature.TIME_3_4,
            mood = Mood.EMOTIONAL,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ACOUSTIC,
                "track_chords" to InstrumentType.STRINGS,
                "track_bass" to InstrumentType.BASS_PLUCKED,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_sad_piano",
            name = "Sad Piano Reflection",
            category = "Acoustic & Piano",
            description = "Sparse, lonely minor piano chords designed for poignant commentary",
            recommendedKey = KeyRoot.A,
            recommendedScale = ScaleMode.NATURAL_MINOR,
            recommendedBpm = 62,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.SAD,
            complexity = Complexity.SIMPLE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ACOUSTIC,
                "track_chords" to InstrumentType.PAD_WARM,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_chill",
            name = "Chill Coffee",
            category = "Chill & Lo-fi",
            description = "Soft plucks, warm organ groove, and steady modern chill flow",
            recommendedKey = KeyRoot.G,
            recommendedScale = ScaleMode.MAJOR,
            recommendedBpm = 85,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.CALM,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.SYNTH_PLUCK,
                "track_chords" to InstrumentType.ORGAN,
                "track_bass" to InstrumentType.BASS_PLUCKED,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_dreamy",
            name = "Dreamy Clouds",
            category = "Atmospheric",
            description = "Ethereal pads, delicate music box chimes, and impressionistic harmony",
            recommendedKey = KeyRoot.B,
            recommendedScale = ScaleMode.MAJOR,
            recommendedBpm = 65,
            recommendedTimeSignature = TimeSignature.TIME_6_8,
            mood = Mood.ROMANTIC,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.MUSIC_BOX,
                "track_chords" to InstrumentType.PAD_AMBIENT,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_romantic",
            name = "Romantic Melody",
            category = "Acoustic & Piano",
            description = "Heartwarming piano progression with soaring orchestral strings",
            recommendedKey = KeyRoot.E,
            recommendedScale = ScaleMode.MAJOR,
            recommendedBpm = 72,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.ROMANTIC,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ACOUSTIC,
                "track_chords" to InstrumentType.STRINGS_ORCHESTRAL,
                "track_bass" to InstrumentType.BASS_PLUCKED,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_epic_orchestral",
            name = "Epic Orchestral",
            category = "Cinematic & Film",
            description = "Stirring harmonic minor strings and thunderous pulses for cinematic builds",
            recommendedKey = KeyRoot.D,
            recommendedScale = ScaleMode.HARMONIC_MINOR,
            recommendedBpm = 110,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.EPIC,
            complexity = Complexity.COMPLEX,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.STRINGS_ORCHESTRAL,
                "track_chords" to InstrumentType.STRINGS,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_retro_synth",
            name = "Retro Synthwave",
            category = "Electronic & Modern",
            description = "80s analog synth leads, detuned warm pads, and punchy bass arps",
            recommendedKey = KeyRoot.A,
            recommendedScale = ScaleMode.NATURAL_MINOR,
            recommendedBpm = 115,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.ENERGETIC,
            complexity = Complexity.COMPLEX,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.SYNTH_BRIGHT_LEAD,
                "track_chords" to InstrumentType.SYNTH_WARM,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_electronic",
            name = "Electronic Pulse",
            category = "Electronic & Modern",
            description = "Driving synthesized rhythms and bright lead melodies for gaming content",
            recommendedKey = KeyRoot.F,
            recommendedScale = ScaleMode.DORIAN,
            recommendedBpm = 124,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.ENERGETIC,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.SYNTH_BRIGHT_LEAD,
                "track_chords" to InstrumentType.PAD_WARM,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_relaxing",
            name = "Relaxing Zen",
            category = "Atmospheric",
            description = "Pentatonic flute tones, soft bells, and flowing organic calm",
            recommendedKey = KeyRoot.G,
            recommendedScale = ScaleMode.MAJOR,
            recommendedBpm = 58,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.CALM,
            complexity = Complexity.SIMPLE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.FLUTE,
                "track_chords" to InstrumentType.BELL_SOFT,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_suspense",
            name = "Suspense & Tension",
            category = "Cinematic & Film",
            description = "Tense Phrygian dissonance and shadowy drone for mystery commentary",
            recommendedKey = KeyRoot.C_SHARP,
            recommendedScale = ScaleMode.PHRYGIAN,
            recommendedBpm = 80,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.MYSTERIOUS,
            complexity = Complexity.COMPLEX,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ACOUSTIC,
                "track_chords" to InstrumentType.PAD_DARK,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_mysterious",
            name = "Mysterious Enigma",
            category = "Atmospheric",
            description = "Modal Dorian progressions, music box chimes, and deep atmospheric swells",
            recommendedKey = KeyRoot.B,
            recommendedScale = ScaleMode.DORIAN,
            recommendedBpm = 72,
            recommendedTimeSignature = TimeSignature.TIME_6_8,
            mood = Mood.MYSTERIOUS,
            complexity = Complexity.MODERATE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.MUSIC_BOX,
                "track_chords" to InstrumentType.PAD_AMBIENT,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_energetic",
            name = "Energetic Momentum",
            category = "Electronic & Modern",
            description = "Fast Mixolydian lead, punchy drums, and uplifting high-energy pulse",
            recommendedKey = KeyRoot.A,
            recommendedScale = ScaleMode.MIXOLYDIAN,
            recommendedBpm = 130,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.ENERGETIC,
            complexity = Complexity.COMPLEX,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.SYNTH_BRIGHT_LEAD,
                "track_chords" to InstrumentType.ORGAN,
                "track_bass" to InstrumentType.BASS_SYNTH,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        ),
        CreatorPreset(
            id = "preset_minimalist",
            name = "Minimalist Patterns",
            category = "Acoustic & Piano",
            description = "Repetitive interlocking piano figures with slow incremental harmonic shifts",
            recommendedKey = KeyRoot.C,
            recommendedScale = ScaleMode.MAJOR,
            recommendedBpm = 95,
            recommendedTimeSignature = TimeSignature.TIME_4_4,
            mood = Mood.CALM,
            complexity = Complexity.SIMPLE,
            trackInstruments = mapOf(
                "track_melody" to InstrumentType.PIANO_ACOUSTIC,
                "track_chords" to InstrumentType.SYNTH_PLUCK,
                "track_bass" to InstrumentType.BASS_PLUCKED,
                "track_rhythm" to InstrumentType.PERCUSSION
            )
        )
    )

    fun getAllPresets(): List<CreatorPreset> {
        val userPresets = getCustomPresets()
        return builtInPresets + userPresets
    }

    fun getCustomPresets(): List<CreatorPreset> {
        val list = mutableListOf<CreatorPreset>()
        try {
            if (customPresetsFile.exists()) {
                val json = customPresetsFile.readText()
                if (json.isNotBlank()) {
                    val arr = JSONArray(json)
                    for (i in 0 until arr.length()) {
                        list.add(CreatorPreset.fromJson(arr.getJSONObject(i)))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read custom presets", e)
        }
        return list
    }

    fun saveCustomPreset(preset: CreatorPreset): Boolean {
        return try {
            val existing = getCustomPresets().filterNot { it.id == preset.id }.toMutableList()
            existing.add(preset.copy(isUserCustom = true))
            val arr = JSONArray()
            existing.forEach { arr.put(it.toJson()) }
            customPresetsFile.writeText(arr.toString(2))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom preset", e)
            false
        }
    }

    fun deleteCustomPreset(presetId: String): Boolean {
        return try {
            val existing = getCustomPresets().filterNot { it.id == presetId }
            val arr = JSONArray()
            existing.forEach { arr.put(it.toJson()) }
            customPresetsFile.writeText(arr.toString(2))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete custom preset", e)
            false
        }
    }
}
