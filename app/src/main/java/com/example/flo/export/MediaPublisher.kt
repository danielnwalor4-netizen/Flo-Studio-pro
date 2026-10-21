package com.example.flo.export

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Android 14 Scoped Storage compliant media publisher.
 * Indexes completed exports via MediaScannerConnection and inserts a copy into
 * public MediaStore (Music/FloStudio, Movies/FloStudio) with IS_PENDING safety,
 * ensuring exported files are immediately visible in system media players,
 * while preserving the original app-scoped File for FileProvider and in-app management.
 */
object MediaPublisher {
    private const val TAG = "FloMediaPublisher"

    fun publishExport(
        context: Context,
        file: File,
        mimeType: String,
        subfolder: String = "FloStudio"
    ) {
        try {
            // 1. Always trigger MediaScannerConnection on the app-scoped file
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                null
            )

            // 2. On Android 10+ (API 29..36), copy to public MediaStore collection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val isVideo = mimeType.startsWith("video/")
                val collectionUri = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val relativeDir = if (isVideo) {
                    "${Environment.DIRECTORY_MOVIES}/$subfolder"
                } else {
                    "${Environment.DIRECTORY_MUSIC}/$subfolder"
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val itemUri = resolver.insert(collectionUri, values)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { outStream ->
                        file.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                    Log.d(TAG, "Successfully published to public MediaStore: $itemUri")
                }
            }
        } catch (e: Exception) {
            // Non-fatal: original app-specific file remains intact and shareable via FileProvider
            Log.w(TAG, "Non-fatal exception while publishing to public MediaStore", e)
        }
    }
}
