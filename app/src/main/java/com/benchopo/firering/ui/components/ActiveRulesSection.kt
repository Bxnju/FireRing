package com.benchopo.firering.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.benchopo.firering.model.ActiveJackRule
import com.benchopo.firering.model.RuleType

@Composable
fun ActiveRulesSection(
    activeRules: Map<String, ActiveJackRule>,
    onRuleClick: (String) -> Unit
) {
    if (activeRules.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            "Active Jack Rules",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(activeRules.values.toList()) { rule ->
                ActiveRuleCard(rule = rule, onClick = { onRuleClick(rule.id) })
            }
        }
    }
}

@Composable
fun ActiveRuleCard(
    rule: ActiveJackRule,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when (rule.type) {
                RuleType.PHYSICAL -> MaterialTheme.colorScheme.tertiaryContainer
                RuleType.CUSTOM -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                rule.title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "by ${rule.createdByPlayerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Tap for details",
                style = MaterialTheme.typography.bodySmall,
                fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.8,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ActiveRuleDetailDialog(
    rule: ActiveJackRule,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(rule.title) },
        text = {
            Column {
                Text(rule.description)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Created by: ${rule.createdByPlayerName}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "This rule is active until the creator's next turn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}