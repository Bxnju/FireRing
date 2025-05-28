package com.benchopo.firering.model

import java.util.UUID

data class ActiveJackRule(
    val id: String = UUID.randomUUID().toString(),
    val ruleId: String = "",
    val title: String = "",
    val description: String = "",
    val type: RuleType = RuleType.STANDARD,
    val gameMode: GameMode = GameMode.NORMAL,
    val createdByPlayerId: String = "",
    val createdByPlayerName: String = "",
    val expiresAfterPlayerId: String = "",
    val isCustom: Boolean = false,
    val createdTurnCount: Int = 0
)