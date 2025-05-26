package com.benchopo.firering.model

data class MiniGameSession(
    val miniGameId: String = "",
    val startedByPlayerId: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val state: String = "WAITING", // WAITING, COMPLETED
    val loserIds: List<String> = emptyList(),
    val completedAt: Long? = null
)
