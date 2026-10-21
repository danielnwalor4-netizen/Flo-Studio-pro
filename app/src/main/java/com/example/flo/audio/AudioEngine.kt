package com.example.flo.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.flo.model.InstrumentType
import com.example.flo.model.MusicalPitch
import com.example.flo.model.Note
import com.example.flo.model.SoundParameters
import com.example.flo.model.Track
import com.example.flo.model.TrackAutomation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class PlaybackState {
    STOPPED,
    PLAYING,
    PAUSED
}

/**
 * M3: High-performance Synthesizer Voice supporting 18 locally synthesized instruments
 * and procedural percussion (Kick, Snare, Hi-Hats).
 */
class SynthesizerVoice(
    val pitch: Int,
    val velocity: Float,
    val instrument: InstrumentType,
    val soundParams: SoundParameters,
    val noteId: String? = null,
    val trackVolume: Float = 1.0f
) {
    val frequency = MusicalPitch.pitchToFrequency(pitch)
    var isNoteOn: Boolean = true
    var isFinished: Boolean = false

    private var sampleCount: Long = 0
    private var releaseStartSample: Long = 0
    private var phase: Double = 0.0
    private var phase2: Double = 0.0
    private var phase3: Double = 0.0
    private var phase4: Double = 0.0

    // Random noise generator for percussion and breath
    private val noiseRandom = Random(pitch.toLong())

    fun release() {
        if (isNoteOn) {
            isNoteOn = false
            releaseStartSample = sampleCount
        }
    }

    fun renderSample(sampleRate: Int): Float {
        if (isFinished) return 0f

        val t = sampleCount.toDouble() / sampleRate
        val deltaPhase = 2.0 * PI * frequency / sampleRate

        phase = (phase + deltaPhase) % (2.0 * PI)
        phase2 = (phase2 + deltaPhase * 2.001) % (2.0 * PI)
        phase3 = (phase3 + deltaPhase * 3.002) % (2.0 * PI)
        phase4 = (phase4 + deltaPhase * 4.004) % (2.0 * PI)

        // Custom ADSR envelope based on instrument family
        val attackSec = when (instrument) {
            InstrumentType.PAD_WARM, InstrumentType.PAD_DARK, InstrumentType.PAD_AMBIENT -> 0.18
            InstrumentType.STRINGS, InstrumentType.STRINGS_ORCHESTRAL -> 0.08
            InstrumentType.FLUTE -> 0.05
            InstrumentType.ORGAN -> 0.02
            InstrumentType.PERCUSSION -> 0.001
            else -> max(0.003, (soundParams.attack / 50.0).toDouble())
        }

        val releaseSec = when (instrument) {
            InstrumentType.PAD_WARM, InstrumentType.PAD_AMBIENT -> 0.8
            InstrumentType.BELL_SOFT, InstrumentType.MUSIC_BOX -> 0.6
            InstrumentType.PERCUSSION -> 0.05
            else -> max(0.05, (soundParams.release / 2.0).toDouble())
        }

        val amplitudeEnv: Double = if (isNoteOn) {
            if (t < attackSec) {
                t / attackSec
            } else {
                when (instrument) {
                    InstrumentType.PIANO_ACOUSTIC -> exp(-(t - attackSec) * 1.4)
                    InstrumentType.PIANO_ELECTRIC -> exp(-(t - attackSec) * 1.1)
                    InstrumentType.BELL_SOFT -> exp(-(t - attackSec) * 0.7)
                    InstrumentType.MUSIC_BOX -> exp(-(t - attackSec) * 1.8)
                    InstrumentType.SYNTH_PLUCK -> exp(-(t - attackSec) * 3.5)
                    InstrumentType.BASS_PLUCKED -> exp(-(t - attackSec) * 2.2)
                    InstrumentType.BASS_SYNTH -> 0.85 * exp(-(t - attackSec) * 1.8) + 0.15
                    InstrumentType.PERCUSSION -> {
                        // Drums have rapid natural decay
                        when (pitch) {
                            CompositionEngine.DRUM_KICK -> exp(-t * 14.0)
                            CompositionEngine.DRUM_SNARE -> exp(-t * 11.0)
                            CompositionEngine.DRUM_HIHAT_CLOSED -> exp(-t * 28.0)
                            CompositionEngine.DRUM_HIHAT_OPEN -> exp(-t * 5.5)
                            else -> exp(-t * 15.0)
                        }
                    }
                    else -> 1.0 // Sustained instruments (Strings, Pads, Organ, Leads, Flute)
                }
            }
        } else {
            // Note release phase
            val tRel = (sampleCount - releaseStartSample).toDouble() / sampleRate
            if (tRel > releaseSec) {
                isFinished = true
                0.0
            } else {
                (1.0 - tRel / releaseSec).coerceIn(0.0, 1.0)
            }
        }

        if (amplitudeEnv <= 0.0001 && !isNoteOn) {
            isFinished = true
            return 0f
        }

        // Timbre synthesis for all 18 instruments
        val sampleValue: Double = when (instrument) {
            InstrumentType.PIANO_ACOUSTIC -> {
                val h1 = sin(phase)
                val h2 = sin(phase2) * exp(-t * 2.5) * 0.55
                val h3 = sin(phase3) * exp(-t * 4.0) * 0.35
                val h4 = sin(phase4) * exp(-t * 6.5) * 0.20
                val hammer = if (t < 0.015) sin(t * 1400.0) * (1.0 - t / 0.015) * 0.2 else 0.0
                (h1 + h2 + h3 + h4 + hammer) * 0.65
            }
            InstrumentType.PIANO_ELECTRIC -> {
                val mod = sin(phase * 4.0) * exp(-t * 3.0) * 0.45
                val carrier = sin(phase + mod)
                val tremolo = 1.0 + 0.1 * sin(2.0 * PI * 4.5 * t)
                (carrier + sin(phase * 0.5) * 0.2) * 0.65 * tremolo
            }
            InstrumentType.BELL_SOFT -> {
                val b1 = sin(phase)
                val b2 = sin(phase * 2.76) * 0.55
                val b3 = sin(phase * 5.40) * 0.30
                val b4 = sin(phase * 8.93) * 0.15
                (b1 + b2 + b3 + b4) * 0.5
            }
            InstrumentType.MUSIC_BOX -> {
                val m1 = sin(phase)
                val m2 = sin(phase * 3.01) * 0.6
                val m3 = sin(phase * 5.98) * 0.3
                (m1 + m2 + m3) * 0.55
            }
            InstrumentType.SYNTH_PLUCK -> {
                // Low-pass filtered saw wave with snappy decay
                val raw = sin(phase) + 0.5 * sin(phase * 2.0) + 0.3 * sin(phase * 3.0)
                val filterEnv = exp(-t * 6.0)
                raw * (0.3 + 0.7 * filterEnv) * 0.7
            }
            InstrumentType.SYNTH_WARM -> {
                val osc1 = sin(phase)
                val osc2 = sin(phase * 1.003) * 0.75
                val osc3 = sin(phase * 0.997) * 0.75
                (osc1 + osc2 + osc3) * 0.4
            }
            InstrumentType.SYNTH_BRIGHT_LEAD -> {
                val saw = sin(phase) + 0.6 * sin(phase * 2.0) + 0.4 * sin(phase * 3.0) + 0.25 * sin(phase * 4.0)
                val driven = saw * 1.6
                (driven / (1.0 + kotlin.math.abs(driven))) * 0.65
            }
            InstrumentType.SYNTH_SOFT_LEAD -> {
                val vibrato = 1.0 + 0.005 * sin(2.0 * PI * 5.0 * t)
                val lead = sin(phase * vibrato) + 0.35 * sin(phase * vibrato * 2.0)
                lead * 0.7
            }
            InstrumentType.STRINGS -> {
                val vibrato = 1.0 + 0.004 * sin(2.0 * PI * 5.2 * t)
                val p = phase * vibrato
                (sin(p) + 0.5 * sin(p * 2.0) + 0.25 * sin(p * 3.0) + 0.15 * sin(p * 4.0)) * 0.45
            }
            InstrumentType.STRINGS_ORCHESTRAL -> {
                val p1 = phase
                val p2 = phase * 2.001
                val pSub = phase * 0.501
                (sin(p1) * 0.5 + sin(p2) * 0.3 + sin(pSub) * 0.35) * 0.5
            }
            InstrumentType.FLUTE -> {
                val vibrato = 1.0 + 0.006 * sin(2.0 * PI * 5.5 * t)
                val tone = sin(phase * vibrato) + 0.15 * sin(phase * vibrato * 2.0)
                val breath = (noiseRandom.nextDouble() * 2.0 - 1.0) * 0.08
                (tone + breath) * 0.65
            }
            InstrumentType.ORGAN -> {
                // Tonewheel Drawbar simulation: 16', 8', 4', 2'
                val f1 = sin(phase * 0.5) * 0.4  // Sub
                val f2 = sin(phase) * 0.8        // Fundamental
                val f3 = sin(phase * 2.0) * 0.5  // Octave
                val f4 = sin(phase * 3.0) * 0.35 // 5th
                val f5 = sin(phase * 4.0) * 0.25 // 2nd octave
                (f1 + f2 + f3 + f4 + f5) * 0.4
            }
            InstrumentType.BASS_SYNTH -> {
                val sub = sin(phase)
                val punch = sin(phase * 2.0) * exp(-t * 4.0) * 0.45
                (sub * 0.85 + punch) * 0.8
            }
            InstrumentType.BASS_PLUCKED -> {
                val fundamental = sin(phase)
                val fret = sin(phase * 3.0) * exp(-t * 8.0) * 0.3
                (fundamental + fret) * 0.75
            }
            InstrumentType.PAD_WARM -> {
                val p1 = sin(phase)
                val p2 = sin(phase * 1.004) * 0.8
                val p3 = sin(phase * 0.996) * 0.8
                (p1 + p2 + p3) * 0.35
            }
            InstrumentType.PAD_DARK -> {
                val f = sin(phase)
                val fifth = sin(phase * 1.498) * 0.5
                val sub = sin(phase * 0.5) * 0.4
                (f + fifth + sub) * 0.4
            }
            InstrumentType.PAD_AMBIENT -> {
                val f = sin(phase)
                val fifth = sin(phase * 1.502) * 0.45
                val oct = sin(phase * 2.004) * 0.35
                (f + fifth + oct) * 0.38
            }
            InstrumentType.PERCUSSION -> {
                // Procedural Drum Synthesis
                when (pitch) {
                    CompositionEngine.DRUM_KICK -> {
                        // Pitch drop from 140 Hz down to 45 Hz
                        val kickFreq = 45.0 + 95.0 * exp(-t * 28.0)
                        val kickPhase = (phase * (kickFreq / max(1.0, frequency))) % (2.0 * PI)
                        val click = if (t < 0.005) (noiseRandom.nextDouble() * 2.0 - 1.0) * 0.3 else 0.0
                        (sin(kickPhase) + click) * 0.95
                    }
                    CompositionEngine.DRUM_SNARE -> {
                        // Body tone (180 Hz) + noise burst
                        val body = sin(phase) * exp(-t * 15.0) * 0.4
                        val noise = (noiseRandom.nextDouble() * 2.0 - 1.0) * 0.65
                        (body + noise) * 0.85
                    }
                    CompositionEngine.DRUM_HIHAT_CLOSED -> {
                        // Metallic high-pass noise burst
                        val noise = (noiseRandom.nextDouble() * 2.0 - 1.0)
                        noise * 0.55
                    }
                    CompositionEngine.DRUM_HIHAT_OPEN -> {
                        // Sizzling metallic noise
                        val noise = (noiseRandom.nextDouble() * 2.0 - 1.0)
                        noise * 0.65
                    }
                    else -> {
                        (noiseRandom.nextDouble() * 2.0 - 1.0) * 0.5
                    }
                }
            }
        }

        sampleCount++
        return (sampleValue * amplitudeEnv * velocity * soundParams.volume * trackVolume).toFloat()
    }
}

