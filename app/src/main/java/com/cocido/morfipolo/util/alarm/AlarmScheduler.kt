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
 */
object AlarmScheduler {
    
    private const val TAG = "AlarmScheduler"
    private const val REMINDER_HOUR = 9 // 9am
    private const val REMINDER_MINUTE = 0
    private const val ALARM_REQUEST_CODE = 9001
    
    /**
     * Programa la alarma diaria a las 9am del próximo día laboral.
     * Esta alarma se ejecutará incluso si la app está cerrada.
     */
    fun scheduleDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Verificar si tenemos permiso para programar alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "⚠️ No tenemos permiso para alarmas exactas. Solicitando...")
                // En Android 12+ el usuario debe dar permiso manualmente
                // La app debería mostrar un diálogo explicando esto
                // Por ahora, intentamos programar de todas formas
            }
        }
        
        val nextAlarmTime = calculateNextAlarmTime()
        val pendingIntent = createAlarmPendingIntent(context)
        
        try {
            // Cancelar alarma anterior si existe
            alarmManager.cancel(pendingIntent)
            
            // Programar nueva alarma exacta
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // setExactAndAllowWhileIdle funciona incluso en modo Doze
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
            Log.d(TAG, "✅ Alarma programada para: $formattedTime")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Error de seguridad al programar alarma: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al programar alarma: ${e.message}")
        }
    }
    
    /**
     * Cancela la alarma diaria programada.
     */
    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmPendingIntent(context)
        
        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "🛑 Alarma cancelada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar alarma: ${e.message}")
        }
    }
    
    /**
     * Calcula el próximo momento para la alarma:
     * - Si hoy es día laboral y aún no son las 9am, programa para hoy a las 9am
     * - Si ya pasaron las 9am o es fin de semana, programa para el próximo día laboral a las 9am
     */
    private fun calculateNextAlarmTime(): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
        
        Log.d(TAG, "Hora actual: ${formatCalendar(now)}")
        Log.d(TAG, "Hora objetivo inicial: ${formatCalendar(calendar)}")
        
        // Si ya pasaron las 9am hoy, avanzar a mañana
        if (calendar.timeInMillis <= now.timeInMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "Ya pasaron las 9am, avanzando a mañana")
        }
        
        // Avanzar hasta el próximo día laboral (lunes a viernes)
        while (isWeekend(calendar)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "Es fin de semana, avanzando al siguiente día")
        }
        
        val delayMillis = calendar.timeInMillis - now.timeInMillis
        val delayHours = delayMillis / (1000 * 60 * 60)
        val delayMinutes = (delayMillis / (1000 * 60)) % 60
        
        Log.d(TAG, "Próxima alarma: ${formatCalendar(calendar)} (en ${delayHours}h ${delayMinutes}m)")
        
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
    private fun createAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DAILY_REMINDER
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
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

