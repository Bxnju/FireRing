package com.benchopo.firering.data

import com.benchopo.firering.model.Card
import com.benchopo.firering.model.CardRule

fun generateDeck(): List<Card> {
    val suits = listOf("Hearts", "Diamonds", "Clubs", "Spades")
    val values = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")

    // Mapeo de reglas básicas (puedes ajustar títulos y descripciones)
    val rulesMap = mapOf(
        "A" to CardRule("Waterfall", "Everyone starts drinking at the same time."),
        "2" to CardRule("You", "You choose someone to drink."),
        "3" to CardRule("Me", "You drink."),
        "4" to CardRule("Floor", "Last one to touch the floor drinks."),
        "5" to CardRule("Guys", "All guys drink."),
        "6" to CardRule("Girls", "All girls drink."),
        "7" to CardRule("Heaven", "Last one to raise hand drinks."),
        "8" to CardRule("Mate", "Choose a drinking buddy."),
        "9" to CardRule("Rhyme", "Say a word; others must rhyme or drink."),
        "10" to CardRule("Categories", "Pick a category; others must answer or drink."),
        "J" to CardRule("Never Have I Ever", "Say something you haven't done."),
        "Q" to CardRule("Question Master", "Ask questions; if someone fails to answer, they drink."),
        "K" to CardRule("King's Cup", "Pour some drink into the cup.")
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
