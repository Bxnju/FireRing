package com.benchopo.firering.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.benchopo.firering.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    // luego agregaremos: const val LOBBY = "lobby", const val GAME = "game", etc.
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
    }
}
