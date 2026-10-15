package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.model.VideoAspect

data class ImportedVideoInfo(
    val durationMs: Long,
    val durationSec: Float,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val effectiveAspect: VideoAspect
)

object VideoImportManager {

    /**
     * Extracts complete video information including duration, dimensions, rotation,
     * and calculates the accurate aspect ratio from imported media metadata.
     */
    fun getVideoInfo(context: Context, uri: Uri): ImportedVideoInfo {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val parsedDuration = durationStr?.toLongOrNull() ?: 12000L
            val durationMs = if (parsedDuration > 0L) parsedDuration else 12000L
            val durationSec = (durationMs / 1000.0f).coerceAtLeast(1.0f)

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            val rawWidth = widthStr?.toIntOrNull() ?: 1920
            val rawHeight = heightStr?.toIntOrNull() ?: 1080
            val rotation = rotationStr?.toIntOrNull() ?: 0

            // If rotation is 90 or 270 degrees, effective width and height are swapped
            val effectiveWidth = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
            val effectiveHeight = if (rotation == 90 || rotation == 270) rawWidth else rawHeight

            val ratio = effectiveWidth.toFloat() / effectiveHeight.toFloat().coerceAtLeast(1f)

            val effectiveAspect = when {
                ratio > 2.0f -> VideoAspect.RATIO_21_9
                ratio > 1.4f -> VideoAspect.RATIO_16_9
                ratio < 0.65f -> VideoAspect.RATIO_9_16
                ratio in 0.70f..0.88f -> VideoAspect.RATIO_4_5
                else -> VideoAspect.RATIO_1_1
            }

            ImportedVideoInfo(
                durationMs = durationMs,
                durationSec = durationSec,
                width = effectiveWidth,
                height = effectiveHeight,
                rotation = rotation,
                effectiveAspect = effectiveAspect
            )
        } catch (_: Exception) {
            ImportedVideoInfo(
                durationMs = 12000L,
                durationSec = 12.0f,
                width = 1920,
                height = 1080,
                rotation = 0,
                effectiveAspect = VideoAspect.RATIO_16_9
            )
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Extracts real video duration in milliseconds using MediaMetadataRetriever.
     * Falls back to metadata or 12000L if metadata is unreadable.
     */
    fun getRealDuration(context: Context, uri: Uri): Long {
        return getVideoInfo(context, uri).durationMs
    }
}
