package com.example.flo.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.pow

/**
 * Musical pitch utilities and MIDI helpers.
 * Standard MIDI pitch 60 is Middle C (C4), frequency 261.63 Hz.
 */
object MusicalPitch {
    val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun getPitchName(pitch: Int): String {
        val noteIndex = (pitch % 12 + 12) % 12
        val octave = (pitch / 12) - 1
        return "${NOTE_NAMES[noteIndex]}$octave"
    }

    fun isBlackKey(pitch: Int): Boolean {
        val noteIndex = (pitch % 12 + 12) % 12
        return noteIndex in listOf(1, 3, 6, 8, 10)
    }

    fun pitchToFrequency(pitch: Int): Double {
        return 440.0 * 2.0.pow((pitch - 69).toDouble() / 12.0)
    }

    const val MIN_PITCH = 24 // C1 (32.7 Hz)
    const val MAX_PITCH = 84 // C6 (1046.5 Hz)
    const val DEFAULT_ROOT_PITCH = 60 // C4
}

enum class VideoExportRatio(val displayName: String, val subtitle: String, val aspectWidth: Int, val aspectHeight: Int) {
    NONE("Viewport Off", "Full Workspace", 16, 9),
    RATIO_9_16("9:16 Shorts/TikTok", "Portrait Recording Frame", 9, 16),
    RATIO_16_9("16:9 YouTube", "Landscape Recording Frame", 16, 9)
}

/**
 * M2: Supported musical moods.
 */
enum class Mood(val displayName: String, val description: String) {
    CALM("Calm", "Gentle, spacious, smooth movement with floating harmony"),
    SAD("Sad", "Reflective, tender minor themes with intimate melancholy"),
    DARK("Dark", "Ominous, shadowy minor tones with deep lower register"),
    HAPPY("Happy", "Bright, uplifting major contours and warm bounce"),
    EMOTIONAL("Emotional", "Heartfelt, soaring progressions and lyrical arches"),
    EPIC("Epic", "Grand, powerful harmonic movement with driving energy"),
    MYSTERIOUS("Mysterious", "Suspenseful, modal ambiguity with intriguing intervals"),
    ROMANTIC("Romantic", "Warm impressionistic chords and expressive rubato phrases"),
    ENERGETIC("Energetic", "Pulsing rhythms, active basslines and driving drive")
}

enum class Genre(val displayName: String) {
    CINEMATIC("Cinematic"),
    AMBIENT("Ambient"),
    CLASSICAL("Classical"),
    ELECTRONIC("Electronic"),
    LO_FI("Lo-fi"),
    MINIMAL_PIANO("Minimal Piano"),
    EXPERIMENTAL("Experimental")
}

/**
 * M3: Expanded locally synthesized instrument families.
 */
enum class InstrumentType(val displayName: String, val category: String) {
    PIANO_ACOUSTIC("Grand Piano", "Keyboard"),
    PIANO_ELECTRIC("Electric Piano", "Keyboard"),
    BELL_SOFT("Soft Bell", "Chime"),
    MUSIC_BOX("Music Box", "Chime"),
    SYNTH_PLUCK("Plucked Synth", "Synth"),
    SYNTH_WARM("Warm Synthesizer", "Synth"),
    SYNTH_BRIGHT_LEAD("Bright Lead", "Synth"),
    SYNTH_SOFT_LEAD("Soft Lead", "Synth"),
    STRINGS("Strings Ensemble", "Orchestral"),
    STRINGS_ORCHESTRAL("Orchestral Strings", "Orchestral"),
    FLUTE("Flute Tone", "Woodwind"),
    ORGAN("Tonewheel Organ", "Keyboard"),
    BASS_SYNTH("Deep Synth Bass", "Bass"),
    BASS_PLUCKED("Plucked Bass", "Bass"),
    PAD_WARM("Warm Pad", "Pad"),
    PAD_DARK("Dark Pad", "Pad"),
    PAD_AMBIENT("Ambient Pad", "Pad"),
    PERCUSSION("Drum Kit", "Drums");

