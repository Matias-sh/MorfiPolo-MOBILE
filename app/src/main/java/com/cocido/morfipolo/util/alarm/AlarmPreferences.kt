package com.cocido.morfipolo.util.alarm

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestiona el estado de las notificaciones enviadas para evitar duplicados.
 * Guarda qué notificaciones se han enviado para cada día.
 */
class AlarmPreferences(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "alarm_notification_prefs"
        private const val KEY_CUSTOM_NOTIFICATION = "custom_notification_"
        private const val KEY_LAST_CLEANUP_DATE = "last_cleanup_date"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Obtiene la fecha de hoy en formato string.
     */
    private fun getTodayString(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * Verifica si se envió una notificación personalizada específica hoy.
     * @param notificationId ID único de la notificación personalizada
     */
    fun wasCustomNotificationSent(notificationId: String): Boolean {
        val today = getTodayString()
        val key = "$KEY_CUSTOM_NOTIFICATION${notificationId}_$today"
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Marca que se envió una notificación personalizada específica hoy.
     * @param notificationId ID único de la notificación personalizada
     */
    fun setCustomNotificationSent(notificationId: String) {
        val today = getTodayString()
        val key = "$KEY_CUSTOM_NOTIFICATION${notificationId}_$today"
        prefs.edit().putBoolean(key, true).apply()
        android.util.Log.d("AlarmPreferences", "✅ Marcado: notificación $notificationId enviada para $today")
        cleanupOldEntries()
    }
    
    /**
     * Resetea todas las notificaciones del día actual (útil para testing).
     */
    fun resetTodayNotifications() {
        val today = getTodayString()
        val editor = prefs.edit()
        
        // Buscar y eliminar todas las claves de hoy
        prefs.all.keys.filter { 
            it.startsWith(KEY_CUSTOM_NOTIFICATION) && it.endsWith("_$today") 
        }.forEach { key ->
            editor.remove(key)
        }
        
        editor.apply()
        android.util.Log.d("AlarmPreferences", "🔄 Reset de notificaciones para $today")
    }
    
    /**
     * Limpia entradas antiguas para evitar que las preferencias crezcan indefinidamente.
     * Solo mantiene los últimos 7 días.
     */
    private fun cleanupOldEntries() {
        val today = getTodayString()
        val lastCleanup = prefs.getString(KEY_LAST_CLEANUP_DATE, null)
        
        // Solo limpiar una vez por día
        if (lastCleanup == today) return
        
        try {
            val calendar = Calendar.getInstance()
            val keysToRemove = mutableListOf<String>()
            
            // Calcular fecha límite (7 días atrás)
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            val limitDate = calendar.time
            
            // Revisar todas las claves
            prefs.all.keys.forEach { key ->
                if (key.startsWith(KEY_CUSTOM_NOTIFICATION)) {
                    // Extraer la fecha de la clave
                    val dateStr = key.substringAfterLast("_")
                    try {
                        val keyDate = dateFormat.parse(dateStr)
                        if (keyDate != null && keyDate.before(limitDate)) {
                            keysToRemove.add(key)
                        }
                    } catch (e: Exception) {
                        // Fecha inválida, eliminar
                        keysToRemove.add(key)
                    }
                }
            }
            
            // Eliminar claves antiguas
            if (keysToRemove.isNotEmpty()) {
                val editor = prefs.edit()
                keysToRemove.forEach { key -> editor.remove(key) }
                editor.putString(KEY_LAST_CLEANUP_DATE, today)
                editor.apply()
                android.util.Log.d("AlarmPreferences", "🧹 Limpiadas ${keysToRemove.size} entradas antiguas")
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmPreferences", "Error al limpiar entradas antiguas", e)
        }
    }
}
