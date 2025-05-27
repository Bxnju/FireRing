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
fun CreateRoomScreen(
        navController: NavController,
        userViewModel: UserViewModel,
        gameViewModel: GameViewModel
) {
    var playerName by remember { mutableStateOf("") }

    val roomCode by gameViewModel.roomCode.collectAsState()
    val loading by gameViewModel.loading.collectAsState()

    // Navigate to lobby when room is created
    LaunchedEffect(roomCode) {
        roomCode?.let {
            navController.navigate(Routes.LOBBY.replace("{roomCode}", it)) { popUpTo(Routes.HOME) }
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Create Room") },
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                    onClick = {
                        if (playerName.isNotBlank()) {
                            gameViewModel.createRoom(playerName)
                        }
                    },
                    enabled = !loading && playerName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Room")
                }
            }
        }
    }
}
