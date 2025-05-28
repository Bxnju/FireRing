package com.benchopo.firering.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benchopo.firering.model.MiniGame
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.tooling.preview.Preview
import com.benchopo.firering.model.MiniGameType
import com.benchopo.firering.model.GameMode
import kotlin.random.Random

@Composable
fun MiniGameSelector(
    games: List<MiniGame>,
    onGameSelected: (MiniGame) -> Unit,
    onCustomGameCreated: (MiniGame) -> Unit,
    onDismiss: () -> Unit,
    currentGameMode: GameMode = GameMode.NORMAL
) {
    var expandedGameId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        // Select random game if back button is pressed
        if (games.isNotEmpty()) {
            val randomGame = games.random()
            onGameSelected(randomGame)
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Select random game if dialog is dismissed
            if (games.isNotEmpty()) {
                val randomGame = games.random()
                onGameSelected(randomGame)
            } else {
                onDismiss()
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select a Mini Game")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "(You must select a game to continue)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (expandedGameId == "create_custom") {
                    CustomGameCreator(
                        onGameCreated = { newGame ->
                            onCustomGameCreated(newGame)
                            onGameSelected(newGame)
                        },
                        onCancel = { expandedGameId = null },
                        currentGameMode = currentGameMode
                    )
                } else if (expandedGameId != null) {
                    // Show expanded game details
                    val game = games.find { it.id == expandedGameId }
                    if (game != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                game.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                game.description,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { expandedGameId = null }
                                ) {
                                    Text("Back")
                                }

                                Button(
                                    onClick = { onGameSelected(game) }
                                ) {
                                    Text("Select")
                                }
                            }
                        }
                    }
                } else {
                    // Show grid of games
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Add Random selection button at top
                        Button(
                            onClick = {
                                val randomGame = games.random()
                                onGameSelected(randomGame)
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text("Select Random Game")
                        }

                        // Add Create Custom Game button
                        Button(
                            onClick = { expandedGameId = "create_custom" },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Create Custom Game")
                        }

                        games.forEach { game ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { expandedGameId = game.id }
                            ) {
                                Text(
                                    game.title,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {} // No Cancel button
    )
}

@Preview
@Composable
fun MiniGameSelectorPreview() {
    // Use correct parameters for MiniGame
    val games = List(5) { index ->
        MiniGame(
            id = "game_$index",
            title = "Game $index",
            description = "Description for Game $index",
            type = MiniGameType.CHALLENGE,  // Use type instead of category
            gameMode = GameMode.NORMAL      // Use gameMode, not difficulty
        )
    }

    MiniGameSelector(
        games = games,
        onGameSelected = { },
        onCustomGameCreated = { },
        onDismiss = { }
    )
}