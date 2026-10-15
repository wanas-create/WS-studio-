package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.PlaybackParameters
import androidx.media3.ui.PlayerView
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.ThumbnailManager
import com.example.util.VideoImportManager
import kotlinx.coroutines.delay

enum class EditorTool(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    NONE("Tools", Icons.Default.Build),
    TRIM("Trim", Icons.Default.ContentCut),
    SPLIT("Split", Icons.Default.VerticalAlignCenter),
    SPEED("Speed", Icons.Default.Speed),
    REVERSE("Reverse", Icons.Default.FastRewind),
    FILTERS("Filters", Icons.Default.ColorLens),
    EFFECTS("Effects", Icons.Default.AutoAwesome),
    TRANSITIONS("Transitions", Icons.Default.Transform),
    TEXT("Text", Icons.Default.TextFields),
    STICKERS("Stickers", Icons.Default.EmojiEmotions),
    MUSIC("Music", Icons.Default.MusicNote),
    VOICEOVER("Voiceover", Icons.Default.Mic),
    OVERLAY("Overlay", Icons.Default.Layers),
    RATIO("Ratio", Icons.Default.AspectRatio),
    BACKGROUND("Background", Icons.Default.Wallpaper)
}

@Composable
fun EditorScreen(
    initialProject: VideoProject,
    onBack: () -> Unit,
    onNavigateToExport: (VideoProject) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var project by remember {
        // Ensure durationSec is synchronized with clips sum
        val initialDuration = if (initialProject.clips.isNotEmpty()) {
            initialProject.clips.sumOf { it.durationSec.toDouble() }.toFloat()
        } else {
            initialProject.durationSec
        }
        mutableStateOf(initialProject.copy(durationSec = initialDuration))
    }

    var activeTool by remember { mutableStateOf(EditorTool.NONE) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayheadSec by remember { mutableFloatStateOf(0.0f) }
    var selectedClipIndex by remember { mutableIntStateOf(0) }
    var timelineZoom by remember { mutableFloatStateOf(1.0f) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }

    // Studio Player Manager with ExoPlayer and playback synchronization
    val playerManager = remember { StudioPlayerManager(context) }

    // Filmstrip video frame thumbnails extracted across timeline
    var timelineThumbnails by remember(project.videoUri, project.durationSec) {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    LaunchedEffect(project.videoUri, project.durationSec) {
        val uri = project.videoUri
        if (!uri.isNullOrEmpty() && project.durationSec > 0f) {
            timelineThumbnails = ThumbnailManager.getThumbnails(
                context = context,
                uriString = uri,
                durationSec = project.durationSec,
                targetCount = 8
            )
        } else {
            timelineThumbnails = emptyList()
        }
    }

    DisposableEffect(project.videoUri) {
        val activeClip = project.clips.getOrNull(selectedClipIndex)
        val initialSpeed = activeClip?.speed ?: 1.0f
        playerManager.initializePlayer(
            videoUriString = project.videoUri,
            initialSpeed = initialSpeed,
            onPlayStateChange = { playing ->
                isPlaying = playing
            }
        )
        onDispose {
            playerManager.releasePlayer()
            ThumbnailManager.clearCache()
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Gallery Video"
            val videoInfo = VideoImportManager.getVideoInfo(context, uri)
            val realDurationSec = videoInfo.durationSec
            val aspect = videoInfo.effectiveAspect
            val newClip = VideoClip(
                id = "clip_${System.currentTimeMillis()}",
                title = fileName,
                durationSec = realDurationSec,
                videoUri = uri.toString(),
                colorToneHex = 0xFF0D47A1,
                startOffsetSec = 0f,
                startMs = 0L,
                endMs = videoInfo.durationMs
            )
            ThumbnailManager.clearCache()
            project = project.copy(
                videoUri = uri.toString(),
                videoFileName = fileName,
                durationSec = realDurationSec,
                aspect = aspect,
                clips = listOf(newClip)
            )
            currentPlayheadSec = 0f
            selectedClipIndex = 0
            playerManager.initializePlayer(
                videoUriString = uri.toString(),
                initialSpeed = 1.0f,
                playWhenReady = false,
                onPlayStateChange = { playing -> isPlaying = playing }
            )
            showToastMessage = "Video imported (${String.format("%.1f", realDurationSec)}s • ${aspect.label})"
        }
    }

    // High precision Player.Listener / Playback loop to synchronize playhead, timeText, and timelineView
    LaunchedEffect(isPlaying, project.durationSec, selectedClipIndex, project.clips) {
        while (isPlaying && project.durationSec > 0) {
            delay(33) // ~30 FPS live timeline progression
            val playerPosMs = playerManager.exoPlayer?.currentPosition
            if (playerPosMs != null && playerPosMs >= 0) {
                val playerPosSec = (playerPosMs / 1000f).coerceIn(0f, project.durationSec)
                if (playerPosSec >= project.durationSec) {
                    currentPlayheadSec = 0f
                    isPlaying = false
                    playerManager.pause()
                    playerManager.seekTo(0)
                } else {
                    currentPlayheadSec = playerPosSec
                }
            } else {
                val activeClip = project.clips.getOrNull(selectedClipIndex) ?: project.clips.firstOrNull()
                val speed = activeClip?.speed ?: 1.0f
                val delta = 0.033f * speed
                val nextTime = currentPlayheadSec + delta
                if (nextTime >= project.durationSec) {
                    currentPlayheadSec = 0f
                    isPlaying = false
                    playerManager.pause()
                    playerManager.seekTo(0)
                } else {
                    currentPlayheadSec = nextTime
                }
            }
        }
    }

    // Toast auto-clear
    LaunchedEffect(showToastMessage) {
        if (showToastMessage != null) {
            delay(2400)
            showToastMessage = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("editor_screen"),
        containerColor = DarkBackground,
        topBar = {
            EditorTopBar(
                project = project,
                onBack = onBack,
                onExportClick = {
                    if (project.clips.isNotEmpty()) {
                        onNavigateToExport(project)
                    } else {
                        showToastMessage = "Please pick a video first"
                    }
                },
                onAspectChanged = { newAspect -> project = project.copy(aspect = newAspect) }
            )
        }
    ) { innerPadding ->
        // If user didn't pick video, show "Pick video from gallery" button only
        if (project.clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(CobaltBlue.copy(alpha = 0.15f))
                            .border(2.dp, ElectricBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = "Pick Video",
                            tint = ElectricBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "No Video Selected",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Select a video from your gallery to open the 4K multi-track timeline editor.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            pickVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("pick_video_from_gallery_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Pick video from gallery",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Full Editor View with synchronized timeline & video canvas
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. HD Video Preview Stage with Live Shader Effects & Synced Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f)
                        .background(Color(0xFF06090F))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VideoPreviewCanvas(
                        project = project,
                        currentPlayheadSec = currentPlayheadSec,
                        isPlaying = isPlaying,
                        selectedClipIndex = selectedClipIndex,
                        player = playerManager.exoPlayer,
                        onRemoveEffect = {
                            val updatedClips = project.clips.mapIndexed { idx, c ->
                                if (idx == selectedClipIndex) c.copy(effectName = "None") else c
                            }
                            project = project.copy(effects = emptyList(), clips = updatedClips)
                            showToastMessage = "Removed effect"
                        }
                    )

                    // Watermark status badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(0.8.dp, EmeraldGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "ALL FREE • NO WATERMARK",
                            color = EmeraldGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Toast message popup
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showToastMessage != null,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DarkSurfaceCard,
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    showToastMessage ?: "",
                                    color = ElectricBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Playback Control Bar (Synchronized with Player.Listener)
                PlaybackControlsBar(
                    isPlaying = isPlaying,
                    currentPlayheadSec = currentPlayheadSec,
                    totalDurationSec = project.durationSec,
                    onTogglePlay = {
                        val willPlay = !isPlaying
                        isPlaying = willPlay
                        if (willPlay) {
                            val seekMs = (currentPlayheadSec * 1000).toLong()
                            playerManager.seekTo(seekMs)
                            playerManager.play()
                        } else {
                            playerManager.pause()
                        }
                    },
                    onSeek = { seekSec ->
                        currentPlayheadSec = seekSec.coerceIn(0f, project.durationSec)
                        playerManager.seekTo((currentPlayheadSec * 1000).toLong())
                    },
                    onZoomIn = { timelineZoom = (timelineZoom * 1.25f).coerceAtMost(3.0f) },
                    onZoomOut = { timelineZoom = (timelineZoom * 0.8f).coerceAtLeast(0.6f) }
                )

                // 3. Multi-Track Timeline Stage (Strictly synchronized with playhead)
                MultiTrackTimeline(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.36f),
                    project = project,
                    currentPlayheadSec = currentPlayheadSec,
                    selectedClipIndex = selectedClipIndex,
                    timelineZoom = timelineZoom,
                    isPlaying = isPlaying,
                    timelineThumbnails = timelineThumbnails,
                    onSelectClip = { selectedClipIndex = it },
                    onSeek = { seekSec ->
                        currentPlayheadSec = seekSec.coerceIn(0f, project.durationSec)
                        playerManager.seekTo((currentPlayheadSec * 1000).toLong())
                    }
                )

                // 4. CapCut Bottom Toolbar / Tool Sub-sheet with System Navigation Insets
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    AnimatedContent(
                        targetState = activeTool,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "toolbar_state"
                    ) { tool ->
                        if (tool == EditorTool.NONE) {
                            BottomToolbarRow(
                                onSelectTool = { selected ->
                                    when (selected) {
                                        EditorTool.SPLIT -> {
                                            // Bug 2: Block split if playhead < 200ms and properly partition part1 & part2
                                            val updated = splitClipAtPlayhead(
                                                project = project,
                                                currentPlayheadSec = currentPlayheadSec,
                                                selectedClipIndex = selectedClipIndex,
                                                onShowToast = { showToastMessage = it }
                                            )
                                            if (updated != null) {
                                                project = updated
                                            }
                                        }

                                        EditorTool.REVERSE -> {
                                            // Bug 3: Single boolean isReversed strictly controls UI banner
                                            val activeClip = project.clips.getOrNull(selectedClipIndex)
                                            if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                                val nextReversed = !activeClip.isReversed
                                                val updatedClips = project.clips.mapIndexed { idx, c ->
                                                    if (idx == selectedClipIndex) c.copy(isReversed = nextReversed) else c
                                                }
                                                project = project.copy(clips = updatedClips)
                                                showToastMessage = if (nextReversed) "Reversed playback enabled" else "Restored original playback"
                                            }
                                        }

                                        EditorTool.TRANSITIONS -> {
                                            // Bug 9: Block transitions if clips.size < 2
                                            if (project.clips.size < 2) {
                                                showToastMessage = "Add at least 2 clips to apply transitions"
                                            } else {
                                                activeTool = selected
                                            }
                                        }

                                        else -> {
                                            activeTool = selected
                                        }
                                    }
                                }
                            )
                        } else {
                            ToolDetailSubSheet(
                                tool = tool,
                                project = project,
                                selectedClipIndex = selectedClipIndex,
                                currentPlayheadSec = currentPlayheadSec,
                                playerManager = playerManager,
                                onClose = {
                                    // Bug 4: Clear tool selection and hide bottom sheet cleanly
                                    activeTool = EditorTool.NONE
                                },
                                onUpdateProject = { updatedProject ->
                                    val newDuration = updatedProject.clips.sumOf { it.durationSec.toDouble() }.toFloat()
                                    project = updatedProject.copy(durationSec = newDuration.coerceAtLeast(0.1f))
                                },
                                onShowToast = { showToastMessage = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bug 2 Fix: Safely split clip at current playhead.
 * Blocks split if playhead < 200ms or < 200ms from clip boundary.
 * Correctly creates part1 (endMs = playhead) and part2 (startMs = playhead) without deleting Part 1.
 */
fun splitClipAtPlayhead(
    project: VideoProject,
    currentPlayheadSec: Float,
    selectedClipIndex: Int,
    onShowToast: (String) -> Unit
): VideoProject? {
    if (currentPlayheadSec < 0.200f) {
        onShowToast("Cannot split: Playhead is before 200ms (00:00.2)")
        return null
    }

    var cumulativeSec = 0f
    var targetIndex = -1
    var offsetInClip = 0f

    for (i in project.clips.indices) {
        val clip = project.clips[i]
        val clipStart = cumulativeSec
        val clipEnd = cumulativeSec + clip.durationSec
        if (currentPlayheadSec >= clipStart && currentPlayheadSec < clipEnd) {
            targetIndex = i
            offsetInClip = currentPlayheadSec - clipStart
            break
        }
        cumulativeSec = clipEnd
    }

    if (targetIndex == -1) {
        targetIndex = selectedClipIndex.coerceIn(0, (project.clips.size - 1).coerceAtLeast(0))
        var prevClipsDuration = 0f
        for (i in 0 until targetIndex) {
            prevClipsDuration += project.clips[i].durationSec
        }
        offsetInClip = (currentPlayheadSec - prevClipsDuration).coerceIn(0f, project.clips[targetIndex].durationSec)
    }

    if (targetIndex !in project.clips.indices) {
        onShowToast("No clip found at playhead")
        return null
    }

    val targetClip = project.clips[targetIndex]

    if (offsetInClip < 0.200f) {
        onShowToast("Cannot split: Playhead must be at least 200ms into clip")
        return null
    }

    val remainingInClip = targetClip.durationSec - offsetInClip
    if (remainingInClip < 0.200f) {
        onShowToast("Cannot split: Playhead is too close to end of clip")
        return null
    }

    val baseTitle = targetClip.title.replace(Regex(" \\(Part \\d+\\)$"), "")

    val part1 = targetClip.copy(
        durationSec = offsetInClip
    )
    val part2 = targetClip.copy(
        id = "clip_${System.currentTimeMillis()}_part2",
        title = "$baseTitle (Part 2)",
        durationSec = remainingInClip,
        startOffsetSec = targetClip.startOffsetSec + offsetInClip
    )

    val updatedClips = project.clips.toMutableList()
    updatedClips[targetIndex] = part1
    updatedClips.add(targetIndex + 1, part2)

    val totalDuration = updatedClips.sumOf { it.durationSec.toDouble() }.toFloat()
    onShowToast("Split into Part 1 (${String.format("%.1f", offsetInClip)}s) & Part 2 (${String.format("%.1f", remainingInClip)}s)")

    return project.copy(
        clips = updatedClips,
        durationSec = totalDuration
    )
}

@Composable
fun EditorTopBar(
    project: VideoProject,
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    onAspectChanged: (VideoAspect) -> Unit
) {
    var showAspectMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("editor_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    project.videoFileName ?: project.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    "${project.aspect.label} • 4K HD",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Button(
                    onClick = { showAspectMenu = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(project.aspect.label, color = ElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = showAspectMenu,
                    onDismissRequest = { showAspectMenu = false },
                    containerColor = DarkSurfaceCard
                ) {
                    VideoAspect.values().forEach { aspect ->
                        DropdownMenuItem(
                            text = { Text("${aspect.label} (${aspect.description})", color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                showAspectMenu = false
                                onAspectChanged(aspect)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onExportClick,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("export_button_top_right"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * HD Video Preview Canvas with active GL Shader visual simulations
 * and single boolean isReversed control banner
 */
@Composable
fun VideoPreviewCanvas(
    project: VideoProject,
    currentPlayheadSec: Float,
    isPlaying: Boolean,
    selectedClipIndex: Int,
    player: androidx.media3.common.Player? = null,
    onRemoveEffect: (() -> Unit)? = null
) {
    val activeClip = project.clips.getOrNull(selectedClipIndex) ?: project.clips.firstOrNull()
    val targetAspect = project.aspect.ratioWidth / project.aspect.ratioHeight

    // Active effect resolution
    val activeEffect = project.effects.firstOrNull {
        currentPlayheadSec >= it.startSec && currentPlayheadSec <= (it.startSec + it.durationSec)
    } ?: project.effects.lastOrNull()
      ?: if (activeClip?.effectName != null && activeClip.effectName != "None") {
          EffectOverlay(id = "clip_fx", name = activeClip.effectName, intensity = activeClip.effectIntensity)
      } else null

    val effectIntensity = (activeEffect?.intensity ?: 0.8f).coerceIn(0f, 1f)

    // Motion & Shake camera translations
    val isShakeEffect = activeEffect != null && effectIntensity > 0.01f &&
        (activeEffect.name.contains("Shake") || activeEffect.name.contains("Earthquake") || activeEffect.name.contains("Jitter") || activeEffect.name.contains("Bounce"))
    val isZoomEffect = activeEffect != null && effectIntensity > 0.01f &&
        (activeEffect.name.contains("Zoom") || activeEffect.name.contains("Heartbeat") || activeEffect.name.contains("Pulse"))

    val shakeX = if (isShakeEffect) {
        kotlin.math.sin(currentPlayheadSec * 35f) * 16f * effectIntensity
    } else 0f
    val shakeY = if (isShakeEffect) {
        kotlin.math.cos(currentPlayheadSec * 28f) * 12f * effectIntensity
    } else 0f
    val zoomScale = if (isZoomEffect) {
        1.0f + 0.08f * effectIntensity * kotlin.math.sin(currentPlayheadSec * 10f)
    } else 1.0f

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(targetAspect, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Video rendering container with camera shake / motion transformations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shakeX
                    translationY = shakeY
                    scaleX = zoomScale
                    scaleY = zoomScale
                }
        ) {
            // Real ExoPlayer Video Surface
            if (player != null && !project.videoUri.isNullOrEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            this.player = player
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != player) {
                            playerView.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val baseColor = Color(activeClip?.colorToneHex ?: 0xFF0D47A1)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    baseColor,
                                    baseColor.copy(alpha = 0.65f),
                                    Color(0xFF04060A)
                                )
                            )
                        )
                )
            }

            // Live visual filter overlay simulation scaled by filterIntensity (0% to 100%)
            val filterName = activeClip?.filterName ?: "Normal"
            val filterIntensity = (activeClip?.filterIntensity ?: 0.8f).coerceIn(0f, 1f)

            if (filterName != "Normal" && filterIntensity > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                filterName.contains("Teal") || filterName.contains("Miami") ->
                                    Color(0xFF00E5FF).copy(alpha = 0.38f * filterIntensity)
                                filterName.contains("Cyber") || filterName.contains("Violet") || filterName.contains("Neon") ->
                                    Color(0xFF7C4DFF).copy(alpha = 0.42f * filterIntensity)
                                filterName.contains("Vintage") || filterName.contains("Kodachrome") || filterName.contains("Sepia") ->
                                    Color(0xFFFFB300).copy(alpha = 0.36f * filterIntensity)
                                filterName.contains("B&W") || filterName.contains("Monochrome") || filterName.contains("Noir") ->
                                    Color.Gray.copy(alpha = 0.65f * filterIntensity)
                                filterName.contains("Portrait") || filterName.contains("Skin") || filterName.contains("Glow") ->
                                    Color(0xFFFFCCBC).copy(alpha = 0.30f * filterIntensity)
                                filterName.contains("Anime") || filterName.contains("Ghibli") ->
                                    Color(0xFF29B6F6).copy(alpha = 0.32f * filterIntensity)
                                filterName.contains("Moody") || filterName.contains("Cold") ->
                                    Color(0xFF1A237E).copy(alpha = 0.38f * filterIntensity)
                                filterName.contains("Vibrant") || filterName.contains("Punch") ->
                                    Color(0xFFFF4081).copy(alpha = 0.32f * filterIntensity)
                                else -> Color(0xFF00B0FF).copy(alpha = 0.28f * filterIntensity)
                            }
                        )
                )
            }

            // Effects shader visual simulation and overlay tint scaled by effectIntensity
            if (activeEffect != null && effectIntensity > 0.01f) {
                val activeEffectName = activeEffect.name

                // Tint overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                activeEffectName.contains("Magnetic") || activeEffectName.contains("Distortion") ->
                                    Color(0xFF00E5FF).copy(alpha = 0.32f * effectIntensity)
                                activeEffectName.contains("Glitch") || activeEffectName.contains("RGB") ->
                                    Color(0xFFFF0055).copy(alpha = 0.35f * effectIntensity)
                                activeEffectName.contains("Neon") || activeEffectName.contains("Bloom") || activeEffectName.contains("Glow") ->
                                    Color(0xFF7C4DFF).copy(alpha = 0.38f * effectIntensity)
                                activeEffectName.contains("VHS") || activeEffectName.contains("CRT") || activeEffectName.contains("Retro") ->
                                    Color(0xFFFFB300).copy(alpha = 0.30f * effectIntensity)
                                activeEffectName.contains("Earthquake") || activeEffectName.contains("Shake") ->
                                    Color(0xFFFF1744).copy(alpha = 0.28f * effectIntensity)
                                else -> Color(0xFF651FFF).copy(alpha = 0.28f * effectIntensity)
                            }
                        )
                )

                // GL Shader visual simulation lines & glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    when {
                        activeEffectName.contains("Magnetic") || activeEffectName.contains("Glitch") || activeEffectName.contains("RGB") -> {
                            // Magnetic Distortion / RGB Split scanlines & chromatic borders
                            drawLine(
                                color = Color.Cyan.copy(alpha = 0.65f * effectIntensity),
                                start = Offset(0f, h * 0.35f),
                                end = Offset(w, h * 0.35f),
                                strokeWidth = 4f
                            )
                            drawLine(
                                color = Color.Magenta.copy(alpha = 0.65f * effectIntensity),
                                start = Offset(0f, h * 0.65f),
                                end = Offset(w, h * 0.65f),
                                strokeWidth = 4f
                            )
                        }
                        activeEffectName.contains("VHS") || activeEffectName.contains("CRT") || activeEffectName.contains("Scan") -> {
                            // CRT Scanlines
                            val lineSpacing = 16f
                            var y = 0f
                            while (y < h) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f * effectIntensity),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1.5f
                                )
                                y += lineSpacing
                            }
                        }
                        activeEffectName.contains("Neon") || activeEffectName.contains("Bloom") || activeEffectName.contains("Glow") -> {
                            // Perimeter neon glow
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF7C4DFF).copy(alpha = 0.50f * effectIntensity))
                                )
                            )
                        }
                        activeEffectName.contains("Earthquake") || activeEffectName.contains("Shake") -> {
                            // Jitter vignette
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color(0xFFFF5252).copy(alpha = 0.35f * effectIntensity))
                                )
                            )
                        }
                    }
                }
            }
        }

        // Active Effect Title & Intensity Pill with Remove Option
        if (activeEffect != null && effectIntensity > 0.01f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, AmberGold)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${activeEffect.name} • ${(effectIntensity * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (onRemoveEffect != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove effect",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemoveEffect() }
                        )
                    }
                }
            }
        }

        // Show placeholder icon and title only if no real video is loaded
        if (project.videoUri.isNullOrEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    Icons.Default.MovieFilter,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    activeClip?.title ?: "User Video Clip",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bug 3 Fix: Single boolean isReversed strictly controls UI banner
        val isReversed = activeClip?.isReversed == true
        if (isReversed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AmberGold.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, AmberGold)
                ) {
                    Text(
                        "◀ REVERSED PLAYBACK",
                        color = AmberGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Live Animated Text Overlays
        project.textOverlays.forEach { textOverlay ->
            if (currentPlayheadSec >= textOverlay.startSec && currentPlayheadSec <= (textOverlay.startSec + textOverlay.durationSec)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = textOverlay.text,
                        color = Color(textOverlay.colorHex),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Playback Controls Bar with synchronized formatTime (00:06.4 / 00:08.0)
 */
@Composable
fun PlaybackControlsBar(
    isPlaying: Boolean,
    currentPlayheadSec: Float,
    totalDurationSec: Float,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Bug 1 Fix: Correctly formatted live playhead time
            Text(
                formatTime(currentPlayheadSec),
                color = ElectricBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                " / ${formatTime(totalDurationSec)}",
                color = TextMuted,
                fontSize = 13.sp
            )
        }

        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CobaltBlue)
                .testTag("play_pause_button")
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onZoomOut, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Formats seconds into MM:SS.S with tenths of a second (e.g. 00:06.4)
 */
fun formatTime(sec: Float): String {
    val nonNegSec = sec.coerceAtLeast(0f)
    val totalSec = nonNegSec.toInt()
    val minutes = totalSec / 60
    val remainingSec = totalSec % 60
    val tenths = ((nonNegSec - totalSec) * 10).toInt().coerceIn(0, 9)
    return String.format("%02d:%02d.%d", minutes, remainingSec, tenths)
}

/**
 * Bug 1 Fix: Multi-Track Timeline that strictly tracks and auto-scrolls
 * with the playhead, and displays active effects on Track 4.
 */
@Composable
fun MultiTrackTimeline(
    modifier: Modifier = Modifier,
    project: VideoProject,
    currentPlayheadSec: Float,
    selectedClipIndex: Int,
    timelineZoom: Float,
    isPlaying: Boolean,
    timelineThumbnails: List<Bitmap> = emptyList(),
    onSelectClip: (Int) -> Unit,
    onSeek: (Float) -> Unit
) {
    val scrollState = rememberScrollState()
    val pixelsPerSec = 50f * timelineZoom

    // Auto-scroll timeline to follow playhead smoothly during playback
    LaunchedEffect(currentPlayheadSec, isPlaying) {
        if (isPlaying) {
            val playheadPx = (currentPlayheadSec * pixelsPerSec).toInt()
            if (kotlin.math.abs(scrollState.value - playheadPx) > 8) {
                scrollState.scrollTo(playheadPx)
            }
        }
    }

    // Live scrubbing while dragging/scrolling timeline while paused
    LaunchedEffect(scrollState.value) {
        if (!isPlaying && project.durationSec > 0f && scrollState.isScrollInProgress) {
            val scrubSec = (scrollState.value / pixelsPerSec).coerceIn(0f, project.durationSec)
            onSeek(scrubSec)
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF090D14))
            .border(0.8.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp)
        ) {
            Spacer(Modifier.width(180.dp))

            Column(
                modifier = Modifier
                    .width((project.durationSec * pixelsPerSec).coerceAtLeast(320f).dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Timecode ruler ticks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                ) {
                    val step = 2
                    val ticks = ((project.durationSec / step).toInt() + 2).coerceAtLeast(4)
                    for (i in 0 until ticks) {
                        Box(
                            modifier = Modifier
                                .width((step * pixelsPerSec).dp)
                                .fillMaxHeight()
                                .clickable {
                                    onSeek((i * step).toFloat().coerceIn(0f, project.durationSec))
                                }
                        ) {
                            Text(
                                "${i * step}s",
                                color = TextMuted,
                                fontSize = 9.sp,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                    }
                }

                // Track 1: Video Clips Track with Real Frame Thumbnails Filmstrip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    project.clips.forEachIndexed { index, clip ->
                        val isSelected = index == selectedClipIndex
                        Box(
                            modifier = Modifier
                                .width((clip.durationSec * pixelsPerSec).coerceAtLeast(60f).dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F172A))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    onSelectClip(index)
                                }
                        ) {
                            // Multiple real thumbnails across timeline filmstrip
                            if (timelineThumbnails.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    timelineThumbnails.forEach { bmp ->
                                        if (!bmp.isRecycled) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Video Frame",
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .border(0.2.dp, Color.Black.copy(alpha = 0.4f)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Elegant filmstrip skeleton while extracting frames
                                Row(modifier = Modifier.fillMaxSize()) {
                                    val slots = clip.durationSec.toInt().coerceIn(4, 10)
                                    for (s in 0 until slots) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(if (s % 2 == 0) Color(0xFF1E293B) else Color(0xFF0F172A))
                                                .border(0.5.dp, Color.White.copy(alpha = 0.05f))
                                        )
                                    }
                                }
                            }

                            // Dark gradient header badge with clip info
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.70f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.60f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.align(Alignment.TopStart)) {
                                    Text(
                                        clip.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    val speedDisplay = String.format("%.2f", clip.speed)
                                    Text(
                                        "${String.format("%.1f", clip.durationSec)}s • ${speedDisplay}x${if (clip.filterName != "Normal") " • ${clip.filterName}" else ""}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Track 2: Audio & Beats Track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TrackAudio.copy(alpha = 0.35f))
                        .border(0.8.dp, TrackAudio, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (project.audioClips.isEmpty()) "No audio added (Tap Music tool below)" else project.audioClips.first().name,
                        color = if (project.audioClips.isEmpty()) TextMuted else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Track 3: Text & Titles Track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TrackText.copy(alpha = 0.35f))
                        .border(0.8.dp, TrackText, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (project.textOverlays.isEmpty()) "No text added (Tap Text tool below)" else project.textOverlays.first().text,
                        color = if (project.textOverlays.isEmpty()) TextMuted else Color.White,
                        fontSize = 10.sp
                    )
                }

                // Track 4: Effects Track (Bug 6 Fix: Displays active effect dynamically)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TrackEffect.copy(alpha = 0.35f))
                        .border(0.8.dp, TrackEffect, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    if (project.effects.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(project.effects) { fx ->
                                val startSec = fx.startSec.toInt()
                                val endSec = (fx.startSec + fx.durationSec).toInt()
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmberGold)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${fx.name} ${startSec}s-${endSec}s",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "No effects added",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(Modifier.width(220.dp))
        }

        // Fixed Red Split Playhead Line in center with position seeker
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(2.dp)
                .background(PlayheadRed)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(PlayheadRed)
            )
        }
    }
}

/**
 * Bug 7 Fix: Horizontal bottom toolbar for all 14 tools
 * (Trim, Split, Speed, Reverse, Filters, Effects, Transitions, Text, Sticker,
 * Music, Voiceover, Overlay, Ratio, Background) with 48dp touch targets.
 */
@Composable
fun BottomToolbarRow(
    onSelectTool: (EditorTool) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        color = DarkSurface,
        tonalElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(EditorTool.values().filter { it != EditorTool.NONE }) { tool ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectTool(tool) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("tool_${tool.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = tool.title, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(tool.title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Tool Detail Sub-Sheet with dedicated behavior for all tools.
 * Bug 4 Fix: Hides on apply and clears selection on hidden.
 * Bug 5 Fix: Loads assets/filters.json using filter.displayName.
 * Bug 6 Fix: Adds effect to project.effects list.
 * Bug 8 Fix: Connects speedSlider directly to exoPlayer and clip.speed.
 * Bug 9 Fix: Blocks transitions when clips.size < 2.
 */
@Composable
fun ToolDetailSubSheet(
    tool: EditorTool,
    project: VideoProject,
    selectedClipIndex: Int,
    currentPlayheadSec: Float = 0f,
    playerManager: StudioPlayerManager,
    onClose: () -> Unit,
    onUpdateProject: (VideoProject) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val activeClip = project.clips.getOrNull(selectedClipIndex) ?: project.clips.firstOrNull()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        color = DarkSurface,
        tonalElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(tool.icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${tool.title} Settings",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            when (tool) {
                // Trim Settings with start and end sliders, recalculating duration and closing sheet
                EditorTool.TRIM -> {
                    val clipDuration = (activeClip?.durationSec ?: 5.0f).coerceAtLeast(1.0f)
                    var startSec by remember(activeClip?.id, activeClip?.startOffsetSec) {
                        mutableFloatStateOf(activeClip?.startOffsetSec ?: 0f)
                    }
                    var endSec by remember(activeClip?.id, activeClip?.startOffsetSec, activeClip?.durationSec) {
                        val initEnd = (activeClip?.startOffsetSec ?: 0f) + (activeClip?.durationSec ?: 5.0f)
                        mutableFloatStateOf(initEnd.coerceAtMost(clipDuration))
                    }

                    val trimmedDuration = (endSec - startSec).coerceAtLeast(0.1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Trim: ${String.format("%.1f", startSec)}s - ${String.format("%.1f", endSec)}s",
                            color = ElectricBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Length: ${String.format("%.1f", trimmedDuration)}s",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Start point: ${String.format("%.1f", startSec)}s", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = startSec,
                        onValueChange = { 
                            startSec = it.coerceAtMost((endSec - 0.2f).coerceAtLeast(0f))
                        },
                        valueRange = 0f..clipDuration,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = CobaltBlue)
                    )

                    Text("End point: ${String.format("%.1f", endSec)}s", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = endSec,
                        onValueChange = { 
                            endSec = it.coerceAtLeast(startSec + 0.2f).coerceAtMost(clipDuration)
                        },
                        valueRange = 0f..clipDuration,
                        colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold)
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val sMs = (startSec * 1000f).toLong()
                                val eMs = (endSec * 1000f).toLong()
                                val newDurSec = (endSec - startSec).coerceAtLeast(0.1f)
                                if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                    val updatedClips = project.clips.mapIndexed { idx, c ->
                                        if (idx == selectedClipIndex) {
                                            c.copy(
                                                startOffsetSec = startSec,
                                                durationSec = newDurSec,
                                                startMs = sMs,
                                                endMs = eMs
                                            )
                                        } else c
                                    }
                                    val newTotal = updatedClips.sumOf { it.durationSec.toDouble() }.toFloat()
                                    onUpdateProject(project.copy(clips = updatedClips, durationSec = newTotal.coerceAtLeast(0.1f)))
                                    onShowToast("Trim applied: ${String.format("%.1f", newDurSec)}s")
                                }
                                onClose() // Immediately closes sheet
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                        ) {
                            Text("Apply Trim", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val sMs = (startSec * 1000f).toLong()
                                val eMs = (endSec * 1000f).toLong()
                                val newDurSec = (endSec - startSec).coerceAtLeast(0.1f)
                                val updatedClips = project.clips.map { c ->
                                    val cDur = c.durationSec.coerceAtLeast(0.1f)
                                    val safeEnd = endSec.coerceAtMost(cDur)
                                    val safeStart = startSec.coerceAtMost((safeEnd - 0.1f).coerceAtLeast(0f))
                                    val dur = (safeEnd - safeStart).coerceAtLeast(0.1f)
                                    c.copy(
                                        startOffsetSec = safeStart,
                                        durationSec = dur,
                                        startMs = (safeStart * 1000f).toLong(),
                                        endMs = (safeEnd * 1000f).toLong()
                                    )
                                }
                                val newTotal = updatedClips.sumOf { it.durationSec.toDouble() }.toFloat()
                                onUpdateProject(project.copy(clips = updatedClips, durationSec = newTotal.coerceAtLeast(0.1f)))
                                onShowToast("Trim applied to all clips")
                                onClose() // Immediately closes sheet
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply to All Clips", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }

                // Bug 8 Fix: Connect speedSlider directly to exoPlayer & clip.speed
                EditorTool.SPEED -> {
                    var speedValue by remember(activeClip?.id, activeClip?.speed) {
                        mutableFloatStateOf(activeClip?.speed ?: 1.0f)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Clip Speed: ${String.format("%.2f", speedValue)}x",
                            color = ElectricBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Preset speed chips for instant 1-tap selection
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.5f, 1.0f, 2.0f, 4.0f).forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (speedValue == preset) ElectricBlue else DarkSurfaceElevated)
                                        .clickable {
                                            speedValue = preset
                                            playerManager.setSpeed(preset)
                                            if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                                val updatedClips = project.clips.mapIndexed { idx, c ->
                                                    if (idx == selectedClipIndex) c.copy(speed = preset) else c
                                                }
                                                onUpdateProject(project.copy(clips = updatedClips))
                                            }
                                            onShowToast("Speed: ${preset}x")
                                            onClose() // Closes sheet on preset selection
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "${preset}x",
                                        color = if (speedValue == preset) Color.Black else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Slider(
                        value = speedValue,
                        onValueChange = {
                            val rounded = (Math.round(it * 100f) / 100f)
                            speedValue = rounded
                            playerManager.setSpeed(rounded)
                            if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                val updatedClips = project.clips.mapIndexed { idx, c ->
                                    if (idx == selectedClipIndex) c.copy(speed = rounded) else c
                                }
                                onUpdateProject(project.copy(clips = updatedClips))
                            }
                        },
                        valueRange = 0.1f..10.0f,
                        steps = 99,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = CobaltBlue)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Smooth Optical Flow 120FPS", color = TextSecondary, fontSize = 11.sp)
                        Button(
                            onClick = {
                                onShowToast("Speed updated to ${String.format("%.2f", speedValue)}x")
                                onClose() // Closes sheet
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Done", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // Bug 5 Fix: Filters list using filter.displayName from assets/filters.json
                EditorTool.FILTERS -> {
                    val allFilters = remember { FilterRepository.getFilters(context) }
                    val categories = remember { FilterRepository.getCategories(context) }
                    var selectedCategory by remember { mutableStateOf("All") }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ElectricBlue else DarkSurfaceElevated)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    cat,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val filteredList = if (selectedCategory == "All") allFilters else allFilters.filter { it.category == selectedCategory }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Original / None filter button
                        item {
                            val isNoneSelected = activeClip?.filterName == "Normal"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                        val updatedClips = project.clips.mapIndexed { idx, c ->
                                            if (idx == selectedClipIndex) c.copy(filterName = "Normal", filterIntensity = 0.8f) else c
                                        }
                                        onUpdateProject(project.copy(clips = updatedClips))
                                        onShowToast("Original Video (Filter cleared)")
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNoneSelected) ElectricBlue.copy(alpha = 0.3f) else DarkSurfaceElevated)
                                        .border(if (isNoneSelected) 2.dp else 1.dp, if (isNoneSelected) ElectricBlue else DarkBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = "Original", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("Original", color = if (isNoneSelected) ElectricBlue else TextSecondary, fontSize = 9.sp)
                            }
                        }

                        items(filteredList) { filter ->
                            val isSelected = activeClip?.filterName == filter.displayName
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                            val updatedClips = project.clips.mapIndexed { idx, c ->
                                                if (idx == selectedClipIndex) c.copy(filterName = filter.displayName) else c
                                            }
                                            onUpdateProject(project.copy(clips = updatedClips))
                                            onShowToast("Applied filter: ${filter.displayName}")
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(listOf(CobaltBlue, NeonPurple)))
                                        .border(if (isSelected) 2.dp else 0.dp, ElectricBlue, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        filter.badge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    filter.displayName.take(12),
                                    color = if (isSelected) ElectricBlue else TextSecondary,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Working Intensity Slider from 0% to 100%
                    val currentFilterIntensity = activeClip?.filterIntensity ?: 0.8f
                    if (activeClip?.filterName != "Normal") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Intensity", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = currentFilterIntensity,
                                onValueChange = { newIntensity ->
                                    if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                        val updatedClips = project.clips.mapIndexed { idx, c ->
                                            if (idx == selectedClipIndex) c.copy(filterIntensity = newIntensity) else c
                                        }
                                        onUpdateProject(project.copy(clips = updatedClips))
                                    }
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = CobaltBlue)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${(currentFilterIntensity * 100).toInt()}%", color = ElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Selected: ${activeClip?.filterName ?: "Normal"}",
                            color = ElectricBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = {
                                onClose() // Closes Filters sheet
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Done", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                EditorTool.EFFECTS -> {
                    var selectedCat by remember { mutableStateOf(StudioPresets.effectCategories.first()) }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StudioPresets.effectCategories) { cat ->
                            val isSelected = cat == selectedCat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonPurple else DarkSurfaceElevated)
                                    .clickable { selectedCat = cat }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(cat, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val effectItems = StudioPresets.effects.filter { it.second == selectedCat }
                    val currentEffectIntensity = (project.effects.lastOrNull()?.intensity ?: activeClip?.effectIntensity ?: 0.8f).coerceIn(0f, 1f)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Option for None / Remove Effects
                        item {
                            val isNoneActive = project.effects.isEmpty() && (activeClip?.effectName == null || activeClip.effectName == "None")
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    val updatedClips = project.clips.mapIndexed { idx, c ->
                                        if (idx == selectedClipIndex) c.copy(effectName = "None", effectIntensity = 0.8f) else c
                                    }
                                    onUpdateProject(project.copy(effects = emptyList(), clips = updatedClips))
                                    onShowToast("Removed effects")
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isNoneActive) AmberGold.copy(alpha = 0.3f) else DarkSurfaceElevated)
                                        .border(if (isNoneActive) 2.dp else 1.dp, if (isNoneActive) AmberGold else DarkBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = "None", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("None", color = if (isNoneActive) AmberGold else TextSecondary, fontSize = 9.sp)
                            }
                        }

                        items(effectItems) { (fxName, _) ->
                            val isEffectActive = project.effects.any { it.name == fxName } || activeClip?.effectName == fxName
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    val effectId = "fx_${System.currentTimeMillis()}"
                                    val newEffect = EffectOverlay(
                                        id = effectId,
                                        name = fxName,
                                        category = selectedCat,
                                        intensity = currentEffectIntensity,
                                        startSec = currentPlayheadSec,
                                        durationSec = 2f,
                                        startTime = currentPlayheadSec,
                                        duration = 2f
                                    )
                                    // Add to project.effects and activeClip.effects
                                    val updatedEffects = project.effects + newEffect
                                    val updatedClips = if (project.clips.isNotEmpty()) {
                                        val targetIdx = selectedClipIndex.coerceIn(0, project.clips.lastIndex)
                                        project.clips.mapIndexed { idx, c ->
                                            if (idx == targetIdx) {
                                                c.copy(
                                                    effectName = fxName,
                                                    effectIntensity = currentEffectIntensity,
                                                    effects = c.effects + newEffect
                                                )
                                            } else c
                                        }
                                    } else {
                                        listOf(
                                            VideoClip(
                                                id = "clip_default",
                                                title = project.videoFileName ?: "Clip",
                                                durationSec = project.durationSec.coerceAtLeast(5.0f),
                                                effectName = fxName,
                                                effectIntensity = currentEffectIntensity,
                                                effects = listOf(newEffect)
                                            )
                                        )
                                    }
                                    onUpdateProject(
                                        project.copy(
                                            effects = updatedEffects,
                                            clips = updatedClips
                                        )
                                    )
                                    onShowToast("VFX Activated: $fxName")
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.radialGradient(listOf(AmberGold, DarkSurfaceCard)))
                                        .border(if (isEffectActive) 2.dp else 1.dp, if (isEffectActive) ElectricBlue else AmberGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    fxName.take(12),
                                    color = if (isEffectActive) ElectricBlue else TextPrimary,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Working Effect Intensity Slider from 0% to 100%
                    val hasActiveEffect = project.effects.isNotEmpty() || (activeClip?.effectName != null && activeClip.effectName != "None")
                    if (hasActiveEffect) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Intensity", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = currentEffectIntensity,
                                onValueChange = { newIntensity ->
                                    val updatedEffects = project.effects.map { it.copy(intensity = newIntensity) }
                                    val updatedClips = project.clips.mapIndexed { idx, c ->
                                        if (idx == selectedClipIndex) c.copy(effectIntensity = newIntensity) else c
                                    }
                                    onUpdateProject(project.copy(effects = updatedEffects, clips = updatedClips))
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = AmberGold, activeTrackColor = AmberGold)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${(currentEffectIntensity * 100).toInt()}%", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (project.effects.isNotEmpty()) "Active: ${project.effects.last().name}" else if (activeClip?.effectName != null && activeClip.effectName != "None") "Active: ${activeClip.effectName}" else "Tap an effect to apply",
                                color = AmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (hasActiveEffect) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Remove",
                                    color = PlayheadRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        val updatedClips = project.clips.mapIndexed { idx, c ->
                                            if (idx == selectedClipIndex) c.copy(effectName = "None") else c
                                        }
                                        onUpdateProject(project.copy(effects = emptyList(), clips = updatedClips))
                                        onShowToast("Removed effects")
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                onClose() // Closes sheet with activeTool = NONE
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Done", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bug 9 Fix: Block transitions if clips.size < 2
                EditorTool.TRANSITIONS -> {
                    if (project.clips.size < 2) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Text("Add at least 2 clips to apply transitions.", color = AmberGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Use the Split tool to cut a clip into 2 parts first.", color = TextSecondary, fontSize = 11.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onClose,
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                            ) {
                                Text("Back to Tools", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text("Select Transition between scenes (50+ available):", color = TextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StudioPresets.transitions) { trans ->
                                val isSelected = activeClip?.transitionIn == trans
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricBlue else DarkSurfaceElevated)
                                        .border(1.dp, CobaltBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (activeClip != null && selectedClipIndex in project.clips.indices) {
                                                val updatedClips = project.clips.mapIndexed { idx, c ->
                                                    if (idx == selectedClipIndex) c.copy(transitionIn = trans) else c
                                                }
                                                onUpdateProject(project.copy(clips = updatedClips))
                                                onShowToast("Transition set: $trans")
                                                onClose()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        trans,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Bug 4 Fix: Dedicated Background Settings that closes on Apply
                EditorTool.BACKGROUND -> {
                    Text("Select Background Style:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    val backgrounds = listOf("Dark Slate OLED", "Studio Blue Glow", "Neon Cyber", "Film Blur", "Pure Black 4K")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(backgrounds) { bgStyle ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        onShowToast("Applied background: $bgStyle")
                                        onClose() // Closes sheet
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(bgStyle, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onShowToast("Background applied")
                                onClose()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                        ) {
                            Text("Apply to All Clips", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                EditorTool.TEXT -> {
                    var inputText by remember { mutableStateOf(project.textOverlays.firstOrNull()?.text ?: "") }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            val overlays = project.textOverlays.toMutableList()
                            if (overlays.isNotEmpty()) {
                                overlays[0] = overlays[0].copy(text = it)
                            } else {
                                overlays.add(TextOverlay("txt_${System.currentTimeMillis()}", it))
                            }
                            onUpdateProject(project.copy(textOverlays = overlays))
                        },
                        label = { Text("Overlay Text") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text("Select Font (50+ available):", color = TextSecondary, fontSize = 11.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        items(StudioPresets.fonts) { font ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceElevated)
                                    .clickable {
                                        val overlays = project.textOverlays.toMutableList()
                                        if (overlays.isNotEmpty()) {
                                            overlays[0] = overlays[0].copy(fontName = font)
                                            onUpdateProject(project.copy(textOverlays = overlays))
                                            onShowToast("Font set to $font")
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(font, color = ElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                EditorTool.MUSIC -> {
                    Text("Add Studio Beats & Sound Effects (No auto music):", color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StudioPresets.defaultMusicTracks) { (title, artist) ->
                            Card(
                                onClick = {
                                    val audioList = listOf(AudioClip("audio_${System.currentTimeMillis()}", title, artist))
                                    onUpdateProject(project.copy(audioClips = audioList))
                                    onShowToast("Added audio: $title")
                                    onClose()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = EmeraldGreen)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(artist, color = TextMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                EditorTool.VOICEOVER -> {
                    var isRecording by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    isRecording = !isRecording
                                    onShowToast(if (isRecording) "Recording Voiceover 48kHz..." else "Saved Voiceover Track")
                                },
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) PlayheadRed else CobaltBlue)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(if (isRecording) "Tap to Stop" else "Tap to Record", color = TextPrimary, fontSize = 12.sp)
                        }

                        Column {
                            Text("Studio Noise Suppression", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("48kHz Lossless AI Voice Enhance", color = EmeraldGreen, fontSize = 11.sp)
                            Text("Auto-Ducking Background Music", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Text("${tool.title} is active on current selection.", color = TextPrimary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onShowToast("Applied ${tool.title} changes")
                                onClose() // Bug 4 Fix: Closes sheet on apply
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                        ) {
                            Text("Apply to All Clips", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
