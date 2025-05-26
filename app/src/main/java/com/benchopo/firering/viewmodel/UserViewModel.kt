package com.benchopo.firering.viewmodel

import androidx.lifecycle.ViewModel
import com.benchopo.firering.model.*
import kotlinx.coroutines.flow.*

class UserViewModel : ViewModel() {
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName

    // User-specific methods like setting display name, etc.
    fun setUserInfo(id: String, name: String) {
        _userId.value = id
        _displayName.value = name
    }

    fun clearUserData() {
        _userId.value = null
        _displayName.value = null
    }
}
