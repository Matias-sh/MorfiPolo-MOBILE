package com.cocido.morfipolo.util.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

/**
 * Clase utilitaria para programar alarmas exactas para las notificaciones personalizadas.
 * Usa AlarmManager con setExactAndAllowWhileIdle() para garantizar que la alarma
 * se ejecute incluso cuando la app está completamente cerrada.
 * 
 * Las notificaciones son completamente configurables por el usuario desde
 * el apartado de Notificaciones en la app.
 */
object AlarmScheduler {
    
    private const val TAG = "AlarmScheduler"
    private const val PREFS_NAME = "alarm_scheduler_prefs"
    private const val KEY_REQUEST_CODES = "scheduled_request_codes"
    
    /**
     * Programa las notificaciones personalizadas configuradas por el usuario.
     * @param context Contexto de la aplicación
     * @param notificationConfigRepository Repositorio de configuraciones
     */
    fun scheduleCustomNotifications(context: Context, notificationConfigRepository: com.cocido.morfipolo.data.repository.NotificationConfigRepository) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Verificar si tenemos permiso para programar alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "⚠️ No tenemos permiso para alarmas exactas")
            }
        }
        
        val notifications = notificationConfigRepository.getEnabledNotifications()
        
        // Cancelar todas las alarmas anteriores
        cancelAllCustomAlarms(context)
        
        if (notifications.isEmpty()) {
            Log.d(TAG, "ℹ️ No hay notificaciones habilitadas para programar")
            return
        }
        
        // Set para guardar los nuevos request codes
        val newRequestCodes = mutableSetOf<Int>()
        
        // Programar cada notificación habilitada
        notifications.forEach { notification ->
            // Programar para cada día de la semana configurado
            notification.daysOfWeek.forEach { dayOfWeek ->
                val requestCode = generateRequestCode(notification.id, dayOfWeek)
                newRequestCodes.add(requestCode)
                
                val nextAlarmTime = calculateNextAlarmTimeForDay(notification.hour, notification.minute, dayOfWeek)
                val pendingIntent = createCustomAlarmPendingIntent(context, requestCode, notification.id, dayOfWeek)
                
                try {
                    val canUseExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        alarmManager.canScheduleExactAlarms()
                    } else {
                        true
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canUseExactAlarm) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime,
                            pendingIntent
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Fallback para equipos Android 12+ sin permiso de alarma exacta.
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            nextAlarmTime,
                            pendingIntent
                        )
                    }
                    
                    val formattedTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(nextAlarmTime))
                    val now = System.currentTimeMillis()
                    val delayMinutes = (nextAlarmTime - now) / (1000 * 60)
                    val isToday = delayMinutes < 24 * 60
                    val todayIndicator = if (isToday) "📍 HOY" else ""
                    Log.d(TAG, "✅ Notificación ${notification.getFormattedTime()} (${com.cocido.morfipolo.domain.model.CustomNotification.getDayNameFull(dayOfWeek)}) → $formattedTime (en ${delayMinutes} min) $todayIndicator")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al programar notificación: ${e.message}")
                }
            }
        }
        
        // Guardar los request codes para poder cancelarlos después
        saveScheduledRequestCodes(context, newRequestCodes)
        Log.d(TAG, "💾 Programadas ${notifications.size} notificaciones (${newRequestCodes.size} alarmas)")
    }
    
    /**
     * Cancela todas las alarmas programadas.
     */
    fun cancelAllCustomAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_CUSTOM_NOTIFICATION
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            
            val savedRequestCodes = getScheduledRequestCodes(context)
            
            savedRequestCodes.forEach { requestCode ->
                try {
                    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    }
                } catch (e: Exception) {
                    // Ignorar errores individuales
                }
            }
            
            saveScheduledRequestCodes(context, emptySet())
            if (savedRequestCodes.isNotEmpty()) {
                Log.d(TAG, "🛑 Alarmas canceladas (${savedRequestCodes.size} códigos)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar alarmas: ${e.message}")
        }
    }
    
    /**
     * Obtiene los request codes guardados en SharedPreferences.
     */
    private fun getScheduledRequestCodes(context: Context): MutableSet<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val codesString = prefs.getString(KEY_REQUEST_CODES, "") ?: ""
        return if (codesString.isEmpty()) {
            mutableSetOf()
        } else {
            codesString.split(",").mapNotNull { it.toIntOrNull() }.toMutableSet()
        }
    }
    
    /**
     * Guarda los request codes en SharedPreferences.
     */
    private fun saveScheduledRequestCodes(context: Context, codes: Set<Int>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REQUEST_CODES, codes.joinToString(",")).commit()
    }
    
    /**
     * Calcula el próximo momento para una alarma en un día específico de la semana.
     */
    private fun calculateNextAlarmTimeForDay(hour: Int, minute: Int, dayOfWeek: Int): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        
        // Convertir día de la semana de nuestro formato (1=Lunes) a Calendar (1=Domingo, 2=Lunes...)
        val calendarDayOfWeek = when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
        
        // Calcular días hasta el día objetivo
        var daysToAdd = (calendarDayOfWeek - currentDayOfWeek + 7) % 7
        
        // Si es el mismo día pero ya pasó la hora, avanzar a la próxima semana
        if (daysToAdd == 0 && calendar.timeInMillis <= now.timeInMillis) {
            daysToAdd = 7
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
        
        return calendar.timeInMillis
    }
    
    /**
     * Genera un request code único para una notificación y día específico.
     */
    private fun generateRequestCode(notificationId: String, dayOfWeek: Int): Int {
        val baseCode = kotlin.math.abs(notificationId.hashCode() % 1000)
        val dayOffset = (dayOfWeek - 1) * 1000
        return 10000 + baseCode + dayOffset
    }
    
    /**
     * Crea el PendingIntent para una notificación.
     */
    private fun createCustomAlarmPendingIntent(context: Context, requestCode: Int, notificationId: String, dayOfWeek: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CUSTOM_NOTIFICATION
            putExtra("notification_id", notificationId)
            putExtra("day_of_week", dayOfWeek)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
    
    /**
     * Obtiene el nombre del día de la semana (formato Calendar).
     */
    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> "Desconocido"
        }
    }
}
