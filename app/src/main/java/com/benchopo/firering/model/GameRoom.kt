package com.benchopo.firering.model

data class GameRoom(
    val roomCode: String = "",
    val hostId: String = "",
    val players: Map<String, Player> = emptyMap(),
    val deck: List<Card> = emptyList(),
    val currentCard: Card? = null,
    val currentPlayerId: String? = null,
    val started: Boolean = false
)
