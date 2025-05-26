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
    suspend fun createGameRoom(hostPlayerName: String, userId: String): String {
        val roomCode = generateRoomCode()
        val deck = generateDeck()
        val hostPlayerId = userId // Use the provided userId instead of generating one
        val player =
                Player(id = hostPlayerId, name = hostPlayerName, isHost = true, isGuest = false)

        // Get default drinks
        val defaultDrinks = getDefaultDrinks()

        // Create room structure
        val roomRef = db.child("rooms").child(roomCode)

        // Create the room info
        val roomInfo =
                mapOf(
                        "roomCode" to roomCode,
                        "hostId" to hostPlayerId,
                        "gameState" to GameState.WAITING.name,
                        "kingsCupCount" to 0,
                        "gameMode" to GameMode.NORMAL.name,
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

        return roomCode
    }

    suspend fun joinRoom(roomCode: String, playerName: String): String? {
        // Check if room exists
        val roomSnapshot = db.child("rooms").child(roomCode).get().await()
        if (!roomSnapshot.exists()) {
            return null
        }

        // Get current game state
        val gameStateSnapshot = roomSnapshot.child("info/gameState").getValue(String::class.java)
        if (gameStateSnapshot == GameState.FINISHED.name) {
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
                            val drawnCards = allCards.filter { it.isDrawn }
                            val deck = allCards.filter { !it.isDrawn }

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
                                            createdAt = createdAt
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

    suspend fun drawCard(roomCode: String, playerId: String): Card? {
        val roomRef = db.child("rooms").child(roomCode)

        // Get undrawn cards
        val cardsSnapshot = roomRef.child("cards").get().await()
        val undrawnCards = mutableListOf<Card>()
        cardsSnapshot.children.forEach { cardSnapshot ->
            val card = cardSnapshot.getValue(Card::class.java)
            if (card != null && !card.isDrawn) {
                undrawnCards.add(card)
            }
        }

        if (undrawnCards.isEmpty()) {
            return null
        }

        // Get a random undrawn card
        val drawnCard = undrawnCards.random()

        // Update card as drawn
        val updates =
                hashMapOf<String, Any>(
                        "cards/${drawnCard.id}/isDrawn" to true,
                        "cards/${drawnCard.id}/drawnByPlayerId" to playerId,
                        "cards/${drawnCard.id}/drawnTimestamp" to System.currentTimeMillis(),
                        "currentCardId" to drawnCard.id
                )

        // If it's a King, update kings cup count
        if (drawnCard.value == "K") {
            val kingsSnapshot = roomRef.child("info/kingsCupCount").get().await()
            val kingsCount = kingsSnapshot.getValue(Long::class.java)?.toInt() ?: 0
            updates["info/kingsCupCount"] = kingsCount + 1
        }

        roomRef.updateChildren(updates).await()

        return drawnCard.copy(
                isDrawn = true,
                drawnByPlayerId = playerId,
                drawnTimestamp = System.currentTimeMillis()
        )
    }

    suspend fun advanceTurn(roomCode: String) {
        val roomRef = db.child("rooms").child(roomCode)

        // Get current player and turn order
        val currentPlayerIdSnapshot = roomRef.child("currentPlayerId").get().await()
        val currentPlayerId = currentPlayerIdSnapshot.getValue(String::class.java) ?: return

        val turnOrderSnapshot = roomRef.child("turnOrder").get().await()
        val turnOrder = turnOrderSnapshot.children.map { it.getValue(String::class.java)!! }

        if (turnOrder.isEmpty()) return

        // Find next player
        val currentIndex = turnOrder.indexOf(currentPlayerId)
        val nextIndex = (currentIndex + 1) % turnOrder.size
        val nextPlayerId = turnOrder[nextIndex]

        // Update current player
        roomRef.child("currentPlayerId").setValue(nextPlayerId).await()
    }

    suspend fun updatePlayerDrinkCount(roomCode: String, playerId: String, increment: Int = 1) {
        val playerRef = db.child("rooms").child(roomCode).child("players").child(playerId)
        val drinkCountSnapshot = playerRef.child("drinkCount").get().await()
        val currentCount = drinkCountSnapshot.getValue(Long::class.java)?.toInt() ?: 0
        playerRef.child("drinkCount").setValue(currentCount + increment).await()
    }

    suspend fun setPlayerMate(roomCode: String, playerId: String, mateId: String) {
        val roomRef = db.child("rooms").child(roomCode)

        // Get all existing mates for both players
        val playerMates = getMateIdsRecursively(roomRef, playerId)
        val mateMates = getMateIdsRecursively(roomRef, mateId)

        // Combine all mates from both players
        val allMates = (playerMates + mateMates + setOf(playerId, mateId)).toSet()

        // Update mates list for all players in the chain
        val updates = mutableMapOf<String, Any>()
        for (id in allMates) {
            val matesList = allMates.filter { it != id }
            updates["players/$id/mateIds"] = matesList
        }

        // Apply all updates at once
        roomRef.updateChildren(updates).await()
    }

    // Helper function to recursively get all mates
    private suspend fun getMateIdsRecursively(
            roomRef: DatabaseReference,
            playerId: String
    ): Set<String> {
        val matesSnapshot = roomRef.child("players/$playerId/mateIds").get().await()
        val directMates = mutableSetOf<String>()

        matesSnapshot.children.forEach {
            val mate = it.getValue(String::class.java)
            if (mate != null) directMates.add(mate)
        }

        // If no mates, return empty set
        if (directMates.isEmpty()) return emptySet()

        // Recursively get mates of mates
        val allMates = directMates.toMutableSet()
        for (mateId in directMates) {
            allMates.addAll(getMateIdsRecursively(roomRef, mateId))
        }

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

            // 2. Handle current player transition if needed
            val currentPlayerIdRef = roomRef.child("currentPlayerId")
            val currentPlayerIdSnapshot = currentPlayerIdRef.get().await()
            val currentPlayerId = currentPlayerIdSnapshot.getValue(String::class.java)

            // 3. Handle host transition if needed
            val infoRef = roomRef.child("info")
            val hostIdSnapshot = infoRef.child("hostId").get().await()
            val hostId = hostIdSnapshot.getValue(String::class.java)

            // 4. Create a batch update
            val updates = mutableMapOf<String, Any?>()

            // Update turn order
            if (turnOrder.isNotEmpty()) {
                updates["turnOrder"] = turnOrder
            }

            // If this was the current player, advance to next
            if (currentPlayerId == playerId && turnOrder.isNotEmpty()) {
                updates["currentPlayerId"] = turnOrder[0]
            }

            // If this was the host, assign a new host
            if (hostId == playerId && turnOrder.isNotEmpty()) {
                val newHostId = turnOrder[0]
                updates["info/hostId"] = newHostId
                updates["players/$newHostId/isHost"] = true
            }

            // If no players left, mark game as finished
            if (turnOrder.isEmpty()) {
                updates["info/gameState"] = GameState.FINISHED.name
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
}
