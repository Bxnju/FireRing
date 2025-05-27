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

    private var activeRoomJob: Job? = null

    // Called when creating a new room
    fun createRoom(hostPlayerName: String, onComplete: () -> Unit = {}) {
        Log.d("GameViewModel", "Creating room with host: $hostPlayerName")
        _loading.value = true
        viewModelScope.launch {
            try {
                // 1. Generate player ID
                val userId = UUID.randomUUID().toString()
                Log.d("GameViewModel", "Generated user ID: $userId")

                // 2. Create the room in Firebase
                val code = repository.createGameRoom(hostPlayerName, userId)
                Log.d("GameViewModel", "Room created successfully with code: $code")

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
                    "J" -> {
                        _showJackRuleSelector.value = true
                        loadRulesAndGames(_gameRoom.value?.gameMode ?: GameMode.NORMAL)
                    }
                    "10" -> {
                        _showMiniGameSelector.value = true
                        loadRulesAndGames(_gameRoom.value?.gameMode ?: GameMode.NORMAL)
                    }
                    else -> {
                        // For non-special cards, auto-advance the turn after a short delay
                        if (card != null && !isSpecialCard(card.value)) {
                            viewModelScope.launch {
                                delay(5000) // 5 seconds
                                repository.advanceTurn(roomCode)
                                _drawnCard.value = null
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
                _drawnCard.value = null
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
        } else if (currentCardId == null && _drawnCard.value != null) {
            // If there's no current card in the room but we have one locally, clear it
            Log.d("GameViewModel", "Clearing local drawn card as room has no current card")
            _drawnCard.value = null
        }

        // NEW CODE: Check for Jack Rule selection by any player
        if (gameRoom.currentJackRuleId != null && _selectedJackRule.value == null) {
            // A Jack Rule was selected, but we don't have it locally - load it
            Log.d("GameViewModel", "Jack Rule selected by another player, loading rule details")

            // Load the jack rules if not already loaded
            if (_jackRules.value.isEmpty()) {
                loadRulesAndGames(gameRoom.gameMode)
            }

            // Find the selected rule in our local list
            val rule = _jackRules.value.find { it.id == gameRoom.currentJackRuleId }
            if (rule != null) {
                Log.d("GameViewModel", "Found selected Jack Rule: ${rule.title}")
                _selectedJackRule.value = rule
            }
        } else if (gameRoom.currentJackRuleId == null && _selectedJackRule.value != null) {
            // The Jack Rule was cleared in the database, clear it locally too
            Log.d("GameViewModel", "Jack Rule was cleared, updating local state")
            _selectedJackRule.value = null
        }

        // NEW CODE: Check for Mini Game selection by any player
        if (gameRoom.currentMiniGameId != null && _selectedMiniGame.value == null) {
            // A Mini Game was selected, but we don't have it locally - load it
            Log.d("GameViewModel", "Mini Game selected by another player, loading game details")

            // Load the mini games if not already loaded
            if (_miniGames.value.isEmpty()) {
                loadRulesAndGames(gameRoom.gameMode)
            }

            // Find the selected game in our local list
            val game = _miniGames.value.find { it.id == gameRoom.currentMiniGameId }
            if (game != null) {
                Log.d("GameViewModel", "Found selected Mini Game: ${game.title}")
                _selectedMiniGame.value = game
            }
        } else if (gameRoom.currentMiniGameId == null && _selectedMiniGame.value != null) {
            // The Mini Game was cleared in the database, clear it locally too
            Log.d("GameViewModel", "Mini Game was cleared, updating local state")
            _selectedMiniGame.value = null
        }
    }

    // Load rules and games
    private fun loadRulesAndGames(gameMode: GameMode) {
        _jackRules.value = GameRulesProvider.getDefaultJackRules(gameMode)
        _miniGames.value = GameRulesProvider.getDefaultMiniGames(gameMode)
    }

    // Select a Jack Rule
    fun selectJackRule(rule: JackRule) {
        _selectedJackRule.value = rule
        _showJackRuleSelector.value = false

        // Notify other players of the selection
        val roomCode = _roomCode.value ?: return
        val playerId = _playerId.value ?: return

        viewModelScope.launch {
            try {
                repository.selectJackRule(roomCode, playerId, rule.id)
            } catch (e: Exception) {
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

                // Clear in Firebase using repository
                repository.clearSelections(roomCode)
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error clearing selections: ${e.message}")
                _error.value = "Failed to clear selections: ${e.message}"
            }
        }
    }
}