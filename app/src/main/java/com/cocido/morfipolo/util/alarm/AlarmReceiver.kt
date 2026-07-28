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
 * BroadcastReceiver que recibe las alarmas de notificaciones personalizadas.
 * 
 * Todas las notificaciones son configurables por el usuario desde el apartado
 * de Notificaciones en la app. Por defecto vienen predefinidas:
 * - 9:00 AM (Lunes a Viernes)
 * - 9:30 AM (Lunes a Viernes)
 * - 10:00 AM (Lunes a Viernes)
 * 
 * El usuario puede modificarlas, eliminarlas o agregar nuevas.
 * 
 * LÓGICA DE NOTIFICACIONES:
 * 1. ¿Hay menú publicado? → NO → No enviar notificación
 * 2. ¿Usuario logueado y ya votó? → SÍ → No enviar notificación
 * 3. En cualquier otro caso → Enviar notificación
 */
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_CUSTOM_NOTIFICATION = "com.cocido.morfipolo.CUSTOM_NOTIFICATION"
        private const val WAKE_LOCK_TIMEOUT = 30000L // 30 segundos
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        
        if (action != ACTION_CUSTOM_NOTIFICATION) {
            Log.w(TAG, "Acción desconocida: $action")
            return
        }
        
        val notificationId = intent.getStringExtra("notification_id")
        val dayOfWeek = intent.getIntExtra("day_of_week", -1)
        
        if (notificationId == null || dayOfWeek == -1) {
            Log.w(TAG, "⚠️ Notificación sin ID o día de semana")
            return
        }
        
        handleCustomNotification(context, notificationId, dayOfWeek)
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
                Log.d(TAG, "📍 Procesando notificación: $notificationId para día $dayOfWeek")
                
                val app = context.applicationContext as? MorfipoloApplication
                if (app == null) {
                    Log.w(TAG, "No se pudo obtener MorfipoloApplication")
                    return@launch
                }
                
                // Verificar que la notificación existe y está habilitada
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
                
                // 1. Verificar si hay menú publicado
                val menu = getTodayMenu(context)
                if (menu == null) {
                    Log.d(TAG, "❌ No hay menú publicado para hoy, NO se envía notificación")
                    return@launch
                }
                
                Log.d(TAG, "✅ Menú encontrado: ${menu.description}")
                
                // 2. Verificar si el usuario ya votó (solo si está logueado)
                val userId = app.sessionManager.getCurrentUserId()
                if (userId != null) {
                    try {
                        Log.d(TAG, "🔍 Verificando si el usuario ya votó...")
                        val userVote = app.voteRepository.getUserVoteForMenu(menu.id, userId, maxPagesToSearch = 10)
                        if (userVote != null) {
                            Log.d(TAG, "✅ Usuario ya votó (opción: ${userVote.option.name}), NO se envía notificación")
                            return@launch
                        }
                        Log.d(TAG, "ℹ️ Usuario NO ha votado aún")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Error al verificar voto: ${e.message}")
                        // Continuar con la notificación (mejor notificar de más que de menos)
                    }
                } else {
                    Log.d(TAG, "ℹ️ Usuario no logueado, enviando notificación para que abra la app")
                }
                
                // 3. Verificar si ya se envió esta notificación hoy
                val alarmPrefs = AlarmPreferences(context)
                if (alarmPrefs.wasCustomNotificationSent(notificationId)) {
                    Log.d(TAG, "⏭️ Ya se envió la notificación $notificationId hoy")
                    return@launch
                }
                
                // 4. Enviar notificación
                val menuContent = formatMenuOptions(menu)
                val notificationSent = sendNotification(context, menuContent)
                
                if (notificationSent) {
                    alarmPrefs.setCustomNotificationSent(notificationId)
                    Log.d(TAG, "✅ Notificación $notificationId enviada exitosamente")
                } else {
                    Log.w(TAG, "⚠️ No se pudo enviar la notificación $notificationId")
                }
                
                // 5. Reprogramar para la próxima vez
                AlarmScheduler.scheduleCustomNotifications(context, app.notificationConfigRepository)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al procesar notificación: ${e.message}", e)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Obtiene el menú del día actual.
     */
    private suspend fun getTodayMenu(context: Context): Menu? {
        return try {
            val app = context.applicationContext as? MorfipoloApplication ?: return null
            
            val todayCalendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayDate = todayCalendar.time
            val todayString = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(todayDate)
            
            val menu = app.menuRepository.getMenuByDate(todayDate)
            
            // Solo devolver menú si corresponde al día actual y está publicado/abierto.
            if (menu != null && menu.date == todayString && menu.status == "open") {
                menu
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al obtener menú: ${e.message}")
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
     * Envía la notificación.
     */
    private fun sendNotification(context: Context, menuDescription: String?): Boolean {
        return try {
            // Verificar permisos en Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                
                if (!hasPermission) {
                    Log.e(TAG, "❌ No hay permiso de notificaciones")
                    return false
                }
            }
            
            val notificationHelper = NotificationHelper(context)
            val sent = notificationHelper.showDailyReminderNotification(menuDescription)
            
            if (sent) {
                Log.d(TAG, "✅ Notificación enviada exitosamente")
            }
            sent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar notificación: ${e.message}", e)
            false
        }
    }
}
