package com.benchopo.firering.model

data class Player(
        val id: String = "",
        val name: String = "",
        val isHost: Boolean = false,
        val isGuest: Boolean = true,
        val selectedDrinkId: String? = null,
        val drinkCount: Int = 0,
        val mateIds: List<String> = emptyList(),
        val isConnected: Boolean = true,
        val lastActiveTimestamp: Long = System.currentTimeMillis(),
        val customRuleIds: List<String> = emptyList(),
        val turnIndex: Int? = null
)
