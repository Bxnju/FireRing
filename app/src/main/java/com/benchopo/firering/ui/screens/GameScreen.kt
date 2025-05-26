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
        var currentRule by remember { mutableStateOf("Draw a card to start playing!") }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Game Room: $roomCode") },
                                actions = {
                                        IconButton(
                                                onClick = {
                                                        navController.navigate(Routes.HOME) {
                                                                popUpTo(Routes.HOME) {
                                                                        inclusive = true
                                                                }
                                                        }
                                                }
                                        ) { Text("Exit") }
                                }
                        )
                }
        ) { paddingValues ->
                Column(
                        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // Card display area
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        "Cards will appear here",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                )
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
                                                currentRule,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Draw card button
                        Button(
                                onClick = { currentRule = "You drew a card! Follow the rule..." },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) { Text("Draw Card") }
                }
        }
}
