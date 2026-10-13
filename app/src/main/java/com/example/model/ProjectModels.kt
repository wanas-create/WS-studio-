package com.example.model

enum class VideoAspect(val label: String, val ratioWidth: Float, val ratioHeight: Float, val description: String) {
    RATIO_16_9("16:9", 16f, 9f, "YouTube / Widescreen"),
    RATIO_9_16("9:16", 9f, 16f, "TikTok / Shorts / Reels"),
    RATIO_1_1("1:1", 1f, 1f, "Instagram Feed"),
    RATIO_4_5("4:5", 4f, 5f, "Portrait Social"),
    RATIO_21_9("21:9", 21f, 9f, "Cinematic Ultra-Wide")
}

data class VideoClip(
    val id: String,
    val title: String,
    val durationSec: Float = 5.0f,
    val startOffsetSec: Float = 0f,
    val speed: Float = 1.0f,
    val filterName: String = "Normal",
    val filterIntensity: Float = 0.8f,
    val effectName: String = "None",
    val isReversed: Boolean = false,
    val volume: Float = 1.0f,
    val transitionIn: String = "None",
    val colorToneHex: Long = 0xFF1E3A8A,
    val videoUri: String? = null,
    val effects: List<EffectOverlay> = emptyList(),
    val startMs: Long = (startOffsetSec * 1000f).toLong(),
    val endMs: Long = ((startOffsetSec + durationSec) * 1000f).toLong()
)

data class AudioClip(
    val id: String,
    val name: String,
    val artist: String = "Studio Audio",
    val durationSec: Float = 12.0f,
    val startOffsetSec: Float = 0f,
    val volume: Float = 0.8f,
    val waveformPoints: List<Float> = listOf(0.4f, 0.7f, 0.9f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.4f, 0.2f, 0.7f, 0.6f)
)

data class TextOverlay(
    val id: String,
    val text: String,
    val fontName: String = "Impact Pro",
    val animationType: String = "Typewriter",
    val colorHex: Long = 0xFFFFFFFF,
    val strokeColorHex: Long = 0xFF000000,
    val startSec: Float = 0f,
    val durationSec: Float = 3.5f,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f,
    val scale: Float = 1.0f
)

data class EffectOverlay(
    val id: String,
    val name: String = id,
    val category: String = "Video Effects",
    val intensity: Float = 0.8f,
    val startSec: Float = 0f,
    val durationSec: Float = 2f,
    val startTime: Float = startSec,
    val duration: Float = durationSec
)

data class VideoProject(
    val id: String,
    val title: String,
    val aspect: VideoAspect = VideoAspect.RATIO_16_9,
    val resolution: String = "1080p",
    val fps: Int = 60,
    val durationSec: Float = 0.0f,
    val lastModified: Long = System.currentTimeMillis(),
    val videoUri: String? = null,
    val videoFileName: String? = null,
    val clips: List<VideoClip> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val effects: List<EffectOverlay> = emptyList()
)
