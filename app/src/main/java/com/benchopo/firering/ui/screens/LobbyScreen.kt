package com.benchopo.firering.ui.screens

import android.annotation.SuppressLint
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
import androidx.navigation.NavController
import com.benchopo.firering.model.GameState
import com.benchopo.firering.navigation.Routes
import com.benchopo.firering.viewmodel.ConnectionViewModel
import com.benchopo.firering.viewmodel.GameViewModel
import com.benchopo.firering.viewmodel.UserViewModel

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
        roomCode: String,
        navController: NavController,
        userViewModel: UserViewModel,
        gameViewModel: GameViewModel,
        connectionViewModel: ConnectionViewModel
) {
    val scope = rememberCoroutineScope()

    // Collect states from viewmodel
    val gameRoom by gameViewModel.gameRoom.collectAsState()
    val loading by gameViewModel.loading.collectAsState()
    val playerId by gameViewModel.playerId.collectAsState()

    // Update how we check if the player is host
    val currentPlayerId = gameViewModel.playerId.collectAsState().value

    // Check if current player is host by checking the isHost flag directly from player object
    val isHost =
            remember(gameRoom, currentPlayerId) {
                val result = currentPlayerId == gameRoom?.hostId
                Log.d(
                        "LobbyScreen",
                        "isHost calculation: $result (currentPlayerId=$currentPlayerId, hostId=${gameRoom?.hostId})"
                )
                result
            }

    // Track players
    val players = gameRoom?.players?.values?.toList() ?: emptyList()

    // Debug output
    LaunchedEffect(gameRoom, currentPlayerId, players) {
        Log.d("LobbyScreen", "Room data updated: ${gameRoom?.roomCode}")
        Log.d("LobbyScreen", "Players: ${players.map { it.name }}")
        Log.d(
                "LobbyScreen",
                "Is host: $isHost (playerId=$currentPlayerId, hostId=${gameRoom?.hostId})"
        )
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

    // Debug logs to help diagnose host status
    LaunchedEffect(gameRoom, currentPlayerId) {
        Log.d("LobbyScreen", "Current player ID: $currentPlayerId")
        Log.d("LobbyScreen", "Host ID from room: ${gameRoom?.hostId}")
        Log.d("LobbyScreen", "Room players: ${gameRoom?.players?.keys}")
        Log.d(
                "LobbyScreen",
                "Current player isHost property: ${gameRoom?.players?.get(currentPlayerId)?.isHost}"
        )
        Log.d("LobbyScreen", "Host check by ID comparison: ${currentPlayerId == gameRoom?.hostId}")
    }

    // Ensure room is loaded and player ID is set correctly
    LaunchedEffect(roomCode, gameViewModel.playerId.value) {
        Log.d(
                "LobbyScreen",
                "Ensuring room loaded: $roomCode with player: ${gameViewModel.playerId.value}"
        )
        gameViewModel.ensureRoomLoaded(roomCode)

        // If we know the room code but not the player ID, try to retrieve it
        if (gameViewModel.playerId.value == null) {
            val hostId = gameViewModel.gameRoom.value?.hostId
            if (hostId != null) {
                Log.d("LobbyScreen", "Setting player ID to host: $hostId")
                gameViewModel.setPlayerId(hostId) // Use the public method instead
            }
        }
    }

    // Add this LaunchedEffect to redirect if we end up in LobbyScreen without valid data
    LaunchedEffect(gameRoom, currentPlayerId) {
        // If after room loading we still don't have valid data, go back to home
        if (gameRoom == null && currentPlayerId == null) {
            Log.d("LobbyScreen", "No valid room data, returning to home")
            navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        }
    }

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
                                        gameViewModel.leaveRoom {
                                            // This is now called after leave completes
                                            Log.d("LobbyScreen", "Navigating to home")
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
