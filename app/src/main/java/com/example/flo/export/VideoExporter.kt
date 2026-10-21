package com.example.flo.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import android.view.Surface
import com.example.flo.audio.CompositionEngine
import com.example.flo.model.MusicalPitch
import com.example.flo.model.Project
import com.example.flo.model.VideoExportRatio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Milestone 7: Synchronized Piano-Roll Video Exporter (MP4)
 * Generates genuine MP4 videos with H.264 video and AAC audio multiplexed via MediaMuxer.
 * Supports 9:16 vertical (TikTok/Reels/Shorts) with smooth horizontal playhead tracking
 * and 16:9 landscape (YouTube) format, both rendered off-screen with bounded memory usage.
 */
object VideoExporter {
    private const val TAG = "FloVideoExporter"

    data class PendingSample(
        val isVideo: Boolean,
        val data: ByteArray,
        val size: Int,
        val presentationTimeUs: Long,
        val flags: Int
    )

    private const val MAX_PENDING_SAMPLES = 2048
    private const val MAX_PENDING_BYTES = 16 * 1024 * 1024L

    suspend fun exportVideo(
        context: Context,
        project: Project,
        ratio: VideoExportRatio,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.Default) {
        val width = if (ratio == VideoExportRatio.RATIO_9_16) 720 else 1280
        val height = if (ratio == VideoExportRatio.RATIO_9_16) 1280 else 720
        val fps = 30
        val sampleRate = AudioExporter.SAMPLE_RATE
        val bpm = project.bpm
        val secondsPerBeat = 60.0 / bpm

        // Export full arrangement duration (up to totalBeats) + 1.0s release tail
        val exportBeats = project.totalBeats
        val compositionDurationSec = exportBeats * secondsPerBeat
        val totalDurationSec = compositionDurationSec + 1.0
        val totalFrames = max(30, (totalDurationSec * fps).toInt())

        val safeProjectName = project.name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "Movies").apply { mkdirs() }
        val outputFile = File(outputDir, "FloStudio_${safeProjectName}_${ratio.aspectWidth}x${ratio.aspectHeight}_$timestamp.mp4")
        if (outputFile.exists()) outputFile.delete()

        var videoCodec: MediaCodec? = null
        var audioCodec: MediaCodec? = null
        var mediaMuxer: MediaMuxer? = null
        var inputSurface: Surface? = null
        var muxerStarted = false
        val pendingSamples = mutableListOf<PendingSample>()
        var totalPendingBytes = 0L

        try {
            // 1. Synthesize Audio PCM stream in advance
            onProgress(0.05f)
            Log.d(TAG, "Synthesizing audio for video export ($totalDurationSec sec)...")
            val pcmData = AudioExporter.renderPcmBytes(project, totalDurationSec) { audioProg ->
                onProgress(0.05f + audioProg * 0.15f) // 5% to 20%
            }

            // 2. Setup Video Encoder (H.264 / AVC)
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, if (ratio == VideoExportRatio.RATIO_9_16) 3_500_000 else 4_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = videoCodec.createInputSurface()
            videoCodec.start()

            // 3. Setup Audio Encoder (AAC)
            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 160_000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioCodec.start()

            // 4. Setup MediaMuxer
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            // 5. Drawing Paints
            val bgPaint = Paint().apply { color = Color.parseColor("#0C0D14") }
            val cardPaint = Paint().apply { color = Color.parseColor("#151622") }
            val gridPaint = Paint().apply { color = Color.parseColor("#1F2133"); strokeWidth = 1f }
            val barPaint = Paint().apply { color = Color.parseColor("#32354F"); strokeWidth = 2f }

            val melodyPaint = Paint().apply { color = Color.parseColor("#9D65FF") }
            val melodyActivePaint = Paint().apply { color = Color.parseColor("#D8B4FE") }
            val chordsPaint = Paint().apply { color = Color.parseColor("#00E5FF") }
            val chordsActivePaint = Paint().apply { color = Color.parseColor("#80F2FF") }
            val bassPaint = Paint().apply { color = Color.parseColor("#FF5252") }
            val bassActivePaint = Paint().apply { color = Color.parseColor("#FF8A80") }
            val drumsPaint = Paint().apply { color = Color.parseColor("#00E676") }
            val drumsActivePaint = Paint().apply { color = Color.parseColor("#69F0AE") }

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = if (ratio == VideoExportRatio.RATIO_9_16) 26f else 24f
                isAntiAlias = true
                isFakeBoldText = true
            }
            val subTextPaint = Paint().apply {
                color = Color.parseColor("#9AA0B9")
                textSize = if (ratio == VideoExportRatio.RATIO_9_16) 18f else 16f
                isAntiAlias = true
            }
            val playheadPaint = Paint().apply {
                color = Color.parseColor("#00E5FF")
                strokeWidth = 3f
                isAntiAlias = true
            }
            val playheadGlowPaint = Paint().apply {
                color = Color.parseColor("#3300E5FF")
                strokeWidth = 9f
                isAntiAlias = true
            }
            val whiteKeyPaint = Paint().apply { color = Color.parseColor("#25283B") }
            val blackKeyPaint = Paint().apply { color = Color.parseColor("#11121D") }
            val keyLabelPaint = Paint().apply {
                color = Color.parseColor("#9AA0B9")
                textSize = 12f
                isAntiAlias = true
            }

