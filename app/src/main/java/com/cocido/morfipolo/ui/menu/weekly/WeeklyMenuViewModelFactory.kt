package com.cocido.morfipolo.ui.menu.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.remote.AuthManager
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.data.repository.VoteRepository

class WeeklyMenuViewModelFactory(
    private val menuRepository: MenuRepository,
    private val authManager: AuthManager,
    private val voteRepository: VoteRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeeklyMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeeklyMenuViewModel(menuRepository, authManager, voteRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

