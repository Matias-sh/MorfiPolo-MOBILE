package com.cocido.morfipolo.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cocido.morfipolo.domain.model.CustomNotification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Repositorio para gestionar las configuraciones de notificaciones personalizadas.
 * Guarda y carga las notificaciones desde SharedPreferences usando JSON.
 */
class NotificationConfigRepository(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "notification_config_prefs"
        private const val KEY_NOTIFICATIONS = "custom_notifications"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Obtiene todas las notificaciones configuradas.
     */
    fun getAllNotifications(): List<CustomNotification> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<CustomNotification>>() {}.type
                val notifications = gson.fromJson<List<CustomNotification>>(json, type) ?: emptyList()
                // Log para debugging
                android.util.Log.d("NotificationConfigRepository", "📥 Cargadas ${notifications.size} notificaciones desde SharedPreferences")
                notifications.forEach { notification ->
                    val daysStr = notification.daysOfWeek.sorted().joinToString(", ") { 
                        com.cocido.morfipolo.domain.model.CustomNotification.getDayNameFull(it) 
                    }
                    android.util.Log.d("NotificationConfigRepository", "  - ${notification.getFormattedTime()}: días [$daysStr] (valores: ${notification.daysOfWeek.sorted()})")
                }
                notifications
            } catch (e: Exception) {
                android.util.Log.e("NotificationConfigRepository", "Error al parsear notificaciones: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Guarda todas las notificaciones.
     */
    fun saveNotifications(notifications: List<CustomNotification>) {
        try {
            val json = gson.toJson(notifications)
            prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
            android.util.Log.d("NotificationConfigRepository", "✅ Guardadas ${notifications.size} notificaciones")
            // Log detallado de cada notificación para debugging
            notifications.forEach { notification ->
                val daysStr = notification.daysOfWeek.sorted().joinToString(", ") { 
                    com.cocido.morfipolo.domain.model.CustomNotification.getDayNameFull(it) 
                }
                android.util.Log.d("NotificationConfigRepository", "  - ${notification.getFormattedTime()}: días [$daysStr] (valores: ${notification.daysOfWeek.sorted()})")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationConfigRepository", "Error al guardar notificaciones: ${e.message}")
        }
    }
    
    /**
     * Obtiene una notificación por su ID.
     */
    fun getNotificationById(id: String): CustomNotification? {
        return getAllNotifications().find { it.id == id }
    }
    
    /**
     * Agrega o actualiza una notificación.
     */
    fun saveNotification(notification: CustomNotification) {
        val notifications = getAllNotifications().toMutableList()
        val existingIndex = notifications.indexOfFirst { it.id == notification.id }
        
        if (existingIndex >= 0) {
            notifications[existingIndex] = notification
        } else {
            notifications.add(notification)
        }
        
        saveNotifications(notifications)
    }
    
    /**
     * Elimina una notificación por su ID.
     */
    fun deleteNotification(id: String) {
        val notifications = getAllNotifications().toMutableList()
        notifications.removeAll { it.id == id }
        saveNotifications(notifications)
    }
    
    /**
     * Obtiene todas las notificaciones habilitadas.
     */
    fun getEnabledNotifications(): List<CustomNotification> {
        return getAllNotifications().filter { it.isEnabled }
    }
    
    /**
     * Obtiene todas las notificaciones habilitadas para un día específico.
     */
    fun getEnabledNotificationsForDay(dayOfWeek: Int): List<CustomNotification> {
        return getEnabledNotifications().filter { it.isScheduledForDay(dayOfWeek) }
    }
}
