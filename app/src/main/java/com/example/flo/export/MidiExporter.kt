package com.example.flo.export

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.flo.audio.CompositionEngine
import com.example.flo.model.InstrumentType
import com.example.flo.model.Note
import com.example.flo.model.Project
import com.example.flo.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Milestone 6: Standard MIDI File (SMF Format 1) Multi-Track Exporter.
 * Generates standards-compliant .mid files with separate tracks for Melody, Chords, Bass, and Rhythm,
 * preserving timing resolution (PPQN=480), tempo, time signatures, velocities, and GM instrument programs.
 */
object MidiExporter {
    private const val TAG = "FloMidiExporter"
    const val PPQN = 480 // Standard Pulses Per Quarter Note resolution

    private sealed class MidiEvent(val tick: Long) {
        class NoteOn(tick: Long, val channel: Int, val pitch: Int, val velocity: Int) : MidiEvent(tick)
        class NoteOff(tick: Long, val channel: Int, val pitch: Int) : MidiEvent(tick)
        class ProgramChange(tick: Long, val channel: Int, val program: Int) : MidiEvent(tick)
        class TrackName(tick: Long, val name: String) : MidiEvent(tick)
    }

    suspend fun exportMidi(
        context: Context,
        project: Project,
        customFileName: String? = null
    ): File? = withContext(Dispatchers.Default) {
        val safeProjectName = project.name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = if (!customFileName.isNullOrBlank()) {
            val base = customFileName.trim()
            if (base.endsWith(".mid", ignoreCase = true)) base else "$base.mid"
        } else {
            "FloStudio_${safeProjectName}_$timestamp.mid"
        }

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "Music").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)
        if (outputFile.exists()) outputFile.delete()

        try {
            val midiBytes = generateMidiBytes(project)
            FileOutputStream(outputFile).use { it.write(midiBytes) }
            MediaPublisher.publishExport(context, outputFile, "audio/midi", "FloStudio")
            Log.d(TAG, "MIDI export succeeded: ${outputFile.absolutePath} (${midiBytes.size} bytes)")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "MIDI export failed", e)
            outputFile.delete()
            null
        }
    }

    /**
     * Builds a valid Standard MIDI File (Format 1) as a byte array.
     */
    fun generateMidiBytes(project: Project): ByteArray {
        val tracksToExport = if (project.tracks.isNotEmpty()) {
            project.tracks
        } else {
            listOf(Track("track_master", "Master Track", instrumentType = project.instrument, colorHex = 0xFF9D65FFL))
        }

        val trackCount = 1 + tracksToExport.size // Conductor track + individual instrument tracks
        val out = ByteArrayOutputStream()

        // 1. Header Chunk (MThd)
        out.write("MThd".toByteArray(Charsets.US_ASCII))
        writeBigEndian32(out, 6) // Header length is 6 bytes
        writeBigEndian16(out, 1) // Format 1: Multi-track synchronous
        writeBigEndian16(out, trackCount)
        writeBigEndian16(out, PPQN) // 480 ticks per quarter note

        // 2. Track 0: Conductor Track (Tempo, Time Signature, Song Name)
        val conductorTrack = buildConductorTrack(project)
        out.write(conductorTrack)

        // 3. Tracks 1..N: Musical Tracks
        tracksToExport.forEachIndexed { index, track ->
            val channel = if (track.id == "track_rhythm" || track.instrumentType == InstrumentType.PERCUSSION) {
                9 // Channel 10 (0-indexed 9) is standard General MIDI Percussion
            } else {
                index % 16.let { if (it >= 9) it + 1 else it } // Avoid channel 9 for melodic instruments
            }

            val trackNotes = project.notes.filter { it.trackId == track.id }
            val trackBytes = buildInstrumentTrack(track, channel, trackNotes, project)
            out.write(trackBytes)
        }

        return out.toByteArray()
    }

    private fun buildConductorTrack(project: Project): ByteArray {
        val trackData = ByteArrayOutputStream()
        var currentTick = 0L

        // Sequence / Track Name Meta Event
        writeVlq(trackData, 0)
        trackData.write(0xFF)
        trackData.write(0x03)
        val nameBytes = project.name.toByteArray(Charsets.UTF_8)
        writeVlq(trackData, nameBytes.size.toLong())
        trackData.write(nameBytes)

        // Time Signature Meta Event: FF 58 04 nn dd cc bb
        // nn: numerator, dd: log2(denominator), cc: 24 MIDI clocks per metronome tick, bb: 8 32nd notes per quarter note
        writeVlq(trackData, 0)
        trackData.write(0xFF)
        trackData.write(0x58)
        trackData.write(0x04)
        trackData.write(project.timeSignatureNumerator)
        val denomPower = when (project.timeSignatureDenominator) {
            2 -> 1
            4 -> 2
            8 -> 3
            16 -> 4
            else -> 2
        }
        trackData.write(denomPower)
        trackData.write(24) // 24 MIDI clocks per metronome click
        trackData.write(8)  // 8 32nd notes in a MIDI quarter note

        // Set Tempo Meta Event: FF 51 03 tt tt tt (microseconds per quarter note)
        val bpm = project.bpm.coerceIn(20, 300)
        val usPerQuarter = (60_000_000L / bpm).toInt()
        writeVlq(trackData, 0)
        trackData.write(0xFF)
        trackData.write(0x51)
        trackData.write(0x03)
        trackData.write((usPerQuarter shr 16) and 0xFF)
        trackData.write((usPerQuarter shr 8) and 0xFF)
        trackData.write(usPerQuarter and 0xFF)

        // End of Track Meta Event: FF 2F 00
        val totalTicks = (project.totalBeats * PPQN).toLong()
        val deltaToEnd = (totalTicks - currentTick).coerceAtLeast(0L)
        writeVlq(trackData, deltaToEnd)
        trackData.write(0xFF)
        trackData.write(0x2F)
        trackData.write(0x00)

        // Wrap into MTrk chunk
        val result = ByteArrayOutputStream()
        result.write("MTrk".toByteArray(Charsets.US_ASCII))
        writeBigEndian32(result, trackData.size())
        result.write(trackData.toByteArray())
        return result.toByteArray()
    }

    private fun buildInstrumentTrack(
        track: Track,
        channel: Int,
        notes: List<Note>,
        project: Project
    ): ByteArray {
        val trackData = ByteArrayOutputStream()

        // Track Name Meta Event
        writeVlq(trackData, 0)
        trackData.write(0xFF)
        trackData.write(0x03)
        val trackNameBytes = track.name.toByteArray(Charsets.UTF_8)
        writeVlq(trackData, trackNameBytes.size.toLong())
        trackData.write(trackNameBytes)

        // Program Change (if not drum channel)
        if (channel != 9) {
            val gmProgram = getGeneralMidiProgram(track.instrumentType)
            writeVlq(trackData, 0)
            trackData.write(0xC0 or (channel and 0x0F))
            trackData.write(gmProgram and 0x7F)
        }

        // Collect and sort note events
        val events = mutableListOf<MidiEvent>()
        for (note in notes) {
            val startTick = (note.startBeat * PPQN).toLong().coerceAtLeast(0L)
            val durationTicks = (note.durationBeats * PPQN).toLong().coerceAtLeast(1L)
            val endTick = startTick + durationTicks
            val velocity = (note.velocity * 127).roundToInt().coerceIn(1, 127)

            // Pitch mapping for drums if percussion track
            val midiPitch = if (channel == 9) {
                mapDrumPitchToGm(note.pitch)
            } else {
                note.pitch.coerceIn(0, 127)
            }

            events.add(MidiEvent.NoteOn(startTick, channel, midiPitch, velocity))
            events.add(MidiEvent.NoteOff(endTick, channel, midiPitch))
        }

        // Sort events: chronologically by tick. If ticks match, NoteOff before NoteOn
        events.sortWith(compareBy({ it.tick }, { if (it is MidiEvent.NoteOff) 0 else 1 }))

        var lastTick = 0L
        for (event in events) {
            val delta = (event.tick - lastTick).coerceAtLeast(0L)
            writeVlq(trackData, delta)
            lastTick = event.tick

            when (event) {
                is MidiEvent.NoteOn -> {
                    trackData.write(0x90 or (event.channel and 0x0F))
                    trackData.write(event.pitch and 0x7F)
                    trackData.write(event.velocity and 0x7F)
                }
                is MidiEvent.NoteOff -> {
                    trackData.write(0x80 or (event.channel and 0x0F))
                    trackData.write(event.pitch and 0x7F)
                    trackData.write(0x40) // Default release velocity
                }
                else -> {}
            }
        }

        // End of Track
        val totalTicks = (project.totalBeats * PPQN).toLong()
        val deltaToEnd = (totalTicks - lastTick).coerceAtLeast(0L)
        writeVlq(trackData, deltaToEnd)
        trackData.write(0xFF)
        trackData.write(0x2F)
        trackData.write(0x00)

        // Wrap into MTrk chunk
        val result = ByteArrayOutputStream()
        result.write("MTrk".toByteArray(Charsets.US_ASCII))
        writeBigEndian32(result, trackData.size())
        result.write(trackData.toByteArray())
        return result.toByteArray()
    }

    /**
     * Maps Flo Studio InstrumentType to the closest General MIDI program number (0..127).
     */
    fun getGeneralMidiProgram(instrument: InstrumentType): Int = when (instrument) {
        InstrumentType.PIANO_ACOUSTIC -> 0   // Acoustic Grand Piano
        InstrumentType.PIANO_ELECTRIC -> 4   // Electric Piano 1 (Rhodes)
        InstrumentType.BELL_SOFT -> 14       // Tubular Bells
        InstrumentType.MUSIC_BOX -> 10       // Music Box
        InstrumentType.SYNTH_PLUCK -> 80     // Lead 1 (square)
        InstrumentType.SYNTH_WARM -> 81      // Lead 2 (sawtooth)
        InstrumentType.SYNTH_BRIGHT_LEAD -> 87 // Lead 8 (bass + lead)
        InstrumentType.SYNTH_SOFT_LEAD -> 85 // Lead 6 (voice)
        InstrumentType.STRINGS -> 48         // String Ensemble 1
        InstrumentType.STRINGS_ORCHESTRAL -> 49 // String Ensemble 2
        InstrumentType.FLUTE -> 73           // Flute
        InstrumentType.ORGAN -> 19           // Church Organ
        InstrumentType.BASS_SYNTH -> 38      // Synth Bass 1
        InstrumentType.BASS_PLUCKED -> 33    // Electric Bass (finger)
        InstrumentType.PAD_WARM -> 89        // Pad 2 (warm)
        InstrumentType.PAD_DARK -> 95        // Pad 8 (sweep)
        InstrumentType.PAD_AMBIENT -> 91     // Pad 4 (choir/space)
        InstrumentType.PERCUSSION -> 0       // Channel 10 handles drum sounds
    }

    /**
     * Maps Flo Studio procedural drum note pitches to standard General MIDI drum keys.
     */
    fun mapDrumPitchToGm(pitch: Int): Int = when (pitch) {
        CompositionEngine.DRUM_KICK -> 36        // Bass Drum 1
        CompositionEngine.DRUM_SNARE -> 38       // Acoustic Snare
        CompositionEngine.DRUM_HIHAT_CLOSED -> 42 // Closed Hi-Hat
        CompositionEngine.DRUM_HIHAT_OPEN -> 46   // Open Hi-Hat
        else -> pitch.coerceIn(35, 81)
    }

    /**
     * Writes a variable-length quantity (VLQ) according to the MIDI specification.
     */
    fun writeVlq(out: ByteArrayOutputStream, value: Long) {
        var v = value
        val buffer = ByteArray(4)
        var i = 0
        buffer[i++] = (v and 0x7F).toByte()
        v = v ushr 7
        while (v > 0) {
            buffer[i++] = ((v and 0x7F) or 0x80).toByte()
            v = v ushr 7
        }
        while (i > 0) {
            out.write(buffer[--i].toInt())
        }
    }

    private fun writeBigEndian16(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeBigEndian32(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 24) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }
}
