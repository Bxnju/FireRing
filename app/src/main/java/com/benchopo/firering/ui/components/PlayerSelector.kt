// Create a new file: PlayerSelector.kt
package com.benchopo.firering.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benchopo.firering.model.Player
import androidx.activity.compose.BackHandler

@Composable
fun PlayerSelector(
    players: List<Player>,
    currentPlayerId: String,
    onPlayerSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedPlayerId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        // Select random player if back button is pressed
        if (players.isNotEmpty()) {
            val otherPlayers = players.filter { it.id != currentPlayerId }
            if (otherPlayers.isNotEmpty()) {
                val randomPlayer = otherPlayers.random()
                onPlayerSelected(randomPlayer.id, randomPlayer.name)
            } else {
                // If only current player, select self
                val self = players.find { it.id == currentPlayerId }
                if (self != null) {
                    onPlayerSelected(self.id, self.name)
                } else {
                    onDismiss()
                }
            }
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Select random player if dialog is dismissed
            if (players.isNotEmpty()) {
                val otherPlayers = players.filter { it.id != currentPlayerId }
                if (otherPlayers.isNotEmpty()) {
                    val randomPlayer = otherPlayers.random()
                    onPlayerSelected(randomPlayer.id, randomPlayer.name)
                } else {
                    // If only current player, select self
                    val self = players.find { it.id == currentPlayerId }
                    if (self != null) {
                        onPlayerSelected(self.id, self.name)
                    } else {
                        onDismiss()
                    }
                }
            } else {
                onDismiss()
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Choose Who Drinks")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "(You must select a player)",
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
                if (expandedPlayerId != null) {
                    // Show expanded player details
                    val player = players.find { it.id == expandedPlayerId }
                    if (player != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                player.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Drinks consumed: ${player.drinkCount} 🍺",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { expandedPlayerId = null }
                                ) {
                                    Text("Back")
                                }

                                Button(
                                    onClick = { onPlayerSelected(player.id, player.name) }
                                ) {
                                    Text("Select")
                                }
                            }
                        }
                    }
                } else {
                    // Show grid of players
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Add Random selection button at top
                        Button(
                            onClick = {
                                Log.d("PlayerSelector", "Random player selection chosen")
                                val otherPlayers = players.filter { it.id != currentPlayerId }
                                if (otherPlayers.isNotEmpty()) {
                                    val randomPlayer = otherPlayers.random()
                                    onPlayerSelected(randomPlayer.id, randomPlayer.name)
                                } else {
                                    // If only current player, select self
                                    val self = players.find { it.id == currentPlayerId }
                                    if (self != null) {
                                        onPlayerSelected(self.id, self.name)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text("Select Random Player")
                        }

                        players.forEach { player ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { expandedPlayerId = player.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        player.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    Text(
                                        "${player.drinkCount} 🍺",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {} // No Cancel button
    )
}