package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Breathing Electric Blue & Cyan Glow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Smooth 2-second zoom effect
    var scaleTarget by remember { mutableFloatStateOf(0.75f) }
    val animatedScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
        label = "logo_zoom"
    )

    LaunchedEffect(Unit) {
        scaleTarget = 1.08f
        delay(2000) // 2 seconds exactly
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Pure black background
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layered Electric Blue & Cyan Radial Glow Aura
        Box(
            modifier = Modifier
                .size(380.dp)
                .scale(animatedScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.55f),
                            Color(0xFF2979FF).copy(alpha = glowAlpha * 0.35f),
                            Color(0xFF00103A).copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // WSS 3D Logo container in Center with Blue Glow & Squircle Border
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(animatedScale)
                    .shadow(
                        elevation = 36.dp,
                        shape = RoundedCornerShape(38.dp),
                        spotColor = Color(0xFF00E5FF).copy(alpha = glowAlpha),
                        ambientColor = Color(0xFF2979FF)
                    )
                    .clip(RoundedCornerShape(38.dp))
                    .background(Color(0xFF020408))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = glowAlpha),
                                Color(0xFF2979FF),
                                Color(0xFF00B0FF)
                            )
                        ),
                        shape = RoundedCornerShape(38.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_wss_logo),
                    contentDescription = "WSS 3D Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "WS STUDIO",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF00E5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "4K PRO",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ULTRA HD VIDEO ENGINE",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }

        // Bottom status tag
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "CapCut Clone • 100% Free • No Watermark",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
