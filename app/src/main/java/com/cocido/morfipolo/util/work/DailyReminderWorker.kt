package com.cocido.morfipolo.util.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.util.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.TimeZone

/**
 * Worker que envía una notificación diaria a las 9am recordando al usuario
 * que debe anotarse en la comida del día.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Usar Calendar con zona horaria local del dispositivo
        val now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        android.util.Log.d(TAG, "🕘 Worker ejecutado a las ${String.format("%02d", currentHour)}:${String.format("%02d", currentMinute)} (hora local del dispositivo)")
        
        try {
            val app = applicationContext as? MorfipoloApplication
                ?: run {
                    android.util.Log.e(TAG, "❌ No se pudo obtener MorfipoloApplication")
                    return@withContext Result.retry()
                }

            // No verificar si el usuario está logueado - enviar notificación siempre
            android.util.Log.d(TAG, "Verificando menú del día (sin requerir login)...")

            // Obtener menú del día actual usando zona horaria local
            val today = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Verificar que sea día laboral (lunes a viernes) usando hora local
            val dayOfWeek = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).get(Calendar.DAY_OF_WEEK)
            val isWeekday = dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY
            val dayName = when(dayOfWeek) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> "Desconocido"
            }
            android.util.Log.d(TAG, "Día de la semana: $dayName (isWeekday: $isWeekday)")
            
            if (!isWeekday) {
                android.util.Log.d(TAG, "⚠️ Es fin de semana ($dayName), no se envía recordatorio")
                // Re-programar para el próximo lunes
                scheduleNextReminder(applicationContext)
                return@withContext Result.success()
            }

            android.util.Log.d(TAG, "Buscando menú del día...")
            val menuRepository = app.menuRepository
            
            // Intentar obtener el menú, pero si falla por falta de sesión, enviar notificación genérica
            val menu = try {
                menuRepository.getMenuByDate(today.time)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "No se pudo obtener menú (posible falta de sesión), enviando notificación genérica")
                null
            }

            android.util.Log.d(TAG, "Menú encontrado: ${if (menu != null) "Sí (ID: ${menu.id}, status: ${menu.status})" else "No"}")
            
            // Enviar notificación siempre, con o sin menú
            // Si hay menú y no es borrador, incluir descripción
            // Si no hay menú o es borrador, enviar notificación genérica
            val menuDescription = if (menu != null && menu.status != "draft") {
                menu.description
            } else {
                null
            }
            
            android.util.Log.d(TAG, "✅ Enviando recordatorio diario...")
            
            try {
                val notificationHelper = NotificationHelper(applicationContext)
                val sent = notificationHelper.showDailyReminderNotification(menuDescription)
                
                if (sent) {
                    android.util.Log.d(TAG, "✅✅✅ Recordatorio enviado exitosamente")
                } else {
                    android.util.Log.w(TAG, "⚠️ No se pudo enviar el recordatorio (posible falta de permisos)")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error al enviar notificación", e)
            }

            // Re-programar para mañana a las 9am
            scheduleNextReminder(applicationContext)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al enviar recordatorio", e)
            // Re-programar para mañana incluso si hay error
            scheduleNextReminder(applicationContext)
            Result.success()
        }
    }

    /**
     * Programa el próximo recordatorio para el próximo día laboral (lunes a viernes) a las 9am
     */
    private fun scheduleNextReminder(context: Context) {
        try {
            // Usar zona horaria local del dispositivo explícitamente
            val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, REMINDER_MINUTE)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
            android.util.Log.d(TAG, "Hora actual del dispositivo: ${String.format("%02d", now.get(Calendar.HOUR_OF_DAY))}:${String.format("%02d", now.get(Calendar.MINUTE))}")
            android.util.Log.d(TAG, "Hora objetivo: ${String.format("%02d", REMINDER_HOUR)}:${String.format("%02d", REMINDER_MINUTE)}")

            // Si ya pasaron las 9am hoy, programar para mañana
            if (calendar.timeInMillis <= now.timeInMillis) {
                android.util.Log.d(TAG, "Ya pasaron las 9am hoy, programando para mañana")
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Avanzar hasta el próximo día laboral (lunes a viernes)
            while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                   calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                android.util.Log.d(TAG, "Es fin de semana, avanzando al siguiente día")
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            val delayMillis = calendar.timeInMillis - now.timeInMillis
            android.util.Log.d(TAG, "Delay calculado: ${delayMillis / 1000 / 60} minutos (${delayMillis / 1000 / 60 / 60} horas)")
            
            // No requerir conexión a internet para enviar notificaciones
            // Las notificaciones se pueden enviar sin conexión
            // No usar constraints para que se ejecute incluso en modo Doze
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false) // Ejecutar incluso con batería baja
                .setRequiresCharging(false) // No requerir que esté cargando
                .setRequiresDeviceIdle(false) // Ejecutar incluso si el dispositivo está en uso
                .build()

            val nextReminderWork = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag("daily_reminder")
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "daily_reminder_work",
                androidx.work.ExistingWorkPolicy.REPLACE,
                nextReminderWork
            )

            val nextReminderTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(calendar.time)
            android.util.Log.d(TAG, "✅ Próximo recordatorio programado para: $nextReminderTime")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error al programar próximo recordatorio", e)
        }
    }

    companion object {
        private const val TAG = "DailyReminderWorker"
        private const val REMINDER_HOUR = 9 // 9am
        private const val REMINDER_MINUTE = 0

        /**
         * Programa la notificación diaria a las 9am
         * @param context Contexto de la aplicación
         */
        fun scheduleDailyReminder(context: Context) {
            try {
                // Usar zona horaria local del dispositivo explícitamente
                val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
                    set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                    set(Calendar.MINUTE, REMINDER_MINUTE)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val now = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
                val timeZone = TimeZone.getDefault()
                android.util.Log.d(TAG, "Zona horaria del dispositivo: ${timeZone.id} (${timeZone.displayName})")
                android.util.Log.d(TAG, "Hora actual del dispositivo: ${String.format("%02d", now.get(Calendar.HOUR_OF_DAY))}:${String.format("%02d", now.get(Calendar.MINUTE))}")
                android.util.Log.d(TAG, "Hora objetivo: ${String.format("%02d", REMINDER_HOUR)}:${String.format("%02d", REMINDER_MINUTE)}")

                // Si ya pasaron las 9am hoy, programar para mañana
                if (calendar.timeInMillis <= now.timeInMillis) {
                    android.util.Log.d(TAG, "Ya pasaron las 9am hoy, programando para mañana")
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                // Avanzar hasta el próximo día laboral (lunes a viernes)
                while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                       calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    android.util.Log.d(TAG, "Es fin de semana, avanzando al siguiente día")
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                val delayMillis = calendar.timeInMillis - now.timeInMillis
                android.util.Log.d(TAG, "Delay hasta próximo recordatorio: ${delayMillis / 1000 / 60} minutos (${delayMillis / 1000 / 60 / 60} horas)")
                
                // No requerir conexión a internet para enviar notificaciones
                // Configurar para que se ejecute incluso en modo Doze
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(false) // Ejecutar incluso con batería baja
                    .setRequiresCharging(false) // No requerir que esté cargando
                    .setRequiresDeviceIdle(false) // Ejecutar incluso si el dispositivo está en uso
                    .build()

                // Usar OneTimeWorkRequest que se re-programa a sí mismo
                val dailyReminderWork = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .addTag("daily_reminder")
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.LINEAR,
                        androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "daily_reminder_work",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    dailyReminderWork
                )

                val nextReminderTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(calendar.time)
                android.util.Log.d(TAG, "✅ Recordatorio diario programado para: $nextReminderTime")
                
                // Verificar que el trabajo esté en cola
                val workInfo = WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork("daily_reminder_work")
                    .get()
                
                if (workInfo.isNotEmpty()) {
                    val state = workInfo[0].state
                    android.util.Log.d(TAG, "Estado del trabajo: $state")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error al programar recordatorio diario", e)
            }
        }
        
        /**
         * Método de prueba para enviar notificación inmediatamente (útil para debugging)
         */
        fun sendTestNotification(context: Context) {
            try {
                android.util.Log.d(TAG, "🧪 Enviando notificación de prueba...")
                val notificationHelper = com.cocido.morfipolo.util.notifications.NotificationHelper(context)
                val sent = notificationHelper.showDailyReminderNotification("Menú de prueba")
                if (sent) {
                    android.util.Log.d(TAG, "✅ Notificación de prueba enviada")
                } else {
                    android.util.Log.w(TAG, "⚠️ No se pudo enviar notificación de prueba")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error al enviar notificación de prueba", e)
            }
        }
    }
}

