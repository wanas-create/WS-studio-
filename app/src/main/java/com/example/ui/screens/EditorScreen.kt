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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
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

    var project by remember { mutableStateOf(initialProject) }
    var activeTool by remember { mutableStateOf(EditorTool.NONE) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayheadSec by remember { mutableFloatStateOf(0.0f) }
    var selectedClipIndex by remember { mutableIntStateOf(0) }
    var timelineZoom by remember { mutableFloatStateOf(1.0f) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Gallery Video"
            val newClip = VideoClip(
                id = "clip_${System.currentTimeMillis()}",
                title = fileName,
                durationSec = 8.0f,
                videoUri = uri.toString(),
                colorToneHex = 0xFF0D47A1
            )
            project = project.copy(
                videoUri = uri.toString(),
                videoFileName = fileName,
                durationSec = 8.0f,
                clips = listOf(newClip)
            )
            currentPlayheadSec = 0f
            showToastMessage = "Video loaded from gallery"
        }
    }

    // Live Playback Engine loop
    LaunchedEffect(isPlaying, project.durationSec) {
        while (isPlaying && project.durationSec > 0) {
            delay(50)
            val activeClip = project.clips.getOrNull(selectedClipIndex)
            val speed = activeClip?.speed ?: 1.0f
            currentPlayheadSec += 0.05f * speed
            if (currentPlayheadSec >= project.durationSec) {
                currentPlayheadSec = 0f
                isPlaying = false
            }
        }
    }

    // Toast auto-clear
    LaunchedEffect(showToastMessage) {
        if (showToastMessage != null) {
            delay(2000)
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
            // Full Editor View with multi-track timeline & video canvas
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. HD Video Preview Stage (Shows ONLY user selected video)
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
                        selectedClipIndex = selectedClipIndex
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

                // 2. Playback Control Bar
                PlaybackControlsBar(
                    isPlaying = isPlaying,
                    currentPlayheadSec = currentPlayheadSec,
                    totalDurationSec = project.durationSec,
                    onTogglePlay = { isPlaying = !isPlaying },
                    onSeek = { currentPlayheadSec = it.coerceIn(0f, project.durationSec) },
                    onZoomIn = { timelineZoom = (timelineZoom * 1.25f).coerceAtMost(3.0f) },
                    onZoomOut = { timelineZoom = (timelineZoom * 0.8f).coerceAtLeast(0.6f) }
                )

                // 3. Multi-Track Timeline Stage
                MultiTrackTimeline(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.36f),
                    project = project,
                    currentPlayheadSec = currentPlayheadSec,
                    selectedClipIndex = selectedClipIndex,
                    timelineZoom = timelineZoom,
                    onSelectClip = { selectedClipIndex = it },
                    onSeek = { currentPlayheadSec = it }
                )

                // 4. CapCut Bottom Toolbar / Tool Sub-sheet
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
                                        val clips = project.clips.toMutableList()
                                        if (selectedClipIndex in clips.indices) {
                                            val clip = clips[selectedClipIndex]
                                            val cutPoint = (currentPlayheadSec - clip.startOffsetSec).coerceIn(0.5f, clip.durationSec - 0.5f)
                                            val firstHalf = clip.copy(durationSec = cutPoint)
                                            val secondHalf = clip.copy(
                                                id = "clip_split_${System.currentTimeMillis()}",
                                                title = "${clip.title} (Part 2)",
                                                durationSec = clip.durationSec - cutPoint,
                                                startOffsetSec = clip.startOffsetSec + cutPoint
                                            )
                                            clips[selectedClipIndex] = firstHalf
                                            clips.add(selectedClipIndex + 1, secondHalf)
                                            project = project.copy(clips = clips)
                                            showToastMessage = "Split clip at ${String.format("%.1f", currentPlayheadSec)}s"
                                        }
                                    }
                                    EditorTool.REVERSE -> {
                                        val clips = project.clips.toMutableList()
                                        if (selectedClipIndex in clips.indices) {
                                            val clip = clips[selectedClipIndex]
                                            clips[selectedClipIndex] = clip.copy(isReversed = !clip.isReversed)
                                            project = project.copy(clips = clips)
                                            showToastMessage = if (!clip.isReversed) "Reversing clip playback..." else "Restored original playback"
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
                            onClose = { activeTool = EditorTool.NONE },
                            onUpdateProject = { updatedProject ->
                                project = updatedProject
                            },
                            onShowToast = { showToastMessage = it }
                        )
                    }
                }
            }
        }
    }
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

