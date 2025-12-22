package com.cocido.morfipolo.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.repository.NotificationConfigRepository
import com.cocido.morfipolo.domain.model.CustomNotification
import com.cocido.morfipolo.util.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationSettingsUiState {
    object Loading : NotificationSettingsUiState()
    data class Success(val notifications: List<CustomNotification>) : NotificationSettingsUiState()
    data class Error(val message: String) : NotificationSettingsUiState()
}

class NotificationSettingsViewModel(
    private val notificationConfigRepository: NotificationConfigRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationSettingsUiState>(NotificationSettingsUiState.Loading)
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                val notifications = notificationConfigRepository.getAllNotifications()
                _uiState.value = NotificationSettingsUiState.Success(notifications)
            } catch (e: Exception) {
                _uiState.value = NotificationSettingsUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun saveNotification(notification: CustomNotification) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                notificationConfigRepository.saveNotification(notification)
                // Reprogramar alarmas en background para no bloquear UI
                AlarmScheduler.scheduleCustomNotifications(context, notificationConfigRepository)
                loadNotifications()
            } catch (e: Exception) {
                _uiState.value = NotificationSettingsUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun deleteNotification(notification: CustomNotification) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                notificationConfigRepository.deleteNotification(notification.id)
                // Reprogramar alarmas en background para no bloquear UI
                AlarmScheduler.scheduleCustomNotifications(context, notificationConfigRepository)
                loadNotifications()
            } catch (e: Exception) {
                _uiState.value = NotificationSettingsUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun toggleNotification(notification: CustomNotification, isEnabled: Boolean) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val updatedNotification = notification.copy(isEnabled = isEnabled)
                notificationConfigRepository.saveNotification(updatedNotification)
                // Reprogramar alarmas en background para no bloquear UI
                AlarmScheduler.scheduleCustomNotifications(context, notificationConfigRepository)
                loadNotifications()
            } catch (e: Exception) {
                _uiState.value = NotificationSettingsUiState.Error(e.message ?: "Error al actualizar")
            }
        }
    }
}
