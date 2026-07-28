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
        private const val KEY_DEFAULTS_CREATED = "default_notifications_created"
        private const val KEY_LEGACY_DEFAULTS_CLEANED = "legacy_default_notifications_cleaned"
        private const val KEY_DEFAULTS_SEEDED_V2 = "default_notifications_seeded_v2"
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
            // Usar commit() en lugar de apply() para garantizar que se guarde antes de continuar
            prefs.edit().putString(KEY_NOTIFICATIONS, json).commit()
            android.util.Log.d("NotificationConfigRepository", "✅ Guardadas ${notifications.size} notificaciones (commit sincrónico)")
            // Log detallado de cada notificación para debugging
            notifications.forEach { notification ->
                val daysStr = notification.daysOfWeek.sorted().joinToString(", ") { 
                    com.cocido.morfipolo.domain.model.CustomNotification.getDayNameFull(it) 
                }
                android.util.Log.d("NotificationConfigRepository", "  - ${notification.getFormattedTime()} [id=${notification.id}]: días [$daysStr] (valores: ${notification.daysOfWeek.sorted()})")
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
        
        android.util.Log.d("NotificationConfigRepository", "📝 Guardando notificación: id=${notification.id}, hora=${notification.getFormattedTime()}")
        android.util.Log.d("NotificationConfigRepository", "   existingIndex=$existingIndex, total notificaciones=${notifications.size}")
        
        if (existingIndex >= 0) {
            android.util.Log.d("NotificationConfigRepository", "   🔄 ACTUALIZANDO notificación existente en índice $existingIndex")
            notifications[existingIndex] = notification
        } else {
            android.util.Log.d("NotificationConfigRepository", "   ➕ AGREGANDO nueva notificación")
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
     * Siembra los recordatorios por defecto (9:00 y 10:00, lunes a viernes)
     * para que la app funcione "out of the box".
     *
     * Solo se ejecuta una vez (flag v2) y solo si el usuario no tiene ninguna
     * notificación configurada: respeta a quien ya creó las suyas, y si el
     * usuario borra los defaults después, NO se vuelven a crear.
     */
    fun createDefaultNotificationsIfNeeded() {
        if (prefs.getBoolean(KEY_DEFAULTS_SEEDED_V2, false)) return

        if (getAllNotifications().isEmpty()) {
            val weekdays = setOf(
                CustomNotification.MONDAY,
                CustomNotification.TUESDAY,
                CustomNotification.WEDNESDAY,
                CustomNotification.THURSDAY,
                CustomNotification.FRIDAY
            )
            val defaults = listOf(
                CustomNotification(
                    id = "default_v2_9_00",
                    hour = 9,
                    minute = 0,
                    isEnabled = true,
                    daysOfWeek = weekdays
                ),
                CustomNotification(
                    id = "default_v2_10_00",
                    hour = 10,
                    minute = 0,
                    isEnabled = true,
                    daysOfWeek = weekdays
                )
            )
            saveNotifications(defaults)
            android.util.Log.d("NotificationConfigRepository", "🌱 Sembrados recordatorios por defecto (9:00 y 10:00, Lun-Vie)")
        }

        prefs.edit().putBoolean(KEY_DEFAULTS_SEEDED_V2, true).apply()
    }

    /**
     * Limpia una configuración legacy donde se creaban 3 notificaciones por defecto
     * (9:00, 9:30 y 10:00 Lun-Vie) aunque el usuario no las hubiera configurado.
     */
    fun clearLegacyDefaultNotificationsIfNeeded() {
        if (prefs.getBoolean(KEY_LEGACY_DEFAULTS_CLEANED, false)) return

        val notifications = getAllNotifications()
        val defaultIds = setOf("default_9_00", "default_9_30", "default_10_00")
        val weekdays = setOf(
            CustomNotification.MONDAY,
            CustomNotification.TUESDAY,
            CustomNotification.WEDNESDAY,
            CustomNotification.THURSDAY,
            CustomNotification.FRIDAY
        )

        val hasExactlyLegacyDefaults = notifications.size == 3 && notifications.all { notification ->
            notification.id in defaultIds &&
                notification.isEnabled &&
                notification.daysOfWeek == weekdays &&
                (
                    (notification.id == "default_9_00" && notification.hour == 9 && notification.minute == 0) ||
                    (notification.id == "default_9_30" && notification.hour == 9 && notification.minute == 30) ||
                    (notification.id == "default_10_00" && notification.hour == 10 && notification.minute == 0)
                )
        }

        if (hasExactlyLegacyDefaults) {
            saveNotifications(emptyList())
            android.util.Log.d(
                "NotificationConfigRepository",
                "🧹 Configuración legacy detectada: se eliminaron notificaciones predefinidas automáticas"
            )
        }

        prefs.edit().putBoolean(KEY_LEGACY_DEFAULTS_CLEANED, true).apply()
    }
}
