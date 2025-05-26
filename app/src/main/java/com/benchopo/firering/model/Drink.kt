package com.benchopo.firering.model

data class Drink(
        val id: String = "",
        val name: String = "",
        val alcoholContent: Double? = null,
        val emoji: String? = null,
        val createdByPlayerId: String? = null,
        val isCustom: Boolean = false
)
