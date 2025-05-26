package com.benchopo.firering.model

enum class GameState {
    WAITING,   // Waiting for players to join
    PLAYING,   // Game in progress
    PAUSED,    // Game temporarily paused
    FINISHED   // Game has ended
}