package com.benchopo.firering.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benchopo.firering.data.FirebaseRepository
import com.benchopo.firering.model.*
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    private var activeRoomJob: Job? = null

    // Called when creating a new room
    fun createRoom(hostPlayerName: String, onComplete: () -> Unit = {}) {
        _loading.value = true
        viewModelScope.launch {
            try {
                // 1. Generate player ID
                val userId = UUID.randomUUID().toString()

                // 2. Create the room in Firebase
                val code = repository.createGameRoom(hostPlayerName, userId)

                // 3. Set local state
                _playerId.value = userId
                _roomCode.value = code
                userViewModel.setUserInfo(userId, hostPlayerName)

                // 4. Get initial room data once (not as a continuous flow)
                val initialRoom = repository.getRoomOnce(code)
                if (initialRoom != null) {
                    _gameRoom.value = initialRoom
                    _currentPlayer.value = initialRoom.players[userId]
                }

                // 5. Now we can safely navigate
                onComplete()

                // 6. Start continuous flow collection AFTER navigation
                viewModelScope.launch {
                    repository.getRoom(code).collect { room ->
                        if (room != null) {
                            _gameRoom.value = room
                            _playerId.value?.let { pid -> _currentPlayer.value = room.players[pid] }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to create room", e)
                _error.value = "Failed to create room: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Called when joining an existing room
    fun joinRoom(roomCode: String, playerName: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val playerId = repository.joinRoom(roomCode, playerName)
                if (playerId != null) {
                    _roomCode.value = roomCode
                    _playerId.value = playerId

                    // Start listening for room updates
                    repository.getRoom(roomCode).collect { room ->
                        if (room != null) {
                            _gameRoom.value = room
                            _currentPlayer.value = room.players[playerId]
                        }
                    }
                } else {
                    _error.value = "Room not found or game already started"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to join room: ${e.message}"
            } finally {
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
    fun drawCard() {
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

                // Auto-advance turn if not special card requiring action
                if (card != null && !isSpecialCard(card.value)) {
                    repository.advanceTurn(roomCode)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to draw card: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
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
        val roomCode = _roomCode.value
        val playerId = _playerId.value

        if (roomCode == null || playerId == null) {
            Log.e("GameViewModel", "Cannot leave room - roomCode=$roomCode, playerId=$playerId")
            onComplete() // Still allow navigation even if we can't properly leave
            return
        }

        _loading.value = true
        viewModelScope.launch {
            try {
                // Cancel any active room subscriptions
                activeRoomJob?.cancel()
                activeRoomJob = null

                repository.leaveRoom(roomCode, playerId)
                Log.d("GameViewModel", "Successfully left room")
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error leaving room", e)
            } finally {
                // Always clear state and complete
                clearGameData()
                _loading.value = false
                onComplete()
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
        Log.d("GameViewModel", "Ensuring room is loaded: $roomCode")

        if (_roomCode.value != roomCode) {
            _roomCode.value = roomCode
            // Clear and reload if room code changed
            _gameRoom.value = null
        }

        // Only reload if we don't have room data yet
        if (_gameRoom.value == null) {
            // Cancel any existing room subscription
            activeRoomJob?.cancel()

            activeRoomJob =
                    viewModelScope.launch {
                        try {
                            Log.d("GameViewModel", "Starting to collect room data")
                            repository.getRoom(roomCode).collect { room ->
                                if (room != null) {
                                    Log.d(
                                            "GameViewModel",
                                            "Room data received: ${room.roomCode}, players: ${room.players.size}"
                                    )
                                    _gameRoom.value = room

                                    // Update current player if possible
                                    _playerId.value?.let { pid ->
                                        val player = room.players[pid]
                                        Log.d("GameViewModel", "Current player update: $player")
                                        _currentPlayer.value = player
                                    }
                                } else {
                                    Log.w("GameViewModel", "Received null room data")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("GameViewModel", "Error loading room", e)
                            _error.value = "Failed to load room: ${e.message}"
                        }
                    }
        }
    }

    // Clear game data
    fun clearGameData() {
        _roomCode.value = null
        _playerId.value = null
        _gameRoom.value = null
        _currentPlayer.value = null
        _drawnCard.value = null
        _error.value = null
    }

    // Add this public method to set player ID
    fun setPlayerId(id: String?) {
        _playerId.value = id
    }
}
