package com.benchopo.firering.model

data class CardRule(
        val id: String = "",
        val cardValue: String = "", // The card value this applies to
        val title: String = "",
        val description: String = "",
        val ruleType: RuleType = RuleType.STANDARD,
        val createdByPlayerId: String? = null,
        val isCustom: Boolean = false,
        val isDefault: Boolean = true
) {
    constructor(
            title: String,
            description: String
    ) : this(
            id = title.lowercase().replace(" ", "_"),
            cardValue = "",
            title = title,
            description = description
    )
}
