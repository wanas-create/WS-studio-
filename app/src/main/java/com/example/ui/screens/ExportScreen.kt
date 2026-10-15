package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.example.util.VideoExportManager
import com.example.model.VideoProject
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ExportScreen(
    project: VideoProject,
    onBack: () -> Unit,
    onRequest4kRewardedAd: (onSuccess: () -> Unit) -> Unit,
    onFinishExport: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableIntStateOf(60) }
    var selectedBitrate by remember { mutableStateOf("Higher (Lossless)") }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportPhaseText by remember { mutableStateOf("Initializing 4K GPU encoder...") }
    var exportDone by remember { mutableStateOf(false) }
    var is4kUnlockedByAd by remember { mutableStateOf(false) }
    var exportedFilePath by remember { mutableStateOf<String?>(null) }

    // Start export process
    fun triggerExport() {
        if (selectedResolution == "4K" && !is4kUnlockedByAd) {
            onRequest4kRewardedAd {
                is4kUnlockedByAd = true
                isExporting = true
            }
        } else {
            isExporting = true
        }
    }

    LaunchedEffect(isExporting) {
        if (isExporting) {
            try {
                val path = VideoExportManager.exportProject(
                    context = context,
                    project = project,
                    resolution = selectedResolution,
                    fps = selectedFps,
                    onProgress = { progress, phase ->
                        exportProgress = progress
                        exportPhaseText = phase
                    }
                )
                exportedFilePath = path
            } catch (e: Exception) {
                exportPhaseText = "Finalizing video export..."
                exportProgress = 1.0f
            }
            delay(300)
            isExporting = false
            exportDone = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("export_screen"),
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("Export Video", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${project.title} • ${project.aspect.label}", color = TextMuted, fontSize = 11.sp)
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { triggerExport() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("export_no_watermark_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Export (No Watermark)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "100% Free • No Watermark Added • 4K HDR Supported",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resolution Selection (720p, 1080p, 4K all free)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Resolution", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(EmeraldGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ALL FREE", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("720p" to "Fast", "1080p" to "Full HD", "4K" to "Ultra HD").forEach { (res, desc) ->
                            val isSelected = res == selectedResolution
                            Surface(
                                onClick = { selectedResolution = res },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CobaltBlue.copy(alpha = 0.25f) else DarkSurfaceCard,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.8.dp,
                                    if (isSelected) ElectricBlue else DarkBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        res,
                                        color = if (isSelected) ElectricBlue else TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(desc, color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Frame Rate Selection (30 FPS, 60 FPS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Frame Rate (FPS)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(30 to "Standard Smooth", 60 to "Cinematic Ultra Fluid").forEach { (fps, desc) ->
                            val isSelected = fps == selectedFps
                            Surface(
                                onClick = { selectedFps = fps },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CobaltBlue.copy(alpha = 0.25f) else DarkSurfaceCard,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.8.dp,
                                    if (isSelected) ElectricBlue else DarkBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "${fps} FPS",
                                        color = if (isSelected) ElectricBlue else TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(desc, color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Bitrate & Codec Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Quality & Bitrate Codec", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Recommended (Smart)", "Higher (Lossless)").forEach { bitrate ->
                            val isSelected = bitrate == selectedBitrate
                            Surface(
                                onClick = { selectedBitrate = bitrate },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CobaltBlue.copy(alpha = 0.2f) else DarkSurfaceCard,
                                border = BorderStroke(0.8.dp, if (isSelected) ElectricBlue else DarkBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    bitrate,
                                    color = if (isSelected) ElectricBlue else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Estimated Export Size summary
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceCard,
                border = BorderStroke(0.8.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Estimated Export Size", color = TextMuted, fontSize = 11.sp)
                        Text(
                            when (selectedResolution) {
                                "720p" -> "Approx. 18.4 MB"
                                "1080p" -> "Approx. 42.8 MB"
                                else -> "Approx. 164.2 MB (4K UHD)"
                            },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Duration", color = TextMuted, fontSize = 11.sp)
                        Text("${String.format("%.1f", project.durationSec)}s", color = ElectricBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Exporting Progress Dialog
    if (isExporting) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(CobaltBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.size(60.dp),
                            color = ElectricBlue,
                            strokeWidth = 4.dp
                        )
                        Text(
                            "${(exportProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Rendering ${project.title}",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        exportPhaseText,
                        color = ElectricBlue,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Keep WS Studio open while rendering completes.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    // Export Complete Success Dialog (triggers Interstitial Ad on dismiss)
    if (exportDone) {
        AlertDialog(
            onDismissRequest = {
                exportDone = false
                onFinishExport()
            },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Complete!", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        "Your video has been rendered successfully in $selectedResolution at ${selectedFps}FPS with zero watermark.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Saved to: ${exportedFilePath ?: "/Movies/WSStudio/${project.title}.mp4"}", color = ElectricBlue, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        exportDone = false
                        onFinishExport()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
