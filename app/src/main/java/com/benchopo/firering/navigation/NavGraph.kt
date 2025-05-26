package com.benchopo.firering.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.benchopo.firering.ui.screens.*

object Routes {
    const val HOME = "home"
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LOBBY = "lobby/{roomCode}"
    const val GAME = "game/{roomCode}"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.CREATE_ROOM) { CreateRoomScreen(navController) }
        composable(Routes.JOIN_ROOM) { JoinRoomScreen(navController) }
        composable(
                Routes.LOBBY,
                arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            LobbyScreen(navController, roomCode)
        }
        composable(
                Routes.GAME,
                arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            GameScreen(navController, roomCode)
        }
    }
}
