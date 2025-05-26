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
        val availableDrinks: List<Drink> = emptyList(),
        val activeRules: Map<String, ActiveRule> = emptyMap(),
        val activeGame: MiniGameSession? = null,
        val gameMode: GameMode = GameMode.NORMAL,
        val createdAt: Long = System.currentTimeMillis(),
        val settings: GameSettings = GameSettings()
)
