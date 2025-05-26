package com.benchopo.firering.model

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val pinHash: String = "",  // We'll store a hash of the PIN, not the PIN itself
    val lastLogin: Long = System.currentTimeMillis(),
    val savedRuleIds: List<String> = emptyList(),
    val customRuleIds: List<String> = emptyList(),
    val favoriteRoomCodes: List<String> = emptyList()
)