package com.benchopo.firering.model

data class MiniGame(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: MiniGameType = MiniGameType.CHALLENGE,
    val gameMode: GameMode = GameMode.NORMAL,
    val isCustom: Boolean = false,
    val createdByPlayerId: String? = null,
    val popularity: Int = 0
)