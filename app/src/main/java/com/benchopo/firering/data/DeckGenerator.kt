package com.benchopo.firering.data

import com.benchopo.firering.model.Card
import com.benchopo.firering.model.CardRule

fun generateDeck(): List<Card> {
    val suits = listOf("Hearts", "Diamonds", "Clubs", "Spades")
    val values = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")

    // Updated card rules based on the provided game rules
    val rulesMap = mapOf(
        "A" to CardRule("All Drink", "All players drink."),
        "2" to CardRule("Choose", "Choose someone to drink."),
        "3" to CardRule("Me", "You drink."),
        "4" to CardRule("Hoes", "All girls playing drink."),
        "5" to CardRule("Thumbs", "All players put thumbs on table, last one drinks."),
        "6" to CardRule("Dicks", "All men playing drink."),
        "7" to CardRule("Heaven", "All players raise hands, last one drinks."),
        "8" to CardRule("Mate", "Choose a drinking buddy who drinks when you drink."),
        "9" to CardRule("Rhyme", "Say a word; others must rhyme or drink."),
        "10" to CardRule("Game", "Select a Mini Game for everyone to play."),
        "J" to CardRule("Rule", "Select a Jack Rule to establish until your next turn."),
        "Q" to CardRule("Cultura Chupistica", "Name a category, players take turns naming items."),
        "K" to CardRule("King's Cup", "Pour drink into the King's Cup. Fourth King drinker drinks it all.")
    )

    val deck = mutableListOf<Card>()

    for (suit in suits) {
        for (value in values) {
            val rule = rulesMap[value]
            deck.add(Card(value = value, suit = suit, rule = rule))
        }
    }

    deck.shuffle()
    return deck
}

fun getCardRule(card: Card?): String {
    if (card == null) return "Draw a card to see the rule"

    return when (card.value) {
        "A" -> "All players drink."
        "2" -> "Choose: The player who drew the card chooses who will drink."
        "3" -> "Me: The player who drew the card drinks."
        "4" -> "Hoes: All girls playing drink."
        "5" -> "Thumbs: All players must put thumbs on the table, last one drinks."
        "6" -> "Dicks: All men playing drink."
        "7" -> "Heaven: All players must raise both hands, last one drinks."
        "8" -> "Mate: Choose another player as your mate. When one drinks, both drink."
        "9" -> "Rhyme: Say a word, others must rhyme or drink."
        "10" -> "Game: Select a Mini Game for everyone to play. Losers drink."
        "J" -> "Rule: Select a Jack Rule that all must follow until your next turn."
        "Q" -> "Cultura Chupistica: Name a category, players take turns naming items in that category."
        "K" -> "King's Cup: Pour drink into the King's Cup. Fourth King drawer drinks it all."
        else -> "Unknown card rule"
    }
}
