package com.benchopo.firering.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benchopo.firering.model.GameMode
import com.benchopo.firering.model.MiniGame
import com.benchopo.firering.model.MiniGameType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGameCreator(
    onGameCreated: (MiniGame) -> Unit,
    onCancel: () -> Unit,
    currentGameMode: GameMode = GameMode.NORMAL
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MiniGameType.CHALLENGE) }
    var isTitleError by remember { mutableStateOf(false) }
    var isDescriptionError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Create Custom Mini Game",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                isTitleError = it.isBlank()
            },
            label = { Text("Game Title") },
            modifier = Modifier.fillMaxWidth(),
            isError = isTitleError,
            supportingText = {
                if (isTitleError) {
                    Text("Title cannot be empty")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                isDescriptionError = it.isBlank()
            },
            label = { Text("Game Description") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            isError = isDescriptionError,
            supportingText = {
                if (isDescriptionError) {
                    Text("Description cannot be empty")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Game Type")
        Spacer(modifier = Modifier.height(8.dp))

        // Wrap in a scrollable Row to ensure all options are visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            MiniGameType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = {
                        // Properly format the type name for display
                        Text(
                            when(type) {
                                MiniGameType.CHALLENGE -> "Challenge"
                                MiniGameType.REACTION -> "Reaction"
                                MiniGameType.PHYSICAL -> "Physical"
                                MiniGameType.SELECTION -> "Selection"
                                // Handle any future additions with a fallback
                                else -> type.name.lowercase().capitalize()
                            }
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onCancel
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        isTitleError = true
                        return@Button
                    }
                    if (description.isBlank()) {
                        isDescriptionError = true
                        return@Button
                    }

                    // Create the custom game
                    val customGame = MiniGame(
                        id = "custom_game_${UUID.randomUUID()}",
                        title = title,
                        description = description,
                        type = selectedType,
                        gameMode = currentGameMode,
                        isCustom = true,
                        createdByPlayerId = null, // Will be set by the ViewModel
                        popularity = 0
                    )

                    onGameCreated(customGame)
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Create Game")
            }
        }
    }
}