package com.benchopo.firering.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.benchopo.firering.model.GameState
import com.benchopo.firering.navigation.Routes
import com.benchopo.firering.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
        navController: NavController,
        roomCode: String,
        gameViewModel: GameViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    // Collect states from viewmodel
    val gameRoom by gameViewModel.gameRoom.collectAsState()
    val loading by gameViewModel.loading.collectAsState()
    val playerId by gameViewModel.playerId.collectAsState()

    // Check if we're the host
    val isHost = gameRoom?.hostId == playerId

    // Track players
    val players = gameRoom?.players?.values?.toList() ?: emptyList()

    // Debug output
    LaunchedEffect(gameRoom) {
        Log.d("LobbyScreen", "Room data updated: ${gameRoom?.roomCode}")
        Log.d("LobbyScreen", "Players: ${players.map { it.name }}")
        Log.d("LobbyScreen", "Is host: $isHost (playerId=$playerId, hostId=${gameRoom?.hostId})")
    }

    // Ensure we get room updates
    LaunchedEffect(roomCode) {
        Log.d("LobbyScreen", "Setting up room code: $roomCode")
        if (gameViewModel.roomCode.value == null) {
            Log.d("LobbyScreen", "Setting room code in viewmodel")
            // Force reload room data if needed
            gameViewModel.loadRoom(roomCode)
        }
    }

    // Navigate to game when it starts
    LaunchedEffect(gameRoom?.gameState) {
        if (gameRoom?.gameState == GameState.PLAYING) {
            navController.navigate(Routes.GAME.replace("{roomCode}", roomCode)) {
                popUpTo(Routes.LOBBY) { inclusive = true }
            }
        }
    }

    var showLeaveDialog by remember { mutableStateOf(false) }

    // Handle back press
    BackHandler { showLeaveDialog = true }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Lobby") },
                        navigationIcon = {
                            IconButton(onClick = { showLeaveDialog = true }) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Room code display
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Room Code", style = MaterialTheme.typography.titleMedium)
                    Text(
                            roomCode,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                    )
                    Text(
                            "Share this code with your friends",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Players (${players.size})", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(8.dp))

            // Debug message if no players
            if (players.isEmpty()) {
                Text(
                        "Waiting for players to join...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                )
            }

            // Player list
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(players) { player ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    player.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                            )
                            if (player.isHost) {
                                Text(
                                        "Host",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!player.isConnected) {
                                Text(
                                        "Offline",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start game button (only for host)
            if (isHost) {
                Button(
                        onClick = { gameViewModel.startGame() },
                        enabled = !loading && players.size > 0,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                        )
                    } else {
                        Text("Start Game")
                    }
                }
            } else {
                Text(
                        "Waiting for host to start the game...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                )
            }

            // Leave game dialog
            if (showLeaveDialog) {
                AlertDialog(
                        onDismissRequest = { showLeaveDialog = false },
                        title = { Text("Leave Game") },
                        text = { Text("Are you sure you want to leave this game?") },
                        confirmButton = {
                            Button(
                                    onClick = {
                                        Log.d("LobbyScreen", "Confirming leave game")
                                        scope.launch {
                                            gameViewModel.leaveRoom()
                                            Log.d("LobbyScreen", "Left game, navigating to home")
                                            navController.navigate(Routes.HOME) {
                                                popUpTo(Routes.HOME) { inclusive = true }
                                            }
                                        }
                                    }
                            ) { Text("Leave") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
                        }
                )
            }
        }
    }
}
