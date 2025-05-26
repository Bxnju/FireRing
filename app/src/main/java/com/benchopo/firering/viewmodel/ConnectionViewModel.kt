package com.benchopo.firering.viewmodel

import androidx.lifecycle.ViewModel
import com.benchopo.firering.model.*
import kotlinx.coroutines.flow.*

class ConnectionViewModel(
        private val userViewModel: UserViewModel,
        private val gameViewModel: GameViewModel
) : ViewModel() {
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    // Connection management methods
    fun updateOnlineStatus(isOnline: Boolean) {
        _isOnline.value = isOnline
        userViewModel.userId.value?.let { userId ->
            gameViewModel.roomCode.value?.let { roomCode ->
                // Update online status in Firebase
            }
        }
    }
}
