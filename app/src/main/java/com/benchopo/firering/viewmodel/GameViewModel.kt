package com.benchopo.firering.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benchopo.firering.data.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun createRoom(hostPlayerName: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val code = repository.createGameRoom(hostPlayerName)
                _roomCode.value = code
            } catch (e: Exception) {
                e.printStackTrace()
                _roomCode.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    // Aquí puedes agregar funciones para unirse a sala, sacar carta, etc.
}
