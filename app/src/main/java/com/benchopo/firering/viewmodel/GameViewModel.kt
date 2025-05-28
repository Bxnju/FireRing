package com.benchopo.firering.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benchopo.firering.data.FirebaseRepository
import com.benchopo.firering.data.GameRulesProvider
import com.benchopo.firering.model.*
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class GameViewModel(private val userViewModel: UserViewModel) : ViewModel() {
    private val repository = FirebaseRepository()

    private var mateSelectionInProgress = false

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode

    private val _playerId = MutableStateFlow<String?>(null)
    val playerId: StateFlow<String?> = _playerId

    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
    val gameRoom: StateFlow<GameRoom?> = _gameRoom

    private val _currentPlayer = MutableStateFlow<Player?>(null)
    val currentPlayer: StateFlow<Player?> = _currentPlayer

    private val _drawnCard = MutableStateFlow<Card?>(null)
    val drawnCard: StateFlow<Card?> = _drawnCard

    // State for Jack Rules and Mini Games
    private val _jackRules = MutableStateFlow<List<JackRule>>(emptyList())
    val jackRules: StateFlow<List<JackRule>> = _jackRules

    private val _miniGames = MutableStateFlow<List<MiniGame>>(emptyList())
    val miniGames: StateFlow<List<MiniGame>> = _miniGames

    // Selected rule/game
    private val _selectedJackRule = MutableStateFlow<JackRule?>(null)
    val selectedJackRule: StateFlow<JackRule?> = _selectedJackRule

    private val _selectedMiniGame = MutableStateFlow<MiniGame?>(null)
    val selectedMiniGame: StateFlow<MiniGame?> = _selectedMiniGame

    // State for UI showing
    private val _showJackRuleSelector = MutableStateFlow(false)
    val showJackRuleSelector: StateFlow<Boolean> = _showJackRuleSelector

    private val _showMiniGameSelector = MutableStateFlow(false)
    val showMiniGameSelector: StateFlow<Boolean> = _showMiniGameSelector

    // State for player selection (Card 2)
    private val _showPlayerSelector = MutableStateFlow(false)
    val showPlayerSelector: StateFlow<Boolean> = _showPlayerSelector

    // State for mate selection (Card 8)
    private val _showMateSelector = MutableStateFlow(false)
    val showMateSelector: StateFlow<Boolean> = _showMateSelector

    private val _selectedDrinkerId = MutableStateFlow<String?>(null)
    val selectedDrinkerId: StateFlow<String?> = _selectedDrinkerId

    private val _selectedDrinkerName = MutableStateFlow<String?>(null)
    val selectedDrinkerName: StateFlow<String?> = _selectedDrinkerName

    private val _newMateRelationships = MutableStateFlow<Set<String>>(emptySet())
    val newMateRelationships: StateFlow<Set<String>> = _newMateRelationships

    // Add these properties to your GameViewModel class
    private val _activeJackRules = MutableStateFlow<Map<String, ActiveJackRule>>(emptyMap())
    val activeJackRules: StateFlow<Map<String, ActiveJackRule>> = _activeJackRules

    // Add a selected active rule state for the detail view
    private val _selectedActiveRule = MutableStateFlow<ActiveJackRule?>(null)
    val selectedActiveRule: StateFlow<ActiveJackRule?> = _selectedActiveRule


    private var activeRoomJob: Job? = null

    // New properties for game mode
    private val _selectedGameMode = MutableStateFlow(GameMode.NORMAL)
    val selectedGameMode: StateFlow<GameMode> = _selectedGameMode

    // Called when creating a new room
    fun createRoom(hostPlayerName: String, onComplete: () -> Unit = {}) {
        Log.d("GameViewModel", "Creating room with host: $hostPlayerName, mode: ${_selectedGameMode.value}")
        _loading.value = true
        viewModelScope.launch {
            try {
                // 1. Generate player ID
                val userId = UUID.randomUUID().toString()
                Log.d("GameViewModel", "Generated user ID: $userId")

                // 2. Create the room in Firebase with the selected game mode
                val code = repository.createGameRoom(hostPlayerName, userId, _selectedGameMode.value)
                Log.d("GameViewModel", "Room created successfully with code: $code and mode: ${_selectedGameMode.value}")

                // 3. Set local state
                _playerId.value = userId
                _roomCode.value = code
                userViewModel.setUserInfo(userId, hostPlayerName)
                Log.d("GameViewModel", "Local state updated with room code and player ID")

                // 4. Get initial room data once (not as a continuous flow)
                val initialRoom = repository.getRoomOnce(code)
                if (initialRoom != null) {
                    Log.d(
                            "GameViewModel",
                            "Initial room data loaded: ${initialRoom.players.size} players"
                    )
                    _gameRoom.value = initialRoom
                    _currentPlayer.value = initialRoom.players[userId]
                } else {
                    Log.w("GameViewModel", "Initial room data is null")
                }

                // 5. Now we can safely navigate
                Log.d("GameViewModel", "Calling completion callback")
                onComplete()

                // 6. Start continuous flow collection AFTER navigation
                viewModelScope.launch {
                    Log.d("GameViewModel", "Starting room data collection for code: $code")
                    repository.getRoom(code).collect { room ->
                        if (room != null) {
                            Log.d("GameViewModel", "Room update received: ${room.players.size} players")
                            _gameRoom.value = room
                            _playerId.value?.let { pid ->
                                _currentPlayer.value = room.players[pid]
                                Log.d("GameViewModel", "Current player updated: ${room.players[pid]?.name}")
                            }
                            updateCurrentCardFromRoom(room) // Add this line
                        } else {
                            Log.w("GameViewModel", "Received null room in data collection")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to create room", e)
                _error.value = "Failed to create room: ${e.message}"
            } finally {
                Log.d("GameViewModel", "Create room process completed, loading = false")
                _loading.value = false
            }
        }
    }

    // Called when joining an existing room
    fun joinRoom(roomCode: String, playerName: String, onComplete: () -> Unit = {}) {
        Log.d("GameViewModel", "Joining room: $roomCode with player name: $playerName")
        _loading.value = true
        viewModelScope.launch {
            try {
                val playerId = repository.joinRoom(roomCode, playerName)
                if (playerId != null) {
                    Log.d("GameViewModel", "Successfully joined room. Player ID: $playerId")
                    _roomCode.value = roomCode
                    _playerId.value = playerId

                    // Get initial room data synchronously before navigation
                    val initialRoom = repository.getRoomOnce(roomCode)
                    if (initialRoom != null) {
                        Log.d(
                                "GameViewModel",
                                "Initial room data loaded: ${initialRoom.players.size} players"
                        )
                        _gameRoom.value = initialRoom
                        _currentPlayer.value = initialRoom.players[playerId]
                    } else {
                        Log.w("GameViewModel", "Initial room data is null")
                    }

                    // Call completion callback to trigger navigation
                    onComplete()

                    // Start collecting room updates after navigation
                    activeRoomJob =
                            viewModelScope.launch {
                                Log.d("GameViewModel", "Starting to collect room updates")
                                repository.getRoom(roomCode).collect { room ->
                                    if (room != null) {
                                        Log.d("GameViewModel", "Room update received: ${room.players.size} players")
                                        _gameRoom.value = room
                                        _currentPlayer.value = room.players[playerId]
                                        updateCurrentCardFromRoom(room) // Add this line
                                    } else {
                                        Log.w("GameViewModel", "Received null room data in join flow")
                                    }
                                }
                            }
                } else {
                    Log.w("GameViewModel", "Failed to join room - null player ID returned")
                    _error.value = "Room not found or game already started"
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Exception joining room", e)
                _error.value = "Failed to join room: ${e.message}"
            } finally {
                Log.d("GameViewModel", "Join room process completed, loading = false")
                _loading.value = false
            }
        }
    }

    // Called when host starts the game
    fun startGame() {
        val roomCode = _roomCode.value ?: return
        _loading.value = true
        viewModelScope.launch {
            try {
                repository.startGame(roomCode)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to start game: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Called when player draws a card
    open fun drawCard() {
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return
        val room = _gameRoom.value ?: return

        // Verify it's this player's turn
        if (room.currentPlayerId != playerId) {
            _error.value = "It's not your turn!"
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                val card = repository.drawCard(roomCode, playerId)
                _drawnCard.value = card

                // Check for special cards
                when (card?.value) {
                    "2" -> {
                        // Show player selector for choosing who drinks
                        _showPlayerSelector.value = true
                    }
                    "8" -> {
                        // Show mate selector
                        _showMateSelector.value = true
                    }
                    "J" -> {
                        // FIXED: Load rules first, then show selector
                        loadRulesAndGames(_gameRoom.value?.gameMode ?: GameMode.NORMAL, onLoadComplete = {
                            // Only show selector after rules are loaded
                            _showJackRuleSelector.value = true
                        })
                    }
                    "10" -> {
                        // FIXED: Load games first, then show selector
                        loadRulesAndGames(_gameRoom.value?.gameMode ?: GameMode.NORMAL, onLoadComplete = {
                            // Only show selector after games are loaded
                            _showMiniGameSelector.value = true
                        })
                    }
                    else -> {
                        // For non-special cards, auto-advance the turn
                        if (card != null && !isSpecialCard(card.value)) {
                            viewModelScope.launch {
                                repository.advanceTurn(roomCode)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to draw card", e)
                _error.value = "Failed to draw card: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Replace getCurrentCardRule with this simpler version
    fun getCurrentCardRule(): String {
        return _drawnCard.value?.ruleDescription ?: "Draw a card to see the rule"
    }

    // Called after completing card actions to advance turn
    fun advanceTurn() {
        val roomCode = _roomCode.value ?: return
        viewModelScope.launch {
            try {
                repository.advanceTurn(roomCode)
                // Remove this line to keep the card visible:
                // _drawnCard.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to advance turn: ${e.message}"
            }
        }
    }

    // Handle drink updates
    fun updateDrinks(playerId: String, increment: Int = 1) {
        val roomCode = _roomCode.value ?: return
        viewModelScope.launch {
            try {
                repository.updatePlayerDrinkCount(roomCode, playerId, increment)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to update drinks: ${e.message}"
            }
        }
    }

    // Set player as mate
    fun setMate(mateId: String) {
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return
        viewModelScope.launch {
            try {
                repository.setPlayerMate(roomCode, playerId, mateId)
                repository.setPlayerMate(roomCode, mateId, playerId)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to set mate: ${e.message}"
            }
        }
    }

    // Add custom rule
    fun addRule(title: String, description: String) {
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return
        val ruleId = "rule_${System.currentTimeMillis()}"

        val activeRule =
                ActiveRule(
                        id = ruleId,
                        title = title,
                        description = description,
                        createdByPlayerId = playerId,
                        createdAt = System.currentTimeMillis(),
                        expiresAfterPlayerId = playerId,
                        ruleType = RuleType.CUSTOM
                )

        viewModelScope.launch {
            try {
                repository.addActiveRule(roomCode, activeRule)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to add rule: ${e.message}"
            }
        }
    }

    // Check if card requires special handling
    private fun isSpecialCard(cardValue: String): Boolean {
        return cardValue in listOf("2", "8", "J", "10")
    }

    // Clear error message
    fun clearError() {
        _error.value = null
    }

    // Add this method to handle setting player online/offline
    fun setPlayerOnlineStatus(isOnline: Boolean) {
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return

        viewModelScope.launch {
            try {
                repository.setPlayerOnlineStatus(roomCode, playerId, isOnline)
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to update player status: ${e.message}"
            }
        }
    }

    // Add lifecycle handling in init
    init {
        // Set player online when ViewModel is created
        viewModelScope.launch {
            // We'll use this for setting online status when appropriate
        }
    }

    // Add cleanup in onCleared
    override fun onCleared() {
        super.onCleared()
        // Set player offline when ViewModel is cleared
        _roomCode.value?.let { roomCode ->
            _playerId.value?.let { playerId ->
                viewModelScope.launch {
                    try {
                        repository.setPlayerOnlineStatus(roomCode, playerId, false)
                    } catch (e: Exception) {
                        // Ignore errors during cleanup
                    }
                }
            }
        }
    }

    // Add a method to leave the room

    fun leaveRoom(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val roomCode = _roomCode.value
                val playerId = _playerId.value

                if (roomCode != null && playerId != null) {
                    // Handle special cards auto-selection
                    if ((_showJackRuleSelector.value || _showMiniGameSelector.value) &&
                        gameRoom.value?.currentPlayerId == playerId) {

                        Log.d("GameViewModel", "Player leaving during selection, auto-selecting random rule/game")

                        if (_showJackRuleSelector.value && _jackRules.value.isNotEmpty()) {
                            val randomRule = _jackRules.value.random()
                            selectJackRule(randomRule)
                        }

                        if (_showMiniGameSelector.value && _miniGames.value.isNotEmpty()) {
                            val randomGame = _miniGames.value.random()
                            selectMiniGame(randomGame)
                        }

                        advanceTurn()
                    }

                    // Use leaveRoom instead of removePlayerFromRoom
                    repository.leaveRoom(roomCode, playerId)
                }

                clearGameData()
                _loading.value = false
                onComplete()
            } catch (e: Exception) {
                _error.value = "Failed to leave room: ${e.message}"
                _loading.value = false
            }
        }
    }

    // Add this new method to help with loading room data

    fun loadRoom(roomCode: String) {
        if (_roomCode.value != roomCode) {
            _roomCode.value = roomCode
        }

        // Cancel any existing room subscription
        activeRoomJob?.cancel()

        activeRoomJob =
                viewModelScope.launch {
                    try {
                        // Start collecting room updates
                        repository.getRoom(roomCode).collect { room ->
                            Log.d("GameViewModel", "Room update: $room")
                            if (room != null) {
                                _gameRoom.value = room
                                // Update current player if we have playerId
                                _playerId.value?.let { pid ->
                                    _currentPlayer.value = room.players[pid]
                                }
                                updateCurrentCardFromRoom(room) // Add this line
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("GameViewModel", "Error loading room", e)
                        _error.value = "Failed to load room: ${e.message}"
                    }
                }
    }

    // Ensure room is loaded
    fun ensureRoomLoaded(roomCode: String) {
        Log.d(
                "GameViewModel",
                "Ensuring room is loaded: $roomCode, current room: ${_roomCode.value}"
        )

        if (_roomCode.value != roomCode) {
            Log.d("GameViewModel", "Room code changed, updating to: $roomCode")
            _roomCode.value = roomCode
            _gameRoom.value = null
        }

        if (_gameRoom.value == null) {
            Log.d("GameViewModel", "Game room is null, starting new data collection")
            activeRoomJob?.cancel()
            Log.d("GameViewModel", "Previous room job cancelled")

            activeRoomJob =
                    viewModelScope.launch {
                        try {
                            Log.d("GameViewModel", "Starting to collect room data for: $roomCode")
                            repository.getRoom(roomCode).collect { room ->
                                if (room != null) {
                                    Log.d(
                                            "GameViewModel",
                                            "Room data received: ${room.roomCode}, players: ${room.players.size}, hostId: ${room.hostId}"
                                    )
                                    _gameRoom.value = room

                                    _playerId.value?.let { pid ->
                                        val player = room.players[pid]
                                        Log.d(
                                                "GameViewModel",
                                                "Current player update - ID: $pid, Player: ${player?.name}, IsHost: ${player?.isHost}"
                                        )
                                        _currentPlayer.value = player
                                    }
                                } else {
                                    Log.w(
                                            "GameViewModel",
                                            "Received null room data in ensureRoomLoaded"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("GameViewModel", "Error loading room", e)
                            _error.value = "Failed to load room: ${e.message}"
                        }
                    }
        } else {
            Log.d("GameViewModel", "Game room already loaded, skipping data collection")
        }
    }

    // Clear game data
    fun clearGameData() {
        Log.d("GameViewModel", "Clearing all game data")

        // Cancel any active room subscription first
        activeRoomJob?.cancel()
        activeRoomJob = null

        // Clear all state variables
        _roomCode.value = null
        _playerId.value = null
        _gameRoom.value = null
        _currentPlayer.value = null
        _drawnCard.value = null
        _error.value = null
        _loading.value = false

        Log.d("GameViewModel", "Game data cleared")
    }

    fun resetLoadingState() {
        _loading.value = false
    }

    // Add this public method to set player ID
    fun setPlayerId(id: String?) {
        _playerId.value = id
    }

    // Add this function to synchronize the current card for all players
    private fun updateCurrentCardFromRoom(gameRoom: GameRoom) {
        // Update active Jack Rules from the game room
        _activeJackRules.value = gameRoom.activeJackRules

        // If there's a current card ID but our local drawnCard doesn't match it,
        // find the card in the game room's drawn cards
        val currentCardId = gameRoom.currentCardId
        if (currentCardId != null && (_drawnCard.value == null || _drawnCard.value?.id != currentCardId)) {
            Log.d("GameViewModel", "Updating current card to match room's currentCardId: $currentCardId")

            // Find the card in the drawn cards list
            val currentCard = gameRoom.drawnCards.find { it.id == currentCardId }

            if (currentCard != null) {
                Log.d("GameViewModel", "Found current card: ${currentCard.value} of ${currentCard.suit}")
                _drawnCard.value = currentCard
            } else {
                Log.d("GameViewModel", "Could not find card with ID: $currentCardId in drawn cards")
            }
        }

        // Check for Jack Rule selection by any player
        if (gameRoom.currentJackRuleId != null && _selectedJackRule.value == null) {
            // A Jack Rule was selected, but we don't have it locally - load it
            Log.d("GameViewModel", "Jack Rule selected by another player, loading rule details")

            // Find the selected rule in our local list
            val rule = _jackRules.value.find { it.id == gameRoom.currentJackRuleId }
            if (rule != null) {
                Log.d("GameViewModel", "Found selected Jack Rule: ${rule.title}")
                _selectedJackRule.value = rule
            } else {
                // Rule not found locally - might be a custom rule created by another player
                // Load it directly from Firebase
                Log.d("GameViewModel", "Rule not found locally. Loading from Firebase: ${gameRoom.currentJackRuleId}")
                viewModelScope.launch {
                    try {
                        val roomCode = _roomCode.value ?: return@launch
                        val customRule = repository.getCustomJackRule(roomCode, gameRoom.currentJackRuleId)
                        if (customRule != null) {
                            Log.d("GameViewModel", "Loaded custom rule from Firebase: ${customRule.title}")

                            // Add to local list for future reference
                            val updatedRules = _jackRules.value.toMutableList()
                            updatedRules.add(customRule)
                            _jackRules.value = updatedRules

                            // Update selected rule
                            _selectedJackRule.value = customRule
                        }
                    } catch (e: Exception) {
                        Log.e("GameViewModel", "Error loading custom rule", e)
                    }
                }
            }
        } else if (gameRoom.currentJackRuleId == null && _selectedJackRule.value != null) {
            // The Jack Rule was cleared in the database, clear it locally too
            Log.d("GameViewModel", "Jack Rule was cleared, updating local state")
            _selectedJackRule.value = null
        }

        // Check for Mini Game selection by any player
        if (gameRoom.currentMiniGameId != null && _selectedMiniGame.value == null) {
            // A Mini Game was selected, but we don't have it locally - load it
            Log.d("GameViewModel", "Mini Game selected by another player, loading game details")

            // Find the selected game in our local list
            val game = _miniGames.value.find { it.id == gameRoom.currentMiniGameId }
            if (game != null) {
                Log.d("GameViewModel", "Found selected Mini Game: ${game.title}")
                _selectedMiniGame.value = game
            } else {
                // Game not found locally - might be a custom game created by another player
                // Load it directly from Firebase
                Log.d("GameViewModel", "Game not found locally. Loading from Firebase: ${gameRoom.currentMiniGameId}")
                viewModelScope.launch {
                    try {
                        val roomCode = _roomCode.value ?: return@launch
                        val customGame = repository.getCustomMiniGame(roomCode, gameRoom.currentMiniGameId)
                        if (customGame != null) {
                            Log.d("GameViewModel", "Loaded custom game from Firebase: ${customGame.title}")

                            // Add to local list for future reference
                            val updatedGames = _miniGames.value.toMutableList()
                            updatedGames.add(customGame)
                            _miniGames.value = updatedGames

                            // Update selected game
                            _selectedMiniGame.value = customGame
                        }
                    } catch (e: Exception) {
                        Log.e("GameViewModel", "Error loading custom game", e)
                    }
                }
            }
        } else if (gameRoom.currentMiniGameId == null && _selectedMiniGame.value != null) {
            // The Mini Game was cleared in the database, clear it locally too
            Log.d("GameViewModel", "Mini Game was cleared, updating local state")
            _selectedMiniGame.value = null
        }

        // Check for selected drinker by any player
        if (gameRoom.selectedDrinkerId != null && _selectedDrinkerId.value == null) {
            // A player was selected to drink, but we don't have it locally
            Log.d("GameViewModel", "Player selected to drink by another player")

            // Get the selected player name
            val selectedPlayerName = gameRoom.players[gameRoom.selectedDrinkerId]?.name ?: "Unknown"

            // Update local state
            _selectedDrinkerId.value = gameRoom.selectedDrinkerId
            _selectedDrinkerName.value = selectedPlayerName
        } else if (gameRoom.selectedDrinkerId == null && _selectedDrinkerId.value != null) {
            // The selected drinker was cleared in the database, clear it locally too
            Log.d("GameViewModel", "Selected drinker was cleared, updating local state")
            _selectedDrinkerId.value = null
            _selectedDrinkerName.value = null
        }

        // Check for mate changes
        val oldPlayer = _currentPlayer.value
        val newPlayer = gameRoom.players[_playerId.value]

        // First check for mate changes before updating current player
        detectNewMates(oldPlayer, newPlayer)

        // Update current player after checking for changes
        _playerId.value?.let { pid ->
            _currentPlayer.value = gameRoom.players[pid]
        }
    }

    // Load rules and games
    private fun loadRulesAndGames(gameMode: GameMode, onLoadComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Get default rules and games
                val defaultRules = GameRulesProvider.getDefaultJackRules(gameMode)
                val defaultGames = GameRulesProvider.getDefaultMiniGames(gameMode)

                // Get custom rules and games from Firebase
                val roomCode = _roomCode.value ?: return@launch
                val customRules = repository.getCustomJackRules(roomCode)
                val customGames = repository.getCustomMiniGames(roomCode)

                Log.d("GameViewModel", "Loaded ${customRules.size} custom Jack Rules")
                Log.d("GameViewModel", "Loaded ${customGames.size} custom Mini Games")

                // Combine default and custom content
                _jackRules.value = defaultRules + customRules
                _miniGames.value = defaultGames + customGames

                // Check if there's a currently selected rule or game and make sure we have it locally
                val gameRoom = _gameRoom.value
                if (gameRoom != null) {
                    if (gameRoom.currentJackRuleId != null &&
                        !_jackRules.value.any { it.id == gameRoom.currentJackRuleId }) {
                        // We don't have the current rule locally, load it
                        Log.d("GameViewModel", "Current Jack Rule not found locally, fetching: ${gameRoom.currentJackRuleId}")
                        val currentRule = repository.getCustomJackRule(roomCode, gameRoom.currentJackRuleId)
                        if (currentRule != null) {
                            val updatedRules = _jackRules.value.toMutableList()
                            updatedRules.add(currentRule)
                            _jackRules.value = updatedRules
                            _selectedJackRule.value = currentRule
                        }
                    }

                    if (gameRoom.currentMiniGameId != null &&
                        !_miniGames.value.any { it.id == gameRoom.currentMiniGameId }) {
                        // We don't have the current game locally, load it
                        Log.d("GameViewModel", "Current Mini Game not found locally, fetching: ${gameRoom.currentMiniGameId}")
                        val currentGame = repository.getCustomMiniGame(roomCode, gameRoom.currentMiniGameId)
                        if (currentGame != null) {
                            val updatedGames = _miniGames.value.toMutableList()
                            updatedGames.add(currentGame)
                            _miniGames.value = updatedGames
                            _selectedMiniGame.value = currentGame
                        }
                    }
                }

                Log.d("GameViewModel", "Finished loading rules and games: ${_jackRules.value.size} rules, ${_miniGames.value.size} games")

                // ADDED: Call completion callback after data is loaded
                onLoadComplete()
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error loading rules and games", e)
                _error.value = "Failed to load rules and games: ${e.message}"
            }
        }
    }

    // Select a Jack Rule
    fun selectJackRule(rule: JackRule) {
        _selectedJackRule.value = rule
        _showJackRuleSelector.value = false

        // Notify other players of the selection
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return
        val playerName = _gameRoom.value?.players?.get(playerId)?.name ?: "Unknown"

        Log.d("GameViewModel", "Player $playerName ($playerId) selected Jack Rule: ${rule.title}")

        viewModelScope.launch {
            try {
                // Select the rule
                repository.selectJackRule(roomCode, playerId, rule.id)

                // Get current turn count DIRECTLY FROM FIREBASE, not from GameRoom object
                val turnCountSnapshot = repository.getTurnCount(roomCode)
                val currentTurnCount = turnCountSnapshot ?: 0

                Log.d("GameViewModel", "Current turn count for Jack Rule (from Firebase): $currentTurnCount")

                // Add as active rule with turn count
                val activeRule = ActiveJackRule(
                    ruleId = rule.id,
                    title = rule.title,
                    description = rule.description,
                    type = rule.type,
                    gameMode = rule.gameMode,
                    createdByPlayerId = playerId,
                    createdByPlayerName = playerName,
                    expiresAfterPlayerId = playerId,
                    isCustom = rule.isCustom ?: false,
                    createdTurnCount = currentTurnCount
                )

                repository.addActiveJackRule(roomCode, activeRule)
                Log.d("GameViewModel", "Added active Jack Rule with turn count: $currentTurnCount")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error selecting Jack Rule", e)
                _error.value = "Failed to select rule: ${e.message}"
            }
        }
    }

    // Select a Mini Game
    fun selectMiniGame(game: MiniGame) {
        _selectedMiniGame.value = game
        _showMiniGameSelector.value = false

        // Notify other players of the selection
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return

        viewModelScope.launch {
            try {
                repository.selectMiniGame(roomCode, playerId, game.id)
            } catch (e: Exception) {
                _error.value = "Failed to select mini game: ${e.message}"
            }
        }
    }

    // Method to select a player to drink
    fun selectPlayerToDrink(playerId: String, playerName: String) {
        Log.d("GameViewModel", "Selecting player to drink: $playerName (ID: $playerId)")

        _showPlayerSelector.value = false
        _selectedDrinkerId.value = playerId
        _selectedDrinkerName.value = playerName

        val roomCode = _roomCode.value ?: return
        val currentPlayerId = _playerId.value ?: return

        viewModelScope.launch {
            try {
                repository.selectPlayerToDrink(roomCode, currentPlayerId, playerId)
            } catch (e: Exception) {
                _error.value = "Failed to select player: ${e.message}"
            }
        }
    }

    // Add method to handle mate selection
    // Update the selectMate method to advance turn automatically
    fun selectMate(mateId: String) {
        Log.d("GameViewModel", "Selecting mate with ID: $mateId")

        _showMateSelector.value = false

        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return

        val playerName = _gameRoom.value?.players?.get(playerId)?.name ?: "Unknown"
        val mateName = _gameRoom.value?.players?.get(mateId)?.name ?: "Unknown"

        Log.d("GameViewModel", "Player '$playerName' selected '$mateName' as mate")

        viewModelScope.launch {
            try {
                // Set the notification for a brief moment to show the Snackbar
                _newMateRelationships.value = setOf(mateId)

                // Update the database with the mate relationship
                repository.setPlayerMate(roomCode, playerId, mateId)

                // Wait a short time for UI feedback before advancing
                delay(1500)

                // Automatically advance turn (no need to wait for button click)
                advanceTurn()

                // Clear notifications after a delay to allow UI feedback
                delay(3000)
                _newMateRelationships.value = emptySet()

            } catch (e: Exception) {
                Log.e("GameViewModel", "Error setting mate relationship", e)
                _error.value = "Failed to set mate: ${e.message}"
            }
        }
    }

    // Replace clearMateNotification with a simpler dismissal method
    fun dismissMateNotification() {
        _newMateRelationships.value = emptySet()
    }

    // Clear selections
    fun clearSelections() {
        val roomCode = _roomCode.value ?: return

        viewModelScope.launch {
            try {
                // Clear local state
                _selectedJackRule.value = null
                _selectedMiniGame.value = null
                _showJackRuleSelector.value = false
                _showMiniGameSelector.value = false
                _showPlayerSelector.value = false
                _showMateSelector.value = false
                _selectedDrinkerId.value = null
                _selectedDrinkerName.value = null

                // Clear in Firebase using repository
                repository.clearSelections(roomCode)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error clearing selections: ${e.message}")
                _error.value = "Failed to clear selections: ${e.message}"
            }
        }
    }

    private fun detectNewMates(oldPlayer: Player?, newPlayer: Player?) {
        // Skip automatic detection if manual selection is in progress
        if (mateSelectionInProgress) {
            Log.d("GameViewModel", "Skipping automatic mate detection during manual selection")
            return
        }

        if (oldPlayer != null && newPlayer != null && oldPlayer.id == newPlayer.id) {
            // Check if any new mates were added
            val oldMates = oldPlayer.mateIds.toSet()
            val newMates = newPlayer.mateIds.toSet()

            Log.d("GameViewModel", "Checking mate changes for player ${newPlayer.name}")
            Log.d("GameViewModel", "Old mates: $oldMates")
            Log.d("GameViewModel", "New mates: $newMates")

            if (newMates.size > oldMates.size) {
                // New mates were added
                val addedMates = newMates - oldMates
                Log.d("GameViewModel", "New mate relationships detected: $addedMates")
                // Only set if we have changes and aren't in manual selection mode
                if (addedMates.isNotEmpty()) {
                    _newMateRelationships.value = addedMates
                }
            }
        }
    }

    fun setGameMode(mode: GameMode) {
        _selectedGameMode.value = mode
    }

    // Method to create a custom Jack Rule
    fun createCustomJackRule(rule: JackRule) {
        Log.d("GameViewModel", "Creating custom Jack Rule: ${rule.title}")

        // Ensure the room and player IDs are available
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return

        // Set the creator ID
        val updatedRule = rule.copy(createdByPlayerId = playerId)

        // Add to local list for immediate use
        val currentRules = _jackRules.value.toMutableList()
        currentRules.add(updatedRule)
        _jackRules.value = currentRules

        // Save to Firebase
        viewModelScope.launch {
            try {
                repository.saveCustomJackRule(roomCode, updatedRule)
                Log.d("GameViewModel", "Custom Jack Rule saved successfully")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error saving custom Jack Rule", e)
                _error.value = "Failed to save custom rule: ${e.message}"
            }
        }
    }

    // Method to create a custom Mini Game
    fun createCustomMiniGame(game: MiniGame) {
        Log.d("GameViewModel", "Creating custom Mini Game: ${game.title}")

        // Ensure the room and player IDs are available
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return

        // Set the creator ID
        val updatedGame = game.copy(createdByPlayerId = playerId)

        // Add to local list for immediate use
        val currentGames = _miniGames.value.toMutableList()
        currentGames.add(updatedGame)
        _miniGames.value = currentGames

        // Save to Firebase
        viewModelScope.launch {
            try {
                repository.saveCustomMiniGame(roomCode, updatedGame)
                Log.d("GameViewModel", "Custom Mini Game saved successfully")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error saving custom Mini Game", e)
                _error.value = "Failed to save custom game: ${e.message}"
            }
        }
    }
    // Add method to view active rule details
    fun viewActiveRuleDetails(ruleId: String) {
        _selectedActiveRule.value = _activeJackRules.value[ruleId]
    }

    // Add method to close rule detail view
    fun closeActiveRuleDetails() {
        _selectedActiveRule.value = null
    }
}