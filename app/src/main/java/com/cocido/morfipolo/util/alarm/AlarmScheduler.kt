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
 * Programa tres alarmas con lógica inteligente:
 * - 9:00 AM: Recordatorio general (solo si hay menú)
 * - 9:30 AM: Recordatorio de reintento (solo si no se notificó a las 9am y hay menú)
 * - 10:00 AM: Recordatorio urgente (solo si no votó y hay menú)
 */
object AlarmScheduler {
    
    private const val TAG = "AlarmScheduler"
    private const val REMINDER_HOUR_9AM = 9
    private const val REMINDER_HOUR_930AM = 9
    private const val REMINDER_MINUTE_930AM = 30
    private const val REMINDER_HOUR_10AM = 10
    private const val REMINDER_MINUTE = 0
    private const val ALARM_REQUEST_CODE_9AM = 9001
    private const val ALARM_REQUEST_CODE_930AM = 9301
    private const val ALARM_REQUEST_CODE_10AM = 10001
    
    /**
     * Programa las alarmas diarias a las 9am, 9:30am y 10am del próximo día laboral.
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
        
        // Programar alarma de 9am (recordatorio principal)
        scheduleAlarmAtHour(context, alarmManager, REMINDER_HOUR_9AM, REMINDER_MINUTE, ALARM_REQUEST_CODE_9AM, AlarmReceiver.ACTION_REMINDER_9AM)
        
        // Programar alarma de 9:30am (reintento si no se notificó a las 9am)
        scheduleAlarmAtHour(context, alarmManager, REMINDER_HOUR_930AM, REMINDER_MINUTE_930AM, ALARM_REQUEST_CODE_930AM, AlarmReceiver.ACTION_REMINDER_930AM)
        
        // Programar alarma de 10am (recordatorio urgente si no votó)
        scheduleAlarmAtHour(context, alarmManager, REMINDER_HOUR_10AM, REMINDER_MINUTE, ALARM_REQUEST_CODE_10AM, AlarmReceiver.ACTION_REMINDER_10AM)
    }
    
    /**
     * Programa una alarma específica a una hora y minuto determinados.
     */
    private fun scheduleAlarmAtHour(
        context: Context,
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        requestCode: Int,
        action: String
    ) {
        val nextAlarmTime = calculateNextAlarmTime(hour, minute)
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
            val timeStr = String.format("%d:%02d", hour, minute)
            Log.d(TAG, "✅ Alarma $timeStr programada para: $formattedTime")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Error de seguridad al programar alarma ${hour}:${minute}: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al programar alarma ${hour}:${minute}: ${e.message}")
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
            
            // Cancelar alarma de 9:30am
            val pendingIntent930am = createAlarmPendingIntent(context, ALARM_REQUEST_CODE_930AM, AlarmReceiver.ACTION_REMINDER_930AM)
            alarmManager.cancel(pendingIntent930am)
            
            // Cancelar alarma de 10am
            val pendingIntent10am = createAlarmPendingIntent(context, ALARM_REQUEST_CODE_10AM, AlarmReceiver.ACTION_REMINDER_10AM)
            alarmManager.cancel(pendingIntent10am)
            
            Log.d(TAG, "🛑 Alarmas canceladas (9am, 9:30am y 10am)")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar alarmas: ${e.message}")
        }
    }
    
    /**
     * Calcula el próximo momento para la alarma a una hora y minuto específicos:
     * - Si hoy es día laboral y aún no es la hora, programa para hoy
     * - Si ya pasó la hora o es fin de semana, programa para el próximo día laboral
     */
    private fun calculateNextAlarmTime(hour: Int, minute: Int = 0): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
        val timeStr = String.format("%d:%02d", hour, minute)
        
        Log.d(TAG, "Hora actual: ${formatCalendar(now)}")
        Log.d(TAG, "Hora objetivo $timeStr: ${formatCalendar(calendar)}")
        
        // Si ya pasó la hora hoy, avanzar a mañana
        if (calendar.timeInMillis <= now.timeInMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "Ya pasaron las $timeStr, avanzando a mañana")
        }
        
        // Avanzar hasta el próximo día laboral (lunes a viernes)
        while (isWeekend(calendar)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "Es fin de semana, avanzando al siguiente día")
        }
        
        val delayMillis = calendar.timeInMillis - now.timeInMillis
        val delayHours = delayMillis / (1000 * 60 * 60)
        val delayMinutes = (delayMillis / (1000 * 60)) % 60
        
        Log.d(TAG, "Próxima alarma $timeStr: ${formatCalendar(calendar)} (en ${delayHours}h ${delayMinutes}m)")
        
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
    
    /**
     * Programa las notificaciones personalizadas configuradas por el usuario.
     * @param context Contexto de la aplicación
     * @param notificationConfigRepository Repositorio de configuraciones
     */
    fun scheduleCustomNotifications(context: Context, notificationConfigRepository: com.cocido.morfipolo.data.repository.NotificationConfigRepository) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notifications = notificationConfigRepository.getEnabledNotifications()
        
        // Cancelar todas las alarmas personalizadas anteriores
        cancelAllCustomAlarms(context)
        
        // Programar cada notificación habilitada
        notifications.forEach { notification ->
            // Programar para cada día de la semana configurado
            notification.daysOfWeek.forEach { dayOfWeek ->
                val requestCode = generateRequestCode(notification.id, dayOfWeek)
                scheduledRequestCodes.add(requestCode) // Registrar el request code usado
                
                val nextAlarmTime = calculateNextAlarmTimeForDay(notification.hour, notification.minute, dayOfWeek)
                val pendingIntent = createCustomAlarmPendingIntent(context, requestCode, notification.id, dayOfWeek)
                
                try {
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
                    Log.d(TAG, "✅ Notificación personalizada ${notification.getFormattedTime()} (${com.cocido.morfipolo.domain.model.CustomNotification.getDayNameFull(dayOfWeek)}) programada para: $formattedTime")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al programar notificación personalizada: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Cancela todas las alarmas personalizadas.
     * Usa un enfoque más eficiente: guarda los request codes usados y solo cancela esos.
     */
    private val scheduledRequestCodes = mutableSetOf<Int>()
    
    private fun cancelAllCustomAlarms(context: Context) {
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
            
            // Solo cancelar los request codes que realmente se usaron
            scheduledRequestCodes.toList().forEach { requestCode ->
                try {
                    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    }
                } catch (e: Exception) {
                    // Ignorar errores
                }
            }
            
            scheduledRequestCodes.clear()
            Log.d(TAG, "🛑 Alarmas personalizadas canceladas")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar alarmas personalizadas: ${e.message}")
        }
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
        
        // Si no hay días que agregar (no debería pasar), agregar 7 días
        if (daysToAdd == 0) {
            daysToAdd = 7
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
        
        return calendar.timeInMillis
    }
    
    /**
     * Genera un request code único para una notificación y día específico.
     * Usa un rango más pequeño y predecible para evitar crear demasiados PendingIntents.
     */
    private fun generateRequestCode(notificationId: String, dayOfWeek: Int): Int {
        // Usar un hash más simple y limitado a un rango razonable
        val baseCode = kotlin.math.abs(notificationId.hashCode() % 1000) // 0-999
        val dayOffset = (dayOfWeek - 1) * 1000 // 0, 1000, 2000, 3000, 4000, 5000, 6000
        return 10000 + baseCode + dayOffset // Rango: 10000-16999
    }
    
    /**
     * Crea el PendingIntent para una notificación personalizada.
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
}


