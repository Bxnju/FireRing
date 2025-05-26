package com.benchopo.firering.model

data class Card(
        val id: String = "",
        val value: String = "", // "A", "2", "3", ..., "K"
        val suit: String = "", // "Hearts", "Diamonds", etc.
        val ruleId: String? = null, // Reference to the rule
        val isDrawn: Boolean = false,
        val drawnByPlayerId: String? = null,
        val drawnTimestamp: Long? = null,
        val positionInRing: Int = 0,
        val isInRing: Boolean = true // For ring breaking detection
) {
    constructor(
            value: String,
            suit: String,
            rule: CardRule?
    ) : this(id = "$value-$suit", value = value, suit = suit, ruleId = rule?.id)
}
