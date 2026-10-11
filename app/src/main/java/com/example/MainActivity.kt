package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.*
import com.example.model.VideoAspect
import com.example.model.VideoClip
import com.example.model.VideoProject
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class AppNavScreen {
    SPLASH,
    MAIN,
    EDITOR,
    AI_HUB,
    EXPORT
}

enum class MainBottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    TEMPLATES("Templates", Icons.Default.DashboardCustomize),
    ME("Me", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WSStudioApp()
            }
        }
    }
}

@Composable
fun WSStudioApp() {
    var currentScreen by remember { mutableStateOf(AppNavScreen.SPLASH) }
    var currentBottomTab by remember { mutableStateOf(MainBottomTab.HOME) }

    // Projects list starts completely empty - NO pre-existing video, NO demo video anywhere
    var projects by remember { mutableStateOf<List<VideoProject>>(emptyList()) }

    var activeEditingProject by remember { mutableStateOf<VideoProject?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Ads states
    var showAppOpenAd by remember { mutableStateOf(false) }
    var showInterstitialAd by remember { mutableStateOf(false) }
    var interstitialTitle by remember { mutableStateOf("Sponsored Preview") }
    var onInterstitialDismissCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showRewardedAd by remember { mutableStateOf(false) }
    var onRewardedSuccessCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Screen Router
    when (currentScreen) {
        AppNavScreen.SPLASH -> {
            SplashScreen(
                onSplashFinished = {
                    showAppOpenAd = true
                    currentScreen = AppNavScreen.MAIN
                }
            )
        }

        AppNavScreen.MAIN -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = DarkBackground,
                bottomBar = {
                    Column {
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.height(64.dp)
                        ) {
                            MainBottomTab.values().forEach { tab ->
                                val selected = tab == currentBottomTab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentBottomTab = tab },
                                    icon = {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.label,
                                            tint = if (selected) ElectricBlue else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            tab.label,
                                            color = if (selected) ElectricBlue else TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = CobaltBlue.copy(alpha = 0.25f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentBottomTab) {
                        MainBottomTab.HOME -> {
                            HomeScreen(
                                projects = projects,
                                onNewProjectWithVideo = { uri, aspect ->
                                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Gallery Video"
                                    val newProject = VideoProject(
                                        id = "proj_${System.currentTimeMillis()}",
                                        title = fileName,
                                        aspect = aspect,
                                        durationSec = 8.0f,
                                        videoUri = uri.toString(),
                                        videoFileName = fileName,
                                        clips = listOf(
                                            VideoClip(
                                                id = "clip_1",
                                                title = fileName,
                                                durationSec = 8.0f,
                                                videoUri = uri.toString(),
                                                colorToneHex = 0xFF0D47A1
                                            )
                                        ),
                                        audioClips = emptyList() // No auto music
                                    )
                                    projects = listOf(newProject) + projects
                                    activeEditingProject = newProject
                                    currentScreen = AppNavScreen.EDITOR
                                },
                                onOpenProject = { project ->
                                    activeEditingProject = project
                                    currentScreen = AppNavScreen.EDITOR
                                },
                                onOpenAiHub = {
                                    currentScreen = AppNavScreen.AI_HUB
                                },
                                onOpenTemplates = {
                                    currentBottomTab = MainBottomTab.TEMPLATES
                                },
                                onOpenSettings = {
                                    showSettingsDialog = true
                                },
                                onDeleteProject = { projectId ->
                                    projects = projects.filter { it.id != projectId }
                                }
                            )
                        }

                        MainBottomTab.TEMPLATES -> {
                            TemplatesScreen(
                                onNavigateToImport = {
                                    currentBottomTab = MainBottomTab.HOME
                                }
                            )
                        }

                        MainBottomTab.ME -> {
                            MeScreen(
                                projectsCount = projects.size,
                                onOpenSettings = { showSettingsDialog = true }
                            )
                        }
                    }
                }
            }
        }

        AppNavScreen.EDITOR -> {
            activeEditingProject?.let { editingProj ->
                EditorScreen(
                    initialProject = editingProj,
                    onBack = {
                        currentScreen = AppNavScreen.MAIN
                    },
                    onNavigateToExport = { updatedProject ->
                        projects = projects.map { if (it.id == updatedProject.id) updatedProject else it }
                        activeEditingProject = updatedProject
                        currentScreen = AppNavScreen.EXPORT
                    }
                )
            } ?: run {
                // If user didn't pick video yet, open editor in gallery picker state
                val emptyProject = VideoProject(
                    id = "proj_${System.currentTimeMillis()}",
                    title = "New Project",
                    clips = emptyList(),
                    audioClips = emptyList()
                )
                EditorScreen(
                    initialProject = emptyProject,
                    onBack = { currentScreen = AppNavScreen.MAIN },
                    onNavigateToExport = { updatedProject ->
                        projects = listOf(updatedProject) + projects
                        activeEditingProject = updatedProject
                        currentScreen = AppNavScreen.EXPORT
                    }
                )
            }
        }

        AppNavScreen.AI_HUB -> {
            AiHubScreen(
                onBackWithAd = {
                    interstitialTitle = "AI Hub Studio Session Complete"
                    onInterstitialDismissCallback = {
                        currentScreen = AppNavScreen.MAIN
                    }
                    showInterstitialAd = true
                }
            )
        }

        AppNavScreen.EXPORT -> {
            activeEditingProject?.let { exportProj ->
                ExportScreen(
                    project = exportProj,
                    onBack = {
                        currentScreen = AppNavScreen.EDITOR
                    },
                    onRequest4kRewardedAd = { onSuccess ->
                        onRewardedSuccessCallback = onSuccess
                        showRewardedAd = true
                    },
                    onFinishExport = {
                        interstitialTitle = "4K Export Lossless Finalized"
                        onInterstitialDismissCallback = {
                            currentScreen = AppNavScreen.MAIN
                        }
                        showInterstitialAd = true
                    }
                )
            } ?: run {
                currentScreen = AppNavScreen.MAIN
            }
        }
    }

    // AppOpen Ad Modal on start
    if (showAppOpenAd) {
        AppOpenAdDialog(
            onDismiss = { showAppOpenAd = false }
        )
    }

    // Interstitial Ad Modal (after export + after AI Hub)
    if (showInterstitialAd) {
        InterstitialAdDialog(
            title = interstitialTitle,
            onDismiss = {
                showInterstitialAd = false
                onInterstitialDismissCallback?.invoke()
                onInterstitialDismissCallback = null
            }
        )
    }

    // Rewarded Ad Modal for 4K export
    if (showRewardedAd) {
        RewardedAdDialog(
            rewardDescription = "Ultra 4K 60FPS Lossless Master Export",
            onRewardGranted = {
                onRewardedSuccessCallback?.invoke()
            },
            onDismiss = {
                showRewardedAd = false
                onRewardedSuccessCallback = null
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}
