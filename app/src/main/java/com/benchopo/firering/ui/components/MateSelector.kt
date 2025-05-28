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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.benchopo.firering.model.Player
import androidx.activity.compose.BackHandler

@Composable
fun MateSelector(
    players: List<Player>,
    currentPlayerId: String,
    onMateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedPlayerId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        // Select random mate if back button is pressed
        if (players.isNotEmpty()) {
            val otherPlayers = players.filter { it.id != currentPlayerId }
            if (otherPlayers.isNotEmpty()) {
                val randomPlayer = otherPlayers.random()
                onMateSelected(randomPlayer.id)
            } else {
                onDismiss()
            }
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Select random mate if dialog is dismissed
            if (players.isNotEmpty()) {
                val otherPlayers = players.filter { it.id != currentPlayerId }
                if (otherPlayers.isNotEmpty()) {
                    val randomPlayer = otherPlayers.random()
                    onMateSelected(randomPlayer.id)
                } else {
                    onDismiss()
                }
            } else {
                onDismiss()
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Choose Your Drinking Mate")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "(You must select a drinking mate)",
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

                            // Show current mates if any
                            if (player.mateIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Current Mates:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )

                                player.mateIds.forEach { mateId ->
                                    val mateName = players.find { it.id == mateId }?.name ?: "Unknown"
                                    Text(
                                        "• $mateName",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Choosing this player will make all their mates your mates too!",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Add this inside the expanded player view when showing player details
                            if (player.mateIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Mate Chain Effect",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "If you select ${player.name}, you'll be connected to them and all their mates:",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // List all players who will be in the chain
                                        val allMatesInChain = mutableListOf<String>()
                                        allMatesInChain.add(player.name)

                                        player.mateIds.forEach { mateId ->
                                            val mateName = players.find { it.id == mateId }?.name ?: "Unknown"
                                            allMatesInChain.add(mateName)
                                        }

                                        allMatesInChain.forEach { name ->
                                            Text(
                                                "• $name",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "When ANY of you drink, EVERYONE drinks! 🍻",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }

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
                                    onClick = { onMateSelected(player.id) }
                                ) {
                                    Text("Select as Mate")
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
                                Log.d("MateSelector", "Random mate selection chosen")
                                val otherPlayers = players.filter { it.id != currentPlayerId }
                                if (otherPlayers.isNotEmpty()) {
                                    val randomPlayer = otherPlayers.random()
                                    onMateSelected(randomPlayer.id)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text("Select Random Mate")
                        }

                        players.forEach { player ->
                            if (player.id != currentPlayerId) {  // Don't show current player
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { expandedPlayerId = player.id }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                player.name,
                                                style = MaterialTheme.typography.titleSmall
                                            )

                                            if (player.mateIds.isNotEmpty()) {
                                                Text(
                                                    "${player.mateIds.size} mates",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // If player has mates, show a hint
                                        if (player.mateIds.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Has existing mate relationships",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
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