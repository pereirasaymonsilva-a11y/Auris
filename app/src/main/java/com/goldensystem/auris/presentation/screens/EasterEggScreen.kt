package com.goldensystem.auris.presentation.components.lollipopland

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun LollipopLandOverlay(
    isMiniPlayerVisible: Boolean,
    onPlayRandom: () -> Unit,
    onClose: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // Estados do jogo
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }
    var gameRunning by remember { mutableStateOf(true) }
    var playerX by remember { mutableStateOf(0f) }
    
    // Itens na tela
    var lollipops by remember { mutableStateOf(listOf<FallingItem>()) }
    var obstacles by remember { mutableStateOf(listOf<FallingItem>()) }
    
    val playerWidth = 60.dp
    val itemSize = 40.dp
    
    // Controle de spawn
    LaunchedEffect(gameRunning) {
        while (gameRunning) {
            delay(800L)
            if (!gameRunning) break
            
            // Spawna pirulito ou obstáculo
            val randomX = (0..(screenWidth.value.toInt() - itemSize.value.toInt())).random().toFloat()
            
            if (Random.nextFloat() < 0.7f) { // 70% chance de pirulito
                lollipops = lollipops + FallingItem(
                    id = System.currentTimeMillis(),
                    x = randomX.dp,
                    y = 0.dp
                )
            } else {
                obstacles = obstacles + FallingItem(
                    id = System.currentTimeMillis(),
                    x = randomX.dp,
                    y = 0.dp
                )
            }
        }
    }
    
    // Atualiza posição dos itens
    LaunchedEffect(gameRunning, lollipops, obstacles) {
        while (gameRunning) {
            delay(16L) // ~60fps
            
            lollipops = lollipops.mapNotNull { item ->
                val newY = item.y + 8.dp
                if (newY > screenHeight) null
                else item.copy(y = newY)
            }
            
            obstacles = obstacles.mapNotNull { item ->
                val newY = item.y + 12.dp // obstáculos caem mais rápido
                if (newY > screenHeight) null
                else item.copy(y = newY)
            }
        }
    }
    
    // Colisões
    LaunchedEffect(lollipops, obstacles, playerX) {
        val playerRect = androidx.compose.ui.geometry.Rect(
            left = playerX,
            top = screenHeight.value - 100,
            right = playerX + playerWidth.value,
            bottom = screenHeight.value - 40
        )
        
        // Verifica pirulitos coletados
        val collected = lollipops.filter { item ->
            val itemRect = androidx.compose.ui.geometry.Rect(
                left = item.x.value,
                top = item.y.value,
                right = item.x.value + itemSize.value,
                bottom = item.y.value + itemSize.value
            )
            playerRect.intersect(itemRect) != androidx.compose.ui.geometry.Rect.Zero
        }
        
        if (collected.isNotEmpty()) {
            score += collected.size
            lollipops = lollipops - collected.toSet()
        }
        
        // Verifica obstáculos
        val hit = obstacles.filter { item ->
            val itemRect = androidx.compose.ui.geometry.Rect(
                left = item.x.value,
                top = item.y.value,
                right = item.x.value + itemSize.value,
                bottom = item.y.value + itemSize.value
            )
            playerRect.intersect(itemRect) != androidx.compose.ui.geometry.Rect.Zero
        }
        
        if (hit.isNotEmpty()) {
            lives -= hit.size
            obstacles = obstacles - hit.toSet()
            
            if (lives <= 0) {
                gameRunning = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a0a2e)) // Roxo escuro temático
    ) {
        // Canvas do jogo
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isMiniPlayerVisible) 80.dp else 16.dp)
        ) {
            // Desenha pirulitos
            lollipops.forEach { item ->
                drawLollipop(
                    center = Offset(item.x.value, item.y.value),
                    size = itemSize.value
                )
            }
            
            // Desenha obstáculos (dentes podres)
            obstacles.forEach { item ->
                drawBadTooth(
                    center = Offset(item.x.value, item.y.value),
                    size = itemSize.value
                )
            }
            
            // Desenha o coletor (boca feliz)
            drawMouth(
                x = playerX,
                y = size.height - 80f,
                width = playerWidth.value,
                isHappy = true
            )
        }
        
        // Área de drag para mover o coletor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .draggable(
                    state = rememberDraggableState { delta ->
                        playerX = (playerX + delta).coerceIn(0f, screenWidth.value - playerWidth.value)
                    },
                    orientation = Orientation.Horizontal
                )
        )
        
        // UI do jogo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🍭 $score",
                    color = Color.White,
                    fontSize = 24.sp
                )
                Text(
                    text = "❤️ $lives",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }
        
        // Game Over
        if (!gameRunning) {
            GameOverDialog(
                score = score,
                onPlayAgain = {
                    score = 0
                    lives = 3
                    lollipops = emptyList()
                    obstacles = emptyList()
                    playerX = screenWidth.value / 2 - playerWidth.value / 2
                    gameRunning = true
                },
                onClose = onClose
            )
        }
        
        // Mini player (mesmo do BrickBreaker)
        if (isMiniPlayerVisible) {
            MiniPlayer(
                onPlayRandom = onPlayRandom,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

data class FallingItem(
    val id: Long,
    val x: Dp,
    val y: Dp
)

// Funções de desenho (implementar igual ao BrickBreaker)
fun DrawScope.drawLollipop(center: Offset, size: Float) {
    val stick = Path().apply {
        moveTo(center.x, center.y + size * 0.3f)
        lineTo(center.x, center.y + size * 0.8f)
    }
    drawPath(stick, Color(0xFF8B4513), strokeWidth = size * 0.1f)
    
    drawCircle(
        color = Color(0xFFFF69B4),
        radius = size * 0.25f,
        center = Offset(center.x, center.y + size * 0.2f)
    )
    drawCircle(
        color = Color(0xFFDDA0DD),
        radius = size * 0.17f,
        center = Offset(center.x, center.y + size * 0.2f)
    )
}

fun DrawScope.drawBadTooth(center: Offset, size: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - size * 0.3f)
        quadTo(center.x + size * 0.2f, center.y, center.x, center.y + size * 0.4f)
        quadTo(center.x - size * 0.2f, center.y, center.x, center.y - size * 0.3f)
        close()
    }
    drawPath(path, Color(0xFF4A4A4A))
    drawCircle(
        color = Color(0xFFFF4444),
        radius = size * 0.1f,
        center = Offset(center.x - size * 0.1f, center.y)
    )
    drawCircle(
        color = Color(0xFFFF4444),
        radius = size * 0.1f,
        center = Offset(center.x + size * 0.1f, center.y)
    )
}

fun DrawScope.drawMouth(x: Float, y: Float, width: Float, isHappy: Boolean) {
    val mouthPath = if (isHappy) {
        Path().apply {
            moveTo(x, y)
            quadTo(x + width / 2, y + width * 0.6f, x + width, y)
        }
    } else {
        Path().apply {
            moveTo(x, y + width * 0.4f)
            quadTo(x + width / 2, y, x + width, y + width * 0.4f)
        }
    }
    drawPath(mouthPath, Color.White, strokeWidth = width * 0.1f)
    drawCircle(Color.White, width * 0.15f, Offset(x + width * 0.2f, y - width * 0.1f))
    drawCircle(Color.White, width * 0.15f, Offset(x + width * 0.8f, y - width * 0.1f))
}
