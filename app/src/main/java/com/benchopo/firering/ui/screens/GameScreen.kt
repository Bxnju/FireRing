package com.benchopo.firering.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.util.Log
import com.benchopo.firering.R
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.benchopo.firering.model.GameMode
import com.benchopo.firering.model.GameRoom
import com.benchopo.firering.model.Player
import com.benchopo.firering.ui.components.ActiveRuleDetailDialog
import com.benchopo.firering.ui.components.ActiveRulesSection
import com.benchopo.firering.ui.components.JackRuleSelector
import com.benchopo.firering.ui.components.MiniGameSelector
import com.benchopo.firering.ui.components.PlayerSelector
import com.benchopo.firering.ui.components.MateSelector
import kotlin.math.sqrt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    roomCode: String,
    gameViewModel: GameViewModel,
    userViewModel: UserViewModel,
) {
    //Easter egg smash bottle
    val context = LocalContext.current
    val shakeThreshold = 12f
    var lastShakeTime by remember { mutableLongStateOf(0L) }
    var shakeCount by remember { mutableIntStateOf(0) }
    val sensorManager =
        remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }

    // Easter egg smash bottle - Shake detection logic
    DisposableEffect(Unit) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

                    if (acceleration > shakeThreshold) {
                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastShakeTime > 500) {
                            shakeCount++
                            lastShakeTime = currentTime

                            if (shakeCount >= 4) {
                                mediaPlayer = MediaPlayer.create(context, R.raw.bottle_smash)
                                mediaPlayer?.start()
                                shakeCount = 0
                            }
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Add exit dialog state
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showCardHistoryModal by remember { mutableStateOf(false) }
    var showDrinkAlert by remember { mutableStateOf(false) }
    var lastDrinkMilestone by remember { mutableIntStateOf(-1) }

    // Handle system back button
    BackHandler {
        Log.d("GameScreen", "Back button pressed, showing leave dialog")
        showLeaveDialog = true
    }

    // Collect game state
    val gameRoom by gameViewModel.gameRoom.collectAsState()
    val currentPlayer by gameViewModel.currentPlayer.collectAsState()
    val playerId by gameViewModel.playerId.collectAsState()
    val drawnCard by gameViewModel.drawnCard.collectAsState()
    val loading by gameViewModel.loading.collectAsState()

    // Add these state collections for Jack Rules and Mini Games
    val showJackRuleSelector by gameViewModel.showJackRuleSelector.collectAsState(false)
    val showMiniGameSelector by gameViewModel.showMiniGameSelector.collectAsState(false)
    val jackRules by gameViewModel.jackRules.collectAsState(emptyList())
    val miniGames by gameViewModel.miniGames.collectAsState(emptyList())
    val selectedJackRule by gameViewModel.selectedJackRule.collectAsState()
    val selectedMiniGame by gameViewModel.selectedMiniGame.collectAsState()

    // Add selected drinker state collections
    val showPlayerSelector by gameViewModel.showPlayerSelector.collectAsState(false)
    val selectedDrinkerId by gameViewModel.selectedDrinkerId.collectAsState()
    val selectedDrinkerName by gameViewModel.selectedDrinkerName.collectAsState()

    // Add this state collection at the top of GameScreen with other state collections
    val showMateSelector by gameViewModel.showMateSelector.collectAsState(false)

    // Add to the GameScreen state collection
    val newMateRelationships by gameViewModel.newMateRelationships.collectAsState()

    // Determine if it's this player's turn
    val isCurrentPlayerTurn = remember(gameRoom, playerId) {
        gameRoom?.currentPlayerId == playerId
    }

    // Get the player whose turn it is
    val currentTurnPlayer = remember(gameRoom) {
        gameRoom?.currentPlayerId?.let { gameRoom?.players?.get(it) }
    }

    // Get all drawn cards in order
    val drawnCards = remember(gameRoom) {
        val cards =
            gameRoom?.drawnCards?.sortedByDescending { it.drawnTimestamp ?: 0 } ?: emptyList()
        Log.d("GameScreen", "Drawn cards from gameRoom: ${cards.size} cards")
        cards.forEachIndexed { index, card ->
            Log.d(
                "GameScreen",
                "  Card $index: ${card.value} of ${card.suit}, drawn by: ${card.drawnByPlayerId}, timestamp: ${card.drawnTimestamp}"
            )
        }
        cards
    }

    // Get card rule if a card is drawn
    val cardRule = remember(drawnCard) {
        drawnCard?.ruleDescription ?: "Draw a card to see the rule"
    }

    // Check if deck is empty
    val isGameOver = remember(gameRoom) {
        (gameRoom?.deck?.isEmpty() == true)
    }

    // Add this state variable near your other state declarations at the top of GameScreen
    var drawButtonDisabled by remember { mutableStateOf(false) }

    // Add this to your state collection in GameScreen
    val activeJackRules by gameViewModel.activeJackRules.collectAsState()
    val selectedActiveRule by gameViewModel.selectedActiveRule.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Room: $roomCode", style = MaterialTheme.typography.bodyMedium) },
                actions = {
                    // Add player info button
                    var showPlayerInfo by remember { mutableStateOf(false) }

                    IconButton(onClick = {
                        Log.d("GameScreen", "Player info button clicked, showing sidebar")
                        showPlayerInfo = true
                    }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_players),
                            contentDescription = "Players",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Card History button
                    IconButton(onClick = {
                        showCardHistoryModal = true
                    }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_card_history),
                            contentDescription = "Card History",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Exit button
                    IconButton(onClick = {
                        Log.d("GameScreen", "Exit button pressed, showing leave dialog")
                        showLeaveDialog = true
                    }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_leave),
                            contentDescription = "Leave Game",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Player info drawer
                    if (showPlayerInfo) {
                        PlayerInfoSidebar(
                            players = gameRoom?.players?.values?.toList() ?: emptyList(),
                            onDismiss = {
                                Log.d("GameScreen", "Closing player info sidebar")
                                showPlayerInfo = false
                            }
                        )
                    }

                    // Card History Modal
                    if (showCardHistoryModal) {
                        CardHistoryModal(
                            drawnCards = drawnCards,
                            onDismiss = {
                                showCardHistoryModal = false
                            },
                            gameRoom,
                        )
                    }

                    if (currentPlayer?.drinkCount?.rem(5) == 0
                        && currentPlayer?.drinkCount != 0
                        && currentPlayer?.drinkCount != lastDrinkMilestone
                    ) {
                        showDrinkAlert = true
                        lastDrinkMilestone = currentPlayer!!.drinkCount
                    }

                    if (showDrinkAlert) {
                        DrinksAlert(
                            onDismiss = {
                                showDrinkAlert = false
                            })
                    }

                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Turn indicator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Current Turn",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        currentTurnPlayer?.name ?: "Unknown",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isCurrentPlayerTurn) "It's your turn!" else "Wait for your turn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentPlayerTurn)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card display area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {

                Image(
                    painter = painterResource(id = R.drawable.ic_firering),
                    contentDescription = "Card Back",
                    modifier = Modifier.fillMaxWidth()
                )

                if (drawnCard == null) {
                    Card(
                        modifier = Modifier
                            .size(150.dp, 210.dp)
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Draw a Card",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .size(150.dp, 210.dp)
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                drawnCard?.value ?: "",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                drawnCard?.suit ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current rule display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "What this card means...",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        cardRule,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Active Rules Section
            if (activeJackRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        // Add count of active rules for debugging
                        Text(
                            "${activeJackRules.size} Active Rules",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        ActiveRulesSection(
                            activeRules = activeJackRules,
                            onRuleClick = { ruleId ->
                                gameViewModel.viewActiveRuleDetails(ruleId)
                            }
                        )
                    }
                }

                // Add debug text to see active rules in UI
                Log.d("GameScreen", "Active Jack Rules in GameScreen: ${activeJackRules.size}")
                activeJackRules.forEach { (id, rule) ->
                    Log.d("GameScreen", "Displaying active rule: ${rule.title}")
                }
            }

            // Player Drink Counter Section - Only current player

            // Find current player from the players list
            val myPlayer = remember(gameRoom, playerId) {
                val player = gameRoom?.players?.get(playerId)
                Log.d(
                    "GameScreen",
                    "Current player: ${player?.name}, drink count: ${player?.drinkCount}"
                )
                player
            }

            if (myPlayer != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your Drinks",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrement button (only enabled if count > 0)
                            FilledTonalIconButton(
                                onClick = {
                                    Log.d(
                                        "GameScreen",
                                        "Decrementing drink count for player: ${myPlayer.id}"
                                    )
                                    gameViewModel.updateDrinks(myPlayer.id, -1)
                                },
                                enabled = myPlayer.drinkCount > 0
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Decrease"
                                )
                            }

                            // Drink count display
                            Text(
                                "${myPlayer.drinkCount} 🍺",
                                style = MaterialTheme.typography.displaySmall
                            )

                            // Increment button
                            FilledTonalIconButton(
                                onClick = {
                                    Log.d(
                                        "GameScreen",
                                        "Incrementing drink count for player: ${myPlayer.id}"
                                    )
                                    gameViewModel.updateDrinks(myPlayer.id, 1)
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Player data not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Add after the player's drink counter section - only if player has mates
            if (myPlayer != null && myPlayer.mateIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your Drinking Mates",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Display mates in a simple column
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            myPlayer.mateIds.forEach { mateId ->
                                val mateName = gameRoom?.players?.get(mateId)?.name ?: "Unknown"
                                Text(
                                    "• $mateName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Add expiration information
                        val expiresAfterPlayerId = myPlayer.mateExpiresAfterPlayerId
                        if (expiresAfterPlayerId != null) {
                            val expiresAfterPlayerName =
                                gameRoom?.players?.get(expiresAfterPlayerId)?.name ?: "Unknown"
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "These mate relationships will end after ${expiresAfterPlayerName}'s next turn",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            "When you drink, they drink too!",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Draw card button
            Button(
                onClick = {
                    // Immediately disable button on click
                    drawButtonDisabled = true
                    gameViewModel.drawCard()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                // Add drawButtonDisabled to the enabled conditions
                enabled = isCurrentPlayerTurn && !loading && !isGameOver && !drawButtonDisabled
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isGameOver) "Game Over" else "Draw Card")
                }
            }

            // Add this LaunchedEffect to reset the button state when appropriate
            LaunchedEffect(isCurrentPlayerTurn, drawnCard) {
                if (!isCurrentPlayerTurn || drawnCard != null) {
                    drawButtonDisabled = false
                }
            }

            // Show game over banner if all cards are drawn
            if (isGameOver) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Game Complete!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "All cards have been drawn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Optional: Add button to return to lobby or start new game
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showLeaveDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("End Game")
                        }
                    }
                }
            }
        }
    }

    // Navigate back to home if the room is deleted or has no players
    LaunchedEffect(gameRoom) {
        // If the room was deleted or has no players, navigate back to home
        if (gameRoom == null || gameRoom?.players?.isEmpty() == true) {
            Log.d("GameScreen", "Room no longer exists or has no players, returning to home")
            gameViewModel.clearGameData()
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
            }
        }
    }

    // Force re-fetch game room data on first load
    LaunchedEffect(Unit) {
        Log.d("GameScreen", "Initial load - forcing refresh of room data")
        gameViewModel.ensureRoomLoaded(roomCode)
    }

    // Debug drawn cards data
    LaunchedEffect(gameRoom) {
        if (gameRoom != null) {
            Log.d(
                "GameScreen",
                "Room updated, drawn cards count: ${gameRoom?.drawnCards?.size ?: 0}"
            )
            gameRoom?.drawnCards?.forEach { card ->
                Log.d(
                    "GameScreen",
                    "Drawn card: ${card.value} of ${card.suit} by ${card.drawnByPlayerId}"
                )
            }
        }
    }

    // Add the leave confirmation dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Game") },
            text = { Text("Are you sure you want to leave this game?") },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("GameScreen", "Confirming leave game")
                        gameViewModel.leaveRoom {
                            // This is called after leave completes
                            Log.d("GameScreen", "Navigating to home")
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    }
                ) { Text("Leave", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Update the Jack Rule selection dialog
    if (showJackRuleSelector && isCurrentPlayerTurn) {
        Log.d("GameScreen", "Showing Jack Rule selector with ${jackRules.size} rules")
        JackRuleSelector(
            rules = jackRules,
            onRuleSelected = {
                Log.d("GameScreen", "Jack Rule selected: ${it.title}")
                gameViewModel.selectJackRule(it)
            },
            onCustomRuleCreated = {
                Log.d("GameScreen", "Custom Jack Rule created: ${it.title}")
                gameViewModel.createCustomJackRule(it)
            },
            onDismiss = {
                Log.d("GameScreen", "Jack Rule selection cancelled")
                gameViewModel.clearSelections()
            },
            currentGameMode = gameRoom?.gameMode ?: GameMode.NORMAL
        )
    }

    // Update the Mini Game selection dialog
    if (showMiniGameSelector && isCurrentPlayerTurn) {
        Log.d("GameScreen", "Showing Mini Game selector with ${miniGames.size} games")
        MiniGameSelector(
            games = miniGames,
            onGameSelected = {
                Log.d("GameScreen", "Mini Game selected: ${it.title}")
                gameViewModel.selectMiniGame(it)
            },
            onCustomGameCreated = {
                Log.d("GameScreen", "Custom Mini Game created: ${it.title}")
                gameViewModel.createCustomMiniGame(it)
            },
            onDismiss = {
                Log.d("GameScreen", "Mini Game selection cancelled")
                gameViewModel.clearSelections()
            },
            currentGameMode = gameRoom?.gameMode ?: GameMode.NORMAL
        )
    }

    // Add player selector dialog
    if (showPlayerSelector && isCurrentPlayerTurn) {
        Log.d("GameScreen", "Showing player selector")
        PlayerSelector(
            players = gameRoom?.players?.values?.toList() ?: emptyList(),
            currentPlayerId = playerId ?: "",
            onPlayerSelected = { id, name ->
                Log.d("GameScreen", "Player selected to drink: $name (ID: $id)")
                gameViewModel.selectPlayerToDrink(id, name)
            },
            onDismiss = {
                Log.d("GameScreen", "Player selection cancelled")
                gameViewModel.clearSelections()
            }
        )
    }

    // Add notification dialog for the player who was selected to drink
    if (selectedDrinkerId != null) {
        val isCurrentPlayerSelected = selectedDrinkerId == playerId
        val selectorName = remember(gameRoom, selectedDrinkerName) {
            gameRoom?.selectedDrinkerBy?.let { gameRoom?.players?.get(it)?.name } ?: "Someone"
        }

        Log.d(
            "GameScreen",
            "Showing selected player notification. Current player selected: $isCurrentPlayerSelected"
        )

        AlertDialog(
            onDismissRequest = {
                if (isCurrentPlayerTurn) {
                    Log.d("GameScreen", "Dismissing player selection and advancing turn")
                    gameViewModel.clearSelections()
                    gameViewModel.advanceTurn()
                }
            },
            title = { Text(if (isCurrentPlayerSelected) "You've Been Selected!" else "$selectedDrinkerName Has Been Selected") },
            text = {
                if (isCurrentPlayerSelected) {
                    Text("$selectorName has chosen you to drink. Bottoms up! 🍻")
                } else {
                    Text("$selectorName has chosen $selectedDrinkerName to drink.")
                }
            },
            confirmButton = {
                if (isCurrentPlayerTurn) {
                    Button(
                        onClick = {
                            Log.d("GameScreen", "Player selection confirmed, advancing turn")
                            gameViewModel.clearSelections()
                            gameViewModel.advanceTurn()
                        }
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = {}, enabled = false) {
                        Text("Waiting for ${currentTurnPlayer?.name}")
                    }
                }
            }
        )
    }

    // Show selected Jack Rule to all players
    if (selectedJackRule != null) {
        Log.d("GameScreen", "Displaying selected Jack Rule: ${selectedJackRule?.title}")
        AlertDialog(
            onDismissRequest = {
                if (isCurrentPlayerTurn) {
                    Log.d("GameScreen", "Dismissing Jack Rule and advancing turn")
                    gameViewModel.clearSelections()
                    gameViewModel.advanceTurn()
                }
            },
            title = { Text("Jack Rule: ${selectedJackRule?.title}") },
            text = { Text(selectedJackRule?.description ?: "") },
            confirmButton = {
                if (isCurrentPlayerTurn) {
                    Button(
                        onClick = {
                            Log.d("GameScreen", "Jack Rule completed, advancing turn")
                            gameViewModel.clearSelections()
                            gameViewModel.advanceTurn()
                        }
                    ) {
                        Text("Done")
                    }
                } else {
                    Button(onClick = {}, enabled = false) {
                        Text("Waiting for ${currentTurnPlayer?.name} to finish")
                    }
                }
            }
        )
    }

    // Show selected Mini Game to all players
    if (selectedMiniGame != null) {
        Log.d("GameScreen", "Displaying selected Mini Game: ${selectedMiniGame?.title}")
        AlertDialog(
            onDismissRequest = {
                if (isCurrentPlayerTurn) {
                    Log.d("GameScreen", "Dismissing Mini Game and advancing turn")
                    gameViewModel.clearSelections()
                    gameViewModel.advanceTurn()
                }
            },
            title = { Text("Mini Game: ${selectedMiniGame?.title}") },
            text = { Text(selectedMiniGame?.description ?: "") },
            confirmButton = {
                if (isCurrentPlayerTurn) {
                    Button(
                        onClick = {
                            Log.d("GameScreen", "Mini Game completed, advancing turn")
                            gameViewModel.clearSelections()
                            gameViewModel.advanceTurn()
                        }
                    ) {
                        Text("Done")
                    }
                } else {
                    Button(onClick = {}, enabled = false) {
                        Text("Waiting for ${currentTurnPlayer?.name} to finish")
                    }
                }
            }
        )
    }

    // Add mate selector dialog
    if (showMateSelector && isCurrentPlayerTurn) {
        Log.d("GameScreen", "Showing mate selector")
        MateSelector(
            players = gameRoom?.players?.values?.toList() ?: emptyList(),
            currentPlayerId = playerId ?: "",
            onMateSelected = { mateId ->
                Log.d("GameScreen", "Mate selected: $mateId")
                gameViewModel.selectMate(mateId)
            },
            onDismiss = {
                Log.d("GameScreen", "Mate selection cancelled")
                gameViewModel.clearSelections()
            }
        )
    }

    // Show mate notification when a player becomes someone's mate
    if (newMateRelationships.isNotEmpty() && drawnCard?.value == "8") {
        val mateSelectorName = remember(gameRoom) {
            gameRoom?.currentPlayerId?.let { gameRoom?.players?.get(it)?.name } ?: "Someone"
        }

        val mateNames = remember(gameRoom, newMateRelationships) {
            newMateRelationships.mapNotNull { mateId ->
                gameRoom?.players?.get(mateId)?.name
            }.joinToString(", ")
        }

        // Get expiration information
        val expiresAfterPlayerId = remember(gameRoom, playerId) {
            gameRoom?.players?.get(playerId)?.mateExpiresAfterPlayerId
        }
        val expiresAfterPlayerName = remember(gameRoom, expiresAfterPlayerId) {
            expiresAfterPlayerId?.let { gameRoom?.players?.get(it)?.name } ?: mateSelectorName
        }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("New Drinking Mates!") },
            text = {
                Column {
                    Text("$mateSelectorName has made you a drinking mate!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You're now connected with: $mateNames")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("When any of you drink, everyone in the chain drinks! 🍻")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "These mate relationships will end after $expiresAfterPlayerName's next turn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { }) {
                    Text("Cheers! 🍻")
                }
            }
        )
    }

    // Active Rule Detail Dialog
    if (selectedActiveRule != null) {
        ActiveRuleDetailDialog(
            rule = selectedActiveRule!!,
            onDismiss = {
                gameViewModel.closeActiveRuleDetails()
            }
        )
    }
}


