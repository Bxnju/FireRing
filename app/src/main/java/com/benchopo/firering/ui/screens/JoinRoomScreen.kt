package com.benchopo.firering.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benchopo.firering.navigation.Routes
import com.benchopo.firering.viewmodel.GameViewModel
import com.benchopo.firering.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
        navController: NavController,
        userViewModel: UserViewModel,
        gameViewModel: GameViewModel
) {
    var playerName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }

    val loading by gameViewModel.loading.collectAsState()
    val error by gameViewModel.error.collectAsState()
    val joinedRoomCode by gameViewModel.roomCode.collectAsState()

    // Navigate to lobby when room is joined
    LaunchedEffect(joinedRoomCode) {
        joinedRoomCode?.let {
            navController.navigate(Routes.LOBBY.replace("{roomCode}", it)) { popUpTo(Routes.HOME) }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Join Room") },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                    value = roomCode,
                    onValueChange = {
                        // Convert to uppercase and limit to 5 chars
                        roomCode = it.uppercase().take(5)
                    },
                    label = { Text("Room Code") },
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (error != null) {
                Text(
                        error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                    onClick = {
                        if (playerName.isNotBlank() && roomCode.isNotBlank()) {
                            gameViewModel.joinRoom(roomCode, playerName)
                        }
                    },
                    enabled = !loading && playerName.isNotBlank() && roomCode.length == 5,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                    )
                } else {
                    Text("Join Room")
                }
            }
        }
    }
}
