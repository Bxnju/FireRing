package com.benchopo.firering.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.benchopo.firering.ui.components.GameModeSelector
import com.benchopo.firering.model.GameMode

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
    var clickCount by remember { mutableStateOf(0) }

    // Add state for selected game mode
    val selectedGameMode by gameViewModel.selectedGameMode.collectAsState()

    LaunchedEffect(Unit) {
        Log.d("CreateRoomScreen", "Entered CreateRoomScreen, resetting loading state")
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
                Text("Create Room ", style = MaterialTheme.typography.headlineLarge)

                Image(
                    painter = painterResource(id = if (clickCount > 5) R.drawable.ic_wine_cup else R.drawable.ic_wine_bottle),
                    contentDescription = "Wine Icon",
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

            Spacer(modifier = Modifier.height(24.dp))

            // Game Mode Selector
            GameModeSelector(
                selectedMode = selectedGameMode,
                onModeSelected = { gameViewModel.setGameMode(it) },
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
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent

                    ),
                contentPadding = PaddingValues()
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
                        Text("Create Room", color = Color.White)
                    }
                }
            }
        }
    }
}
