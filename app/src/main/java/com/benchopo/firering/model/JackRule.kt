package com.benchopo.firering.model

data class JackRule(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: RuleType = RuleType.STANDARD,
    val gameMode: GameMode = GameMode.NORMAL,
    val isCustom: Boolean = false,
    val createdByPlayerId: String? = null,
    val popularity: Int = 0
)