            // Piano roll dimensions
            val keyboardW = if (ratio == VideoExportRatio.RATIO_9_16) 65f else 80f
            val headerH = if (ratio == VideoExportRatio.RATIO_9_16) 90f else 65f
            val footerH = if (ratio == VideoExportRatio.RATIO_9_16) 80f else 60f
            val gridTop = headerH
            val gridBottom = height - footerH
            val gridH = gridBottom - gridTop
            val topPitch = 76 // E5
            val bottomPitch = 36 // C2
            val pitchCount = topPitch - bottomPitch + 1
            val rowH = gridH / pitchCount
            val beatW = if (ratio == VideoExportRatio.RATIO_9_16) 110f else 75f

            var pcmOffset = 0
            val pcmChunkSize = 4096 // 1024 stereo 16-bit samples
            var audioDoneSending = false
            var audioDoneReceiving = false
            var videoDoneReceiving = false

            fun tryStartMuxer() {
                if (videoTrackIndex >= 0 && audioTrackIndex >= 0 && !muxerStarted) {
                    try {
                        mediaMuxer.start()
                        muxerStarted = true
                        Log.d(TAG, "MediaMuxer started with videoTrack=$videoTrackIndex, audioTrack=$audioTrackIndex. Flushing ${pendingSamples.size} buffered samples.")
                        val tempInfo = MediaCodec.BufferInfo()
                        for (sample in pendingSamples) {
                            val trackIdx = if (sample.isVideo) videoTrackIndex else audioTrackIndex
                            tempInfo.set(0, sample.size, sample.presentationTimeUs, sample.flags)
                            val bb = ByteBuffer.wrap(sample.data)
                            mediaMuxer.writeSampleData(trackIdx, bb, tempInfo)
                        }
                        pendingSamples.clear()
                        totalPendingBytes = 0L
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start MediaMuxer", e)
                        throw e
                    }
                }
            }

