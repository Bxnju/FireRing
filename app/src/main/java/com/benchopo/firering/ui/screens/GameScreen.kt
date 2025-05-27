package com.benchopo.firering.ui.screens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
        navController: NavController,
        roomCode: String,
        gameViewModel: GameViewModel,
        userViewModel: UserViewModel,
) {
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

    // Get card rule if a card is drawn
    val cardRule = remember(drawnCard) {
        if (drawnCard?.ruleId != null) {
            // In a real implementation, fetch the rule text
            "Rule for ${drawnCard?.value} of ${drawnCard?.suit}"
        } else {
            "Draw a card to see the rule"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Room: $roomCode") },
                actions = {
                    IconButton(onClick = { gameViewModel.leaveRoom {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }}) {
                        Text("Exit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
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
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        cardRule,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Draw card button
            Button(
                onClick = { gameViewModel.drawCard() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isCurrentPlayerTurn && !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Draw Card")
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
        }
    }
}
