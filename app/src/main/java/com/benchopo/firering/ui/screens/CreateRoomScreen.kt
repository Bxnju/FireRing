package com.benchopo.firering.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benchopo.firering.R
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

    LaunchedEffect(Unit) {
        Log.d("CreateRoomScreen", "Entered CreateRoomScreen, resetting loading state")
        gameViewModel.resetLoadingState()
        gameViewModel.clearError()
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("FireRing") },
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
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
            )

            Row {
                Text("Create Room ", style = MaterialTheme.typography.displayMedium)

                Image(
                        painter = painterResource(id = R.drawable.ic_beer_bottle),
                        contentDescription = "Beer Icon",
                        modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                            gameViewModel.createRoom(playerName) {
                                // This will only run after room creation is complete
                                roomCode?.let { code ->
                                    navController.navigate(
                                            Routes.LOBBY.replace("{roomCode}", code)
                                    ) { popUpTo(Routes.HOME) }
                                }
                            }
                        }
                    },
                    enabled = !loading && playerName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent // Hacemos el fondo del botón
                                    // transparente
                                    ),
                    contentPadding = PaddingValues() // Quitamos el padding interno del botón
            ) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                brush =
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color(0xFFFF9800),
                                                                                Color(0xFFFF5722)
                                                                        )
                                                        ),
                                                shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Room", color = Color.White)
                    }
                }
            }
        }
    }
}
