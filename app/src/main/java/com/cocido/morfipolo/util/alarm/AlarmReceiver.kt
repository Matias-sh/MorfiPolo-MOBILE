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
import java.util.*

/**
 * BroadcastReceiver que recibe la alarma diaria a las 9am y envía la notificación.
 * Este receiver funciona incluso cuando la app está completamente cerrada.
 */
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_DAILY_REMINDER = "com.cocido.morfipolo.DAILY_REMINDER_ALARM"
        private const val WAKE_LOCK_TIMEOUT = 10000L // 10 segundos
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DAILY_REMINDER) {
            Log.w(TAG, "Acción desconocida: ${intent?.action}")
            return
        }
        
        Log.d(TAG, "⏰ Alarma recibida a las ${getCurrentTime()}")
        
        // Adquirir wake lock para asegurar que el dispositivo esté despierto
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MorfiPolo::DailyReminderWakeLock"
        )
        
        try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT)
            Log.d(TAG, "🔓 Wake lock adquirido")
            
            // Verificar que sea día laboral
            val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = AlarmScheduler.getDayName(dayOfWeek)
            
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                Log.d(TAG, "⚠️ Es fin de semana ($dayName), no se envía recordatorio")
                // Reprogramar para el próximo lunes
                AlarmScheduler.scheduleDailyAlarm(context)
                return
            }
            
            Log.d(TAG, "📅 Es $dayName, enviando notificación...")
            
            // Enviar notificación
            sendNotification(context)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en onReceive: ${e.message}", e)
        } finally {
            // Liberar wake lock
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(TAG, "🔒 Wake lock liberado")
            }
            
            // Siempre reprogramar la siguiente alarma
            AlarmScheduler.scheduleDailyAlarm(context)
        }
    }
    
    /**
     * Envía la notificación del recordatorio diario.
     */
    private fun sendNotification(context: Context) {
        scope.launch {
            try {
                // Intentar obtener el menú del día si el usuario está logueado
                val menuDescription = tryGetTodayMenuDescription(context)
                
                // Enviar notificación
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showDailyReminderNotification(menuDescription)
                
                Log.d(TAG, "✅✅✅ Notificación enviada exitosamente")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al enviar notificación: ${e.message}", e)
                
                // Intentar enviar notificación genérica como fallback
                try {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.showDailyReminderNotification(null)
                    Log.d(TAG, "✅ Notificación genérica enviada como fallback")
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ Error al enviar notificación fallback: ${e2.message}", e2)
                }
            }
        }
    }
    
    /**
     * Intenta obtener la descripción del menú del día.
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
                menu.description
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