    // Backward compatibility helper
    companion object {
        fun fromLegacyOrName(name: String): InstrumentType {
            return when (name.uppercase()) {
                "PIANO" -> PIANO_ACOUSTIC
                "ELECTRIC_PIANO" -> PIANO_ELECTRIC
                "BELLS" -> BELL_SOFT
                "SYNTH_LEAD" -> SYNTH_BRIGHT_LEAD
                "SYNTH_PAD" -> PAD_WARM
                "BASS" -> BASS_SYNTH
                "AMBIENT_TEXTURE" -> PAD_AMBIENT
                else -> runCatching { valueOf(name) }.getOrDefault(PIANO_ACOUSTIC)
            }
        }
    }
}

enum class KeyRoot(val displayName: String, val pitchOffset: Int) {
    C("C", 0),
    C_SHARP("C#", 1),
    D("D", 2),
    D_SHARP("D#", 3),
    E("E", 4),
    F("F", 5),
    F_SHARP("F#", 6),
    G("G", 7),
    G_SHARP("G#", 8),
    A("A", 9),
    A_SHARP("A#", 10),
    B("B", 11)
}

/**
 * M2: Supported scales including Major, Natural Minor, Harmonic Minor, Dorian, Mixolydian, Phrygian.
 */
enum class ScaleMode(val displayName: String, val intervals: List<Int>) {
    MAJOR("Major", listOf(0, 2, 4, 5, 7, 9, 11)),
    NATURAL_MINOR("Natural Minor", listOf(0, 2, 3, 5, 7, 8, 10)),
    HARMONIC_MINOR("Harmonic Minor", listOf(0, 2, 3, 5, 7, 8, 11)),
    DORIAN("Dorian", listOf(0, 2, 3, 5, 7, 9, 10)),
    MIXOLYDIAN("Mixolydian", listOf(0, 2, 4, 5, 7, 9, 10)),
    PHRYGIAN("Phrygian", listOf(0, 1, 3, 5, 7, 8, 10));

    companion object {
        fun fromLegacyOrName(name: String): ScaleMode {
            return when (name.uppercase()) {
                "MINOR" -> NATURAL_MINOR
                else -> runCatching { valueOf(name) }.getOrDefault(MAJOR)
            }
        }
    }
}

enum class TimeSignature(val numerator: Int, val denominator: Int, val displayName: String, val beatsPerBar: Float) {
    TIME_4_4(4, 4, "4/4", 4.0f),
    TIME_3_4(3, 4, "3/4", 3.0f),
    TIME_6_8(6, 8, "6/8", 3.0f); // 2 dotted quarter beats in compound time

    companion object {
        fun fromNumDenom(num: Int, denom: Int): TimeSignature {
            return values().firstOrNull { it.numerator == num && it.denominator == denom } ?: TIME_4_4
        }
    }
}

enum class MusicalLayer(val displayName: String, val defaultTrackId: String) {
    MELODY("Melody", "track_melody"),
    CHORDS("Chords", "track_chords"),
    BASS("Bass", "track_bass"),
    RHYTHM("Rhythm / Drums", "track_rhythm")
}

enum class DurationPreset(val seconds: Int, val displayName: String) {
    DUR_15(15, "15s"),
    DUR_30(30, "30s"),
    DUR_60(60, "60s"),
    DUR_90(90, "90s"),
    DUR_120(120, "120s")
}

enum class Complexity(val displayName: String) {
    SIMPLE("Simple"),
    MODERATE("Moderate"),
    COMPLEX("Complex")
}

