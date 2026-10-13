package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoImportManager {

    /**
     * Extracts real video duration in milliseconds using MediaMetadataRetriever.
     * Falls back to 8000L if metadata is unreadable or duration is non-positive.
     */
    fun getRealDuration(context: Context, uri: Uri): Long {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val parsed = durationStr?.toLongOrNull() ?: 8000L
            if (parsed > 0L) parsed else 8000L
        } catch (_: Exception) {
            8000L
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {
            }
        }
    }
}
