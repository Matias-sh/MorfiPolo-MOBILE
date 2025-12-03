package com.cocido.morfipolo.util.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

/**
 * Clase utilitaria para programar alarmas exactas para las notificaciones diarias.
 * Usa AlarmManager con setExactAndAllowWhileIdle() para garantizar que la alarma
 * se ejecute incluso cuando la app está completamente cerrada.
 * 
 * Programa dos alarmas:
 * - 9:00 AM: Recordatorio general para votar
 * - 10:00 AM: Recordatorio de seguimiento (solo si no votó)
 */
object AlarmScheduler {
    
    private const val TAG = "AlarmScheduler"
    private const val REMINDER_HOUR_9AM = 9
    private const val REMINDER_HOUR_10AM = 10
    private const val REMINDER_MINUTE = 0
    private const val ALARM_REQUEST_CODE_9AM = 9001
    private const val ALARM_REQUEST_CODE_10AM = 10001
    
    /**
     * Programa las alarmas diarias a las 9am y 10am del próximo día laboral.
     * Estas alarmas se ejecutarán incluso si la app está cerrada.
     */
    fun scheduleDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Verificar si tenemos permiso para programar alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "⚠️ No tenemos permiso para alarmas exactas. Solicitando...")
            }
        }
        
        // Programar alarma de 9am
        scheduleAlarmAtHour(context, alarmManager, REMINDER_HOUR_9AM, ALARM_REQUEST_CODE_9AM, AlarmReceiver.ACTION_REMINDER_9AM)
        
        // Programar alarma de 10am (recordatorio de seguimiento)
        scheduleAlarmAtHour(context, alarmManager, REMINDER_HOUR_10AM, ALARM_REQUEST_CODE_10AM, AlarmReceiver.ACTION_REMINDER_10AM)
    }
    
    /**
     * Programa una alarma específica a una hora determinada.
     */
    private fun scheduleAlarmAtHour(
        context: Context,
        alarmManager: AlarmManager,
        hour: Int,
        requestCode: Int,
        action: String
    ) {
        val nextAlarmTime = calculateNextAlarmTime(hour)
        val pendingIntent = createAlarmPendingIntent(context, requestCode, action)
        
        try {
            // Cancelar alarma anterior si existe
            alarmManager.cancel(pendingIntent)
            
            // Programar nueva alarma exacta
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
            }
            
            val formattedTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(nextAlarmTime))
            Log.d(TAG, "✅ Alarma ${hour}:00 programada para: $formattedTime")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Error de seguridad al programar alarma ${hour}am: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al programar alarma ${hour}am: ${e.message}")
        }
    }
    
    /**
     * Cancela todas las alarmas diarias programadas.
     */
    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        try {
            // Cancelar alarma de 9am
            val pendingIntent9am = createAlarmPendingIntent(context, ALARM_REQUEST_CODE_9AM, AlarmReceiver.ACTION_REMINDER_9AM)
            alarmManager.cancel(pendingIntent9am)
            
            // Cancelar alarma de 10am
            val pendingIntent10am = createAlarmPendingIntent(context, ALARM_REQUEST_CODE_10AM, AlarmReceiver.ACTION_REMINDER_10AM)
            alarmManager.cancel(pendingIntent10am)
            
            Log.d(TAG, "🛑 Alarmas canceladas (9am y 10am)")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar alarmas: ${e.message}")
        }
    }
    
    /**
     * Calcula el próximo momento para la alarma a una hora específica:
     * - Si hoy es día laboral y aún no es la hora, programa para hoy
     * - Si ya pasó la hora o es fin de semana, programa para el próximo día laboral
     */
    private fun calculateNextAlarmTime(hour: Int): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
        
        Log.d(TAG, "Hora actual: ${formatCalendar(now)}")
        Log.d(TAG, "Hora objetivo ${hour}:00: ${formatCalendar(calendar)}")
        
        // Si ya pasó la hora hoy, avanzar a mañana
        if (calendar.timeInMillis <= now.timeInMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "Ya pasaron las ${hour}:00, avanzando a mañana")
        }
        
        // Avanzar hasta el próximo día laboral (lunes a viernes)
        while (isWeekend(calendar)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "Es fin de semana, avanzando al siguiente día")
        }
        
        val delayMillis = calendar.timeInMillis - now.timeInMillis
        val delayHours = delayMillis / (1000 * 60 * 60)
        val delayMinutes = (delayMillis / (1000 * 60)) % 60
        
        Log.d(TAG, "Próxima alarma ${hour}:00: ${formatCalendar(calendar)} (en ${delayHours}h ${delayMinutes}m)")
        
        return calendar.timeInMillis
    }
    
    /**
     * Verifica si el día del calendario es fin de semana.
     */
    private fun isWeekend(calendar: Calendar): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    /**
     * Crea el PendingIntent para la alarma.
     */
    private fun createAlarmPendingIntent(context: Context, requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )
    }
    
    /**
     * Formatea un Calendar para logging.
     */
    private fun formatCalendar(calendar: Calendar): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(calendar.time)
    }
    
    /**
     * Obtiene el nombre del día de la semana.
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