data class SoundParameters(
    val volume: Float = 1.0f,
    val reverb: Float = 16.0f,
    val attack: Float = 2.0f,
    val release: Float = 2.0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("volume", volume.toDouble())
        put("reverb", reverb.toDouble())
        put("attack", attack.toDouble())
        put("release", release.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): SoundParameters {
            return SoundParameters(
                volume = json.optDouble("volume", 1.0).toFloat(),
                reverb = json.optDouble("reverb", 16.0).toFloat(),
                attack = json.optDouble("attack", 2.0).toFloat(),
                release = json.optDouble("release", 2.0).toFloat()
            )
        }
    }
}

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val pitch: Int,             // MIDI note number (e.g. 60 = C4)
    val startBeat: Float,       // Position in musical beats
    val durationBeats: Float,   // Duration in beats
    val velocity: Float = 0.8f, // 0.0 to 1.0
    val trackId: String = "track_melody"
) {
    val endBeat: Float get() = startBeat + durationBeats

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("pitch", pitch)
        put("startBeat", startBeat.toDouble())
        put("durationBeats", durationBeats.toDouble())
        put("velocity", velocity.toDouble())
        put("trackId", trackId)
    }

    companion object {
        fun fromJson(json: JSONObject): Note {
            return Note(
                id = json.optString("id", UUID.randomUUID().toString()),
                pitch = json.getInt("pitch"),
                startBeat = json.getDouble("startBeat").toFloat(),
                durationBeats = json.getDouble("durationBeats").toFloat(),
                velocity = json.optDouble("velocity", 0.8).toFloat(),
                trackId = json.optString("trackId", "track_melody")
            )
        }
    }
}

/**
 * M5: Volume automation point.
 */
data class AutomationPoint(
    val beat: Float,
    val value: Float // 0.0f to 1.0f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("beat", beat.toDouble())
        put("value", value.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): AutomationPoint {
            return AutomationPoint(
                beat = json.getDouble("beat").toFloat(),
                value = json.getDouble("value").toFloat()
            )
        }
    }
}

data class TrackAutomation(
    val trackId: String,
    val points: List<AutomationPoint> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("trackId", trackId)
        val arr = JSONArray()
        points.forEach { arr.put(it.toJson()) }
        put("points", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): TrackAutomation {
            val trackId = json.getString("trackId")
            val arr = json.optJSONArray("points")
            val pts = mutableListOf<AutomationPoint>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    pts.add(AutomationPoint.fromJson(arr.getJSONObject(i)))
                }
            }
            return TrackAutomation(trackId, pts)
        }
    }
}

/**
 * M5: Song Structure Sections (Intro, Verse, Chorus, etc.)
 */
enum class SectionType(val displayName: String) {
    INTRO("Intro"),
    VERSE("Verse"),
    CHORUS("Chorus"),
    BREAKDOWN("Breakdown"),
    BUILDUP("Build-Up"),
    TRANSITION("Transition"),
    OUTRO("Outro"),
    LOOP("Loop")
}

data class ArrangementSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sectionType: SectionType,
    val startBar: Int,
    val lengthBars: Int,
    val energy: Float = 0.7f,
    val activeTrackIds: List<String> = listOf("track_melody", "track_chords", "track_bass", "track_rhythm")
) {
    val endBar: Int get() = startBar + lengthBars

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("sectionType", sectionType.name)
        put("startBar", startBar)
        put("lengthBars", lengthBars)
        put("energy", energy.toDouble())
        val arr = JSONArray()
        activeTrackIds.forEach { arr.put(it) }
        put("activeTrackIds", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): ArrangementSection {
            val arr = json.optJSONArray("activeTrackIds")
            val tracks = mutableListOf<String>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    tracks.add(arr.getString(i))
                }
            } else {
                tracks.addAll(listOf("track_melody", "track_chords", "track_bass", "track_rhythm"))
            }
            return ArrangementSection(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.getString("name"),
                sectionType = runCatching { SectionType.valueOf(json.getString("sectionType")) }.getOrDefault(SectionType.LOOP),
                startBar = json.getInt("startBar"),
                lengthBars = json.getInt("lengthBars"),
                energy = json.optDouble("energy", 0.7).toFloat(),
                activeTrackIds = tracks
            )
        }
    }
}

data class Track(
    val id: String,
    val name: String,
    val instrumentType: InstrumentType,
    val colorHex: Long,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("instrumentType", instrumentType.name)
        put("colorHex", colorHex)
        put("volume", volume.toDouble())
        put("isMuted", isMuted)
        put("isSolo", isSolo)
    }

    companion object {
        fun fromJson(json: JSONObject): Track {
            return Track(
                id = json.getString("id"),
                name = json.getString("name"),
                instrumentType = InstrumentType.fromLegacyOrName(json.getString("instrumentType")),
                colorHex = json.optLong("colorHex", 0xFF9D65FF),
                volume = json.optDouble("volume", 1.0).toFloat(),
                isMuted = json.optBoolean("isMuted", false),
                isSolo = json.optBoolean("isSolo", false)
            )
        }
    }
}