@Composable
fun VideoPreviewCanvas(
    project: VideoProject,
    currentPlayheadSec: Float,
    isPlaying: Boolean,
    selectedClipIndex: Int
) {
    val activeClip = project.clips.getOrNull(selectedClipIndex) ?: project.clips.firstOrNull()
    val targetAspect = project.aspect.ratioWidth / project.aspect.ratioHeight

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(targetAspect, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        val baseColor = Color(activeClip?.colorToneHex ?: 0xFF0D47A1)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            baseColor,
                            baseColor.copy(alpha = 0.6f),
                            Color(0xFF04060A)
                        )
                    )
                )
        )

        // Live visual filter overlay simulation
        val filterName = activeClip?.filterName ?: "Normal"
        if (filterName != "Normal") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            filterName.contains("Teal") -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                            filterName.contains("Cyber") -> Color(0xFF7C4DFF).copy(alpha = 0.25f)
                            filterName.contains("Vintage") -> Color(0xFFFFB300).copy(alpha = 0.2f)
                            filterName.contains("B&W") -> Color.Gray.copy(alpha = 0.35f)
                            else -> Color.Transparent
                        }
                    )
            )
        }

        // Clip Title & Simulated Motion Elements for User Selected Video
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
            Text(
                "Filter: $filterName • Speed: ${activeClip?.speed ?: 1.0}x",
                color = ElectricBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            if (activeClip?.isReversed == true) {
                Text("◀ REVERSED PLAYBACK", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

fun formatTime(sec: Float): String {
    val totalSec = sec.toInt()
    val minutes = totalSec / 60
    val remainingSec = totalSec % 60
    val milli = ((sec - totalSec) * 10).toInt()
    return String.format("%02d:%02d.%d", minutes, remainingSec, milli)
}

@Composable
fun MultiTrackTimeline(
    modifier: Modifier = Modifier,
    project: VideoProject,
    currentPlayheadSec: Float,
    selectedClipIndex: Int,
    timelineZoom: Float,
    onSelectClip: (Int) -> Unit,
    onSeek: (Float) -> Unit
) {
    val scrollState = rememberScrollState()

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
                    .width((project.durationSec * 40 * timelineZoom).coerceAtLeast(300f).dp)
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
                    val ticks = ((project.durationSec / step).toInt() + 1).coerceAtLeast(4)
                    for (i in 0 until ticks) {
                        Box(
                            modifier = Modifier
                                .width((step * 40 * timelineZoom).dp)
                                .fillMaxHeight()
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

                // Track 1: Video Clips Track (User selected video)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    project.clips.forEachIndexed { index, clip ->
                        val isSelected = index == selectedClipIndex
                        Box(
                            modifier = Modifier
                                .width((clip.durationSec * 40 * timelineZoom).dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(clip.colorToneHex))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectClip(index) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column {
                                Text(
                                    clip.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    "${String.format("%.1f", clip.durationSec)}s • ${clip.speed}x",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Track 2: Audio & Beats Track (Empty until user adds music - NO auto music)
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

                // Track 4: Effects Track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TrackEffect.copy(alpha = 0.35f))
                        .border(0.8.dp, TrackEffect, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (project.effects.isEmpty()) "No effects added (Tap Effects tool below)" else project.effects.first().name,
                        color = if (project.effects.isEmpty()) TextMuted else Color.White,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.width(200.dp))
        }

        // Fixed Red Split Playhead Line in center
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

@Composable
fun BottomToolbarRow(
    onSelectTool: (EditorTool) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = DarkSurface,
        tonalElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(EditorTool.values().filter { it != EditorTool.NONE }) { tool ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectTool(tool) }
                        .padding(horizontal = 6.dp)
                        .testTag("tool_${tool.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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

@Composable
fun ToolDetailSubSheet(
    tool: EditorTool,
    project: VideoProject,
    selectedClipIndex: Int,
    onClose: () -> Unit,
    onUpdateProject: (VideoProject) -> Unit,
    onShowToast: (String) -> Unit
) {
    val clips = project.clips.toMutableList()
    val activeClip = clips.getOrNull(selectedClipIndex) ?: clips.firstOrNull()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
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
                EditorTool.SPEED -> {
                    var speedValue by remember { mutableFloatStateOf(activeClip?.speed ?: 1.0f) }
                    var smoothSlowMo by remember { mutableStateOf(true) }

                    Text("Clip Speed Curve: ${String.format("%.2f", speedValue)}x", color = ElectricBlue, fontSize = 12.sp)
                    Slider(
                        value = speedValue,
                        onValueChange = {
                            speedValue = it
                            if (activeClip != null && selectedClipIndex in clips.indices) {
                                clips[selectedClipIndex] = activeClip.copy(speed = it)
                                onUpdateProject(project.copy(clips = clips))
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
                        Text("Smooth Optical Slow-Mo", color = TextSecondary, fontSize = 12.sp)
                        Switch(
                            checked = smoothSlowMo,
                            onCheckedChange = {
                                smoothSlowMo = it
                                onShowToast(if (it) "Smooth Optical Flow 120FPS Enabled" else "Standard Frame Drop")
                            }
                        )
                    }
                }

                EditorTool.FILTERS -> {
                    var selectedCategory by remember { mutableStateOf(StudioPresets.filterCategories.first()) }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StudioPresets.filterCategories) { cat ->
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

                    val filteredList = StudioPresets.filters.filter { it.second == selectedCategory }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredList) { (filterName, _) ->
                            val isSelected = activeClip?.filterName == filterName
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (activeClip != null && selectedClipIndex in clips.indices) {
                                            clips[selectedClipIndex] = activeClip.copy(filterName = filterName)
                                            onUpdateProject(project.copy(clips = clips))
                                            onShowToast("Applied filter: $filterName")
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
                                    Text(filterName.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(filterName.take(8), color = if (isSelected) ElectricBlue else TextSecondary, fontSize = 10.sp)
                            }
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
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(effectItems) { (fxName, _) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    if (activeClip != null && selectedClipIndex in clips.indices) {
                                        clips[selectedClipIndex] = activeClip.copy(effectName = fxName)
                                        onUpdateProject(project.copy(clips = clips))
                                        onShowToast("VFX Activated: $fxName")
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.radialGradient(listOf(AmberGold, DarkSurfaceCard)))
                                        .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Text(fxName.take(8), color = TextPrimary, fontSize = 10.sp)
                            }
                        }
                    }
                }

                EditorTool.TRANSITIONS -> {
                    Text("Select Transition between scenes (50+ available):", color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StudioPresets.transitions) { trans ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, CobaltBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (activeClip != null && selectedClipIndex in clips.indices) {
                                            clips[selectedClipIndex] = activeClip.copy(transitionIn = trans)
                                            onUpdateProject(project.copy(clips = clips))
                                            onShowToast("Transition set: $trans")
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(trans, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
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
                            onClick = { onShowToast("Applied ${tool.title} changes") },
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
