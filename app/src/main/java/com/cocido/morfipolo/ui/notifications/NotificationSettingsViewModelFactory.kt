package com.cocido.morfipolo.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cocido.morfipolo.data.repository.NotificationConfigRepository

class NotificationSettingsViewModelFactory(
    private val notificationConfigRepository: NotificationConfigRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationSettingsViewModel(notificationConfigRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

