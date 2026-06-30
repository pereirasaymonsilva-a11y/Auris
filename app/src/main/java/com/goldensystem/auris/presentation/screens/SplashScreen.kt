// presentation/screens/SplashScreen.kt
package com.goldensystem.auris.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldensystem.auris.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onAnimationComplete: () -> Unit
) {
    // Animações usando Animatable para controle preciso
    val scale = remember { Animatable(0.2f) }
    val rotation = remember { Animatable(-15f) }
    val alpha = remember { Animatable(0f) }
    val glowProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textTranslationY = remember { Animatable(20f) } // Texto sobe 20dp
    val shadeAlpha = remember { Animatable(0f) }
    val finalFade = remember { Animatable(1f) }
    val impactScale = remember { Animatable(1f) } // Efeito de impacto

    // Estado para controlar a transição
    var isAnimationComplete by remember { mutableStateOf(false) }

    // Controlador da sequência de animação
    LaunchedEffect(Unit) {
        // Fase 1: Logo aparece (fade + escala + rotação)
        alpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        delay(50)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(150)

        // Fase 2: Efeito de IMPACTO (logo cresce e volta rápido)
        impactScale.animateTo(
            targetValue = 1.04f,
            animationSpec = tween(80, easing = FastOutSlowInEasing)
        )
        impactScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        delay(50)

        // Fase 3: Sombras e profundidade
        shadeAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        delay(100)

        // Fase 4: Brilho DIAGONAL ANDANDO (translationX)
        glowProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
        delay(100)
        glowProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
        delay(150)

        // Fase 5: Micro-vibração usando keyframes (mais natural)
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 120
                -2f at 0
                2f at 30
                -1.5f at 55
                1.5f at 75
                0f at 120
            }
        )
        delay(50)

        // Fase 6: Texto "Auris" aparece (com subida suave)
        textAlpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        textTranslationY.animateTo(
            targetValue = 0f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        delay(300)

        // Fase 7: Pausa antes de desaparecer
        delay(400)

        // Fase 8: Fade out suave
        finalFade.animateTo(
            targetValue = 0f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
        
        isAnimationComplete = true
    }

    // Quando a animação terminar, navega
    LaunchedEffect(isAnimationComplete) {
        if (isAnimationComplete) {
            delay(50)
            onAnimationComplete()
        }
    }

    // Gradiente de fundo elegante (escuro com toque de cor)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0A12),
            Color(0xFF12121F),
            Color(0xFF0D0D17)
        )
    )

    // Cálculo do brilho andando (translationX)
    val glowTranslationX = (glowProgress.value * 300f) - 150f

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = finalFade.value
            },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush),
            contentAlignment = Alignment.Center
        ) {
            // Partículas de fundo (usando InfiniteTransition)
            FloatingParticles()

            // Container principal com animações
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .graphicsLayer {
                        scaleX = scale.value * impactScale.value
                        scaleY = scale.value * impactScale.value
                        rotationZ = rotation.value
                        alpha = alpha.value
                    }
            ) {
                // Logo com sombra suave
                Box(
                    modifier = Modifier
                        .size(170.dp) // Aumentado de 140 para 170
                        .shadow(
                            elevation = 24.dp,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            clip = false,
                            spotColor = Color(0xFF6C5CE7).copy(alpha = 0.3f * shadeAlpha.value)
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6C5CE7).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                radius = 80f
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    // Logo oficial do Auris
                    Image(
                        painter = painterResource(R.drawable.ic_auris_logo_transparent),
                        contentDescription = "Auris Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Efeito de brilho diagonal ANDANDO (como Samsung)
                if (glowProgress.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = glowProgress.value * 0.35f
                                translationX = glowTranslationX
                            }
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Pequeno highlight circular (efeito de "aurora")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = (1f - scale.value.coerceIn(0f, 1f)) * 0.3f
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6C5CE7).copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                radius = 60f
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }

            // Texto "Auris" com efeito elegante (sobe 15dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 240.dp) // Ajustado para logo maior
                    .graphicsLayer {
                        alpha = textAlpha.value
                        translationY = textTranslationY.value
                    }
            ) {
                Text(
                    text = "Auris",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.alpha(0.9f)
                )
                
                // Linha decorativa sutil (também animada)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .padding(top = 4.dp)
                        .alpha(0.3f * textAlpha.value)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF6C5CE7),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // Partículas com posições aleatórias
    val particles = remember {
        List(25) { idx ->
            ParticleData(
                x = (0..1000).random() / 1000f,
                y = (0..1000).random() / 1000f,
                delayMs = (200 + (0..300).random()).toLong(),
                speed = 1f + (0..500).random() / 1000f,
                size = (1.5f + (0..3).random()).dp,
                alphaBase = 0.08f + (0..200).random() / 1000f * 0.25f
            )
        }
    }

    particles.forEach { particle ->
        // Animação infinita de flutuação
        val floatValue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (1500 + (0..1000).random()).toInt(),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "particle_${particle.hashCode()}"
        )

        Box(
            modifier = Modifier
                .offset(
                    x = (particle.x * 300 - 150).dp,
                    y = (particle.y * 500 - 250).dp + (floatValue * 35).dp
                )
                .size(particle.size)
                .alpha(particle.alphaBase + (0.15f * (1f - floatValue.coerceIn(0f, 1f))))
                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
        )
    }
}

// Classe para dados da partícula
private data class ParticleData(
    val x: Float,
    val y: Float,
    val delayMs: Long,
    val speed: Float,
    val size: androidx.compose.ui.unit.Dp,
    val alphaBase: Float
)