package com.benchopo.firering.model

data class MiniGame(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdByPlayerId: String? = null,
    val isCustom: Boolean = false
)