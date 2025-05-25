package com.benchopo.firering.model

data class Card(
    val value: String = "",     // ej: "K", "7", "A"
    val suit: String = "",      // ej: "Hearts", "Spades"
    val rule: CardRule? = null
)

