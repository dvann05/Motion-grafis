package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import com.example.model.MotionProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

data class Mp4ExportResult(
    val file: File,
    val mediaStoreUri: String?,
    val fileSizeKb: Long
)

object Mp4VideoExporter {

    const val VIDEO_WIDTH = 1080
    const val VIDEO_HEIGHT = 1920
    const val VIDEO_FPS = 30
    private const val BIT_RATE = 6_000_000
    private const val I_FRAME_INTERVAL = 1

    suspend fun exportProjectToMp4(
        context: Context,
        project: MotionProject,
        onProgress: (currentFrame: Int, totalFrames: Int, status: String) -> Unit
    ): Mp4ExportResult = withContext(Dispatchers.Default) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = project.title
            .replace(Regex("[^a-zA-Z0-9]+"), "_")
            .trim('_')
            .ifBlank { "motion_lab_video" }
        val outputFile = File(exportDir, "${sanitizedTitle}_1080p_30fps.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val totalSeconds = project.totalDurationSeconds.coerceIn(5, 90)
        val totalFrames = totalSeconds * VIDEO_FPS

        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            VIDEO_WIDTH,
            VIDEO_HEIGHT
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        var encoder: MediaCodec? = null
        var inputSurface: Surface? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1

        try {
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            for (frameIdx in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val timeSec = frameIdx.toFloat() / VIDEO_FPS.toFloat()

                if (frameIdx % 5 == 0 || frameIdx == totalFrames - 1) {
                    onProgress(
                        frameIdx + 1,
                        totalFrames,
                        "Rendering 1080x1920 @ 30fps • Frame ${frameIdx + 1}/$totalFrames"
                    )
                }

                val canvas = try {
                    inputSurface.lockHardwareCanvas()
                } catch (_: Exception) {
                    inputSurface.lockCanvas(null)
                }

                try {
                    MotionCanvasRenderer.renderFrame(
                        canvas = canvas,
                        width = VIDEO_WIDTH.toFloat(),
                        height = VIDEO_HEIGHT.toFloat(),
                        project = project,
                        globalTimeSec = timeSec
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                drainEncoder(
                    encoder = encoder,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    endOfStream = false,
                    currentTrackIndex = trackIndex,
                    muxerStarted = muxerStarted
                ).let { (newTrack, started) ->
                    trackIndex = newTrack
                    muxerStarted = started
                }
            }

            onProgress(totalFrames, totalFrames, "Finalizing MP4 stream & muxing H.264...")
            encoder.signalEndOfInputStream()
            drainEncoder(
                encoder = encoder,
                muxer = muxer,
                bufferInfo = bufferInfo,
                endOfStream = true,
                currentTrackIndex = trackIndex,
                muxerStarted = muxerStarted
            ).let { (newTrack, started) ->
                trackIndex = newTrack
                muxerStarted = started
            }
        } finally {
            try {
                encoder?.stop()
            } catch (_: Exception) {
            }
            try {
                encoder?.release()
            } catch (_: Exception) {
            }
            try {
                inputSurface?.release()
            } catch (_: Exception) {
            }
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
        }

        // Also copy to MediaStore Movies/MotionLabAI so it's immediately accessible in Android Gallery/Downloads
        val mediaUri = saveToMediaStoreMovies(context, outputFile)
        val sizeKb = (outputFile.length() / 1024L).coerceAtLeast(1L)

        Mp4ExportResult(
            file = outputFile,
            mediaStoreUri = mediaUri?.toString(),
            fileSizeKb = sizeKb
        )
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        currentTrackIndex: Int,
        muxerStarted: Boolean
    ): Pair<Int, Boolean> {
        var trackIndex = currentTrackIndex
        var started = muxerStarted
        val timeoutUs = 10_000L

        while (true) {
            val status = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!started) {
                    val newFormat = encoder.outputFormat
                    trackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    started = true
                }
            } else if (status >= 0) {
                val encodedData = encoder.getOutputBuffer(status)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0 && started) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
        return trackIndex to started
    }

    private fun saveToMediaStoreMovies(context: Context, sourceFile: File): Uri? {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/MotionLabAI"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val itemUri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(itemUri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            itemUri
        } catch (_: Exception) {
            null
        }
    }

    suspend fun copyFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                true
            } catch (_: Exception) {
                false
            }
        }
}