/**
 * M4: Creator Preset Definition.
 */
data class CreatorPreset(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val recommendedKey: KeyRoot,
    val recommendedScale: ScaleMode,
    val recommendedBpm: Int,
    val recommendedTimeSignature: TimeSignature,
    val mood: Mood,
    val complexity: Complexity,
    val trackInstruments: Map<String, InstrumentType>,
    val isUserCustom: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category)
        put("description", description)
        put("recommendedKey", recommendedKey.name)
        put("recommendedScale", recommendedScale.name)
        put("recommendedBpm", recommendedBpm)
        put("timeSignatureNumerator", recommendedTimeSignature.numerator)
        put("timeSignatureDenominator", recommendedTimeSignature.denominator)
        put("mood", mood.name)
        put("complexity", complexity.name)
        put("isUserCustom", isUserCustom)

        val instObj = JSONObject()
        trackInstruments.forEach { (k, v) -> instObj.put(k, v.name) }
        put("trackInstruments", instObj)
    }

    companion object {
        fun fromJson(json: JSONObject): CreatorPreset {
            val key = runCatching { KeyRoot.valueOf(json.getString("recommendedKey")) }.getOrDefault(KeyRoot.A)
            val scale = ScaleMode.fromLegacyOrName(json.getString("recommendedScale"))
            val mood = runCatching { Mood.valueOf(json.getString("mood")) }.getOrDefault(Mood.CALM)
            val comp = runCatching { Complexity.valueOf(json.getString("complexity")) }.getOrDefault(Complexity.MODERATE)
            val timeSig = TimeSignature.fromNumDenom(
                json.optInt("timeSignatureNumerator", 4),
                json.optInt("timeSignatureDenominator", 4)
            )

            val instMap = mutableMapOf<String, InstrumentType>()
            val instObj = json.optJSONObject("trackInstruments")
            if (instObj != null) {
                instObj.keys().forEach { k ->
                    instMap[k] = InstrumentType.fromLegacyOrName(instObj.getString(k))
                }
            } else {
                instMap["track_melody"] = InstrumentType.PIANO_ACOUSTIC
                instMap["track_chords"] = InstrumentType.PAD_WARM
                instMap["track_bass"] = InstrumentType.BASS_SYNTH
                instMap["track_rhythm"] = InstrumentType.PERCUSSION
            }

            return CreatorPreset(
                id = json.getString("id"),
                name = json.getString("name"),
                category = json.optString("category", "Custom"),
                description = json.optString("description", ""),
                recommendedKey = key,
                recommendedScale = scale,
                recommendedBpm = json.optInt("recommendedBpm", 75),
                recommendedTimeSignature = timeSig,
                mood = mood,
                complexity = comp,
                trackInstruments = instMap,
                isUserCustom = json.optBoolean("isUserCustom", false)
            )
        }
    }
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Composition",
    val bpm: Int = 75,
    val timeSignatureNumerator: Int = 4,
    val timeSignatureDenominator: Int = 4,
    val mood: Mood = Mood.DARK,
    val genre: Genre = Genre.CINEMATIC,
    val instrument: InstrumentType = InstrumentType.PIANO_ACOUSTIC,
    val keyRoot: KeyRoot = KeyRoot.A,
    val scaleMode: ScaleMode = ScaleMode.NATURAL_MINOR,
    val durationPreset: DurationPreset = DurationPreset.DUR_60,
    val complexity: Complexity = Complexity.MODERATE,
    val seed: Long = 12345L,
    val bars: Int = 16,
    val soundParameters: SoundParameters = SoundParameters(),
    val tracks: List<Track> = defaultTracks(),
    val notes: List<Note> = emptyList(),
    val sections: List<ArrangementSection> = defaultSections(16),
    val automations: List<TrackAutomation> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
) {
    val timeSignature: TimeSignature
        get() = TimeSignature.fromNumDenom(timeSignatureNumerator, timeSignatureDenominator)

    val beatsPerBar: Float get() = timeSignature.beatsPerBar

    val totalBeats: Float
        get() {
            val plannedBeats = bars * beatsPerBar
            val maxNoteBeat = notes.maxOfOrNull { it.endBeat } ?: plannedBeats
            return maxOf(plannedBeats, maxOf(16f, (maxNoteBeat / beatsPerBar).toInt() * beatsPerBar + beatsPerBar))
        }

    fun toJson(): String {
        val root = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("bpm", bpm)
            put("timeSignatureNumerator", timeSignatureNumerator)
            put("timeSignatureDenominator", timeSignatureDenominator)
            put("mood", mood.name)
            put("genre", genre.name)
            put("instrument", instrument.name)
            put("keyRoot", keyRoot.name)
            put("scaleMode", scaleMode.name)
            put("durationPreset", durationPreset.name)
            put("complexity", complexity.name)
            put("seed", seed)
            put("bars", bars)
            put("soundParameters", soundParameters.toJson())
            put("lastModified", lastModified)

            val tracksArray = JSONArray()
            tracks.forEach { tracksArray.put(it.toJson()) }
            put("tracks", tracksArray)

            val notesArray = JSONArray()
            notes.forEach { notesArray.put(it.toJson()) }
            put("notes", notesArray)

            val sectionsArray = JSONArray()
            sections.forEach { sectionsArray.put(it.toJson()) }
            put("sections", sectionsArray)

            val automationsArray = JSONArray()
            automations.forEach { automationsArray.put(it.toJson()) }
            put("automations", automationsArray)
        }
        return root.toString(2)
    }

    companion object {
        fun defaultTracks(): List<Track> = listOf(
            Track("track_melody", "Melody / Lead", InstrumentType.PIANO_ACOUSTIC, 0xFF9D65FF, volume = 0.95f),
            Track("track_chords", "Chords / Pad", InstrumentType.PAD_WARM, 0xFF00E5FF, volume = 0.80f),
            Track("track_bass", "Bassline", InstrumentType.BASS_SYNTH, 0xFF4A90E2, volume = 0.85f),
            Track("track_rhythm", "Drums / Rhythm", InstrumentType.PERCUSSION, 0xFFE056FD, volume = 0.75f)
        )

        fun defaultSections(bars: Int = 16): List<ArrangementSection> {
            return when (bars) {
                4 -> listOf(
                    ArrangementSection(name = "Loop", sectionType = SectionType.LOOP, startBar = 1, lengthBars = 4, energy = 0.8f)
                )
                8 -> listOf(
                    ArrangementSection(name = "Intro", sectionType = SectionType.INTRO, startBar = 1, lengthBars = 4, energy = 0.6f, activeTrackIds = listOf("track_chords", "track_melody")),
                    ArrangementSection(name = "Chorus", sectionType = SectionType.CHORUS, startBar = 5, lengthBars = 4, energy = 0.9f)
                )
                16 -> listOf(
                    ArrangementSection(name = "Intro", sectionType = SectionType.INTRO, startBar = 1, lengthBars = 4, energy = 0.5f, activeTrackIds = listOf("track_chords", "track_melody")),
                    ArrangementSection(name = "Verse", sectionType = SectionType.VERSE, startBar = 5, lengthBars = 4, energy = 0.7f, activeTrackIds = listOf("track_melody", "track_chords", "track_bass")),
                    ArrangementSection(name = "Chorus", sectionType = SectionType.CHORUS, startBar = 9, lengthBars = 4, energy = 1.0f),
                    ArrangementSection(name = "Outro", sectionType = SectionType.OUTRO, startBar = 13, lengthBars = 4, energy = 0.4f, activeTrackIds = listOf("track_chords", "track_melody"))
                )
                else -> listOf(
                    ArrangementSection(name = "Intro", sectionType = SectionType.INTRO, startBar = 1, lengthBars = 4, energy = 0.5f, activeTrackIds = listOf("track_chords", "track_melody")),
                    ArrangementSection(name = "Verse 1", sectionType = SectionType.VERSE, startBar = 5, lengthBars = 8, energy = 0.7f, activeTrackIds = listOf("track_melody", "track_chords", "track_bass")),
                    ArrangementSection(name = "Chorus 1", sectionType = SectionType.CHORUS, startBar = 13, lengthBars = 8, energy = 0.95f),
                    ArrangementSection(name = "Breakdown", sectionType = SectionType.BREAKDOWN, startBar = 21, lengthBars = 4, energy = 0.4f, activeTrackIds = listOf("track_chords")),
                    ArrangementSection(name = "Outro", sectionType = SectionType.OUTRO, startBar = 25, lengthBars = 8, energy = 0.5f, activeTrackIds = listOf("track_chords", "track_melody"))
                )
            }
        }

        fun fromJson(jsonString: String): Project {
            val root = JSONObject(jsonString)
            val mood = when (root.optString("mood", "DARK").uppercase()) {
                "PEACEFUL" -> Mood.CALM
                "MELANCHOLIC" -> Mood.SAD
                "UPLIFTING" -> Mood.HAPPY
                "TENSE" -> Mood.EPIC
                "DREAMY" -> Mood.ROMANTIC
                else -> runCatching { Mood.valueOf(root.getString("mood")) }.getOrDefault(Mood.DARK)
            }
            val genre = runCatching { Genre.valueOf(root.getString("genre")) }.getOrDefault(Genre.CINEMATIC)
            val instrument = InstrumentType.fromLegacyOrName(root.optString("instrument", "PIANO_ACOUSTIC"))
            val keyRoot = runCatching { KeyRoot.valueOf(root.getString("keyRoot")) }.getOrDefault(KeyRoot.A)
            val scaleMode = ScaleMode.fromLegacyOrName(root.optString("scaleMode", "NATURAL_MINOR"))
            val durationPreset = runCatching { DurationPreset.valueOf(root.getString("durationPreset")) }.getOrDefault(DurationPreset.DUR_60)
            val complexity = runCatching { Complexity.valueOf(root.getString("complexity")) }.getOrDefault(Complexity.MODERATE)
            val seed = root.optLong("seed", 12345L)
            val bars = root.optInt("bars", 16)

            val tracks = mutableListOf<Track>()
            val tracksArray = root.optJSONArray("tracks")
            if (tracksArray != null) {
                for (i in 0 until tracksArray.length()) {
                    tracks.add(Track.fromJson(tracksArray.getJSONObject(i)))
                }
            } else {
                tracks.addAll(defaultTracks())
            }

            val notes = mutableListOf<Note>()
            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    notes.add(Note.fromJson(notesArray.getJSONObject(i)))
                }
            }

            val sections = mutableListOf<ArrangementSection>()
            val sectionsArray = root.optJSONArray("sections")
            if (sectionsArray != null) {
                for (i in 0 until sectionsArray.length()) {
                    sections.add(ArrangementSection.fromJson(sectionsArray.getJSONObject(i)))
                }
            } else {
                sections.addAll(defaultSections(bars))
            }

            val automations = mutableListOf<TrackAutomation>()
            val autoArray = root.optJSONArray("automations")
            if (autoArray != null) {
                for (i in 0 until autoArray.length()) {
                    automations.add(TrackAutomation.fromJson(autoArray.getJSONObject(i)))
                }
            }

            val soundParams = root.optJSONObject("soundParameters")?.let {
                SoundParameters.fromJson(it)
            } ?: SoundParameters()

            return Project(
                id = root.optString("id", UUID.randomUUID().toString()),
                name = root.optString("name", "Untitled Composition"),
                bpm = root.optInt("bpm", 75),
                timeSignatureNumerator = root.optInt("timeSignatureNumerator", 4),
                timeSignatureDenominator = root.optInt("timeSignatureDenominator", 4),
                mood = mood,
                genre = genre,
                instrument = instrument,
                keyRoot = keyRoot,
                scaleMode = scaleMode,
                durationPreset = durationPreset,
                complexity = complexity,
                seed = seed,
                bars = bars,
                soundParameters = soundParams,
                tracks = if (tracks.isEmpty()) defaultTracks() else tracks,
                notes = notes,
                sections = if (sections.isEmpty()) defaultSections(bars) else sections,
                automations = automations,
                lastModified = root.optLong("lastModified", System.currentTimeMillis())
            )
        }
    }
}
