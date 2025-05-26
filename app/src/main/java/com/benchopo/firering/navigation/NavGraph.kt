package com.benchopo.firering.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.benchopo.firering.ui.screens.*
import com.benchopo.firering.viewmodel.ConnectionViewModel
import com.benchopo.firering.viewmodel.GameViewModel
import com.benchopo.firering.viewmodel.UserViewModel

object Routes {
    const val HOME = "home"
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val LOBBY = "lobby/{roomCode}"
    const val GAME = "game/{roomCode}"
}

@Composable
fun NavGraph(
        userViewModel: UserViewModel,
        gameViewModel: GameViewModel,
        connectionViewModel: ConnectionViewModel
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        // Fix: Pass gameViewModel to HomeScreen
        composable(Routes.HOME) { HomeScreen(navController, gameViewModel) }

        composable(Routes.CREATE_ROOM) {
            CreateRoomScreen(navController, userViewModel, gameViewModel)
        }

        composable(Routes.JOIN_ROOM) { JoinRoomScreen(navController, userViewModel, gameViewModel) }

        composable(
                Routes.LOBBY,
                arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            LobbyScreen(
                    navController = navController,
                    roomCode = roomCode,
                    userViewModel = userViewModel,
                    gameViewModel = gameViewModel,
                    connectionViewModel = connectionViewModel
            )
        }

        composable(
                Routes.GAME,
                arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            GameScreen(
                    navController = navController,
                    roomCode = roomCode,
                    userViewModel = userViewModel,
                    gameViewModel = gameViewModel
            )
        }
    }
}
