package com.cocido.morfipolo.util.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.util.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * BroadcastReceiver que recibe las alarmas diarias y envía notificaciones.
 * - 9:00 AM: Recordatorio general para votar
 * - 10:00 AM: Recordatorio de seguimiento (solo si no votó)
 * Este receiver funciona incluso cuando la app está completamente cerrada.
 */
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_REMINDER_9AM = "com.cocido.morfipolo.REMINDER_9AM"
        const val ACTION_REMINDER_10AM = "com.cocido.morfipolo.REMINDER_10AM"
        // Mantener compatibilidad con alarmas anteriores
        const val ACTION_DAILY_REMINDER = "com.cocido.morfipolo.DAILY_REMINDER_ALARM"
        private const val WAKE_LOCK_TIMEOUT = 30000L // 30 segundos para permitir consulta a API
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        
        // Validar acción
        if (action != ACTION_REMINDER_9AM && action != ACTION_REMINDER_10AM && action != ACTION_DAILY_REMINDER) {
            Log.w(TAG, "Acción desconocida: $action")
            return
        }
        
        val isFollowUpReminder = action == ACTION_REMINDER_10AM
        Log.d(TAG, "⏰ Alarma ${if (isFollowUpReminder) "10AM (seguimiento)" else "9AM"} recibida a las ${getCurrentTime()}")
        
        // Usar goAsync() para extender el tiempo de ejecución del BroadcastReceiver
        val pendingResult = goAsync()
        
        // Adquirir wake lock para asegurar que el dispositivo esté despierto
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MorfiPolo::DailyReminderWakeLock"
        )
        
        scope.launch {
            try {
                wakeLock.acquire(WAKE_LOCK_TIMEOUT)
                Log.d(TAG, "🔓 Wake lock adquirido")
                
                // Verificar que sea día laboral
                val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayName = AlarmScheduler.getDayName(dayOfWeek)
                
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    Log.d(TAG, "⚠️ Es fin de semana ($dayName), no se envía recordatorio")
                    return@launch
                }
                
                Log.d(TAG, "📅 Es $dayName, procesando alarma...")
                
                if (isFollowUpReminder) {
                    // Alarma de 10AM: verificar si el usuario ya votó
                    handleFollowUpReminder(context)
                } else {
                    // Alarma de 9AM: enviar recordatorio general
                    sendNotification(context, isFollowUp = false)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en onReceive: ${e.message}", e)
            } finally {
                // Liberar wake lock
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    Log.d(TAG, "🔒 Wake lock liberado")
                }
                
                // Siempre reprogramar las siguientes alarmas
                AlarmScheduler.scheduleDailyAlarm(context)
                
                // Finalizar el BroadcastReceiver
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Maneja el recordatorio de seguimiento de las 10AM.
     * Solo envía notificación si el usuario no ha votado hoy.
     */
    private suspend fun handleFollowUpReminder(context: Context) {
        try {
            val app = context.applicationContext as? MorfipoloApplication
            if (app == null) {
                Log.w(TAG, "No se pudo obtener MorfipoloApplication")
                return
            }
            
            // Verificar si el usuario está logueado
            val userId = app.sessionManager.getCurrentUserId()
            if (userId == null) {
                Log.d(TAG, "⚠️ Usuario no logueado, no se envía recordatorio de seguimiento")
                return
            }
            
            // Obtener el menú del día
            val today = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val menu = app.menuRepository.getMenuByDate(today)
            if (menu == null) {
                Log.d(TAG, "⚠️ No hay menú para hoy, no se envía recordatorio de seguimiento")
                return
            }
            
            // Verificar si el usuario ya votó
            val userVote = app.voteRepository.getUserVoteForMenu(menu.id, userId)
            
            if (userVote != null) {
                Log.d(TAG, "✅ Usuario ya votó (voto: ${userVote.option.name}), no se envía recordatorio")
                return
            }
            
            // El usuario NO ha votado, enviar recordatorio de seguimiento
            Log.d(TAG, "⚠️ Usuario NO ha votado, enviando recordatorio de seguimiento...")
            
            // Formatear las opciones del menú para mostrar en la notificación
            val options = menu.getOptionsOrEmpty()
            val menuContent = if (options.isNotEmpty()) {
                options.mapIndexed { index, option -> 
                    "${index + 1}. ${option.name}"
                }.joinToString("\n")
            } else {
                menu.description
            }
            
            sendNotification(context, isFollowUp = true, menuDescription = menuContent)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al verificar voto del usuario: ${e.message}", e)
            // En caso de error, no enviar notificación para evitar spam
        }
    }
    
    /**
     * Envía la notificación del recordatorio diario.
     * @param isFollowUp Si es true, es el recordatorio de seguimiento (10AM)
     * @param menuDescription Descripción del menú (opcional)
     */
    private suspend fun sendNotification(context: Context, isFollowUp: Boolean, menuDescription: String? = null) {
        try {
            // Si no tenemos descripción del menú, intentar obtenerla
            val finalMenuDescription = menuDescription ?: tryGetTodayMenuDescription(context)
            
            // Enviar notificación
            val notificationHelper = NotificationHelper(context)
            
            if (isFollowUp) {
                notificationHelper.showFollowUpReminderNotification(finalMenuDescription)
            } else {
                notificationHelper.showDailyReminderNotification(finalMenuDescription)
            }
            
            Log.d(TAG, "✅✅✅ Notificación ${if (isFollowUp) "de seguimiento" else "diaria"} enviada exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar notificación: ${e.message}", e)
            
            // Intentar enviar notificación genérica como fallback
            try {
                val notificationHelper = NotificationHelper(context)
                if (isFollowUp) {
                    notificationHelper.showFollowUpReminderNotification(null)
                } else {
                    notificationHelper.showDailyReminderNotification(null)
                }
                Log.d(TAG, "✅ Notificación genérica enviada como fallback")
            } catch (e2: Exception) {
                Log.e(TAG, "❌ Error al enviar notificación fallback: ${e2.message}", e2)
            }
        }
    }
    
    /**
     * Intenta obtener las opciones del menú del día formateadas.
     * Devuelve null si no se puede obtener (sin sesión, sin red, etc.)
     */
    private suspend fun tryGetTodayMenuDescription(context: Context): String? {
        return try {
            val app = context.applicationContext as? MorfipoloApplication
            if (app == null) {
                Log.w(TAG, "No se pudo obtener MorfipoloApplication")
                return null
            }
            
            // Obtener fecha de hoy
            val today = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            // Intentar obtener el menú del día
            val menu = app.menuRepository.getMenuByDate(today)
            
            if (menu != null && menu.status != "draft") {
                Log.d(TAG, "📋 Menú encontrado: ${menu.description}")
                
                // Formatear las opciones del menú
                val options = menu.getOptionsOrEmpty()
                if (options.isNotEmpty()) {
                    options.mapIndexed { index, option -> 
                        "${index + 1}. ${option.name}"
                    }.joinToString("\n")
                } else {
                    menu.description
                }
            } else {
                Log.d(TAG, "📋 No hay menú disponible para hoy")
                null
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo obtener menú (posible falta de sesión): ${e.message}")
            null
        }
    }
    
    /**
     * Obtiene la hora actual formateada.
     */
    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
    }
}


