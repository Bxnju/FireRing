package com.benchopo.firering.model

data class ActiveRule(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val createdByPlayerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAfterPlayerId: String = "",
    val ruleType: RuleType = RuleType.CUSTOM
)