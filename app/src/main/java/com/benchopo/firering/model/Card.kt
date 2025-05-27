package com.benchopo.firering.model

data class Card(
        val id: String = "",
        val value: String = "", // "A", "2", "3", ..., "K"
        val suit: String = "", // "Hearts", "Diamonds", etc.
        val ruleId: String? = null, // Reference to the rule
        val ruleTitle: String = "", // Title of the rule (e.g. "Waterfall")
        val ruleDescription: String = "", // Full description of the rule
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
    ) : this(
            id = "$value-$suit",
            value = value,
            suit = suit,
            ruleId = rule?.id,
            ruleTitle = rule?.title ?: "",
            ruleDescription = getCardRuleText(value)
    )
}

// Helper function to get rule text based on card value
private fun getCardRuleText(value: String): String {
    return when (value) {
        "A" -> "Waterfall: Everyone starts drinking. No one can stop until the person to their right stops."
        "2" -> "You: You choose someone to drink."
        "3" -> "Me: You drink."
        "4" -> "Floor: Last one to touch the floor drinks."
        "5" -> "Guys: All guys drink."
        "6" -> "Girls: All girls drink."
        "7" -> "Heaven: Last one to raise hand drinks."
        "8" -> "Mate: Choose a drinking buddy who drinks when you drink."
        "9" -> "Rhyme: Say a word; others must rhyme or drink."
        "10" -> "Categories: Pick a category; others must answer or drink."
        "J" -> "Never Have I Ever: Say something you haven't done. Those who have done it drink."
        "Q" -> "Question Master: Ask questions; if someone answers (not with a question), they drink."
        "K" -> "King's Cup: Pour some drink into the cup. The player who draws the last King drinks it all!"
        else -> "Unknown card rule"
    }
}
