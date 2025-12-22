package com.cocido.morfipolo.util.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.domain.model.Menu
import com.cocido.morfipolo.domain.model.Vote
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
        const val ACTION_CUSTOM_NOTIFICATION = "com.cocido.morfipolo.CUSTOM_NOTIFICATION"
        private const val WAKE_LOCK_TIMEOUT = 30000L // 30 segundos para permitir consulta a API
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        
        // Validar acción
        val validActions = listOf(ACTION_REMINDER_9AM, ACTION_REMINDER_930AM, ACTION_REMINDER_10AM, ACTION_DAILY_REMINDER, ACTION_CUSTOM_NOTIFICATION)
        if (action !in validActions) {
            Log.w(TAG, "Acción desconocida: $action")
            return
        }
        
        // Si es una notificación personalizada, manejarla de forma diferente
        if (action == ACTION_CUSTOM_NOTIFICATION) {
            val notificationId = intent?.getStringExtra("notification_id")
            val dayOfWeek = intent?.getIntExtra("day_of_week", -1) ?: -1
            
            if (notificationId != null && dayOfWeek != -1) {
                handleCustomNotification(context, notificationId, dayOfWeek)
            } else {
                Log.w(TAG, "⚠️ Notificación personalizada sin ID o día de semana")
            }
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
     * Solo notifica si hay un menú publicado y el usuario no ha votado aún.
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
        
        // 2. Verificar si el usuario ya votó (opcional, pero mejor verificar para evitar spam)
        // OPTIMIZACIÓN: Limitar búsqueda a 10 páginas (suficiente para encontrar votos recientes)
        val app = context.applicationContext as? MorfipoloApplication
        val userId = app?.sessionManager?.getCurrentUserId()
        if (userId != null) {
            try {
                val userVote = app.voteRepository.getUserVoteForMenu(menu.id, userId, maxPagesToSearch = 10)
                if (userVote != null) {
                    Log.d(TAG, "✅ Usuario ya votó (opción: ${userVote.option.name}), NO se envía notificación de 9am")
                    // Marcar como notificado para evitar notificaciones futuras
                    alarmPrefs.setNotified9am()
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error al verificar voto en 9am, continuando con notificación: ${e.message}")
                // Continuar con la notificación si hay error (mejor notificar de más que de menos)
            }
        }
        
        // 3. Enviar notificación
        val menuContent = formatMenuOptions(menu)
        val notificationSent = sendNotification(context, isFollowUp = false, menuDescription = menuContent)
        
        // 4. SOLO marcar que se envió la notificación si realmente se envió exitosamente
        if (notificationSent) {
            alarmPrefs.setNotified9am()
            Log.d(TAG, "✅ Notificación de 9am enviada y registrada exitosamente")
        } else {
            Log.w(TAG, "⚠️ No se pudo enviar la notificación de 9am (posible falta de permisos), NO se marca como enviada")
        }
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
        val notificationSent = sendNotification(context, isFollowUp = false, menuDescription = menuContent)
        
        // 4. SOLO marcar que se envió la notificación si realmente se envió exitosamente
        if (notificationSent) {
            alarmPrefs.setNotified930am()
            Log.d(TAG, "✅ Notificación de 9:30am enviada y registrada exitosamente (menú cargado tarde)")
        } else {
            Log.w(TAG, "⚠️ No se pudo enviar la notificación de 9:30am (posible falta de permisos), NO se marca como enviada")
        }
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
        
        // 1. Verificar si hay menú publicado
        val menu = getTodayMenu(context)
        if (menu == null) {
            Log.d(TAG, "❌ No hay menú publicado para hoy, NO se envía recordatorio de 10am")
            return
        }
        
        // 2. Verificar si el usuario está logueado
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
            // 3. Verificar si el usuario ya votó (PRIORITARIO: verificar estado real antes que flag de notificación)
            // OPTIMIZACIÓN: Limitar búsqueda a 10 páginas (suficiente para encontrar votos recientes)
            // Esto evita recorrer las 76 páginas múltiples veces
            Log.d(TAG, "🔍 Verificando si el usuario ya votó para el menú: ${menu.id} (búsqueda limitada a 10 páginas)")
            var userVote: Vote? = null
            try {
                // Limitar a 10 páginas para optimizar (votos recientes están en las primeras páginas)
                userVote = app.voteRepository.getUserVoteForMenu(menu.id, userId, maxPagesToSearch = 10)
                if (userVote != null) {
                    Log.d(TAG, "✅ Voto encontrado: ${userVote.id} para opción: ${userVote.option.name}")
                } else {
                    Log.d(TAG, "ℹ️ No se encontró voto para este menú (buscado en 10 páginas)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error al obtener voto: ${e.message}")
                // En caso de error, asumir que no hay voto y continuar (mejor notificar de más que de menos)
            }
            
            // Si el usuario ya votó, no enviar notificación (independientemente del flag)
            if (userVote != null) {
                Log.d(TAG, "✅ Usuario ya votó (opción: ${userVote.option.name}), NO se envía recordatorio de 10am")
                return
            }
            
            // 4. Usuario NO ha votado - verificar si ya se envió notificación de 10am
            // IMPORTANTE: Si el flag está marcado pero el usuario NO ha votado, intentar enviar de nuevo
            // para asegurarnos de que realmente se envió (puede haber fallado silenciosamente antes)
            val wasNotifiedBefore = alarmPrefs.wasNotified10am()
            
            if (wasNotifiedBefore) {
                Log.d(TAG, "ℹ️ Flag de 10am está marcado, pero usuario NO ha votado. Verificando si realmente se envió...")
                
                // Verificar si realmente se puede enviar notificación (tiene permisos)
                val canSendNotification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    android.content.pm.PackageManager.PERMISSION_GRANTED ==
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        )
                } else {
                    true // Android < 13 no requiere permiso explícito
                }
                
                if (!canSendNotification) {
                    // El flag está marcado pero no hay permisos - resetear flag y intentar de nuevo
                    Log.w(TAG, "⚠️ Flag de 10am marcado pero no hay permisos de notificaciones. Reseteando flag para reintentar...")
                    alarmPrefs.reset10amNotification()
                } else {
                    // El flag está marcado y hay permisos, pero el usuario NO ha votado
                    // Esto puede significar que:
                    // 1. La notificación se envió pero el usuario no la vio/ignoró
                    // 2. La notificación falló silenciosamente (cambio de hora del sistema, etc.)
                    // Intentar enviar UNA VEZ MÁS para asegurarnos de que realmente se envió
                    Log.w(TAG, "⚠️ Flag marcado pero usuario NO ha votado. Reintentando envío para confirmar que realmente se envió...")
                    // Continuar con el envío más abajo
                }
            }
            
            // 5. El usuario NO ha votado - enviar recordatorio urgente
            // (ya sea porque no se envió antes, o porque estamos verificando que realmente se envió)
            Log.d(TAG, "⚠️ Usuario NO ha votado, enviando recordatorio urgente de 10am...")
            
            val menuContent = formatMenuOptions(menu)
            val notificationSent = sendNotification(context, isFollowUp = true, menuDescription = menuContent)
            
            // 6. SOLO marcar que se envió la notificación si realmente se envió exitosamente
            if (notificationSent) {
                alarmPrefs.setNotified10am()
                if (wasNotifiedBefore) {
                    Log.d(TAG, "✅ Recordatorio urgente de 10am reenviado y registrado exitosamente (era necesario)")
                } else {
                    Log.d(TAG, "✅ Recordatorio urgente de 10am enviado y registrado exitosamente")
                }
            } else {
                // Si falló y el flag estaba marcado, resetearlo para que se intente de nuevo la próxima vez
                if (wasNotifiedBefore) {
                    Log.w(TAG, "⚠️ No se pudo enviar la notificación de 10am. Reseteando flag porque estaba marcado incorrectamente...")
                    alarmPrefs.reset10amNotification()
                } else {
                    Log.w(TAG, "⚠️ No se pudo enviar la notificación de 10am (posible falta de permisos), NO se marca como enviada")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error crítico al verificar voto del usuario: ${e.message}", e)
            // En caso de error crítico, no enviar notificación para evitar spam
            // Es mejor no molestar al usuario si no estamos seguros de que no votó
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
     * @return true si la notificación se envió exitosamente, false en caso contrario
     */
    private fun sendNotification(context: Context, isFollowUp: Boolean, menuDescription: String?): Boolean {
        return try {
            // Verificar permisos de notificaciones en Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                
                if (!hasPermission) {
                    Log.e(TAG, "❌ No hay permiso de notificaciones (POST_NOTIFICATIONS)")
                    return false
                }
                Log.d(TAG, "✅ Permiso de notificaciones verificado")
            }
            
            val notificationHelper = NotificationHelper(context)
            
            val sent = if (isFollowUp) {
                notificationHelper.showFollowUpReminderNotification(menuDescription)
            } else {
                notificationHelper.showDailyReminderNotification(menuDescription)
            }
            
            if (sent) {
                Log.d(TAG, "✅✅✅ Notificación ${if (isFollowUp) "urgente (10am)" else "de recordatorio"} enviada exitosamente")
            } else {
                Log.w(TAG, "⚠️⚠️⚠️ Notificación ${if (isFollowUp) "urgente (10am)" else "de recordatorio"} NO se pudo enviar")
            }
            sent
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar notificación: ${e.message}", e)
            false
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
    
    /**
     * Maneja una notificación personalizada configurada por el usuario.
     */
    private fun handleCustomNotification(context: Context, notificationId: String, dayOfWeek: Int) {
        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MorfiPolo::CustomNotificationWakeLock"
        )
        
        scope.launch {
            try {
                wakeLock.acquire(WAKE_LOCK_TIMEOUT)
                Log.d(TAG, "📍 Procesando notificación personalizada: $notificationId para día $dayOfWeek")
                
                val app = context.applicationContext as? MorfipoloApplication
                if (app == null) {
                    Log.w(TAG, "No se pudo obtener MorfipoloApplication")
                    return@launch
                }
                
                // Obtener la configuración de la notificación
                val notification = app.notificationConfigRepository.getNotificationById(notificationId)
                if (notification == null || !notification.isEnabled) {
                    Log.d(TAG, "⚠️ Notificación $notificationId no encontrada o deshabilitada")
                    return@launch
                }
                
                // Verificar que sea el día correcto
                val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val expectedCalendarDay = when (dayOfWeek) {
                    1 -> Calendar.MONDAY
                    2 -> Calendar.TUESDAY
                    3 -> Calendar.WEDNESDAY
                    4 -> Calendar.THURSDAY
                    5 -> Calendar.FRIDAY
                    6 -> Calendar.SATURDAY
                    7 -> Calendar.SUNDAY
                    else -> return@launch
                }
                
                if (currentDayOfWeek != expectedCalendarDay) {
                    Log.d(TAG, "⚠️ Día de la semana no coincide (esperado: $dayOfWeek, actual: $currentDayOfWeek)")
                    return@launch
                }
                
                // Verificar si hay menú publicado
                val menu = getTodayMenu(context)
                if (menu == null) {
                    Log.d(TAG, "❌ No hay menú publicado para hoy, NO se envía notificación personalizada")
                    return@launch
                }
                
                Log.d(TAG, "✅ Menú encontrado: ${menu.description}")
                
                // Verificar si el usuario ya votó (PRIORITARIO: verificar estado real antes que flag de notificación)
                val userId = app.sessionManager.getCurrentUserId()
                if (userId != null) {
                    try {
                        Log.d(TAG, "🔍 Verificando si el usuario ya votó para el menú: ${menu.id} (búsqueda limitada a 10 páginas)")
                        val userVote = app.voteRepository.getUserVoteForMenu(menu.id, userId, maxPagesToSearch = 10)
                        if (userVote != null) {
                            Log.d(TAG, "✅ Usuario ya votó (opción: ${userVote.option.name}), NO se envía notificación personalizada")
                            return@launch
                        }
                        Log.d(TAG, "ℹ️ Usuario NO ha votado aún")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Error al verificar voto: ${e.message}")
                        // En caso de error, continuar (mejor notificar de más que de menos)
                    }
                } else {
                    Log.d(TAG, "⚠️ Usuario no logueado, no se puede verificar voto")
                }
                
                // Verificar si ya se envió esta notificación personalizada hoy
                val alarmPrefs = AlarmPreferences(context)
                val wasNotifiedBefore = alarmPrefs.wasCustomNotificationSent(notificationId)
                
                if (wasNotifiedBefore) {
                    Log.d(TAG, "⏭️ Ya se envió la notificación personalizada $notificationId hoy, NO se envía de nuevo")
                    return@launch
                }
                
                // Enviar notificación
                val menuContent = formatMenuOptions(menu)
                val notificationSent = sendNotification(context, isFollowUp = false, menuDescription = menuContent)
                
                // SOLO marcar que se envió la notificación si realmente se envió exitosamente
                if (notificationSent) {
                    alarmPrefs.setCustomNotificationSent(notificationId)
                    Log.d(TAG, "✅ Notificación personalizada $notificationId enviada y registrada exitosamente")
                } else {
                    Log.w(TAG, "⚠️ No se pudo enviar la notificación personalizada $notificationId (posible falta de permisos), NO se marca como enviada")
                }
                
                // Reprogramar para la próxima vez
                AlarmScheduler.scheduleCustomNotifications(context, app.notificationConfigRepository)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al procesar notificación personalizada: ${e.message}", e)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }
}
