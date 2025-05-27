package com.benchopo.firering.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benchopo.firering.navigation.Routes
import com.benchopo.firering.viewmodel.GameViewModel
import com.benchopo.firering.viewmodel.UserViewModel
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.benchopo.firering.model.Player


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
        navController: NavController,
        roomCode: String,
        gameViewModel: GameViewModel,
        userViewModel: UserViewModel,
) {
    // Add exit dialog state
    var showLeaveDialog by remember { mutableStateOf(false) }

    // Handle system back button
    BackHandler {
        Log.d("GameScreen", "Back button pressed, showing leave dialog")
        showLeaveDialog = true
    }

    // Collect game state
    val gameRoom by gameViewModel.gameRoom.collectAsState()
    val currentPlayer by gameViewModel.currentPlayer.collectAsState()
    val playerId by gameViewModel.playerId.collectAsState()
    val drawnCard by gameViewModel.drawnCard.collectAsState()
    val loading by gameViewModel.loading.collectAsState()

    // Determine if it's this player's turn
    val isCurrentPlayerTurn = remember(gameRoom, playerId) {
        gameRoom?.currentPlayerId == playerId
    }

    // Get the player whose turn it is
    val currentTurnPlayer = remember(gameRoom) {
        gameRoom?.currentPlayerId?.let { gameRoom?.players?.get(it) }
    }

    // Get all drawn cards in order
    val drawnCards = remember(gameRoom) {
        val cards = gameRoom?.drawnCards?.sortedByDescending { it.drawnTimestamp ?: 0 } ?: emptyList()
        Log.d("GameScreen", "Drawn cards from gameRoom: ${cards.size} cards")
        cards.forEachIndexed { index, card ->
            Log.d("GameScreen", "  Card $index: ${card.value} of ${card.suit}, drawn by: ${card.drawnByPlayerId}, timestamp: ${card.drawnTimestamp}")
        }
        cards
    }

    // Get card rule if a card is drawn
    val cardRule = remember(drawnCard) {
        drawnCard?.ruleDescription ?: "Draw a card to see the rule"
    }

    // Check if deck is empty
    val isGameOver = remember(gameRoom) {
        (gameRoom?.deck?.isEmpty() == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Room: $roomCode") },
                actions = {
                    // Add player info button
                    var showPlayerInfo by remember { mutableStateOf(false) }

                    IconButton(onClick = {
                        Log.d("GameScreen", "Player info button clicked, showing sidebar")
                        showPlayerInfo = true
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "Player Info")
                    }

                    // Exit button
                    IconButton(onClick = {
                        Log.d("GameScreen", "Exit button pressed, showing leave dialog")
                        showLeaveDialog = true
                    }) {
                        Text("Exit")
                    }

                    // Player info drawer
                    if (showPlayerInfo) {
                        PlayerInfoSidebar(
                            players = gameRoom?.players?.values?.toList() ?: emptyList(),
                            onDismiss = {
                                Log.d("GameScreen", "Closing player info sidebar")
                                showPlayerInfo = false
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Turn indicator
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Current Turn",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        currentTurnPlayer?.name ?: "Unknown",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        if (isCurrentPlayerTurn) "It's your turn!" else "Wait for your turn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentPlayerTurn)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (drawnCard == null) {
                    Text(
                        "No card drawn yet",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .size(140.dp, 200.dp)
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                drawnCard?.value ?: "",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                drawnCard?.suit ?: "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current rule display
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Current Rule",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        cardRule,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Card History Section
            if (drawnCards.isNotEmpty()) {
                Log.d("GameScreen", "Showing card history section with ${drawnCards.size} cards")
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Card History",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show the last 3 drawn cards (excluding the current one)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Skip the current card if it's already drawn
                    val historyCards = if (drawnCard != null) {
                        val filtered = drawnCards.filter { it.id != drawnCard?.id }.take(3)
                        Log.d("GameScreen", "History cards (excluding current): ${filtered.size} cards")
                        filtered
                    } else {
                        val taken = drawnCards.take(3)
                        Log.d("GameScreen", "History cards (no current card): ${taken.size} cards")
                        taken
                    }

                    historyCards.forEach() { historyCard ->
                        Log.d("GameScreen", "Rendering history card: ${historyCard.value} of ${historyCard.suit}")
                        Card(
                            modifier = Modifier
                                .size(80.dp, 120.dp)
                                .padding(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    historyCard.value,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    historyCard.suit,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                // Get player who drew this card
                                val playerName = remember(historyCard, gameRoom) {
                                    val name = historyCard.drawnByPlayerId?.let { id ->
                                        val playerName = gameRoom?.players?.get(id)?.name ?: "Unknown"
                                        Log.d("GameScreen", "Card drawn by player: $id, name: $playerName")
                                        playerName
                                    } ?: "Unknown"
                                    name
                                }

                                Text(
                                    "by $playerName",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Log.d("GameScreen", "No card history to show - drawnCards is empty")
            }

            // Player Drink Counter Section - Only current player
            Spacer(modifier = Modifier.height(24.dp))

            // Find current player from the players list
            val myPlayer = remember(gameRoom, playerId) {
                val player = gameRoom?.players?.get(playerId)
                Log.d("GameScreen", "Current player: ${player?.name}, drink count: ${player?.drinkCount}")
                player
            }

            if (myPlayer != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your Drinks",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrement button (only enabled if count > 0)
                            FilledTonalIconButton(
                                onClick = {
                                    Log.d("GameScreen", "Decrementing drink count for player: ${myPlayer.id}")
                                    gameViewModel.updateDrinks(myPlayer.id, -1)
                                },
                                enabled = myPlayer.drinkCount > 0
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                            }

                            // Drink count display
                            Text(
                                "${myPlayer.drinkCount} 🍺",
                                style = MaterialTheme.typography.displaySmall
                            )

                            // Increment button
                            FilledTonalIconButton(
                                onClick = {
                                    Log.d("GameScreen", "Incrementing drink count for player: ${myPlayer.id}")
                                    gameViewModel.updateDrinks(myPlayer.id, 1)
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Player data not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Draw card button
            Button(
                onClick = { gameViewModel.drawCard() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isCurrentPlayerTurn && !loading && !isGameOver // Add !isGameOver condition
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isGameOver) "Game Over" else "Draw Card")
                }
            }

            // Next player button (only show after drawing a card)
            if (drawnCard != null && isCurrentPlayerTurn) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { gameViewModel.advanceTurn() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Next Player")
                }
            }

            // Show game over banner if all cards are drawn
            if (isGameOver) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Game Complete!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "All cards have been drawn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Optional: Add button to return to lobby or start new game
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showLeaveDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("End Game")
                        }
                    }
                }
            }
        }
    }

    // Navigate back to home if the room is deleted or has no players
    LaunchedEffect(gameRoom) {
        // If the room was deleted or has no players, navigate back to home
        if (gameRoom == null || gameRoom?.players?.isEmpty() == true) {
            Log.d("GameScreen", "Room no longer exists or has no players, returning to home")
            gameViewModel.clearGameData()
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
            }
        }
    }

    // Force re-fetch game room data on first load
    LaunchedEffect(Unit) {
        Log.d("GameScreen", "Initial load - forcing refresh of room data")
        gameViewModel.ensureRoomLoaded(roomCode)
    }

    // Debug drawn cards data
    LaunchedEffect(gameRoom) {
        if (gameRoom != null) {
            Log.d("GameScreen", "Room updated, drawn cards count: ${gameRoom?.drawnCards?.size ?: 0}")
            gameRoom?.drawnCards?.forEach { card ->
                Log.d("GameScreen", "Drawn card: ${card.value} of ${card.suit} by ${card.drawnByPlayerId}")
            }
        }
    }

    // Add the leave confirmation dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Game") },
            text = { Text("Are you sure you want to leave this game?") },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("GameScreen", "Confirming leave game")
                        gameViewModel.leaveRoom {
                            // This is called after leave completes
                            Log.d("GameScreen", "Navigating to home")
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    }
                ) { Text("Leave", color = androidx.compose.ui.graphics.Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PlayerInfoSidebar(
    players: List<Player>,
    onDismiss: () -> Unit
) {
    Log.d("GameScreen", "Showing player info sidebar with ${players.size} players")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Players Info") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                players.forEachIndexed { index, player ->
                    Log.d("GameScreen", "Rendering player info: ${player.name}, drinks: ${player.drinkCount}")

                    if (index > 0) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        // Player name with online status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (player.isConnected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                player.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (player.isHost) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "(Host)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Drink count
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Drinks: ${player.drinkCount} 🍺",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // What they're drinking (if selected)
                        player.selectedDrinkId?.let { drinkId ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Drinking: ${getDrinkName(drinkId)} ${getDrinkEmoji(drinkId)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Helper functions for drink info
private fun getDrinkName(drinkId: String): String {
    return when (drinkId) {
        "beer" -> "Beer"
        "wine" -> "Wine"
        "whiskey" -> "Whiskey"
        "vodka" -> "Vodka"
        "water" -> "Water"
        else -> "Custom Drink"
    }
}

private fun getDrinkEmoji(drinkId: String): String {
    return when (drinkId) {
        "beer" -> "🍺"
        "wine" -> "🍷"
        "whiskey" -> "🥃"
        "vodka" -> "🥂"
        "water" -> "💧"
        else -> "🥤"
    }
}
