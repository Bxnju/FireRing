package com.benchopo.firering.model

data class GameRoom(
    val roomCode: String = "",
    val hostId: String = "",
    val players: Map<String, Player> = emptyMap(),
    val deck: List<Card> = emptyList(),
    val drawnCards: List<Card> = emptyList(),
    val currentCardId: String? = null,
    val currentPlayerId: String? = null,
    val gameState: GameState = GameState.WAITING,
    val turnOrder: List<String> = emptyList(),
    val kingsCupCount: Int = 0,
    val gameMode: GameMode = GameMode.NORMAL,
    val createdAt: Long = 0,
    val currentJackRuleId: String? = null,
    val currentJackRuleSelectedBy: String? = null,
    val currentMiniGameId: String? = null,
    val currentMiniGameSelectedBy: String? = null
)
