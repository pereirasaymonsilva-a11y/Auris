// presentation/screens/SplashScreen.kt
package com.goldensystem.auris.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldensystem.auris.R
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreen(
    onFinish: () -> Unit
) {
    /* =======================
       ANIMATIONS (STABLE)
    ======================= */

    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(-8f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(20f) }
    val glow = remember { Animatable(0f) }

    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(500))
        scale.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        rotation.animateTo(0f, spring())

        glow.animateTo(1f, tween(400))
        glow.animateTo(0f, tween(300))

        textAlpha.animateTo(1f, tween(400))
        textOffset.animateTo(0f, tween(400))

        delay(700)

        scale.animateTo(0.92f, tween(250))
        scale.animateTo(1f, spring())

        delay(300)

        alpha.animateTo(0f, tween(400))

        finished = true
    }

    LaunchedEffect(finished) {
        if (finished) onFinish()
    }

    /* =======================
       BACKGROUND
    ======================= */

    val background = Brush.verticalGradient(
        listOf(
            Color(0xFF0B0B12),
            Color(0xFF10101A),
            Color(0xFF0B0B12)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {

        /* =======================
           PARTICLES (SAFE LAYER)
        ======================= */

        FloatingParticles()

        /* =======================
           CENTER LOGO (LOCKED)
        ======================= */

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                    }
            ) {

                // glow base
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF6C5CE7).copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Image(
                    painter = painterResource(R.drawable.ic_auris_logo_transparent),
                    contentDescription = "Auris Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.Center)
                )
            }
        }

        /* =======================
           TEXT (SAFE POSITION)
        ======================= */

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Auris",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .graphicsLayer {
                        translationY = textOffset.value
                    }
            )

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(width = 60.dp, height = 2.dp)
                    .alpha(textAlpha.value * 0.4f)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF6C5CE7),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        /* =======================
           GLOW SWEEP (CONTROLLED)
        ======================= */

        if (glow.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = glow.value * 0.25f
                        translationX = (glow.value * 600f) - 300f
                        rotationZ = -15f
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

/* =======================
   PARTICLES
======================= */

@Composable
private fun FloatingParticles() {

    val particles = remember {
        List(20) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = (2..5).random().dp,
                alpha = Random.nextFloat() * 0.2f
            )
        }
    }

    val transition = rememberInfiniteTransition()

    particles.forEach { p ->

        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(2000 + (0..1000).random()),
                RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .offset(
                        x = (p.x * 400).dp,
                        y = (p.y * 800 + anim * 20).dp
                    )
                    .size(p.size)
                    .alpha(p.alpha)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: androidx.compose.ui.unit.Dp,
    val alpha: Float
)