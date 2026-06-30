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
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onAnimationComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Animações usando Animatable para controle preciso
    val scale = remember { Animatable(0.2f) }
    val rotation = remember { Animatable(-15f) }
    val alpha = remember { Animatable(0f) }
    val glowProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val shadeAlpha = remember { Animatable(0f) }
    val finalFade = remember { Animatable(1f) }

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

        // Fase 2: Sombras e profundidade
        shadeAlpha.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
        delay(100)

        // Fase 3: Brilho diagonal passando
        glowProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        delay(100)
        glowProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
        delay(100)

        // Fase 4: Micro-vibração (2px de tremor) - usando StiffnessMedium em vez de VeryHigh
        val shakeAmount = 2f
        scale.animateTo(
            targetValue = 1f + 0.008f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        rotation.animateTo(
            targetValue = shakeAmount,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        delay(20)
        rotation.animateTo(
            targetValue = -shakeAmount,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        delay(20)
        rotation.animateTo(
            targetValue = shakeAmount * 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(20)
        rotation.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(100)

        // Fase 5: Texto "Auris" aparece
        textAlpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        delay(200)

        // Fase 6: Pausa antes de desaparecer
        delay(400)

        // Fase 7: Fade out suave
        finalFade.animateTo(
            targetValue = 0f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
        
        isAnimationComplete = true
    }

    // Quando a animação terminar, navega
    LaunchedEffect(isAnimationComplete) {
        if (isAnimationComplete) {
            delay(50) // Pequeno delay para o fade completar
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
            // Partículas de fundo (efeito sutil de "estrelas")
            FloatingParticles()

            // Container principal com animações
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                        alpha = alpha.value
                    }
            ) {
                // Logo com sombra suave
                Box(
                    modifier = Modifier
                        .size(140.dp)
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
                        painter = painterResource(R.drawable.ic_auris_logo_transparent), // Use a logo que você tem disponível
                        contentDescription = "Auris Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Efeito de brilho diagonal (shine)
                if (glowProgress.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = glowProgress.value * 0.35f
                            }
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.7f),
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(200f, 200f)
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

            // Texto "Auris" com efeito elegante
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 210.dp)
                    .graphicsLayer {
                        alpha = textAlpha.value
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
                
                // Linha decorativa sutil
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .padding(top = 4.dp)
                        .alpha(0.3f)
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
    val particles = remember {
        List(20) { idx ->
            Triple(
                (0..1000).random() / 1000f,
                (0..1000).random() / 1000f,
                (200 + (0..300).random()).toFloat()
            )
        }
    }

    particles.forEachIndexed { idx, particle ->
        val (x, y, delayMs) = particle
        val floatAnim = remember { Animatable(0f) }
        
        LaunchedEffect(idx) {
            delay(delayMs.toLong())
            while (true) {
                floatAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000 + (0..1000).random(), easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                delay(100)
            }
        }

        Box(
            modifier = Modifier
                .offset(
                    x = (x * 300 - 150).dp,
                    y = (y * 500 - 250).dp + (floatAnim.value * 30).dp
                )
                .size(2.dp)
                .alpha(0.1f + (0.2f * (1f - floatAnim.value.coerceIn(0f, 1f))))
                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
        )
    }
}