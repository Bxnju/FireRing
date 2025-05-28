package com.benchopo.firering.model

import com.google.firebase.database.PropertyName

data class Player(
        val id: String = "",
        val name: String = "",

        @get:PropertyName("isHost")
        @set:PropertyName("isHost")
        var isHost: Boolean = false,

        @get:PropertyName("isGuest")
        @set:PropertyName("isGuest")
        var isGuest: Boolean = true,

        val selectedDrinkId: String? = null,
        val drinkCount: Int = 0,
        val mateIds: List<String> = emptyList(),
        val mateExpiresAfterPlayerId: String? = null,  // New field to track when mates expire

        @get:PropertyName("isConnected")
        @set:PropertyName("isConnected")
        var isConnected: Boolean = true,

        val lastActiveTimestamp: Long = System.currentTimeMillis(),
        val customRuleIds: List<String> = emptyList(),
        val turnIndex: Int? = null
)
