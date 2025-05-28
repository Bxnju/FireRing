package com.benchopo.firering.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benchopo.firering.model.GameMode
import com.benchopo.firering.model.JackRule
import com.benchopo.firering.model.RuleType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRuleCreator(
    onRuleCreated: (JackRule) -> Unit,
    onCancel: () -> Unit,
    currentGameMode: GameMode = GameMode.NORMAL
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(RuleType.STANDARD) }
    var isTitleError by remember { mutableStateOf(false) }
    var isDescriptionError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Create Custom Rule",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                isTitleError = it.isBlank()
            },
            label = { Text("Rule Title") },
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
            label = { Text("Rule Description") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            isError = isDescriptionError,
            supportingText = {
                if (isDescriptionError) {
                    Text("Description cannot be empty")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Rule Type")
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            RuleType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.name.lowercase().capitalize()) },
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

                    // Create the custom rule
                    val customRule = JackRule(
                        id = "custom_rule_${UUID.randomUUID()}",
                        title = title,
                        description = description,
                        type = selectedType,
                        gameMode = currentGameMode,
                        isCustom = true,
                        createdByPlayerId = null, // Will be set by the ViewModel
                        popularity = 0
                    )

                    onRuleCreated(customRule)
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Create Rule")
            }
        }
    }
}