package com.benchopo.firering.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benchopo.firering.model.JackRule
import com.benchopo.firering.model.RuleType
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.tooling.preview.Preview
import com.benchopo.firering.model.GameMode

@Composable
fun JackRuleSelector(
    rules: List<JackRule>,
    onRuleSelected: (JackRule) -> Unit,
    onCustomRuleCreated: (JackRule) -> Unit,
    onDismiss: () -> Unit,
    currentGameMode: GameMode = GameMode.NORMAL
) {
    var expandedRuleId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        // Select random rule if back button is pressed
        if (rules.isNotEmpty()) {
            val randomRule = rules.random()
            onRuleSelected(randomRule)
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            // Select random rule if dialog is dismissed
            if (rules.isNotEmpty()) {
                val randomRule = rules.random()
                onRuleSelected(randomRule)
            } else {
                onDismiss()
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select a Jack Rule")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "(You must select a rule to continue)",
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
                if (expandedRuleId == "create_custom") {
                    CustomRuleCreator(
                        onRuleCreated = { newRule ->
                            onCustomRuleCreated(newRule)
                            onRuleSelected(newRule)
                        },
                        onCancel = { expandedRuleId = null },
                        currentGameMode = currentGameMode
                    )
                } else if (expandedRuleId != null) {
                    // Show expanded rule details
                    val rule = rules.find { it.id == expandedRuleId }
                    if (rule != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                rule.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                rule.description,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { expandedRuleId = null }
                                ) {
                                    Text("Back")
                                }

                                Button(
                                    onClick = { onRuleSelected(rule) }
                                ) {
                                    Text("Select")
                                }
                            }
                        }
                    }
                } else {
                    // Show grid of rules
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Add Random selection button at top
                        Button(
                            onClick = {
                                val randomRule = rules.random()
                                onRuleSelected(randomRule)
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text("Select Random Rule")
                        }

                        // Move the Create Custom Rule button INSIDE the scrollable Column
                        Button(
                            onClick = { expandedRuleId = "create_custom" },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Create Custom Rule")
                        }

                        rules.forEach { rule ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { expandedRuleId = rule.id }
                            ) {
                                Text(
                                    rule.title,
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

@Preview(showBackground = true)
@Composable
fun JackRuleSelectorPreview() {
    // Use MaterialTheme instead of FireringTheme
    MaterialTheme {
        val sampleRules = List(5) { index ->
            JackRule(
                id = "rule_$index",
                title = "Rule $index",
                description = "Description for rule $index",
                type = RuleType.STANDARD,
                gameMode = GameMode.NORMAL
            )
        }

        var selectedRule by remember { mutableStateOf<JackRule?>(null) }

        JackRuleSelector(
            rules = sampleRules,
            onRuleSelected = { rule ->
                selectedRule = rule
            },
            onCustomRuleCreated = { /* Handle custom rule creation */ },
            onDismiss = {}
        )
    }
}