/**
 * Sample-accurate native AudioEngine with multi-track instrument mixing,
 * volume automation, Mute/Solo logic, and feedback reverb.
 */
class AudioEngine {
    private val TAG = "FloAudioEngine"
    private val SAMPLE_RATE = 44100
    private val BUFFER_SIZE = 1024

    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    private val activeVoices = CopyOnWriteArrayList<SynthesizerVoice>()

    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentBeat = MutableStateFlow(0f)
    val currentBeat: StateFlow<Float> = _currentBeat.asStateFlow()

    private val _vuMeterLevel = MutableStateFlow(0f)
    val vuMeterLevel: StateFlow<Float> = _vuMeterLevel.asStateFlow()

    private var currentNotes: List<Note> = emptyList()
    private var tracksMap: Map<String, Track> = emptyMap()
    private var automationsList: List<TrackAutomation> = emptyList()
    private var currentBpm: Int = 75
    private var currentSoundParams: SoundParameters = SoundParameters()
    private var defaultInstrument: InstrumentType = InstrumentType.PIANO_ACOUSTIC
    private var masterVolume: Float = 0.9f
    private var totalBeats: Float = 16f

    // Reverb feedback comb filters
    private val combBufferL = FloatArray(4410)
    private val combBufferR = FloatArray(4850)
    private var combIndexL = 0
    private var combIndexR = 0

