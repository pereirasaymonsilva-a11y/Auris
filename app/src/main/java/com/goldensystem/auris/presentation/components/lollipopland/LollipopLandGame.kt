package com.goldensystem.auris.presentation.components.lollipopland

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun LollipopLandGame(
    isMiniPlayerVisible: Boolean,
    onPlayRandom: () -> Unit,
    onClose: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.value
    val screenHeight = configuration.screenHeightDp.value
    
    var score by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) }
    var gameRunning by remember { mutableStateOf(true) }
    var playerX by remember { mutableStateOf(screenWidth / 2 - 30f) }
    
    var lollipops by remember { mutableStateOf<List<FallingItem>>(emptyList()) }
    var obstacles by remember { mutableStateOf<List<FallingItem>>(emptyList()) }
    
    val playerWidth = 60f
    val itemSize = 40f
    
    // Spawn de itens
    LaunchedEffect(gameRunning) {
        while (gameRunning) {
            delay(800L)
            if (!gameRunning) break
            
            val randomX = Random.nextFloat() * (screenWidth - itemSize)
            
            if (Random.nextFloat() < 0.7f) {
                lollipops = lollipops + FallingItem(
                    id = System.currentTimeMillis(),
                    x = randomX,
                    y = 0f
                )
            } else {
                obstacles = obstacles + FallingItem(
                    id = System.currentTimeMillis(),
                    x = randomX,
                    y = 0f
                )
            }
        }
    }
    
    // Movimento dos itens
    LaunchedEffect(gameRunning) {
        while (gameRunning) {
            delay(16L)
            
            lollipops = lollipops.mapNotNull { item ->
                val newY = item.y + 8f
                if (newY > screenHeight - 100) null
                else item.copy(y = newY)
            }
            
            obstacles = obstacles.mapNotNull { item ->
                val newY = item.y + 12f
                if (newY > screenHeight - 100) null
                else item.copy(y = newY)
            }
        }
    }
    
    // Colisões
    LaunchedEffect(lollipops, obstacles, playerX) {
        val playerLeft = playerX
        val playerRight = playerX + playerWidth
        val playerTop = screenHeight - 100
        val playerBottom = screenHeight - 40
        
        val collected = lollipops.filter { item ->
            item.x + itemSize > playerLeft && item.x < playerRight &&
            item.y + itemSize > playerTop && item.y < playerBottom
        }
        
        if (collected.isNotEmpty()) {
            score += collected.size
            lollipops = lollipops - collected.toSet()
        }
        
        val hit = obstacles.filter { item ->
            item.x + itemSize > playerLeft && item.x < playerRight &&
            item.y + itemSize > playerTop && item.y < playerBottom
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
            .background(Color(0xFF1a0a2e))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isMiniPlayerVisible) 80.dp else 16.dp)
        ) {
            lollipops.forEach { item ->
                drawLollipop(
                    center = Offset(item.x + itemSize/2, item.y + itemSize/2),
                    size = itemSize
                )
            }
            
            obstacles.forEach { item ->
                drawBadTooth(
                    center = Offset(item.x + itemSize/2, item.y + itemSize/2),
                    size = itemSize
                )
            }
            
            drawMouth(
                x = playerX,
                y = size.height - 80f,
                width = playerWidth,
                isHappy = true
            )
        }
        
        // Área de controle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .draggable(
                    state = rememberDraggableState { delta ->
                        playerX = (playerX + delta).coerceIn(0f, screenWidth - playerWidth)
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
        
        // Game Over Dialog
        if (!gameRunning) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Game Over!") },
                text = { Text("Your score: $score\nWant to play again?") },
                confirmButton = {
                    TextButton(onClick = {
                        score = 0
                        lives = 3
                        lollipops = emptyList()
                        obstacles = emptyList()
                        playerX = screenWidth / 2 - playerWidth / 2
                        gameRunning = true
                    }) {
                        Text("Play Again")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose) {
                        Text("Exit")
                    }
                }
            )
        }
    }
}

data class FallingItem(
    val id: Long,
    val x: Float,
    val y: Float
)

fun DrawScope.drawLollipop(center: Offset, size: Float) {
    val stick = Path().apply {
        moveTo(center.x, center.y + size * 0.3f)
        lineTo(center.x, center.y + size * 0.8f)
    }
    drawPath(stick, Color(0xFF8B4513), style = Stroke(width = size * 0.1f))
    
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
        quadraticBezierTo(
            center.x + size * 0.2f, center.y,
            center.x, center.y + size * 0.4f
        )
        quadraticBezierTo(
            center.x - size * 0.2f, center.y,
            center.x, center.y - size * 0.3f
        )
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
            quadraticBezierTo(x + width / 2, y + width * 0.6f, x + width, y)
        }
    } else {
        Path().apply {
            moveTo(x, y + width * 0.4f)
            quadraticBezierTo(x + width / 2, y, x + width, y + width * 0.4f)
        }
    }
    drawPath(mouthPath, Color.White, style = Stroke(width = width * 0.1f))
    drawCircle(Color.White, width * 0.15f, Offset(x + width * 0.2f, y - width * 0.1f))
    drawCircle(Color.White, width * 0.15f, Offset(x + width * 0.8f, y - width * 0.1f))
}