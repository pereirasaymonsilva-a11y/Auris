package com.goldensystem.auris.presentation.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goldensystem.auris.R
import com.goldensystem.auris.presentation.components.brickbreaker.BrickBreakerOverlay
import com.goldensystem.auris.presentation.components.lollipopland.LollipopLandGame
import com.goldensystem.auris.presentation.viewmodel.PlayerViewModel

@Composable
fun EasterEggScreen(
    viewModel: PlayerViewModel,
    onNavigationIconClick: () -> Unit,
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    var hasShownFanToast by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf(0) }

    val stablePlayerState by viewModel.stablePlayerState.collectAsStateWithLifecycle()
    val currentSong = stablePlayerState.currentSong

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(hasShownFanToast) {
        if (hasShownFanToast) return@LaunchedEffect
        Toast.makeText(context, context.getString(R.string.easter_egg_thank_you), Toast.LENGTH_SHORT).show()
        hasShownFanToast = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 260)) +
                scaleIn(
                    initialScale = 0.97f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                ) +
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight / 10 },
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedGame) {
                0 -> BrickBreakerOverlay(
                    isMiniPlayerVisible = currentSong != null,
                    onPlayRandom = { viewModel.playRandomSong() },
                    onClose = {
                        isVisible = false
                        onNavigationIconClick()
                    },
                )
                1 -> LollipopLandGame(
                    isMiniPlayerVisible = currentSong != null,
                    onPlayRandom = { viewModel.playRandomSong() },
                    onClose = {
                        isVisible = false
                        onNavigationIconClick()
                    }
                )
            }
            
            FloatingActionButton(
                onClick = { selectedGame = (selectedGame + 1) % 2 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ) {
                Text(
                    text = if (selectedGame == 0) "🍭" else "🧱",
                    fontSize = 24.sp
                )
            }
        }
    }
}
