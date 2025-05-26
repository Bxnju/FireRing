package com.benchopo.firering.model

data class GameSettings(
        val allowGuestPlayers: Boolean = true,
        val maxPlayers: Int = 10,
        val enableCustomRules: Boolean = true,
        val autoAdvanceTurns: Boolean = true,
        val kingsCupAmount: Int = 4,
        val breakRingEnabled: Boolean = true,
        val breakRingThreshold: Double = 0.33 // 1/3 of cards
)
