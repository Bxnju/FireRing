package com.benchopo.firering.data

import com.benchopo.firering.model.GameRoom
import com.benchopo.firering.model.Player
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseRepository {

    private val db = FirebaseDatabase.getInstance().reference.child("rooms")

    suspend fun createGameRoom(hostPlayerName: String): String {
        val roomCode = generateRoomCode()
        val deck = generateDeck()
        val hostPlayerId = UUID.randomUUID().toString()
        val players = mapOf(hostPlayerId to Player(id = hostPlayerId, name = hostPlayerName))

        val gameRoom = GameRoom(
            roomCode = roomCode,
            hostId = hostPlayerId,
            players = players,
            deck = deck,
            currentCard = null,
            currentPlayerId = hostPlayerId,
            started = false
        )

        db.child(roomCode).setValue(gameRoom).await()
        return roomCode
    }

    private fun generateRoomCode(): String {
        // Código simple alfanumérico de 5 caracteres
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..5).map { chars.random() }.joinToString("")
    }

    // Aquí podemos agregar más funciones: joinRoom, sacar carta, etc.
}
