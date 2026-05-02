package com.goldensystem.auris.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import com.goldensystem.auris.presentation.navigation.Screen
import com.goldensystem.auris.presentation.navigation.navigateSafely
import com.goldensystem.auris.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun PlayerAlbumNavigationEffect(
    navController: NavHostController,
    sheetCollapsedTargetY: Float,
    sheetMotionController: SheetMotionController,
    playerViewModel: PlayerViewModel
) {
    val latestSheetCollapsedTargetY by rememberUpdatedState(sheetCollapsedTargetY)
    LaunchedEffect(navController) {
        playerViewModel.albumNavigationRequests.collectLatest { albumId ->
            sheetMotionController.snapCollapsed(latestSheetCollapsedTargetY)
            playerViewModel.collapsePlayerSheet()

            navController.navigateSafely(Screen.AlbumDetail.createRoute(albumId)) {
                launchSingleTop = false
                navController.currentBackStackEntry?.destination?.route?.let { currentRoute ->
                    if (currentRoute == Screen.AlbumDetail.route) {
                        popUpTo(Screen.AlbumDetail.route) { inclusive = true }
                    }
                }
            }
        }
    }
}
