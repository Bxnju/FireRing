package com.benchopo.firering.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.benchopo.firering.viewmodel.GameViewModel

@Composable
fun HomeScreen(navController: NavController, gameViewModel: GameViewModel = viewModel()) {
    var playerName by remember { mutableStateOf("") }

    val roomCode by gameViewModel.roomCode.collectAsState()
    val loading by gameViewModel.loading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to FireRing 🔥", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = playerName,
            onValueChange = { playerName = it },
            label = { Text("Enter your name") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (playerName.isNotBlank()) {
                    gameViewModel.createRoom(playerName)
                }
            },
            enabled = !loading && playerName.isNotBlank()
        ) {
            Text("Create Room")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = {
            // Aquí podrías implementar luego la lógica para unirse a sala
        }) {
            Text("Join Room")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (loading) {
            CircularProgressIndicator()
        }

        roomCode?.let {
            Text("Room code: $it", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
