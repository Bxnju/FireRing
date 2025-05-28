package com.benchopo.firering.ui.screens

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benchopo.firering.R
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
    val context = LocalContext.current
    var clickCount by remember { mutableIntStateOf(0) }

    var playerName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }

    val loading by gameViewModel.loading.collectAsState()
    val error by gameViewModel.error.collectAsState()
    val joinedRoomCode by gameViewModel.roomCode.collectAsState()

    LaunchedEffect(Unit) {
        Log.d("JoinRoomScreen", "Entered JoinRoomScreen, resetting loading state")
        gameViewModel.resetLoadingState()
        gameViewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("FireRing ")

                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(35.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF000000)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Join Room ", style = MaterialTheme.typography.headlineLarge)

                Image(
                    painter = painterResource(id = if (clickCount > 5) R.drawable.ic_beer else R.drawable.ic_barrel),
                    contentDescription = "Beer Icon",
                    modifier = Modifier.size(46.dp)
                        .clickable (
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            clickCount++
                            if (clickCount >= 10) {
                                clickCount = 0
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

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
                        gameViewModel.joinRoom(roomCode, playerName) {
                            // This will only run after room join is complete
                            joinedRoomCode?.let { code ->
                                navController.navigate(
                                    Routes.LOBBY.replace("{roomCode}", code)
                                ) { popUpTo(Routes.HOME) }
                            }
                        }
                    }
                },
                enabled = !loading && playerName.isNotBlank() && roomCode.length == 5,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent // Hacemos el fondo del botón
                        // transparente
                    ),
                contentPadding = PaddingValues() // Quitamos el padding interno del botón
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
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
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Join Room", color = Color.White)

                            Image(
                                painter = painterResource(id = R.drawable.ic_coctel),
                                contentDescription = "Coctel Icon",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                    }
                }
            }
        }
    }
}
