package com.benchopo.firering.model

data class CustomRule(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val createdByPlayerId: String? = null,
    val popularity: Int = 0
)