    init {
        startAudioTrack()
    }

    @Synchronized
    private fun startAudioTrack() {
        if (isRunning.get()) return

        try {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferCapacity = max(minBuf, BUFFER_SIZE * 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferCapacity)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isRunning.set(true)

            audioThread = Thread({ audioLoop() }, "FloAudioThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            Log.d(TAG, "AudioTrack started successfully at $SAMPLE_RATE Hz")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
        }
    }

    private fun audioLoop() {
        val shortBuffer = ShortArray(BUFFER_SIZE * 2)
        var beatPosition = _currentBeat.value.toDouble()

        while (isRunning.get()) {
            val isPlaying = _playbackState.value == PlaybackState.PLAYING
            val bpm = currentBpm.toDouble()
            val beatsPerSample = (bpm / 60.0) / SAMPLE_RATE
            val startBeatOfChunk = beatPosition
            val endBeatOfChunk = beatPosition + (BUFFER_SIZE * beatsPerSample)

            // Trigger scheduled notes if playing
            if (isPlaying) {
                val hasAnySolo = tracksMap.values.any { it.isSolo }

                for (note in currentNotes) {
                    val noteStart = note.startBeat.toDouble()
                    if (noteStart >= startBeatOfChunk && noteStart < endBeatOfChunk) {
                        if (activeVoices.none { it.noteId == note.id }) {
                            // Check Track Mute & Solo
                            val track = tracksMap[note.trackId]
                            val isSilent = if (track != null) {
                                if (hasAnySolo) !track.isSolo else track.isMuted
                            } else false

                            if (!isSilent) {
                                val instrumentToUse = track?.instrumentType ?: defaultInstrument
                                val baseVol = track?.volume ?: 1.0f

                                // Check Volume Automation
                                val autoVal = interpolateAutomation(note.trackId, note.startBeat)
                                val finalTrackVol = baseVol * autoVal

                                activeVoices.add(
                                    SynthesizerVoice(
                                        pitch = note.pitch,
                                        velocity = note.velocity,
                                        instrument = instrumentToUse,
                                        soundParams = currentSoundParams,
                                        noteId = note.id,
                                        trackVolume = finalTrackVol
                                    )
                                )
                            }
                        }
                    }
                    // Release voice
                    val noteEnd = note.endBeat.toDouble()
                    if (noteEnd <= endBeatOfChunk) {
                        activeVoices.firstOrNull { it.noteId == note.id && it.isNoteOn }?.release()
                    }
                }
            }

            // Synthesize audio buffer
            var sumSquare = 0.0
            val reverbAmount = (currentSoundParams.reverb / 100f).coerceIn(0f, 0.8f)

            for (i in 0 until BUFFER_SIZE) {
                var monoSample = 0f
                val iterator = activeVoices.iterator()
                while (iterator.hasNext()) {
                    val voice = iterator.next()
                    monoSample += voice.renderSample(SAMPLE_RATE)
                }

                if (reverbAmount > 0.01f) {
                    val delayL = combBufferL[combIndexL]
                    val delayR = combBufferR[combIndexR]
                    combBufferL[combIndexL] = monoSample + delayL * (0.6f * reverbAmount)
                    combBufferR[combIndexR] = monoSample + delayR * (0.65f * reverbAmount)
                    combIndexL = (combIndexL + 1) % combBufferL.size
                    combIndexR = (combIndexR + 1) % combBufferR.size

                    val wetL = monoSample * (1f - reverbAmount * 0.4f) + delayL * (reverbAmount * 0.45f)
                    val wetR = monoSample * (1f - reverbAmount * 0.4f) + delayR * (reverbAmount * 0.45f)

                    val clampedL = (wetL * masterVolume).coerceIn(-1.0f, 1.0f)
                    val clampedR = (wetR * masterVolume).coerceIn(-1.0f, 1.0f)

                    shortBuffer[i * 2] = (clampedL * 32767).toInt().toShort()
                    shortBuffer[i * 2 + 1] = (clampedR * 32767).toInt().toShort()

                    sumSquare += (clampedL * clampedL + clampedR * clampedR) / 2.0
                } else {
                    val clamped = (monoSample * masterVolume).coerceIn(-1.0f, 1.0f)
                    val shortVal = (clamped * 32767).toInt().toShort()
                    shortBuffer[i * 2] = shortVal
                    shortBuffer[i * 2 + 1] = shortVal
                    sumSquare += clamped * clamped
                }

                if (isPlaying) {
                    beatPosition += beatsPerSample
                }
            }

            activeVoices.removeAll { it.isFinished }

            try {
                audioTrack?.write(shortBuffer, 0, shortBuffer.size)
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack write error", e)
            }

            val rms = sqrt(sumSquare / BUFFER_SIZE).toFloat()
            _vuMeterLevel.value = (_vuMeterLevel.value * 0.7f + rms * 1.5f * 0.3f).coerceIn(0f, 1f)

            if (isPlaying) {
                if (beatPosition >= totalBeats) {
                    beatPosition = 0.0
                }
                _currentBeat.value = beatPosition.toFloat()
            } else {
                beatPosition = _currentBeat.value.toDouble()
            }
        }
    }

    private fun interpolateAutomation(trackId: String, beat: Float): Float {
        val auto = automationsList.firstOrNull { it.trackId == trackId } ?: return 1.0f
        if (auto.points.isEmpty()) return 1.0f
        if (auto.points.size == 1) return auto.points[0].value

        val sorted = auto.points.sortedBy { it.beat }
        if (beat <= sorted.first().beat) return sorted.first().value
        if (beat >= sorted.last().beat) return sorted.last().value

        for (i in 0 until sorted.size - 1) {
            val p1 = sorted[i]
            val p2 = sorted[i + 1]
            if (beat >= p1.beat && beat <= p2.beat) {
                val fraction = (beat - p1.beat) / (p2.beat - p1.beat)
                return p1.value + fraction * (p2.value - p1.value)
            }
        }
        return 1.0f
    }

    // ==========================================
    // Real-Time Note & Instrument Auditioning
    // ==========================================

    fun previewNoteOn(pitch: Int, velocity: Float = 0.8f, instrument: InstrumentType = defaultInstrument) {
        previewNoteOff(pitch)
        activeVoices.add(
            SynthesizerVoice(
                pitch = pitch,
                velocity = velocity,
                instrument = instrument,
                soundParams = currentSoundParams,
                noteId = "preview_$pitch"
            )
        )
    }

    fun previewNoteOff(pitch: Int) {
        activeVoices.filter { it.noteId == "preview_$pitch" }.forEach { it.release() }
    }

    /**
     * M3: Audition an instrument with a characteristic note.
     */
    fun previewInstrument(instrument: InstrumentType, pitch: Int = 60) {
        val auditionPitch = when (instrument) {
            InstrumentType.BASS_SYNTH, InstrumentType.BASS_PLUCKED -> 36 // Low C2
            InstrumentType.PERCUSSION -> CompositionEngine.DRUM_KICK
            InstrumentType.MUSIC_BOX, InstrumentType.BELL_SOFT -> 72    // High C5
            else -> pitch
        }
        previewNoteOn(auditionPitch, 0.85f, instrument)
        // Auto release after 400ms
        Thread {
            Thread.sleep(400)
            previewNoteOff(auditionPitch)
        }.start()
    }

    // ==========================================
    // Composition Synchronization & Transport
    // ==========================================

    fun updateComposition(
        notes: List<Note>,
        tracks: List<Track>,
        automations: List<TrackAutomation>,
        bpm: Int,
        soundParams: SoundParameters,
        instrument: InstrumentType,
        totalCompositionBeats: Float
    ) {
        currentNotes = notes
        tracksMap = tracks.associateBy { it.id }
        automationsList = automations
        currentBpm = bpm
        currentSoundParams = soundParams
        defaultInstrument = instrument
        totalBeats = totalCompositionBeats
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    fun play() {
        if (_playbackState.value == PlaybackState.PLAYING) return
        _playbackState.value = PlaybackState.PLAYING
    }

    fun pause() {
        if (_playbackState.value != PlaybackState.PLAYING) return
        _playbackState.value = PlaybackState.PAUSED
        activeVoices.filter { it.noteId != null && !it.noteId!!.startsWith("preview_") }.forEach { it.release() }
    }

    fun stop() {
        _playbackState.value = PlaybackState.STOPPED
        _currentBeat.value = 0f
        activeVoices.filter { it.noteId != null && !it.noteId!!.startsWith("preview_") }.forEach { it.release() }
    }

    fun seekToBeat(beat: Float) {
        _currentBeat.value = beat.coerceAtLeast(0f)
        if (_playbackState.value != PlaybackState.PLAYING) {
            activeVoices.filter { it.noteId != null && !it.noteId!!.startsWith("preview_") }.forEach { it.release() }
        }
    }

    fun release() {
        isRunning.set(false)
        try {
            audioThread?.join(500)
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down audio", e)
        }
    }
}
