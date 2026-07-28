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
                android.util.Log.d("NotificationSettingsVM", "💾 Iniciando guardado de notificación: ${notification.id}")
                notificationConfigRepository.saveNotification(notification)
                android.util.Log.d("NotificationSettingsVM", "✅ Notificación guardada, reprogramando alarmas...")
                // Reprogramar alarmas en background para no bloquear UI
                AlarmScheduler.scheduleCustomNotifications(context, notificationConfigRepository)
                android.util.Log.d("NotificationSettingsVM", "🔄 Recargando lista de notificaciones...")
                // Recargar en el mismo hilo IO para evitar race conditions
                val notifications = notificationConfigRepository.getAllNotifications()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = NotificationSettingsUiState.Success(notifications)
                }
                android.util.Log.d("NotificationSettingsVM", "✅ Lista actualizada con ${notifications.size} notificaciones")
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsVM", "❌ Error al guardar: ${e.message}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = NotificationSettingsUiState.Error(e.message ?: "Error al guardar")
                }
            }
        }
    }

    fun deleteNotification(notification: CustomNotification) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("NotificationSettingsVM", "🗑️ Eliminando notificación: ${notification.id}")
                notificationConfigRepository.deleteNotification(notification.id)
                // Reprogramar alarmas en background para no bloquear UI
                AlarmScheduler.scheduleCustomNotifications(context, notificationConfigRepository)
                
                // Recargar en el mismo hilo IO para evitar race conditions
                val notifications = notificationConfigRepository.getAllNotifications()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = NotificationSettingsUiState.Success(notifications)
                }
                android.util.Log.d("NotificationSettingsVM", "✅ Notificación eliminada, quedan ${notifications.size}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsVM", "❌ Error al eliminar: ${e.message}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = NotificationSettingsUiState.Error(e.message ?: "Error al eliminar")
                }
            }
        }
    }

    fun toggleNotification(notification: CustomNotification, isEnabled: Boolean) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Obtener la versión más reciente de la notificación desde el repositorio
                val currentNotification = notificationConfigRepository.getNotificationById(notification.id)
                if (currentNotification == null) {
                    android.util.Log.w("NotificationSettingsVM", "⚠️ Notificación ${notification.id} no encontrada")
                    return@launch
                }
                
                // Actualizar solo el estado isEnabled, manteniendo los demás datos actuales
                val updatedNotification = currentNotification.copy(isEnabled = isEnabled)
                android.util.Log.d("NotificationSettingsVM", "🔄 Toggle notificación: ${updatedNotification.id}, isEnabled=$isEnabled")
                
                notificationConfigRepository.saveNotification(updatedNotification)
                // Reprogramar alarmas en background para no bloquear UI
                AlarmScheduler.scheduleCustomNotifications(context, notificationConfigRepository)
                
                // Recargar en el mismo hilo IO para evitar race conditions
                val notifications = notificationConfigRepository.getAllNotifications()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = NotificationSettingsUiState.Success(notifications)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationSettingsVM", "❌ Error en toggle: ${e.message}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = NotificationSettingsUiState.Error(e.message ?: "Error al actualizar")
                }
            }
        }
    }
}