@Composable
fun DrinksAlert(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row {
                Text("Drink Alert")
                Image(
                    painter = painterResource(id = R.drawable.ic_ron_glass),
                    contentDescription = "Ron Glass Icon",
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val messages = listOf(
                    "You're a heavy drinker!",
                    "Do you know your name?",
                    "Calling an ambulance!",
                    "Sobriety alert! (Or lack thereof).",
                    "Your liver just sent a vacation request.",
                    "Congratulations! You've unlocked the 'Bar Legend' level.",
                    "Mirror, mirror, who's the drunkest of them all?",
                    "Looks like someone needs some water... or a nap!",
                    "Careful! You might start speaking unknown languages.",
                    "The floor is calling you... for a nap.",
                    "You've reached the point of no return! (Or so it seems).",
                    "Remember how you got here? Neither do we.",
                    "Time for a break! Or a very large coffee.",
                    "Your decisions now will be tomorrow's anecdotes.",
                    "Warning! The fun level has exceeded the coherence level.",
                    "You unlocked 'Rhythm-less Dancer' mode!",
                    "Your friends will thank you tomorrow... or not.",
                    "It seems alcohol loves you more than you love yourself.",
                    "Houston, we have a sobriety problem!",
                    "Bravo! You've won the 'Most Committed Player' award.",
                    "Congratulations! You've earned the hangover of the century.",
                    "Your 'I don't care' level has reached its maximum.",
                    "Spoiler alert! Tomorrow your head will hurt.",
                    "Looks like the bar is going to charge you rent.",
                    "You've exceeded the speed limit on the alcohol highway!",
                    "Your neurons are on strike.",
                    "Alcohol has adopted you as its favorite child!",
                    "If you keep this up, dawn will catch you dancing alone.",
                    "You're one drink away from becoming an urban legend!",
                    "Your balance has decided to take the day off.",
                    "The glass is looking at you with envy!",
                    "Looks like you have a date with the toilet.",
                    "Drunk alert in the perimeter!",
                    "Your feet are dancing samba and you don't even know it.",
                    "You've reached alcoholic Nirvana!",
                    "The world looks better upside down, right?",
                    "Gravity is testing you!",
                    "Looks like the floor is your new best friend.",
                    "You're about to discover the meaning of 'everything's spinning'!",
                    "Your reasoning ability has been temporarily disabled."
                )

                val randomMessage = messages.random()

                Text(
                    text = randomMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp)
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

@Composable
fun CardHistoryModal(
    drawnCards: List<com.benchopo.firering.model.Card>,
    onDismiss: () -> Unit,
    gameRoom: GameRoom?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card History") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (drawnCards.isEmpty()) {
                    Text("No cards have been drawn yet.")
                } else {
                    drawnCards.forEachIndexed { index, card ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 1.dp,
                                color = Color.White
                            )
                        }

                        // Get player who drew this card
                        val playerName = remember(card, gameRoom) {
                            val name = card.drawnByPlayerId?.let { id ->
                                val playerName = gameRoom?.players?.get(id)?.name ?: "Unknown"
                                Log.d("GameScreen", "Card drawn by player: $id, name: $playerName")
                                playerName
                            } ?: "Unknown"
                            name
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${card.value} of ${card.suit} by $playerName",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun PlayerInfoSidebar(
    players: List<Player>,
    onDismiss: () -> Unit
) {
    Log.d("GameScreen", "Showing player info sidebar with ${players.size} players")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Players Info") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                players.forEachIndexed { index, player ->
                    Log.d(
                        "GameScreen",
                        "Rendering player info: ${player.name}, drinks: ${player.drinkCount}"
                    )

                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Player name with online status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (player.isConnected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                player.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (player.isHost) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "(Host)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Drink count
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Drinks: ${player.drinkCount} 🍺",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // What they're drinking (if selected)
                        player.selectedDrinkId?.let { drinkId ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Drinking: ${getDrinkName(drinkId)} ${getDrinkEmoji(drinkId)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Show mates
                        if (player.mateIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Drinking Mates:",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            player.mateIds.forEach { mateId ->
                                val mateName = players.find { it.id == mateId }?.name ?: "Unknown"
                                Text(
                                    "• $mateName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Helper functions for drink info
private fun getDrinkName(drinkId: String): String {
    return when (drinkId) {
        "beer" -> "Beer"
        "wine" -> "Wine"
        "whiskey" -> "Whiskey"
        "vodka" -> "Vodka"
        "water" -> "Water"
        else -> "Custom Drink"
    }
}

private fun getDrinkEmoji(drinkId: String): String {
    return when (drinkId) {
        "beer" -> "🍺"
        "wine" -> "🍷"
        "whiskey" -> "🥃"
        "vodka" -> "🥂"
        "water" -> "💧"
        else -> "🥤"
    }
}

// Card History Section
//            if (drawnCards.isNotEmpty()) {
//                Log.d("GameScreen", "Showing card history section with ${drawnCards.size} cards")
//                Spacer(modifier = Modifier.height(24.dp))
//
//                Text(
//                    "Card History",
//                    style = MaterialTheme.typography.titleMedium
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Show the last 3 drawn cards (excluding the current one)
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    // Skip the current card if it's already drawn
//                    val historyCards = if (drawnCard != null) {
//                        val filtered = drawnCards.filter { it.id != drawnCard?.id }.take(3)
//                        Log.d("GameScreen", "History cards (excluding current): ${filtered.size} cards")
//                        filtered
//                    } else {
//                        val taken = drawnCards.take(3)
//                        Log.d("GameScreen", "History cards (no current card): ${taken.size} cards")
//                        taken
//                    }
//
//                    historyCards.forEach() { historyCard ->
//                        Log.d("GameScreen", "Rendering history card: ${historyCard.value} of ${historyCard.suit}")
//                        Card(
//                            modifier = Modifier
//                                .size(80.dp, 120.dp)
//                                .padding(4.dp)
//                        ) {
//                            Column(
//                                modifier = Modifier.fillMaxSize().padding(8.dp),
//                                horizontalAlignment = Alignment.CenterHorizontally,
//                                verticalArrangement = Arrangement.Center
//                            ) {
//                                Text(
//                                    historyCard.value,
//                                    style = MaterialTheme.typography.titleLarge
//                                )
//                                Text(
//                                    historyCard.suit,
//                                    style = MaterialTheme.typography.bodySmall
//                                )
//
//                                // Get player who drew this card
//                                val playerName = remember(historyCard, gameRoom) {
//                                    val name = historyCard.drawnByPlayerId?.let { id ->
//                                        val playerName = gameRoom?.players?.get(id)?.name ?: "Unknown"
//                                        Log.d("GameScreen", "Card drawn by player: $id, name: $playerName")
//                                        playerName
//                                    } ?: "Unknown"
//                                    name
//                                }
//
//                                Text(
//                                    "by $playerName",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    fontSize = 8.sp
//                                )
//                            }
//                        }
//                    }
//                }
//            } else {
//                Log.d("GameScreen", "No card history to show - drawnCards is empty")
//            }