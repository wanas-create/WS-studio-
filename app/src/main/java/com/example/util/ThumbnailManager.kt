package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object ThumbnailManager {
    private val thumbnailCache = ConcurrentHashMap<String, List<Bitmap>>()

    /**
     * Extracts a filmstrip of video frames across the video duration
     * using MediaMetadataRetriever, returning scaled bitmaps.
     */
    suspend fun getThumbnails(
        context: Context,
        uriString: String,
        durationSec: Float,
        targetCount: Int = 8
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        if (uriString.isBlank() || durationSec <= 0f) return@withContext emptyList()

        val cacheKey = "$uriString-$targetCount-${durationSec.toInt()}"
        thumbnailCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty() && !cached.first().isRecycled) {
                return@withContext cached
            }
        }

        val uri = try {
            Uri.parse(uriString)
        } catch (_: Exception) {
            return@withContext emptyList()
        }

        var retriever: MediaMetadataRetriever? = null
        val bitmaps = mutableListOf<Bitmap>()
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            val durationUs = (durationSec * 1_000_000L).toLong().coerceAtLeast(100_000L)
            val stepUs = durationUs / targetCount.coerceAtLeast(1)

            for (i in 0 until targetCount) {
                val timeUs = (i * stepUs + stepUs / 4).coerceIn(0L, durationUs)
                val frame = try {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(timeUs)
                } catch (_: Exception) {
                    null
                }

                if (frame != null) {
                    val targetWidth = 140
                    val targetHeight = (frame.height * targetWidth / frame.width.coerceAtLeast(1)).coerceIn(60, 140)
                    val scaled = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
                    if (scaled != frame) {
                        frame.recycle()
                    }
                    bitmaps.add(scaled)
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {
            }
        }

        if (bitmaps.isNotEmpty()) {
            thumbnailCache[cacheKey] = bitmaps
        }
        bitmaps
    }

    /**
     * Safely recycles and clears all cached video frame bitmaps.
     */
    fun clearCache() {
        for ((_, list) in thumbnailCache) {
            for (bmp in list) {
                if (!bmp.isRecycled) {
                    bmp.recycle()
                }
            }
        }
        thumbnailCache.clear()
    }
}
