package com.cocido.morfipolo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cocido.morfipolo.data.remote.AuthManager
import com.cocido.morfipolo.data.repository.UserRepository

class ProfileViewModelFactory(
    private val userRepository: UserRepository,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

