package com.benchopo.firering.data

import android.util.Log
import com.benchopo.firering.model.*
import com.google.firebase.database.*
import java.util.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseDatabase.getInstance().reference

    // Room Operations
    suspend fun createGameRoom(
        hostPlayerName: String,
        userId: String,
        gameMode: GameMode = GameMode.NORMAL
    ): String {
        Log.d("FirebaseRepository", "Creating game room for host: $hostPlayerName with ID: $userId, mode: $gameMode")
        val roomCode = generateRoomCode()
        Log.d("FirebaseRepository", "Generated room code: $roomCode")

        // Generate a deck appropriate for the game mode
        val deck = generateDeckForGameMode(gameMode)
        Log.d("FirebaseRepository", "Generated deck with ${deck.size} cards for mode: $gameMode")

        val hostPlayerId = userId // Use the provided userId instead of generating one
        val player =
                Player(id = hostPlayerId, name = hostPlayerName, isHost = true, isGuest = false)
        Log.d("FirebaseRepository", "Created host player: $player")

        // Get default drinks
        val defaultDrinks = getDefaultDrinks()
        Log.d("FirebaseRepository", "Added ${defaultDrinks.size} default drinks")

        // Create room structure
        val roomRef = db.child("rooms").child(roomCode)
        Log.d("FirebaseRepository", "Preparing room data structure")

        // Create the room info
        val roomInfo = mapOf(
            "roomCode" to roomCode,
            "hostId" to hostPlayerId,
            "gameState" to GameState.WAITING.name,
            "kingsCupCount" to 0,
            "gameMode" to gameMode.name,
            "createdAt" to System.currentTimeMillis()
        )

        // Set up initial data
        val updates =
                hashMapOf<String, Any>(
                        "info" to roomInfo,
                        "players/$hostPlayerId" to player,
                        "turnOrder" to listOf(hostPlayerId),
                        "settings" to GameSettings()
                )

        // Add each card to the database
        deck.forEachIndexed { index, card ->
            updates["cards/${card.id}"] = card.copy(positionInRing = index)
        }

        // Add default drinks
        defaultDrinks.forEach { drink -> updates["drinks/${drink.id}"] = drink }

        // Write all data at once
        roomRef.updateChildren(updates).await()
        Log.d("FirebaseRepository", "Room data written to database successfully")

        return roomCode
    }

    suspend fun joinRoom(roomCode: String, playerName: String): String? {
        Log.d("FirebaseRepository", "Joining room: $roomCode with player name: $playerName")

        // Check if room exists
        val roomSnapshot = db.child("rooms").child(roomCode).get().await()
        if (!roomSnapshot.exists()) {
            Log.w("FirebaseRepository", "Room $roomCode not found")
            return null
        }

        // Get current game state
        val gameStateSnapshot = roomSnapshot.child("info/gameState").getValue(String::class.java)
        Log.d("FirebaseRepository", "Room game state: $gameStateSnapshot")

        if (gameStateSnapshot == GameState.FINISHED.name) {
            Log.w("FirebaseRepository", "Cannot join - game is already finished")
            return null // Can't join if game has finished
        }

        // Create new player
        val playerId = playerName + UUID.randomUUID().toString()
        val player =
                Player(
                        id = playerId,
                        name = playerName,
                        isHost = false,
                        isGuest = true,
                        isConnected = true,
                        // Use current timestamp instead of ServerValue.TIMESTAMP here
                        lastActiveTimestamp = System.currentTimeMillis()
                )

        // Add player to room
        val roomRef = db.child("rooms").child(roomCode)
        roomRef.child("players").child(playerId).setValue(player).await()

        // Set up disconnect handler
        val onDisconnectRef = roomRef.child("players").child(playerId).child("isConnected")
        onDisconnectRef.onDisconnect().setValue(false)

        // Add player to turn order
        val turnOrderRef = roomRef.child("turnOrder")
        val turnOrderSnapshot = turnOrderRef.get().await()
        val turnOrder =
                turnOrderSnapshot.children.map { it.getValue(String::class.java)!! }.toMutableList()
        turnOrder.add(playerId)
        turnOrderRef.setValue(turnOrder).await()

        Log.d("FirebaseRepository", "Player successfully joined room. ID: $playerId")
        return playerId
    }

    fun getRoom(roomCode: String): Flow<GameRoom?> = callbackFlow {
        val roomRef = db.child("rooms").child(roomCode)
        val listener =
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            trySend(null)
                            return
                        }

                        try {
                            // Extract room info
                            val infoSnapshot = snapshot.child("info")
                            val roomCode =
                                    infoSnapshot.child("roomCode").getValue(String::class.java)
                                            ?: ""
                            val hostId =
                                    infoSnapshot.child("hostId").getValue(String::class.java) ?: ""
                            val gameStateStr =
                                    infoSnapshot.child("gameState").getValue(String::class.java)
                                            ?: GameState.WAITING.name
                            val gameState = GameState.valueOf(gameStateStr)
                            val kingsCupCount =
                                    infoSnapshot
                                            .child("kingsCupCount")
                                            .getValue(Long::class.java)
                                            ?.toInt()
                                            ?: 0
                            val gameModeStr =
                                    infoSnapshot.child("gameMode").getValue(String::class.java)
                                            ?: GameMode.NORMAL.name
                            val gameMode = GameMode.valueOf(gameModeStr)
                            val createdAt =
                                    infoSnapshot.child("createdAt").getValue(Long::class.java)
                                            ?: System.currentTimeMillis()

                            // Extract players
                            val playersMap = mutableMapOf<String, Player>()
                            snapshot.child("players").children.forEach { playerSnapshot ->
                                val player = playerSnapshot.getValue(Player::class.java)
                                if (player != null) {
                                    playersMap[player.id] = player
                                }
                            }

                            // Extract cards
                            val allCards = mutableListOf<Card>()
                            snapshot.child("cards").children.forEach { cardSnapshot ->
                                val card = cardSnapshot.getValue(Card::class.java)
                                if (card != null) {
                                    allCards.add(card)
                                }
                            }

                            // Extract drawn cards IDs for reliable filtering
                            val drawnCardIds = mutableSetOf<String>()
                            snapshot.child("drawnCardsList").children.forEach {
                                drawnCardIds.add(it.key ?: "")
                            }

                            // Extract drawn cards from dedicated list (more reliable)
                            val drawnCards = mutableListOf<Card>()
                            snapshot.child("drawnCardsList").children.forEach { cardSnapshot ->
                                val card = cardSnapshot.getValue(Card::class.java)
                                if (card != null) {
                                    drawnCards.add(card)
                                }
                            }

                            // Fallback to filtering if drawnCardsList is empty
                            if (drawnCards.isEmpty()) {
                                drawnCards.addAll(allCards.filter { it.isDrawn })
                                Log.d("FirebaseRepository", "Using fallback cards filter, found ${drawnCards.size} drawn cards")
                            }

                            // Use drawn card IDs set for more reliable filtering
                            val deck = allCards.filter { !drawnCardIds.contains(it.id) && !it.isDrawn }
                            Log.d("FirebaseRepository", "Deck contains ${deck.size} undrawn cards")

                            // Extract turn order
                            val turnOrder =
                                    snapshot.child("turnOrder").children.mapNotNull {
                                        it.getValue(String::class.java)
                                    }

                            // Extract current card and player
                            val currentCardId =
                                    snapshot.child("currentCardId").getValue(String::class.java)
                            val currentPlayerId =
                                    snapshot.child("currentPlayerId").getValue(String::class.java)

                            // Extract current Jack rule and mini-game
                            val currentJackRuleId =
                                    snapshot.child("currentJackRuleId").getValue(String::class.java)
                            val currentJackRuleSelectedBy =
                                    snapshot.child("currentJackRuleSelectedBy").getValue(String::class.java)
                            val currentMiniGameId =
                                    snapshot.child("currentMiniGameId").getValue(String::class.java)
                            val currentMiniGameSelectedBy =
                                    snapshot.child("currentMiniGameSelectedBy").getValue(String::class.java)

                            // Extract selected drinker info
                            val selectedDrinkerId = snapshot.child("selectedDrinkerId").getValue(String::class.java)
                            val selectedDrinkerBy = snapshot.child("selectedDrinkerBy").getValue(String::class.java)

                            // Extract custom Jack Rules
                            val customJackRules = mutableListOf<JackRule>()
                            snapshot.child("customJackRules").children.forEach { ruleSnapshot ->
                                val rule = ruleSnapshot.getValue(JackRule::class.java)
                                if (rule != null) {
                                    customJackRules.add(rule)
                                }
                            }

                            // Extract custom Mini Games
                            val customMiniGames = mutableListOf<MiniGame>()
                            snapshot.child("customMiniGames").children.forEach { gameSnapshot ->
                                val game = gameSnapshot.getValue(MiniGame::class.java)
                                if (game != null) {
                                    customMiniGames.add(game)
                                }
                            }

                            // Extract active Jack Rules
                            val activeJackRules = mutableMapOf<String, ActiveJackRule>()
                            snapshot.child("activeJackRules").children.forEach { ruleSnapshot ->
                                val rule = ruleSnapshot.getValue(ActiveJackRule::class.java)
                                if (rule != null) {
                                    activeJackRules[rule.id] = rule
                                }
                            }

                            // Build GameRoom object
                            val gameRoom =
                                    GameRoom(
                                            roomCode = roomCode,
                                            hostId = hostId,
                                            players = playersMap,
                                            deck = deck,
                                            drawnCards = drawnCards,
                                            currentCardId = currentCardId,
                                            currentPlayerId = currentPlayerId,
                                            gameState = gameState,
                                            turnOrder = turnOrder,
                                            kingsCupCount = kingsCupCount,
                                            gameMode = gameMode,
                                            createdAt = createdAt,
                                            currentJackRuleId = currentJackRuleId,
                                            currentJackRuleSelectedBy = currentJackRuleSelectedBy,
                                            currentMiniGameId = currentMiniGameId,
                                            currentMiniGameSelectedBy = currentMiniGameSelectedBy,
                                            selectedDrinkerId = selectedDrinkerId,
                                            selectedDrinkerBy = selectedDrinkerBy,
                                            customJackRules = customJackRules,
                                            customMiniGames = customMiniGames,
                                            activeJackRules = activeJackRules
                                    )

                            trySend(gameRoom)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            trySend(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }

        roomRef.addValueEventListener(listener)

        awaitClose { roomRef.removeEventListener(listener) }
    }

    suspend fun startGame(roomCode: String) {
        val roomRef = db.child("rooms").child(roomCode)

        // Shuffle turn order for random starting player
        val turnOrderRef = roomRef.child("turnOrder")
        val turnOrderSnapshot = turnOrderRef.get().await()
        val turnOrder =
                turnOrderSnapshot.children.map { it.getValue(String::class.java)!! }.toMutableList()
        turnOrder.shuffle()
        turnOrderRef.setValue(turnOrder).await()

        // Set current player to first in turn order
        roomRef.child("currentPlayerId").setValue(turnOrder.first()).await()

        // Update game state
        roomRef.child("info/gameState").setValue(GameState.PLAYING.name).await()
    }

    // Update the drawCard method to ensure proper card exclusion

    suspend fun drawCard(roomCode: String, playerId: String): Card? {
        val roomRef = db.child("rooms").child(roomCode)

        // Get undrawn cards with additional logging
        val cardsSnapshot = roomRef.child("cards").get().await()
        val undrawnCards = mutableListOf<Card>()
        val drawnCardIds = mutableSetOf<String>()

        // First, collect IDs of cards in drawnCardsList for accurate filtering
        val drawnCardsListSnapshot = roomRef.child("drawnCardsList").get().await()
        drawnCardsListSnapshot.children.forEach { cardSnapshot ->
            val cardId = cardSnapshot.key
            if (cardId != null) {
                drawnCardIds.add(cardId)
            }
        }

        Log.d("FirebaseRepository", "Found ${drawnCardIds.size} cards in drawnCardsList")

        // Now get undrawn cards, making sure to exclude those in drawnCardsList
        cardsSnapshot.children.forEach { cardSnapshot ->
            val card = cardSnapshot.getValue(Card::class.java)
            if (card != null && !card.isDrawn && !drawnCardIds.contains(card.id)) {
                undrawnCards.add(card)
            }
        }

        Log.d("FirebaseRepository", "Found ${undrawnCards.size} undrawn cards after filtering")

        if (undrawnCards.isEmpty()) {
            Log.d("FirebaseRepository", "No undrawn cards left")
            return null
        }

        // Get a random undrawn card
        val drawnCard = undrawnCards.random()

        // Add more detailed logging
        Log.d("FirebaseRepository", "Drawing card: ${drawnCard.value} of ${drawnCard.suit} (${drawnCard.id})")
        Log.d("FirebaseRepository", "Card currently drawn status: ${drawnCard.isDrawn}")

        // Create a COMPLETE card object with all properties updated
        val updatedCard = drawnCard.copy(
            isDrawn = true,
            drawnByPlayerId = playerId,
            drawnTimestamp = System.currentTimeMillis()
        )

        // Update the ENTIRE card object, not just nested properties
        val updates = hashMapOf<String, Any>(
            // Store the entire updated card object
            "cards/${drawnCard.id}" to updatedCard,
            "currentCardId" to drawnCard.id
        )

        // If it's a King, update kings cup count
        if (drawnCard.value == "K") {
            val kingsSnapshot = roomRef.child("info/kingsCupCount").get().await()
            val kingsCount = kingsSnapshot.getValue(Long::class.java)?.toInt() ?: 0
            updates["info/kingsCupCount"] = kingsCount + 1
        }

        // Add the card to a dedicated drawnCards collection for easier access
        updates["drawnCardsList/${drawnCard.id}"] = updatedCard

        Log.d("FirebaseRepository", "Writing updates to database")
        roomRef.updateChildren(updates).await()
        Log.d("FirebaseRepository", "Card successfully marked as drawn")

        return updatedCard
    }

    suspend fun advanceTurn(roomCode: String) {
        val roomRef = db.child("rooms").child(roomCode)

        // Get current player and turn order
        val currentPlayerIdSnapshot = roomRef.child("currentPlayerId").get().await()
        val currentPlayerId = currentPlayerIdSnapshot.getValue(String::class.java) ?: return

        // Check for expired mates and rules before advancing turn
        checkAndClearExpiredMates(roomCode, currentPlayerId)
        checkAndRemoveExpiredJackRules(roomCode, currentPlayerId)

        // Get and increment turn count
        val turnCountSnapshot = roomRef.child("info/turnCount").get().await()
        val currentTurnCount = turnCountSnapshot.getValue(Long::class.java)?.toInt() ?: 0
        val newTurnCount = currentTurnCount + 1

        val turnOrderSnapshot = roomRef.child("turnOrder").get().await()
        val turnOrder = turnOrderSnapshot.children.map { it.getValue(String::class.java)!! }

        if (turnOrder.isEmpty()) return

        // Find next player
        val currentIndex = turnOrder.indexOf(currentPlayerId)
        val nextIndex = (currentIndex + 1) % turnOrder.size
        val nextPlayerId = turnOrder[nextIndex]

        // Create updates map for all changes
        val updates = hashMapOf<String, Any?>(
            // Update current player
            "currentPlayerId" to nextPlayerId,
            // Update turn count
            "info/turnCount" to newTurnCount,
            // Clear current card
            "currentCardId" to null,
            // Clear Jack Rule and Mini Game selections
            "currentJackRuleId" to null,
            "currentJackRuleSelectedBy" to null,
            "currentMiniGameId" to null,
            "currentMiniGameSelectedBy" to null,
            // Clear selected drinker
            "selectedDrinkerId" to null,
            "selectedDrinkerBy" to null
        )

        // Apply all updates at once
        roomRef.updateChildren(updates).await()
        Log.d("FirebaseRepository", "Advanced turn from player $currentPlayerId to $nextPlayerId (turn #$newTurnCount)")
    }

    suspend fun updatePlayerDrinkCount(roomCode: String, playerId: String, increment: Int = 1) {
        val playerRef = db.child("rooms").child(roomCode).child("players").child(playerId)
        val drinkCountSnapshot = playerRef.child("drinkCount").get().await()
        val currentCount = drinkCountSnapshot.getValue(Long::class.java)?.toInt() ?: 0
        playerRef.child("drinkCount").setValue(currentCount + increment).await()
    }

    // Update the setPlayerMate method to fix the expiration logic
    suspend fun setPlayerMate(roomCode: String, playerId: String, mateId: String) {
        val roomRef = db.child("rooms").child(roomCode)
        Log.d("FirebaseRepository", "Setting mate relationship: $playerId selected $mateId")

        // Get current turn count
        val turnCountSnapshot = roomRef.child("info/turnCount").get().await()
        val currentTurnCount = turnCountSnapshot.getValue(Long::class.java)?.toInt() ?: 0

        // First get all players to check if everyone already has mates
        val playersSnapshot = roomRef.child("players").get().await()
        val allPlayers = playersSnapshot.children.mapNotNull { it.key }
        Log.d("FirebaseRepository", "Total players: ${allPlayers.size}")

        // Get all existing mates for both players
        val playerMates = getMateIdsRecursively(roomRef, playerId)
        Log.d("FirebaseRepository", "Player $playerId existing mates: $playerMates")

        val mateMates = getMateIdsRecursively(roomRef, mateId)
        Log.d("FirebaseRepository", "Player $mateId existing mates: $mateMates")

        // Combine all mates from both players
        val allMates = (playerMates + mateMates + setOf(playerId, mateId)).toSet()
        Log.d("FirebaseRepository", "All mates after combining: $allMates")

        // Update mates list for all players in the chain
        val updates = mutableMapOf<String, Any>()
        for (id in allMates) {
            val matesList = allMates.filter { it != id }
            updates["players/$id/mateIds"] = matesList
            // FIXED: Use playerId instead of undefined currentPlayerId
            updates["players/$id/mateExpiresAfterPlayerId"] = playerId
            updates["players/$id/mateCreatedTurnCount"] = currentTurnCount
        }

        Log.d("FirebaseRepository", "Applying mate updates to ${updates.size} players")
        // Apply all updates at once
        roomRef.updateChildren(updates).await()
        Log.d("FirebaseRepository", "Mate relationships updated successfully")

        debugMateChain(roomCode)
    }

    // Helper function to recursively get all mates
    private suspend fun getMateIdsRecursively(
        roomRef: DatabaseReference,
        playerId: String,
        depth: Int = 0,
        visited: MutableSet<String> = mutableSetOf()
    ): Set<String> {
        // Prevent infinite recursion with cycles in mate relationships
        if (playerId in visited) {
            Log.d("FirebaseRepository", "${"  ".repeat(depth)}Player $playerId already visited, stopping recursion")
            return emptySet()
        }
        visited.add(playerId)

        Log.d("FirebaseRepository", "${"  ".repeat(depth)}Getting mates for player $playerId at depth $depth")
        val matesSnapshot = roomRef.child("players/$playerId/mateIds").get().await()
        val directMates = mutableSetOf<String>()

        matesSnapshot.children.forEach {
            val mate = it.getValue(String::class.java)
            if (mate != null) {
                directMates.add(mate)
                Log.d("FirebaseRepository", "${"  ".repeat(depth)}Found direct mate: $mate")
            }
        }

        // If no mates, return empty set
        if (directMates.isEmpty()) {
            Log.d("FirebaseRepository", "${"  ".repeat(depth)}No mates found for player $playerId")
            return emptySet()
        }

        // Recursively get mates of mates
        val allMates = directMates.toMutableSet()
        for (mateId in directMates) {
            Log.d("FirebaseRepository", "${"  ".repeat(depth)}Getting recursive mates for $mateId")
            allMates.addAll(getMateIdsRecursively(roomRef, mateId, depth + 1, visited))
        }

        Log.d("FirebaseRepository", "${"  ".repeat(depth)}Total mates for player $playerId (including chain): $allMates")
        return allMates
    }

    suspend fun addActiveRule(roomCode: String, rule: ActiveRule) {
        val rulesRef = db.child("rooms").child(roomCode).child("activeRules")
        rulesRef.child(rule.id).setValue(rule).await()
    }

    suspend fun removeActiveRule(roomCode: String, ruleId: String) {
        val rulesRef = db.child("rooms").child(roomCode).child("activeRules")
        rulesRef.child(ruleId).removeValue().await()
    }

    // Add active Jack Rule to the room
    suspend fun addActiveJackRule(roomCode: String, activeRule: ActiveJackRule) {
        Log.d("FirebaseRepository", "Adding active Jack Rule: ${activeRule.title}")
        val roomRef = db.child("rooms").child(roomCode)
        roomRef.child("activeJackRules").child(activeRule.id).setValue(activeRule).await()
        Log.d("FirebaseRepository", "Active Jack Rule added successfully")
    }

    // Remove active Jack Rule from the room
    suspend fun removeActiveJackRule(roomCode: String, ruleId: String) {
        Log.d("FirebaseRepository", "Removing active Jack Rule: $ruleId")
        val roomRef = db.child("rooms").child(roomCode)
        roomRef.child("activeJackRules").child(ruleId).removeValue().await()
        Log.d("FirebaseRepository", "Active Jack Rule removed successfully")
    }

    // Check for expired Jack Rules after a player's turn
    suspend fun checkAndRemoveExpiredJackRules(roomCode: String, currentPlayerId: String) {
        Log.d("FirebaseRepository", "Checking for expired Jack Rules for player $currentPlayerId")
        val roomRef = db.child("rooms").child(roomCode)

        // Get the current turn count
        val turnCountSnapshot = roomRef.child("info/turnCount").get().await()
        val currentTurnCount = turnCountSnapshot.getValue(Long::class.java)?.toInt() ?: 0
        Log.d("FirebaseRepository", "Current turn count: $currentTurnCount")

        // Get all active rules
        val rulesSnapshot = roomRef.child("activeJackRules").get().await()
        val expiredRuleIds = mutableListOf<String>()

        rulesSnapshot.children.forEach { ruleSnapshot ->
            val rule = ruleSnapshot.getValue(ActiveJackRule::class.java)

            if (rule != null) {
                Log.d("FirebaseRepository", "Checking rule: ${rule.title}, " +
                    "expiresAfter: ${rule.expiresAfterPlayerId}, " +
                    "createdTurn: ${rule.createdTurnCount}, " +
                    "currentTurn: $currentTurnCount")

                // Only expire rules when:
                // 1. Current player matches the expiration player AND
                // 2. The rule wasn't created in the current turn
                if (rule.expiresAfterPlayerId == currentPlayerId &&
                    rule.createdTurnCount < currentTurnCount) {

                    Log.d("FirebaseRepository", "Jack Rule '${rule.title}' has expired (after ${currentPlayerId}'s turn)")
                    expiredRuleIds.add(rule.id)
                } else {
                    Log.d("FirebaseRepository", "Jack Rule '${rule.title}' remains active")
                }
            }
        }

        // Remove expired rules
        if (expiredRuleIds.isNotEmpty()) {
            Log.d("FirebaseRepository", "Removing ${expiredRuleIds.size} expired Jack Rules")
            val updates = hashMapOf<String, Any?>()
            expiredRuleIds.forEach { ruleId ->
                updates["activeJackRules/$ruleId"] = null
            }
            roomRef.updateChildren(updates).await()
        } else {
            Log.d("FirebaseRepository", "No expired Jack Rules found")
        }
    }

    suspend fun getTurnCount(roomCode: String): Int {
        val turnCountSnapshot = db.child("rooms").child(roomCode).child("info/turnCount").get().await()
        return turnCountSnapshot.getValue(Long::class.java)?.toInt() ?: 0
    }

    private fun getDefaultDrinks(): List<Drink> {
        return listOf(
                Drink(id = "beer", name = "Beer", alcoholContent = 4.5, emoji = "🍺"),
                Drink(id = "wine", name = "Wine", alcoholContent = 12.0, emoji = "🍷"),
                Drink(id = "whiskey", name = "Whiskey", alcoholContent = 40.0, emoji = "🥃"),
                Drink(id = "vodka", name = "Vodka", alcoholContent = 40.0, emoji = "🥂"),
                Drink(id = "water", name = "Water", alcoholContent = 0.0, emoji = "💧")
        )
    }

    private fun generateRoomCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..5).map { chars.random() }.joinToString("")
    }

    // Add this method to handle player disconnections

    suspend fun setPlayerOnlineStatus(roomCode: String, playerId: String, isOnline: Boolean) {
        val playerRef = db.child("rooms").child(roomCode).child("players").child(playerId)
        val updates =
                hashMapOf<String, Any>(
                        "isConnected" to isOnline,
                        // Use the ServerValue.TIMESTAMP as a raw map value
                        "lastActiveTimestamp" to ServerValue.TIMESTAMP
                )
        playerRef.updateChildren(updates).await()
    }

    // Add this method to leave a room

    suspend fun leaveRoom(roomCode: String, playerId: String) {
        try {
            Log.d("FirebaseRepository", "Removing player $playerId from room $roomCode")
            val roomRef = db.child("rooms").child(roomCode)

            // First check if room and player exist
            val roomSnapshot = roomRef.get().await()
            if (!roomSnapshot.exists()) {
                Log.w("FirebaseRepository", "Room $roomCode doesn't exist")
                return
            }

            val playerSnapshot = roomRef.child("players").child(playerId).get().await()
            if (!playerSnapshot.exists()) {
                Log.w("FirebaseRepository", "Player $playerId doesn't exist in room $roomCode")
                return
            }

            // 1. Remove from turn order first
            val turnOrderRef = roomRef.child("turnOrder")
            val turnOrderSnapshot = turnOrderRef.get().await()
            val turnOrder =
                    turnOrderSnapshot
                            .children
                            .map { it.getValue(String::class.java)!! }
                            .toMutableList()

            Log.d("FirebaseRepository", "Current turn order: $turnOrder")
            turnOrder.remove(playerId)
            Log.d("FirebaseRepository", "New turn order after removal: $turnOrder")

            // 2. Check if there will be any players left
            val isLastPlayer = turnOrder.isEmpty()

            // 3. If this is the last player, we'll delete the entire room
            if (isLastPlayer) {
                Log.d("FirebaseRepository", "Last player leaving room $roomCode - deleting room")
                roomRef.removeValue().await()
                Log.d("FirebaseRepository", "Room $roomCode deleted successfully")
                return
            }

            // 4. Otherwise, update the room for remaining players
            val currentPlayerIdRef = roomRef.child("currentPlayerId")
            val currentPlayerIdSnapshot = currentPlayerIdRef.get().await()
            val currentPlayerId = currentPlayerIdSnapshot.getValue(String::class.java)

            val infoRef = roomRef.child("info")
            val hostIdSnapshot = infoRef.child("hostId").get().await()
            val hostId = hostIdSnapshot.getValue(String::class.java)

            // 5. Create a batch update
            val updates = mutableMapOf<String, Any?>()

            // Update turn order
            updates["turnOrder"] = turnOrder

            // If this was the current player, advance to next
            if (currentPlayerId == playerId) {
                updates["currentPlayerId"] = turnOrder[0]
            }

            // If this was the host, assign a new host
            if (hostId == playerId) {
                val newHostId = turnOrder[0]
                updates["info/hostId"] = newHostId
                updates["players/$newHostId/isHost"] = true
            }

            // Remove player entry
            updates["players/$playerId"] = null

            // Execute all updates in a single batch
            roomRef.updateChildren(updates).await()
            Log.d("FirebaseRepository", "Successfully removed player $playerId from room $roomCode")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error removing player: ${e.message}", e)
            throw e
        }
    }

    // Add this method to FirebaseRepository.kt
    suspend fun getRoomHostId(roomCode: String): String? {
        val roomSnapshot = db.child("rooms").child(roomCode).child("info").get().await()
        return roomSnapshot.child("hostId").getValue(String::class.java)
    }

    suspend fun getRoomOnce(roomCode: String): GameRoom? {
        val roomSnapshot = db.child("rooms").child(roomCode).get().await()
        if (!roomSnapshot.exists()) {
            return null
        }

        // Parse the snapshot into a GameRoom object (reuse your existing parsing logic)
        // This is a one-time fetch, not a continuous flow

        return parseRoomSnapshot(roomSnapshot)
    }

    // Helper method to parse a DataSnapshot into a GameRoom
    private fun parseRoomSnapshot(snapshot: DataSnapshot): GameRoom? {
        if (!snapshot.exists()) {
            return null
        }

        try {
            // Extract room info
            val infoSnapshot = snapshot.child("info")
            val roomCode = infoSnapshot.child("roomCode").getValue(String::class.java) ?: ""
            val hostId = infoSnapshot.child("hostId").getValue(String::class.java) ?: ""
            val gameStateStr =
                    infoSnapshot.child("gameState").getValue(String::class.java)
                            ?: GameState.WAITING.name
            val gameState = GameState.valueOf(gameStateStr)
            val kingsCupCount =
                    infoSnapshot.child("kingsCupCount").getValue(Long::class.java)?.toInt() ?: 0
            val gameModeStr =
                    infoSnapshot.child("gameMode").getValue(String::class.java)
                            ?: GameMode.NORMAL.name
            val gameMode = GameMode.valueOf(gameModeStr)
            val createdAt =
                    infoSnapshot.child("createdAt").getValue(Long::class.java)
                            ?: System.currentTimeMillis()

            // Extract turn count
            val turnCount = infoSnapshot.child("turnCount").getValue(Long::class.java)?.toInt() ?: 0

            // Extract players
            val playersMap = mutableMapOf<String, Player>()
            snapshot.child("players").children.forEach { playerSnapshot ->
                val player = playerSnapshot.getValue(Player::class.java)
                if (player != null) {
                    playersMap[player.id] = player
                }
            }

            // Extract cards
            val allCards = mutableListOf<Card>()
            snapshot.child("cards").children.forEach { cardSnapshot ->
                val card = cardSnapshot.getValue(Card::class.java)
                if (card != null) {
                    allCards.add(card)
                }
            }

            // Extract drawn cards IDs for reliable filtering
            val drawnCardIds = mutableSetOf<String>()
            snapshot.child("drawnCardsList").children.forEach {
                drawnCardIds.add(it.key ?: "")
            }

            // Extract drawn cards from dedicated list (more reliable)
            val drawnCards = mutableListOf<Card>()
            snapshot.child("drawnCardsList").children.forEach { cardSnapshot ->
                val card = cardSnapshot.getValue(Card::class.java)
                if (card != null) {
                    drawnCards.add(card)
                }
            }

            // Fallback to filtering if drawnCardsList is empty
            if (drawnCards.isEmpty()) {
                drawnCards.addAll(allCards.filter { it.isDrawn })
                Log.d("FirebaseRepository", "Using fallback cards filter, found ${drawnCards.size} drawn cards")
            }

            // Use drawn card IDs set for more reliable filtering
            val deck = allCards.filter { !drawnCardIds.contains(it.id) && !it.isDrawn }
            Log.d("FirebaseRepository", "Deck contains ${deck.size} undrawn cards")

            // Extract turn order
            val turnOrder =
                    snapshot.child("turnOrder").children.mapNotNull {
                        it.getValue(String::class.java)
                    }

            // Extract current card and player
            val currentCardId = snapshot.child("currentCardId").getValue(String::class.java)
            val currentPlayerId = snapshot.child("currentPlayerId").getValue(String::class.java)

            // Extract current Jack rule and mini-game
            val currentJackRuleId = snapshot.child("currentJackRuleId").getValue(String::class.java)
            val currentJackRuleSelectedBy =
                    snapshot.child("currentJackRuleSelectedBy").getValue(String::class.java)
            val currentMiniGameId = snapshot.child("currentMiniGameId").getValue(String::class.java)
            val currentMiniGameSelectedBy =
                    snapshot.child("currentMiniGameSelectedBy").getValue(String::class.java)

            // Extract selected drinker info
            val selectedDrinkerId = snapshot.child("selectedDrinkerId").getValue(String::class.java)
            val selectedDrinkerBy = snapshot.child("selectedDrinkerBy").getValue(String::class.java)

            // Extract custom Jack Rules
            val customJackRules = mutableListOf<JackRule>()
            snapshot.child("customJackRules").children.forEach { ruleSnapshot ->
                val rule = ruleSnapshot.getValue(JackRule::class.java)
                if (rule != null) {
                    customJackRules.add(rule)
                }
            }

            // Extract custom Mini Games
            val customMiniGames = mutableListOf<MiniGame>()
            snapshot.child("customMiniGames").children.forEach { gameSnapshot ->
                val game = gameSnapshot.getValue(MiniGame::class.java)
                if (game != null) {
                    customMiniGames.add(game)
                }
            }

            // Extract active Jack Rules
            val activeJackRules = mutableMapOf<String, ActiveJackRule>()
            snapshot.child("activeJackRules").children.forEach { ruleSnapshot ->
                val rule = ruleSnapshot.getValue(ActiveJackRule::class.java)
                if (rule != null) {
                    activeJackRules[rule.id] = rule
                }
            }

            // Build GameRoom object
            return GameRoom(
                    roomCode = roomCode,
                    hostId = hostId,
                    players = playersMap,
                    deck = deck,
                    drawnCards = drawnCards,
                    currentCardId = currentCardId,
                    currentPlayerId = currentPlayerId,
                    gameState = gameState,
                    turnOrder = turnOrder,
                    kingsCupCount = kingsCupCount,
                    gameMode = gameMode,
                    createdAt = createdAt,
                    turnCount = turnCount,
                    currentJackRuleId = currentJackRuleId,
                    currentJackRuleSelectedBy = currentJackRuleSelectedBy,
                    currentMiniGameId = currentMiniGameId,
                    currentMiniGameSelectedBy = currentMiniGameSelectedBy,
                    selectedDrinkerId = selectedDrinkerId,
                    selectedDrinkerBy = selectedDrinkerBy,
                    customJackRules = customJackRules,
                    customMiniGames = customMiniGames,
                    activeJackRules = activeJackRules
            )
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error parsing room snapshot", e)
            return null
        }
    }

    suspend fun selectJackRule(roomCode: String, playerId: String, ruleId: String) {
        val roomRef = db.child("rooms").child(roomCode)

        val updates = hashMapOf<String, Any>(
            "currentJackRuleId" to ruleId,
            "currentJackRuleSelectedBy" to playerId
        )

        roomRef.updateChildren(updates).await()
    }

    suspend fun selectMiniGame(roomCode: String, playerId: String, gameId: String) {
        val roomRef = db.child("rooms").child(roomCode)

        val updates = hashMapOf<String, Any>(
            "currentMiniGameId" to gameId,
            "currentMiniGameSelectedBy" to playerId
        )

        roomRef.updateChildren(updates).await()
    }

    // Add this to FirebaseRepository.kt
    suspend fun clearSelections(roomCode: String) {
        val roomRef = db.child("rooms").child(roomCode)

        val updates = hashMapOf<String, Any?>(
            "currentJackRuleId" to null,
            "currentJackRuleSelectedBy" to null,
            "currentMiniGameId" to null,
            "currentMiniGameSelectedBy" to null,
            "selectedDrinkerId" to null,
            "selectedDrinkerBy" to null
        )

        roomRef.updateChildren(updates).await()
    }

    // Method to set player who should drink
    suspend fun selectPlayerToDrink(roomCode: String, selectingPlayerId: String, selectedPlayerId: String) {
        val roomRef = db.child("rooms").child(roomCode)

        val updates = hashMapOf<String, Any>(
            "selectedDrinkerId" to selectedPlayerId,
            "selectedDrinkerBy" to selectingPlayerId
        )

        roomRef.updateChildren(updates).await()

        // Increment the selected player's drink count
        updatePlayerDrinkCount(roomCode, selectedPlayerId, 1)
    }

    suspend fun checkAndClearExpiredMates(roomCode: String, currentPlayerId: String) {
        Log.d("FirebaseRepository", "Checking for expired mates for player $currentPlayerId")
        val roomRef = db.child("rooms").child(roomCode)

        // Get current turn count to avoid immediate deletion
        val turnCountSnapshot = roomRef.child("info/turnCount").get().await()
        val currentTurnCount = turnCountSnapshot.getValue(Long::class.java)?.toInt() ?: 0

        // Get all players
        val playersSnapshot = roomRef.child("players").get().await()
        val updates = mutableMapOf<String, Any?>()

        playersSnapshot.children.forEach { playerSnapshot ->
            val playerId = playerSnapshot.key ?: return@forEach
            val expiresAfterPlayerIdSnapshot = playerSnapshot.child("mateExpiresAfterPlayerId").getValue(String::class.java)
            val createdTurnCountSnapshot = playerSnapshot.child("mateCreatedTurnCount").getValue(Long::class.java)?.toInt() ?: 0

            // Only expire mates when:
            // 1. Current player matches the expiration player AND
            // 2. The mate relationship wasn't created in the current turn
            if (expiresAfterPlayerIdSnapshot == currentPlayerId && createdTurnCountSnapshot < currentTurnCount) {
                Log.d("FirebaseRepository", "Found expired mate relationship for player $playerId " +
                    "(expires after $currentPlayerId's turn which just completed)")
                updates["players/$playerId/mateIds"] = emptyList<String>()
                updates["players/$playerId/mateExpiresAfterPlayerId"] = null
                updates["players/$playerId/mateCreatedTurnCount"] = null
            }
        }

        if (updates.isNotEmpty()) {
            Log.d("FirebaseRepository", "Clearing expired mate relationships for ${updates.size / 3} players")
            roomRef.updateChildren(updates).await()
        } else {
            Log.d("FirebaseRepository", "No expired mate relationships found")
        }
    }

    // Add this method for debugging mate chains
    suspend fun debugMateChain(roomCode: String) {
        val roomRef = db.child("rooms").child(roomCode)
        val playersSnapshot = roomRef.child("players").get().await()

        Log.d("FirebaseRepository", "===== MATE CHAIN DEBUG =====")

        playersSnapshot.children.forEach { playerSnapshot ->
            val playerId = playerSnapshot.key ?: return@forEach
            val playerName = playerSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
            val mateIds = playerSnapshot.child("mateIds").children.mapNotNull { it.getValue(String::class.java) }
            val expiresAfter = playerSnapshot.child("mateExpiresAfterPlayerId").getValue(String::class.java)

            val mateNames = mateIds.map { mateId ->
                playersSnapshot.child(mateId).child("name").getValue(String::class.java) ?: "Unknown"
            }

            val expiresAfterName = if (expiresAfter != null) {
                playersSnapshot.child(expiresAfter).child("name").getValue(String::class.java) ?: "Unknown"
            } else "None"

            Log.d("FirebaseRepository", "Player: $playerName")
            Log.d("FirebaseRepository", "  Mates: ${if (mateNames.isEmpty()) "None" else mateNames.joinToString()}")
            Log.d("FirebaseRepository", "  Expires after: $expiresAfterName's next turn")
        }

        Log.d("FirebaseRepository", "===== END MATE CHAIN DEBUG =====")
    }

    // Call this in the setPlayerMate method before returning:
    // debugMateChain(roomCode)

    // Add this helper method to create decks tailored to game modes
    fun generateDeckForGameMode(gameMode: GameMode): List<Card> {
        val suits = listOf("Hearts", "Diamonds", "Clubs", "Spades")
        val values = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val deck = mutableListOf<Card>()

        // Rules customized by game mode
        val ruleMap = getRuleMapForGameMode(gameMode)

        for (suit in suits) {
            for (value in values) {
                val ruleId = "rule_${value.lowercase()}"
                val ruleTitle = getRuleTitleForCard(value)
                val ruleDescription = ruleMap[value] ?: getDefaultRuleDescription(value)

                deck.add(
                    Card(
                        id = "$value-$suit-${deck.size}",
                        value = value,
                        suit = suit,
                        ruleId = ruleId,
                        ruleTitle = ruleTitle,
                        ruleDescription = ruleDescription,
                        isDrawn = false
                    )
                )
            }
        }

        deck.shuffle()
        return deck
    }

    private fun getRuleMapForGameMode(gameMode: GameMode): Map<String, String> {
        return when (gameMode) {
            GameMode.NORMAL -> mapOf(
                "A" to "Waterfall: Everyone starts drinking; no one can stop until the person to their right stops.",
                "2" to "You: Choose someone to take a drink.",
                "3" to "Me: You take a drink.",
                "4" to "Floor: Last person to touch the floor drinks.",
                "5" to "Guys: All guys drink.",
                "6" to "Chicks: All girls drink.",
                "7" to "Heaven: Last person to point up drinks.",
                "8" to "Mate: Choose a drinking buddy. They drink when you drink.",
                "9" to "Rhyme: Say a word, and the next person must say a word that rhymes.",
                "10" to "Categories: Choose a category, and each player must name something in that category.",
                "J" to "Rule: Make a rule that everyone must follow until the next Jack is drawn.",
                "Q" to "Question Master: You are the Question Master. Anyone who answers your questions drinks.",
                "K" to "King's Cup: Pour some of your drink into the center cup. Last King drawn drinks it all."
            )

            GameMode.CAMIONERO -> mapOf(
                "A" to "Road Trip: Everyone starts drinking; no one can stop until the person to their right stops.",
                "2" to "Pit Stop: Choose someone to take TWO drinks.",
                "3" to "Fuel Up: You take TWO drinks.",
                "4" to "Highway: Last person to pretend to drive a truck drinks twice.",
                "5" to "Radio Check: Everyone must speak like a trucker using CB radio lingo.",
                "6" to "Passing Lane: Person to your left drinks twice.",
                "7" to "Long Haul: Count to 7, replacing any number containing 7 with 'BEEP'. Fail = drink.",
                "8" to "Co-pilot: Choose a drinking buddy. They drink double when you drink.",
                "9" to "Rest Area: Take a break - everyone else drinks.",
                "10" to "Truck Stop: Everyone drinks & shares their best road trip story.",
                "J" to "Weigh Station: Make a hard-drinking rule everyone must follow.",
                "Q" to "Ticket: You are the Highway Patrol. Hand out drinking tickets until next Queen.",
                "K" to "Diesel King: Pour your drink into the center. Last King drinks it all and does a trucker impression."
            )

            GameMode.DESPECHADO -> mapOf(
                "A" to "Tearfall: Everyone drinks to forget, starting with you and continuing around the circle.",
                "2" to "Ex's Friend: Choose someone to drink - they remind you of your ex's friend.",
                "3" to "Memory Lane: Drink and share something you miss about an ex.",
                "4" to "Drunk Text: Last person to pretend to send a drunk text drinks.",
                "5" to "Situationship: If you've ever been in a situationship, drink.",
                "6" to "Ghosted: If you've ever been ghosted, drink.",
                "7" to "On The Rebound: Last person to grab a personal item drinks.",
                "8" to "Heartbreak Buddy: Choose someone who'll drink whenever love songs are mentioned.",
                "9" to "Red Flags: Everyone names a relationship red flag. Anyone who's ignored that flag drinks.",
                "10" to "Breakup Song: Start singing a breakup song. Everyone must join or drink.",
                "J" to "Therapy Session: Make a rule about oversharing that everyone must follow.",
                "Q" to "Drama Queen/King: You assign drinks to anyone talking about happy relationships.",
                "K" to "Crying in the Club: Pour some drink in the center. Last King drinks it all while pretending to cry."
            )

            GameMode.CALENTURIENTOS -> mapOf(
                "A" to "Hot Waterfall: Everyone drinks; can't stop until the person before stops.",
                "2" to "Truth: Choose someone to answer a spicy truth question or drink twice.",
                "3" to "Dare: You must perform a flirty dare or drink twice.",
                "4" to "Floor is Lava: Last to get their feet off the floor drinks twice.",
                "5" to "Never Have I Ever: Play one round of spicy Never Have I Ever.",
                "6" to "Exes: Take a drink for each of your exes.",
                "7" to "Heaven or Hell: Last to point up or down (your choice) drinks.",
                "8" to "Flirt Mate: Choose someone to flirt with until next 8 is drawn.",
                "9" to "Body Shots: If willing, take a body shot from someone of your choice.",
                "10" to "Touchy Categories: Name spicy categories; everyone must answer.",
                "J" to "Spicy Rule: Make a flirty rule everyone follows until next Jack.",
                "Q" to "Strip Question: Ask questions; wrong answers mean removing an accessory.",
                "K" to "Seduction King/Queen: Pour drink in center. Last King drinks while performing a seductive dance."
            )

            GameMode.MEDIA_COPAS -> mapOf(
                "A" to "Dizzy Waterfall: Everyone drinks, spinning around once before stopping.",
                "2" to "Double Vision: Choose two people to drink.",
                "3" to "Triple Threat: You take three drinks.",
                "4" to "Floor-ish: Last person to touch something low drinks.",
                "5" to "High Five: Everyone must high five you or drink.",
                "6" to "Sick Six: Make a sick face, last to copy drinks.",
                "7" to "No Heaven: No pointing up at all until next 7, violations = drink.",
                "8" to "Mate Squared: Choose TWO drinking buddies who drink whenever you drink.",
                "9" to "Slurred Rhyme: Say a word, next person rhymes while speaking with a slur.",
                "10" to "Forgotten Categories: Choose category, name items, but must forget previous ones.",
                "J" to "Confusing Rule: Make a complex rule that's hard to follow when tipsy.",
                "Q" to "Question Quandary: Everyone must speak in questions or drink.",
                "K" to "Tipsy Royalty: Pour drink in center. Last King drinks it while trying to recite the alphabet backward."
            )
        }
    }

    private fun getRuleTitleForCard(value: String): String {
        return when (value) {
            "A" -> "Waterfall"
            "2" -> "You"
            "3" -> "Me"
            "4" -> "Floor"
            "5" -> "Guys"
            "6" -> "Chicks"
            "7" -> "Heaven"
            "8" -> "Mate"
            "9" -> "Rhyme"
            "10" -> "Categories"
            "J" -> "Make a Rule"
            "Q" -> "Question Master"
            "K" -> "King's Cup"
            else -> "Unknown Rule"
        }
    }

    private fun getDefaultRuleDescription(value: String): String {
        return when (value) {
            "A" -> "Waterfall: Everyone starts drinking; no one can stop until the person to their right stops."
            "2" -> "You: Choose someone to take a drink."
            "3" -> "Me: You take a drink."
            "4" -> "Floor: Last person to touch the floor drinks."
            "5" -> "Guys: All guys drink."
            "6" -> "Chicks: All girls drink."
            "7" -> "Heaven: Last person to point up drinks."
            "8" -> "Mate: Choose a drinking buddy. They drink when you drink."
            "9" -> "Rhyme: Say a word, and the next person must say a word that rhymes."
            "10" -> "Categories: Choose a category, and each player must name something in that category."
            "J" -> "Rule: Make a rule that everyone must follow until the next Jack is drawn."
            "Q" -> "Question Master: You are the Question Master. Anyone who answers your questions drinks."
            "K" -> "King's Cup: Pour some of your drink into the center cup. Last King drawn drinks it all."
            else -> "Draw a card to see the rule"
        }
    }

    // Add these methods to the FirebaseRepository class

    // Save a custom Jack Rule
    suspend fun saveCustomJackRule(roomCode: String, rule: JackRule) {
        Log.d("FirebaseRepository", "Saving custom Jack Rule: ${rule.title}")
        val roomRef = db.child("rooms").child(roomCode)

        // Save to the custom rules collection
        roomRef.child("customJackRules").child(rule.id).setValue(rule).await()
        Log.d("FirebaseRepository", "Custom Jack Rule saved successfully")
    }

    // Save a custom Mini Game
    suspend fun saveCustomMiniGame(roomCode: String, game: MiniGame) {
        Log.d("FirebaseRepository", "Saving custom Mini Game: ${game.title}")
        val roomRef = db.child("rooms").child(roomCode)

        // Save to the custom games collection
        roomRef.child("customMiniGames").child(game.id).setValue(game).await()
        Log.d("FirebaseRepository", "Custom Mini Game saved successfully")
    }

    // Get all custom Jack Rules for a room
    suspend fun getCustomJackRules(roomCode: String): List<JackRule> {
        Log.d("FirebaseRepository", "Fetching custom Jack Rules for room $roomCode")
        val roomRef = db.child("rooms").child(roomCode)

        val rulesSnapshot = roomRef.child("customJackRules").get().await()
        val rules = mutableListOf<JackRule>()

        rulesSnapshot.children.forEach { ruleSnapshot ->
            val rule = ruleSnapshot.getValue(JackRule::class.java)
            if (rule != null) {
                rules.add(rule)
            }
        }

        Log.d("FirebaseRepository", "Fetched ${rules.size} custom Jack Rules")
        return rules
    }

    // Get all custom Mini Games for a room
    suspend fun getCustomMiniGames(roomCode: String): List<MiniGame> {
        Log.d("FirebaseRepository", "Fetching custom Mini Games for room $roomCode")
        val roomRef = db.child("rooms").child(roomCode)

        val gamesSnapshot = roomRef.child("customMiniGames").get().await()
        val games = mutableListOf<MiniGame>()

        gamesSnapshot.children.forEach { gameSnapshot ->
            val game = gameSnapshot.getValue(MiniGame::class.java)
            if (game != null) {
                games.add(game)
            }
        }

        Log.d("FirebaseRepository", "Fetched ${games.size} custom Mini Games")
        return games
    }

    // Add these methods to fetch individual custom rules and games
    suspend fun getCustomJackRule(roomCode: String, ruleId: String): JackRule? {
        Log.d("FirebaseRepository", "Fetching custom Jack Rule with ID: $ruleId")
        val roomRef = db.child("rooms").child(roomCode)

        val ruleSnapshot = roomRef.child("customJackRules").child(ruleId).get().await()
        if (!ruleSnapshot.exists()) {
            Log.d("FirebaseRepository", "Custom Jack Rule not found: $ruleId")
            return null
        }

        val rule = ruleSnapshot.getValue(JackRule::class.java)
        Log.d("FirebaseRepository", "Found custom Jack Rule: ${rule?.title}")
        return rule
    }

    suspend fun getCustomMiniGame(roomCode: String, gameId: String): MiniGame? {
        Log.d("FirebaseRepository", "Fetching custom Mini Game with ID: $gameId")
        val roomRef = db.child("rooms").child(roomCode)

        val gameSnapshot = roomRef.child("customMiniGames").child(gameId).get().await()
        if (!gameSnapshot.exists()) {
            Log.d("FirebaseRepository", "Custom Mini Game not found: $gameId")
            return null
        }

        val game = gameSnapshot.getValue(MiniGame::class.java)
        Log.d("FirebaseRepository", "Found custom Mini Game: ${game?.title}")
        return game
    }
}
