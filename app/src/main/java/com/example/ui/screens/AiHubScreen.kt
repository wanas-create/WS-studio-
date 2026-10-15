package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.AiServiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun AiHubScreen(
    onBackWithAd: () -> Unit
) {
    BackHandler { onBackWithAd() }

    var selectedEngineId by remember { mutableStateOf("gemini") }
    var selectedCategoryFilter by remember { mutableStateOf(AiCategory.ALL) }
    var messages by remember { mutableStateOf(AiCatalog.getInitialMessages()) }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var currentJob by remember { mutableStateOf<Job?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    val activeEngine = remember(selectedEngineId) {
        AiCatalog.engines.find { it.id == selectedEngineId } ?: AiCatalog.engines.first()
    }

    val visibleEngines = remember(selectedCategoryFilter) {
        if (selectedCategoryFilter == AiCategory.ALL) {
            AiCatalog.engines
        } else {
            AiCatalog.engines.filter { it.category == selectedCategoryFilter }
        }
    }

    // Auto-scroll on new messages or when generation starts
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun cancelGeneration() {
        currentJob?.cancel()
        currentJob = null
        if (isGenerating) {
            isGenerating = false
            messages = messages + ChatMessage(
                id = "cancelled_${System.currentTimeMillis()}",
                aiEngineId = activeEngine.id,
                aiName = activeEngine.name,
                sender = "ai",
                text = "⏹️ Generation stopped by user.",
                isError = false
            )
        }
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || isGenerating) return

        val userMsg = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            aiEngineId = activeEngine.id,
            aiName = activeEngine.name,
            sender = "user",
            text = trimmed
        )
        messages = messages + userMsg
        inputText = ""
        isGenerating = true

        val job = coroutineScope.launch {
            try {
                val aiReply = AiCatalog.queryEngine(activeEngine, trimmed, messages)
                messages = messages + aiReply
            } catch (e: Exception) {
                messages = messages + ChatMessage(
                    id = "err_${System.currentTimeMillis()}",
                    aiEngineId = activeEngine.id,
                    aiName = activeEngine.name,
                    sender = "ai",
                    text = "⚠️ Request failed: ${e.localizedMessage ?: "Unknown error"}",
                    isError = true,
                    errorType = "EXCEPTION"
                )
            } finally {
                isGenerating = false
                currentJob = null
            }
        }
        currentJob = job
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AiMainBackground)
            .testTag("ai_hub_screen"),
        containerColor = AiMainBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AiCardSurface)
                    .statusBarsPadding()
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBackWithAd,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ai_hub_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AiTextWhite
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeEngine.name,
                                    color = AiTextWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))

                                val isActive = AiServiceManager.isEngineActive(activeEngine.id)
                                val badgeBg = if (isActive) AiPrimaryBlue else Color(activeEngine.badgeColorHex)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "● LIVE" else activeEngine.tag,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "50 AI Studio Unified Hub • Real-time AI",
                                color = AiTextLightGrey,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Clear chat button
                    IconButton(
                        onClick = {
                            cancelGeneration()
                            messages = emptyList()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ai_clear_chat_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = AiTextLightGrey
                        )
                    }
                }

                HorizontalDivider(color = AiBorderSubtle, thickness = 1.dp)

                // Category Quick Filter Tabs (All 50, Text, Image, Video, Audio)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AiCategory.values()) { cat ->
                        val isSelected = cat == selectedCategoryFilter
                        Surface(
                            onClick = { selectedCategoryFilter = cat },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) AiPrimaryBlue else AiCardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AiPrimaryBlue else AiBorderSubtle
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cat.icon} ${cat.label}",
                                    color = if (isSelected) AiTextWhite else AiTextLightGrey,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 50 AI Horizontal Chips Carousel
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleEngines, key = { it.id }) { engine ->
                        val isSelected = engine.id == selectedEngineId
                        val isConfigured = AiServiceManager.isEngineActive(engine.id)

                        Surface(
                            onClick = { selectedEngineId = engine.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AiPrimaryBlue.copy(alpha = 0.22f) else AiCardSurface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) AiPrimaryBlue else AiBorderSubtle
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isConfigured) AiPrimaryBlue
                                            else Color(engine.badgeColorHex)
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = engine.name,
                                    color = if (isSelected) AiTextWhite else AiTextLightGrey,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = AiBorderSubtle, thickness = 1.dp)
            }
        },
        bottomBar = {
            // Message Input Dock with proper soft keyboard & navigation insets
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = AiCardSurface,
                border = BorderStroke(1.dp, AiBorderSubtle),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Suggested prompt pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { sendMessage(activeEngine.samplePrompt) },
                            shape = RoundedCornerShape(14.dp),
                            color = AiCardSurfaceElevated,
                            border = BorderStroke(0.8.dp, AiPrimaryBlue.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoFixNormal,
                                    contentDescription = null,
                                    tint = AiPrimaryBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Prompt: ${activeEngine.samplePrompt}",
                                    color = AiPrimaryBlue,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Input Text Field & Send/Stop Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Ask ${activeEngine.name}...",
                                    color = AiTextDarkGrey,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input"),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && !isGenerating) {
                                        sendMessage(inputText)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AiCardSurfaceElevated,
                                unfocusedContainerColor = AiCardSurfaceElevated,
                                focusedBorderColor = AiPrimaryBlue,
                                unfocusedBorderColor = AiBorderSubtle,
                                focusedTextColor = AiTextWhite,
                                unfocusedTextColor = AiTextWhite,
                                cursorColor = AiPrimaryBlue
                            ),
                            maxLines = 3
                        )

                        Spacer(Modifier.width(8.dp))

                        if (isGenerating) {
                            // Working Stop/Cancel Button
                            IconButton(
                                onClick = { cancelGeneration() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AiCardSurfaceElevated)
                                    .border(1.dp, AiBorderSubtle, CircleShape)
                                    .testTag("ai_stop_button")
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color(0xFFFF4B6E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Primary Send Button
                            val canSend = inputText.isNotBlank()
                            IconButton(
                                onClick = { sendMessage(inputText) },
                                enabled = canSend,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) AiPrimaryBlue else AiCardSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (canSend) AiPrimaryBlue else AiBorderSubtle,
                                        CircleShape
                                    )
                                    .testTag("ai_send_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (canSend) Color.White else AiTextDarkGrey
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Engine Info Banner at top of chat
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AiCardSurface),
                    border = BorderStroke(1.dp, AiBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AiPrimaryBlue.copy(alpha = 0.2f))
                                .border(1.dp, AiPrimaryBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeEngine.name.take(1),
                                color = AiPrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeEngine.name,
                                    color = AiTextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "(${activeEngine.tag})",
                                    color = AiTextLightGrey,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = activeEngine.description,
                                color = AiTextLightGrey,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AiPrimaryBlue.copy(alpha = 0.15f))
                                .border(1.dp, AiPrimaryBlue.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = AiPrimaryBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Start chatting with ${activeEngine.name}",
                            color = AiTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Select any of the 50 AI models above or type a prompt below. Google Gemini is actively connected for live video scripts and creative direction.",
                            color = AiTextLightGrey,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onCopy = { clipboardManager.setText(AnnotatedString(message.text)) }
                )
            }

            if (isGenerating) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AiCardSurface),
                        border = BorderStroke(1.dp, AiPrimaryBlue.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AiPrimaryBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${activeEngine.name} is processing request...",
                                color = AiTextLightGrey,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { cancelGeneration() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Cancel", color = Color(0xFFFF4B6E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCopy: () -> Unit
) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Sender badge
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                ) {
                    Text(
                        text = message.aiName,
                        color = AiPrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = when {
                    isUser -> AiUserBubbleBlue
                    message.isError -> AiErrorCard
                    else -> AiCardSurface
                },
                border = if (!isUser) {
                    BorderStroke(
                        1.dp,
                        if (message.isError) AiErrorBorder else AiBorderSubtle
                    )
                } else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Text body
                    Text(
                        text = message.text,
                        color = AiTextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Specialized Media Payload Cards based on AI type:
                    when (message.mediaType) {
                        "image" -> {
                            Spacer(Modifier.height(10.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                border = BorderStroke(1.dp, AiBorderSubtle)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AiCardSurfaceElevated)
                                        .padding(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            tint = AiPrimaryBlue,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = message.mediaTitle,
                                            color = AiTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = message.mediaSubtitle,
                                            color = AiTextLightGrey,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        "video" -> {
                            Spacer(Modifier.height(10.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                border = BorderStroke(1.dp, AiBorderSubtle)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AiCardSurfaceElevated)
                                        .padding(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.SlowMotionVideo,
                                            contentDescription = null,
                                            tint = AmberGold,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = message.mediaTitle,
                                            color = AiTextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${message.mediaDuration} • ${message.mediaSubtitle}",
                                            color = AiTextLightGrey,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        "audio" -> {
                            Spacer(Modifier.height(10.dp))
                            var isPlayingAudio by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AiCardSurfaceElevated,
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { isPlayingAudio = !isPlayingAudio },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen)
                                    ) {
                                        Icon(
                                            if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play Audio",
                                            tint = Color.Black
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = message.mediaTitle,
                                            color = AiTextWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = message.mediaSubtitle,
                                            color = AiTextLightGrey,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Text(
                                        text = message.mediaDuration,
                                        color = EmeraldGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Copy action button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = if (isUser) Color.White.copy(alpha = 0.7f) else AiTextLightGrey,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