            // Helper to drain video encoder
            fun drainVideoEncoder(endOfStream: Boolean = false) {
                val timeoutUs = if (endOfStream) 10000L else 0L
                while (true) {
                    val status = videoCodec.dequeueOutputBuffer(videoBufferInfo, timeoutUs)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (videoTrackIndex < 0) {
                            videoTrackIndex = mediaMuxer.addTrack(videoCodec.outputFormat)
                            Log.d(TAG, "Video track registered: index=$videoTrackIndex")
                            tryStartMuxer()
                        }
                    } else if (status >= 0) {
                        val encodedData = videoCodec.getOutputBuffer(status)
                        val isConfig = (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (encodedData != null && videoBufferInfo.size > 0 && !isConfig) {
                            if (muxerStarted) {
                                encodedData.position(videoBufferInfo.offset)
                                encodedData.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                mediaMuxer.writeSampleData(videoTrackIndex, encodedData, videoBufferInfo)
                            } else {
                                if (pendingSamples.size >= MAX_PENDING_SAMPLES || totalPendingBytes + videoBufferInfo.size > MAX_PENDING_BYTES) {
                                    throw IllegalStateException("Video buffer overflow before MediaMuxer started")
                                }
                                val bytes = ByteArray(videoBufferInfo.size)
                                val origPos = encodedData.position()
                                encodedData.position(videoBufferInfo.offset)
                                encodedData.get(bytes)
                                encodedData.position(origPos)
                                pendingSamples.add(
                                    PendingSample(
                                        isVideo = true,
                                        data = bytes,
                                        size = videoBufferInfo.size,
                                        presentationTimeUs = videoBufferInfo.presentationTimeUs,
                                        flags = videoBufferInfo.flags
                                    )
                                )
                                totalPendingBytes += videoBufferInfo.size
                            }
                        }
                        videoCodec.releaseOutputBuffer(status, false)
                        if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            videoDoneReceiving = true
                            break
                        }
                    }
                }
            }

            // Helper to feed and drain audio encoder
            fun pumpAudioEncoder(endOfStream: Boolean = false) {
                // Feed PCM data
                while (!audioDoneSending) {
                    val inputIndex = audioCodec.dequeueInputBuffer(if (endOfStream) 10000L else 0L)
                    if (inputIndex < 0) break

                    val inputBuffer = audioCodec.getInputBuffer(inputIndex) ?: break
                    inputBuffer.clear()

                    val bytesRemaining = pcmData.size - pcmOffset
                    if (bytesRemaining <= 0) {
                        audioCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        audioDoneSending = true
                        break
                    }

                    val toCopy = min(bytesRemaining, min(inputBuffer.remaining(), pcmChunkSize))
                    inputBuffer.put(pcmData, pcmOffset, toCopy)
                    val ptsUs = (pcmOffset.toLong() * 1_000_000L) / (sampleRate * 4) // 4 bytes per stereo sample
                    pcmOffset += toCopy
                    val flags = if (pcmOffset >= pcmData.size && endOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    if (flags != 0) audioDoneSending = true

                    audioCodec.queueInputBuffer(inputIndex, 0, toCopy, ptsUs, flags)
                }

                // Drain encoded audio
                val timeoutUs = if (endOfStream) 10000L else 0L
                while (!audioDoneReceiving) {
                    val status = audioCodec.dequeueOutputBuffer(audioBufferInfo, timeoutUs)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (audioTrackIndex < 0) {
                            audioTrackIndex = mediaMuxer.addTrack(audioCodec.outputFormat)
                            Log.d(TAG, "Audio track registered: index=$audioTrackIndex")
                            tryStartMuxer()
                        }
                    } else if (status >= 0) {
                        val encodedData = audioCodec.getOutputBuffer(status)
                        val isConfig = (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (encodedData != null && audioBufferInfo.size > 0 && !isConfig) {
                            if (muxerStarted) {
                                encodedData.position(audioBufferInfo.offset)
                                encodedData.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                mediaMuxer.writeSampleData(audioTrackIndex, encodedData, audioBufferInfo)
                            } else {
                                if (pendingSamples.size >= MAX_PENDING_SAMPLES || totalPendingBytes + audioBufferInfo.size > MAX_PENDING_BYTES) {
                                    throw IllegalStateException("Audio buffer overflow before MediaMuxer started")
                                }
                                val bytes = ByteArray(audioBufferInfo.size)
                                val origPos = encodedData.position()
                                encodedData.position(audioBufferInfo.offset)
                                encodedData.get(bytes)
                                encodedData.position(origPos)
                                pendingSamples.add(
                                    PendingSample(
                                        isVideo = false,
                                        data = bytes,
                                        size = audioBufferInfo.size,
                                        presentationTimeUs = audioBufferInfo.presentationTimeUs,
                                        flags = audioBufferInfo.flags
                                    )
                                )
                                totalPendingBytes += audioBufferInfo.size
                            }
                        }
                        audioCodec.releaseOutputBuffer(status, false)
                        if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            audioDoneReceiving = true
                            break
                        }
                    }
                }
            }

            // 6. Main Video Rendering Loop
            for (frame in 0 until totalFrames) {
                // Check for coroutine cancellation
                if (!kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.isActive!!) {
                    throw kotlinx.coroutines.CancellationException("Video export cancelled by user")
                }

                val currentSec = frame.toDouble() / fps
                val currentBeat = (currentSec / secondsPerBeat).toFloat()

                // Pump audio and drain video codecs
                pumpAudioEncoder(endOfStream = false)
                drainVideoEncoder(endOfStream = false)

                // Render Piano Roll Frame onto Input Surface
                val canvas = inputSurface.lockCanvas(null)
                try {
                    // Background
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                    // Header
                    canvas.drawRect(0f, 0f, width.toFloat(), headerH, cardPaint)
                    canvas.drawText("FLO STUDIO", 20f, headerH * 0.45f, titlePaint)
                    val headerSubtext = "${project.name} • ${project.mood.displayName} • ${project.bpm} BPM • ${project.keyRoot.displayName} ${project.scaleMode.displayName}"
                    canvas.drawText(headerSubtext, 20f, headerH * 0.80f, subTextPaint)

                    // Smooth horizontal camera tracking
                    val playheadWorldX = currentBeat * beatW
                    val visibleTimelineW = width - keyboardW
                    val cameraOffsetX = if (ratio == VideoExportRatio.RATIO_9_16) {
                        // In 9:16 portrait, smoothly keep playhead at 40% across viewport
                        val targetCamX = playheadWorldX - (visibleTimelineW * 0.40f)
                        val maxCamX = max(0f, (exportBeats + 2) * beatW - visibleTimelineW)
                        targetCamX.coerceIn(0f, maxCamX)
                    } else {
                        // In 16:9 landscape, if arrangement is wide, track when playhead reaches halfway
                        if ((exportBeats * beatW) > visibleTimelineW) {
                            val targetCamX = playheadWorldX - (visibleTimelineW * 0.45f)
                            val maxCamX = max(0f, (exportBeats + 2) * beatW - visibleTimelineW)
                            targetCamX.coerceIn(0f, maxCamX)
                        } else 0f
                    }

                    // Draw Piano Roll Rows
                    for (i in 0 until pitchCount) {
                        val pitch = topPitch - i
                        val y = gridTop + i * rowH
                        val isBlack = MusicalPitch.isBlackKey(pitch)
                        val rowBgPaint = if (isBlack) bgPaint else cardPaint
                        canvas.drawRect(keyboardW, y, width.toFloat(), y + rowH, rowBgPaint)
                        canvas.drawLine(keyboardW, y + rowH, width.toFloat(), y + rowH, gridPaint)
                    }

                    // Draw Vertical Beat & Bar Lines
                    val maxBeatToDraw = (exportBeats + 4).toInt()
                    for (b in 0..maxBeatToDraw) {
                        val x = keyboardW + (b * beatW) - cameraOffsetX
                        if (x in keyboardW..width.toFloat()) {
                            val isBarLine = b % project.timeSignatureNumerator == 0
                            val lineP = if (isBarLine) barPaint else gridPaint
                            canvas.drawLine(x, gridTop, x, gridBottom, lineP)
                            if (isBarLine) {
                                val barNumber = b / project.timeSignatureNumerator + 1
                                canvas.drawText("BAR $barNumber", x + 4f, gridTop + 14f, keyLabelPaint)
                            }
                        }
                    }

                    // Draw Musical Notes with track coloring and glow when active
                    for (note in project.notes) {
                        val rowIndex = topPitch - note.pitch
                        if (rowIndex in 0 until pitchCount) {
                            val y = gridTop + rowIndex * rowH + 2f
                            val noteH = max(2f, rowH - 4f)
                            val noteX = keyboardW + (note.startBeat * beatW) - cameraOffsetX
                            val noteW = max(8f, note.durationBeats * beatW - 2f)

                            if (noteX + noteW >= keyboardW && noteX <= width) {
                                val isActive = currentBeat >= note.startBeat && currentBeat <= note.endBeat
                                val p = when (note.trackId) {
                                    "track_melody" -> if (isActive) melodyActivePaint else melodyPaint
                                    "track_chords" -> if (isActive) chordsActivePaint else chordsPaint
                                    "track_bass" -> if (isActive) bassActivePaint else bassPaint
                                    "track_rhythm" -> if (isActive) drumsActivePaint else drumsPaint
                                    else -> if (isActive) melodyActivePaint else melodyPaint
                                }
                                val left = max(keyboardW, noteX)
                                val right = min(width.toFloat(), noteX + noteW)
                                val rect = RectF(left, y, right, y + noteH)
                                canvas.drawRoundRect(rect, 4f, 4f, p)
                            }
                        }
                    }

                    // Draw Left Piano Keyboard Reference
                    for (i in 0 until pitchCount) {
                        val pitch = topPitch - i
                        val y = gridTop + i * rowH
                        val isBlack = MusicalPitch.isBlackKey(pitch)
                        val keyP = if (isBlack) blackKeyPaint else whiteKeyPaint
                        canvas.drawRect(0f, y, keyboardW, y + rowH, keyP)
                        canvas.drawLine(0f, y + rowH, keyboardW, y + rowH, gridPaint)

                        if (pitch % 12 == 0 || isBlack) {
                            val name = MusicalPitch.getPitchName(pitch)
                            canvas.drawText(name, keyboardW - 28f, y + rowH - 4f, keyLabelPaint)
                        }
                    }
                    canvas.drawLine(keyboardW, gridTop, keyboardW, gridBottom, barPaint)

                    // Draw Playhead
                    val playheadScreenX = keyboardW + playheadWorldX - cameraOffsetX
                    if (playheadScreenX in keyboardW..width.toFloat()) {
                        canvas.drawLine(playheadScreenX, gridTop, playheadScreenX, gridBottom, playheadGlowPaint)
                        canvas.drawLine(playheadScreenX, gridTop, playheadScreenX, gridBottom, playheadPaint)
                    }

                    // Footer with Timecode, Progress Bar, and Audio Waveform VU Simulation
                    canvas.drawRect(0f, gridBottom, width.toFloat(), height.toFloat(), cardPaint)
                    val mins = (currentSec / 60).toInt()
                    val secs = (currentSec % 60).toInt()
                    val ms = ((currentSec - currentSec.toInt()) * 1000).toInt()
                    val timecode = String.format(Locale.US, "%02d:%02d.%03d", mins, secs, ms)
                    canvas.drawText(timecode, 20f, gridBottom + footerH * 0.55f, titlePaint)

                    // Audio VU waveform animation
                    val vuBarCount = if (ratio == VideoExportRatio.RATIO_9_16) 14 else 24
                    val startX = width - (vuBarCount * 12f) - 20f
                    for (vi in 0 until vuBarCount) {
                        val barH = (10f + 25f * sin(currentSec * 8.0 + vi * 0.5).toFloat()).coerceIn(4f, footerH * 0.6f)
                        val barX = startX + vi * 12f
                        val barColor = if (vi >= vuBarCount - 3) Color.RED else if (vi >= vuBarCount - 6) Color.YELLOW else Color.GREEN
                        val barP = Paint().apply { color = barColor }
                        canvas.drawRect(barX, gridBottom + footerH * 0.75f - barH, barX + 8f, gridBottom + footerH * 0.75f, barP)
                    }

                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                val renderProgress = 0.20f + (frame.toFloat() / totalFrames) * 0.70f
                onProgress(renderProgress)
            }

            // 7. Signal EOS on video encoder and audio encoder
            videoCodec.signalEndOfInputStream()

            // Drain remaining video frames until EOS
            var videoDrainTries = 0
            while (!videoDoneReceiving && videoDrainTries < 100) {
                drainVideoEncoder(endOfStream = true)
                videoDrainTries++
            }

            // Drain remaining audio frames until EOS
            var audioDrainTries = 0
            while (!audioDoneReceiving && audioDrainTries < 100) {
                pumpAudioEncoder(endOfStream = true)
                audioDrainTries++
            }

            // Verify MediaMuxer was started and all tracks registered
            if (!muxerStarted) {
                throw IllegalStateException("MediaMuxer never started: tracks were not fully registered (videoTrack=$videoTrackIndex, audioTrack=$audioTrackIndex)")
            }

            try {
                mediaMuxer.stop()
            } catch (e: Exception) {
                throw IllegalStateException("MediaMuxer stop failed: ${e.message}", e)
            }

            if (!outputFile.exists() || outputFile.length() == 0L) {
                throw IllegalStateException("Exported video file is missing or empty")
            }

            MediaPublisher.publishExport(context, outputFile, "video/mp4", "FloStudio")
            onProgress(1.0f)
            Log.d(TAG, "Video export finished successfully at ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Video export failed", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            null
        } finally {
            try {
                videoCodec?.stop()
            } catch (ignored: Exception) {}
            try {
                videoCodec?.release()
            } catch (ignored: Exception) {}
            try {
                audioCodec?.stop()
            } catch (ignored: Exception) {}
            try {
                audioCodec?.release()
            } catch (ignored: Exception) {}
            try {
                inputSurface?.release()
            } catch (ignored: Exception) {}
            try {
                mediaMuxer?.release()
            } catch (ignored: Exception) {}
            pendingSamples.clear()
        }
    }
}
