package com.cocido.morfipolo.util.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.domain.model.Menu
import com.cocido.morfipolo.util.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

/**
 * BroadcastReceiver que recibe las alarmas diarias y envía notificaciones con lógica inteligente.
 * 
 * LÓGICA DE NOTIFICACIONES (siempre valida menú primero):
 * 
 * - 9:00 AM:  ¿Hay menú publicado? → NO → No hacer nada
 *                                  → SÍ → Notificar + Guardar "notificado_9am=true"
 * 
 * - 9:30 AM:  ¿Hay menú publicado? → NO → No hacer nada
 *                                  → SÍ → ¿Se notificó a las 9am? → SÍ → No hacer nada
 *                                                                 → NO → Notificar (menú cargado tarde)
 * 
 * - 10:00 AM: ¿Hay menú publicado? → NO → No hacer nada
 *                                  → SÍ → ¿Usuario ya votó? → SÍ → No hacer nada
 *                                                           → NO → Notificar recordatorio urgente
 * 
 * REGLA FUNDAMENTAL: Sin menú publicado = sin notificación (aplica a todas las alarmas)
 * 
 * Este receiver funciona incluso cuando la app está completamente cerrada.
 */
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_REMINDER_9AM = "com.cocido.morfipolo.REMINDER_9AM"
        const val ACTION_REMINDER_930AM = "com.cocido.morfipolo.REMINDER_930AM"
        const val ACTION_REMINDER_10AM = "com.cocido.morfipolo.REMINDER_10AM"
        // Mantener compatibilidad con alarmas anteriores
        const val ACTION_DAILY_REMINDER = "com.cocido.morfipolo.DAILY_REMINDER_ALARM"
        private const val WAKE_LOCK_TIMEOUT = 30000L // 30 segundos para permitir consulta a API
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        
        // Validar acción
        val validActions = listOf(ACTION_REMINDER_9AM, ACTION_REMINDER_930AM, ACTION_REMINDER_10AM, ACTION_DAILY_REMINDER)
        if (action !in validActions) {
            Log.w(TAG, "Acción desconocida: $action")
            return
        }
        
        val alarmType = when (action) {
            ACTION_REMINDER_9AM, ACTION_DAILY_REMINDER -> "9AM"
            ACTION_REMINDER_930AM -> "9:30AM"
            ACTION_REMINDER_10AM -> "10AM"
            else -> "UNKNOWN"
        }
        
        Log.d(TAG, "⏰ Alarma $alarmType recibida a las ${getCurrentTime()}")
        
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
                
                Log.d(TAG, "📅 Es $dayName, procesando alarma $alarmType...")
                
                // Procesar según el tipo de alarma
                when (action) {
                    ACTION_REMINDER_9AM, ACTION_DAILY_REMINDER -> handle9amReminder(context)
                    ACTION_REMINDER_930AM -> handle930amReminder(context)
                    ACTION_REMINDER_10AM -> handle10amReminder(context)
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
     * Maneja la alarma de las 9:00 AM.
     * Solo notifica si hay un menú publicado.
     */
    private suspend fun handle9amReminder(context: Context) {
        Log.d(TAG, "📍 Procesando alarma 9:00 AM...")
        
        val alarmPrefs = AlarmPreferences(context)
        
        // 1. Verificar si hay menú publicado
        val menu = getTodayMenu(context)
        if (menu == null) {
            Log.d(TAG, "❌ No hay menú publicado para hoy, NO se envía notificación de 9am")
            return
        }
        
        Log.d(TAG, "✅ Menú encontrado: ${menu.description}")
        
        // 2. Enviar notificación
        val menuContent = formatMenuOptions(menu)
        sendNotification(context, isFollowUp = false, menuDescription = menuContent)
        
        // 3. Marcar que se envió la notificación de 9am
        alarmPrefs.setNotified9am()
        Log.d(TAG, "✅ Notificación de 9am enviada y registrada")
    }
    
    /**
     * Maneja la alarma de las 9:30 AM.
     * Solo notifica si:
     * - Hay un menú publicado
     * - NO se envió notificación a las 9am (menú cargado tarde)
     */
    private suspend fun handle930amReminder(context: Context) {
        Log.d(TAG, "📍 Procesando alarma 9:30 AM...")
        
        val alarmPrefs = AlarmPreferences(context)
        
        // 1. Verificar si ya se envió notificación antes (9am o 9:30am)
        if (alarmPrefs.wasAnyReminderNotifiedToday()) {
            Log.d(TAG, "⏭️ Ya se envió notificación de recordatorio hoy, NO se envía notificación de 9:30am")
            return
        }
        
        // 2. Verificar si hay menú publicado
        val menu = getTodayMenu(context)
        if (menu == null) {
            Log.d(TAG, "❌ No hay menú publicado para hoy, NO se envía notificación de 9:30am")
            return
        }
        
        Log.d(TAG, "✅ Menú encontrado (cargado después de las 9am): ${menu.description}")
        
        // 3. Enviar notificación (el menú se cargó entre las 9am y 9:30am)
        val menuContent = formatMenuOptions(menu)
        sendNotification(context, isFollowUp = false, menuDescription = menuContent)
        
        // 4. Marcar que se envió la notificación de 9:30am
        alarmPrefs.setNotified930am()
        Log.d(TAG, "✅ Notificación de 9:30am enviada y registrada (menú cargado tarde)")
    }
    
    /**
     * Maneja la alarma de las 10:00 AM.
     * Solo notifica si:
     * - Hay un menú publicado
     * - El usuario NO ha votado
     */
    private suspend fun handle10amReminder(context: Context) {
        Log.d(TAG, "📍 Procesando alarma 10:00 AM...")
        
        val alarmPrefs = AlarmPreferences(context)
        
        // 1. Verificar si ya se envió notificación de 10am
        if (alarmPrefs.wasNotified10am()) {
            Log.d(TAG, "⏭️ Ya se envió notificación de 10am hoy")
            return
        }
        
        // 2. Verificar si hay menú publicado
        val menu = getTodayMenu(context)
        if (menu == null) {
            Log.d(TAG, "❌ No hay menú publicado para hoy, NO se envía recordatorio de 10am")
            return
        }
        
        // 3. Verificar si el usuario ya votó
        val app = context.applicationContext as? MorfipoloApplication
        if (app == null) {
            Log.w(TAG, "No se pudo obtener MorfipoloApplication")
            return
        }
        
        val userId = app.sessionManager.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "⚠️ Usuario no logueado, no se puede verificar voto")
            return
        }
        
        try {
            val userVote = app.voteRepository.getUserVoteForMenu(menu.id, userId)
            
            if (userVote != null) {
                Log.d(TAG, "✅ Usuario ya votó (opción: ${userVote.option.name}), NO se envía recordatorio de 10am")
                return
            }
            
            // 4. El usuario NO ha votado - enviar recordatorio urgente
            Log.d(TAG, "⚠️ Usuario NO ha votado, enviando recordatorio urgente de 10am...")
            
            val menuContent = formatMenuOptions(menu)
            sendNotification(context, isFollowUp = true, menuDescription = menuContent)
            
            // 5. Marcar que se envió la notificación de 10am
            alarmPrefs.setNotified10am()
            Log.d(TAG, "✅ Recordatorio urgente de 10am enviado y registrado")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al verificar voto del usuario: ${e.message}", e)
            // En caso de error, no enviar notificación para evitar spam
        }
    }
    
    /**
     * Obtiene el menú del día actual.
     * Devuelve null si no hay menú o es un borrador.
     */
    private suspend fun getTodayMenu(context: Context): Menu? {
        return try {
            val app = context.applicationContext as? MorfipoloApplication
            if (app == null) {
                Log.w(TAG, "No se pudo obtener MorfipoloApplication")
                return null
            }
            
            val today = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val menu = app.menuRepository.getMenuByDate(today)
            
            // Solo devolver menú si está publicado (no draft)
            if (menu != null && menu.status != "draft") {
                menu
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al obtener menú del día: ${e.message}")
            null
        }
    }
    
    /**
     * Formatea las opciones del menú para mostrar en la notificación.
     */
    private fun formatMenuOptions(menu: Menu): String {
        val options = menu.getOptionsOrEmpty()
        return if (options.isNotEmpty()) {
            options.mapIndexed { index, option -> 
                "${index + 1}. ${option.name}"
            }.joinToString("\n")
        } else {
            menu.description
        }
    }
    
    /**
     * Envía la notificación del recordatorio.
     * @param isFollowUp Si es true, es el recordatorio urgente (10AM)
     * @param menuDescription Descripción/opciones del menú
     */
    private fun sendNotification(context: Context, isFollowUp: Boolean, menuDescription: String?) {
        try {
            val notificationHelper = NotificationHelper(context)
            
            if (isFollowUp) {
                notificationHelper.showFollowUpReminderNotification(menuDescription)
            } else {
                notificationHelper.showDailyReminderNotification(menuDescription)
            }
            
            Log.d(TAG, "✅✅✅ Notificación ${if (isFollowUp) "urgente (10am)" else "de recordatorio"} enviada exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar notificación: ${e.message}", e)
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
