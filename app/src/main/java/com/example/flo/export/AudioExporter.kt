package com.example.flo.export

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.flo.audio.SynthesizerVoice
import com.example.flo.model.InstrumentType
import com.example.flo.model.Note
import com.example.flo.model.Project
import com.example.flo.model.Track
import com.example.flo.model.TrackAutomation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min

/**
 * Milestone 6: Offline Audio Exporter
 * Renders high-fidelity uncompressed 44.1 kHz 16-bit Stereo PCM WAV files
 * using the exact same local procedural synthesis and reverb as real-time playback.
 */
object AudioExporter {
    private const val TAG = "FloAudioExporter"
    const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2
    private const val BITS_PER_SAMPLE = 16
    private const val CHUNK_SIZE = 2048

    /**
     * Renders the given project to a standard 16-bit stereo WAV file.
     * Uses streaming disk writes to minimize memory consumption for Tecno Camon 20.
     */
    suspend fun exportWav(
        context: Context,
        project: Project,
        customFileName: String? = null,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.Default) {
        val safeProjectName = project.name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = if (!customFileName.isNullOrBlank()) {
            val base = customFileName.trim()
            if (base.endsWith(".wav", ignoreCase = true)) base else "$base.wav"
        } else {
            "FloStudio_${safeProjectName}_$timestamp.wav"
        }

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "Music").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)
        if (outputFile.exists()) outputFile.delete()

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(outputFile)

            // Write 44-byte placeholder WAV header
            val placeholderHeader = ByteArray(44)
            fos.write(placeholderHeader)

            val tracksMap = project.tracks.associateBy { it.id }
            val automations = project.automations
            val bpm = project.bpm.toDouble()
            val beatsPerSample = (bpm / 60.0) / SAMPLE_RATE
            val secondsPerBeat = 60.0 / bpm

            val exportBeats = project.totalBeats
            val compositionSec = exportBeats * secondsPerBeat
            val tailSec = 1.5 // Instrument release and reverb decay tail
            val totalDurationSec = compositionSec + tailSec
            val totalSamples = max(SAMPLE_RATE.toLong(), (totalDurationSec * SAMPLE_RATE).toLong())

            val hasAnySolo = tracksMap.values.any { it.isSolo }
            val activeVoices = mutableListOf<SynthesizerVoice>()

            // Reverb state matching AudioEngine
            val reverbAmount = (project.soundParameters.reverb / 100f).coerceIn(0f, 0.8f)
            val combBufferL = FloatArray(4410)
            val combBufferR = FloatArray(4850)
            var combIndexL = 0
            var combIndexR = 0

            val masterVolume = 0.9f
            val byteBuffer = ByteBuffer.allocate(CHUNK_SIZE * CHANNELS * 2).order(ByteOrder.LITTLE_ENDIAN)

            var sampleIndex = 0L
            var currentBeatPos = 0.0

            while (sampleIndex < totalSamples && coroutineContext.isActive) {
                val chunkSamples = min(CHUNK_SIZE.toLong(), totalSamples - sampleIndex).toInt()
                val startBeatOfChunk = currentBeatPos
                val endBeatOfChunk = currentBeatPos + (chunkSamples * beatsPerSample)

                // Trigger notes within this chunk
                for (note in project.notes) {
                    val noteStart = note.startBeat.toDouble()
                    if (noteStart >= startBeatOfChunk && noteStart < endBeatOfChunk) {
                        if (activeVoices.none { it.noteId == note.id }) {
                            val track = tracksMap[note.trackId]
                            val isSilent = if (track != null) {
                                if (hasAnySolo) !track.isSolo else track.isMuted
                            } else false

                            if (!isSilent) {
                                val instrument = track?.instrumentType ?: project.instrument
                                val baseVol = track?.volume ?: 1.0f
                                val autoVal = interpolateAutomation(automations, note.trackId, note.startBeat)
                                val finalTrackVol = baseVol * autoVal

                                activeVoices.add(
                                    SynthesizerVoice(
                                        pitch = note.pitch,
                                        velocity = note.velocity,
                                        instrument = instrument,
                                        soundParams = project.soundParameters,
                                        noteId = note.id,
                                        trackVolume = finalTrackVol
                                    )
                                )
                            }
                        }
                    }

                    // Release note when it reaches note.endBeat
                    val noteEnd = note.endBeat.toDouble()
                    if (noteEnd <= endBeatOfChunk) {
                        activeVoices.firstOrNull { it.noteId == note.id && it.isNoteOn }?.release()
                    }
                }

                // Render samples in this chunk
                byteBuffer.clear()
                for (i in 0 until chunkSamples) {
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

                        byteBuffer.putShort((clampedL * 32767).toInt().toShort())
                        byteBuffer.putShort((clampedR * 32767).toInt().toShort())
                    } else {
                        val clamped = (monoSample * masterVolume).coerceIn(-1.0f, 1.0f)
                        val shortVal = (clamped * 32767).toInt().toShort()
                        byteBuffer.putShort(shortVal)
                        byteBuffer.putShort(shortVal)
                    }

                    currentBeatPos += beatsPerSample
                }

                activeVoices.removeAll { it.isFinished }

                fos.write(byteBuffer.array(), 0, chunkSamples * CHANNELS * 2)
                sampleIndex += chunkSamples

                onProgress(sampleIndex.toFloat() / totalSamples)
            }

            fos.flush()
            fos.close()
            fos = null

            // If cancelled during export, delete incomplete file
            if (!coroutineContext.isActive) {
                outputFile.delete()
                return@withContext null
            }

            // Write final WAV header with correct sizes
            val totalPcmBytes = sampleIndex * CHANNELS * (BITS_PER_SAMPLE / 8)
            writeWavHeader(outputFile, totalPcmBytes, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE)

            MediaPublisher.publishExport(context, outputFile, "audio/wav", "FloStudio")
            Log.d(TAG, "WAV export succeeded: ${outputFile.absolutePath} ($totalPcmBytes bytes)")
            onProgress(1.0f)
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "WAV export failed", e)
            outputFile.delete()
            null
        } finally {
            try {
                fos?.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Renders the complete project audio into 16-bit PCM little-endian stereo byte array.
     * Used by VideoExporter to feed the AAC encoder for genuine synchronized MP4 audio.
     */
    suspend fun renderPcmBytes(
        project: Project,
        maxDurationSec: Double? = null,
        onProgress: (Float) -> Unit = {}
    ): ByteArray = withContext(Dispatchers.Default) {
        val tracksMap = project.tracks.associateBy { it.id }
        val automations = project.automations
        val bpm = project.bpm.toDouble()
        val beatsPerSample = (bpm / 60.0) / SAMPLE_RATE
        val secondsPerBeat = 60.0 / bpm

        val exportBeats = project.totalBeats
        val compositionSec = exportBeats * secondsPerBeat
        val totalDurationSec = maxDurationSec ?: (compositionSec + 1.0)
        val minSamples = if (maxDurationSec != null) (maxDurationSec * SAMPLE_RATE).toLong() else SAMPLE_RATE.toLong()
        val totalSamples = max(minSamples, (totalDurationSec * SAMPLE_RATE).toLong())

        val hasAnySolo = tracksMap.values.any { it.isSolo }
        val activeVoices = mutableListOf<SynthesizerVoice>()

        val reverbAmount = (project.soundParameters.reverb / 100f).coerceIn(0f, 0.8f)
        val combBufferL = FloatArray(4410)
        val combBufferR = FloatArray(4850)
        var combIndexL = 0
        var combIndexR = 0

        val masterVolume = 0.9f
        val totalBytes = (totalSamples * CHANNELS * 2).toInt()
        val pcmArray = ByteArray(totalBytes)
        val byteBuffer = ByteBuffer.wrap(pcmArray).order(ByteOrder.LITTLE_ENDIAN)

        var sampleIndex = 0L
        var currentBeatPos = 0.0

        while (sampleIndex < totalSamples && coroutineContext.isActive) {
            val chunkSamples = min(CHUNK_SIZE.toLong(), totalSamples - sampleIndex).toInt()
            val startBeatOfChunk = currentBeatPos
            val endBeatOfChunk = currentBeatPos + (chunkSamples * beatsPerSample)

            for (note in project.notes) {
                val noteStart = note.startBeat.toDouble()
                if (noteStart >= startBeatOfChunk && noteStart < endBeatOfChunk) {
                    if (activeVoices.none { it.noteId == note.id }) {
                        val track = tracksMap[note.trackId]
                        val isSilent = if (track != null) {
                            if (hasAnySolo) !track.isSolo else track.isMuted
                        } else false

                        if (!isSilent) {
                            val instrument = track?.instrumentType ?: project.instrument
                            val baseVol = track?.volume ?: 1.0f
                            val autoVal = interpolateAutomation(automations, note.trackId, note.startBeat)
                            val finalTrackVol = baseVol * autoVal

                            activeVoices.add(
                                SynthesizerVoice(
                                    pitch = note.pitch,
                                    velocity = note.velocity,
                                    instrument = instrument,
                                    soundParams = project.soundParameters,
                                    noteId = note.id,
                                    trackVolume = finalTrackVol
                                )
                            )
                        }
                    }
                }

                val noteEnd = note.endBeat.toDouble()
                if (noteEnd <= endBeatOfChunk) {
                    activeVoices.firstOrNull { it.noteId == note.id && it.isNoteOn }?.release()
                }
            }

            for (i in 0 until chunkSamples) {
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

                    byteBuffer.putShort((clampedL * 32767).toInt().toShort())
                    byteBuffer.putShort((clampedR * 32767).toInt().toShort())
                } else {
                    val clamped = (monoSample * masterVolume).coerceIn(-1.0f, 1.0f)
                    val shortVal = (clamped * 32767).toInt().toShort()
                    byteBuffer.putShort(shortVal)
                    byteBuffer.putShort(shortVal)
                }

                currentBeatPos += beatsPerSample
            }

            activeVoices.removeAll { it.isFinished }
            sampleIndex += chunkSamples
            onProgress(sampleIndex.toFloat() / totalSamples)
        }

        pcmArray
    }

    /**
     * Writes standard 44-byte RIFF WAVE header to the specified file.
     */
    fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int = SAMPLE_RATE,
        channels: Int = CHANNELS,
        bitsPerSample: Int = BITS_PER_SAMPLE
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()
        val blockAlign = (channels * bitsPerSample / 8)

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        bb.position(4)
        bb.putInt((totalDataLen and 0xffffffffL).toInt())
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        bb.position(16)
        bb.putInt(16) // SubChunk1Size for PCM
        bb.putShort(1.toShort()) // AudioFormat 1 = PCM
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt((byteRate and 0xffffffffL).toInt())
        bb.putShort(blockAlign.toShort())
        bb.putShort(bitsPerSample.toShort())

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        bb.position(40)
        bb.putInt((totalAudioLen and 0xffffffffL).toInt())

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.write(header)
        raf.close()
    }

    private fun interpolateAutomation(
        automations: List<TrackAutomation>,
        trackId: String,
        beat: Float
    ): Float {
        val auto = automations.firstOrNull { it.trackId == trackId } ?: return 1.0f
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
}
