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
        private const val KEY_NOTIFIED_9AM = "notified_9am_"
        private const val KEY_NOTIFIED_930AM = "notified_930am_"
        private const val KEY_NOTIFIED_10AM = "notified_10am_"
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
     * Verifica si se envió la notificación de las 9am hoy.
     */
    fun wasNotified9am(): Boolean {
        val today = getTodayString()
        return prefs.getBoolean("$KEY_NOTIFIED_9AM$today", false)
    }
    
    /**
     * Marca que se envió la notificación de las 9am hoy.
     */
    fun setNotified9am() {
        val today = getTodayString()
        prefs.edit().putBoolean("$KEY_NOTIFIED_9AM$today", true).apply()
        android.util.Log.d("AlarmPreferences", "✅ Marcado: notificación 9am enviada para $today")
        cleanupOldEntries()
    }
    
    /**
     * Verifica si se envió la notificación de las 9:30am hoy.
     */
    fun wasNotified930am(): Boolean {
        val today = getTodayString()
        return prefs.getBoolean("$KEY_NOTIFIED_930AM$today", false)
    }
    
    /**
     * Marca que se envió la notificación de las 9:30am hoy.
     */
    fun setNotified930am() {
        val today = getTodayString()
        prefs.edit().putBoolean("$KEY_NOTIFIED_930AM$today", true).apply()
        android.util.Log.d("AlarmPreferences", "✅ Marcado: notificación 9:30am enviada para $today")
        cleanupOldEntries()
    }
    
    /**
     * Verifica si se envió la notificación de las 10am hoy.
     */
    fun wasNotified10am(): Boolean {
        val today = getTodayString()
        return prefs.getBoolean("$KEY_NOTIFIED_10AM$today", false)
    }
    
    /**
     * Marca que se envió la notificación de las 10am hoy.
     */
    fun setNotified10am() {
        val today = getTodayString()
        prefs.edit().putBoolean("$KEY_NOTIFIED_10AM$today", true).apply()
        android.util.Log.d("AlarmPreferences", "✅ Marcado: notificación 10am enviada para $today")
        cleanupOldEntries()
    }
    
    /**
     * Verifica si ya se envió alguna notificación de recordatorio hoy (9am o 9:30am).
     * Útil para la lógica de 9:30am que solo debe enviar si no se envió antes.
     */
    fun wasAnyReminderNotifiedToday(): Boolean {
        return wasNotified9am() || wasNotified930am()
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
                if (key.startsWith(KEY_NOTIFIED_9AM) || 
                    key.startsWith(KEY_NOTIFIED_930AM) || 
                    key.startsWith(KEY_NOTIFIED_10AM)) {
                    
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
    
    /**
     * Resetea todas las notificaciones del día actual (útil para testing).
     */
    fun resetTodayNotifications() {
        val today = getTodayString()
        prefs.edit()
            .remove("$KEY_NOTIFIED_9AM$today")
            .remove("$KEY_NOTIFIED_930AM$today")
            .remove("$KEY_NOTIFIED_10AM$today")
            .apply()
        android.util.Log.d("AlarmPreferences", "🔄 Reset de notificaciones para $today")
    }
}






