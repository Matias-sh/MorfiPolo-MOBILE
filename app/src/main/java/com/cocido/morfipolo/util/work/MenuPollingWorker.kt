package com.cocido.morfipolo.util.work

import android.content.Context
import android.content.SharedPreferences
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
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.*

class MenuPollingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val PREFS_NAME = "menu_polling_prefs"
        private const val KEY_LAST_MENU_ID = "last_menu_id"
        private const val KEY_LAST_MENU_CREATED_AT = "last_menu_created_at"
        private const val KEY_LAST_MENU_UPDATED_AT = "last_menu_updated_at"
        private const val KEY_LAST_MENU_STATUS = "last_menu_status"
        private const val KEY_LAST_NOTIFIED_DATE = "last_notified_date" // Fecha del último menú notificado
        private const val KEY_LAST_NOTIFIED_MENU_ID = "last_notified_menu_id" // ID del último menú notificado
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as? MorfipoloApplication
                ?: run {
                    android.util.Log.e("MenuPollingWorker", "No se pudo obtener MorfipoloApplication")
                    return@withContext Result.retry()
                }
            
            val menuRepository = app.menuRepository
            val notificationHelper = NotificationHelper(applicationContext)

            // CRÍTICO: Refrescar sesión antes de hacer la llamada al API
            // Esto asegura que el usuario tenga una sesión válida para recibir notificaciones
            android.util.Log.d("MenuPollingWorker", "Refrescando sesión antes de verificar menú...")
            if (app.sessionManager.isLoggedIn()) {
                val authResult = app.authManager.verifyAndRefreshAuth()
                when (authResult) {
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                        android.util.Log.d("MenuPollingWorker", "✅ Sesión refrescada, verificando menú...")
                    }
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed,
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                        android.util.Log.w("MenuPollingWorker", "⚠️ No hay sesión válida, no se puede verificar menú")
                        // Si no hay sesión, no es un error crítico, simplemente no verificamos el menú
                        // El usuario recibirá notificaciones cuando inicie sesión y el widget se actualice
                        return@withContext Result.success()
                    }
                }
            } else {
                android.util.Log.d("MenuPollingWorker", "No hay sesión activa, no se puede verificar menú")
                return@withContext Result.success()
            }

            // Obtener menú del día actual
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            android.util.Log.d("MenuPollingWorker", "Obteniendo menú del día...")
            val menu = menuRepository.getMenuByDate(today.time)

            // Filtrar menús "draft" - no notificar sobre borradores
            if (menu != null && menu.status != "draft") {
                val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayString = dateFormat.format(today.time)
                
                val lastMenuId = prefs.getString(KEY_LAST_MENU_ID, null)
                val lastMenuCreatedAt = prefs.getString(KEY_LAST_MENU_CREATED_AT, null)
                val lastMenuUpdatedAt = prefs.getString(KEY_LAST_MENU_UPDATED_AT, null)
                val lastMenuStatus = prefs.getString(KEY_LAST_MENU_STATUS, null)
                val lastNotifiedDate = prefs.getString(KEY_LAST_NOTIFIED_DATE, null)
                val lastNotifiedMenuId = prefs.getString(KEY_LAST_NOTIFIED_MENU_ID, null)

                // CRÍTICO: Detectar si es un menú nuevo del día de hoy
                // Notificar si:
                // 1. Es un menú completamente nuevo (nuevo ID)
                // 2. Es el menú del día de hoy y NO se notificó antes para este día
                // 3. Es el mismo menú pero cambió de status a "open" (de draft a open)
                // 4. El menú tiene opciones disponibles Y está abierto
                val isNewMenuForToday = menu.date == todayString && 
                    (lastNotifiedDate != todayString || lastNotifiedMenuId != menu.id)
                
                val isMenuNewlyOpened = menu.status == "open" && 
                    (lastMenuStatus == null || lastMenuStatus != "open" || lastMenuStatus == "draft")
                
                val hasNewOptions = menu.getOptionsOrEmpty().isNotEmpty() && 
                    (lastMenuId == null || lastMenuId != menu.id || isMenuNewlyOpened)

                val shouldNotify = menu.status == "open" && (
                    isNewMenuForToday || // Menú nuevo del día
                    isMenuNewlyOpened || // Menú que se acaba de abrir
                    hasNewOptions // Menú con opciones nuevas
                )

                if (shouldNotify) {
                    android.util.Log.d("MenuPollingWorker", "🆕 Nuevo menú del día detectado: ${menu.id}, fecha: ${menu.date}, status: ${menu.status}")
                    
                    // Construir mensaje descriptivo
                    val optionsText = if (menu.getOptionsOrEmpty().isNotEmpty()) {
                        val optionsList = menu.getOptionsOrEmpty().mapIndexed { index, option ->
                            "${index + 1}. ${option.name}"
                        }
                        optionsList.joinToString("\n")
                    } else {
                        null
                    }
                    
                    android.util.Log.d("MenuPollingWorker", "📢 Enviando notificación de nuevo menú del día...")
                    notificationHelper.showMenuLoadedNotification(
                        menu.description,
                        optionsText
                    )

                    // Guardar información del menú notificado
                    prefs.edit().apply {
                        putString(KEY_LAST_MENU_ID, menu.id)
                        putString(KEY_LAST_MENU_CREATED_AT, menu.created_at)
                        putString(KEY_LAST_MENU_UPDATED_AT, menu.updated_at)
                        putString(KEY_LAST_MENU_STATUS, menu.status)
                        putString(KEY_LAST_NOTIFIED_DATE, todayString) // Guardar fecha notificada
                        putString(KEY_LAST_NOTIFIED_MENU_ID, menu.id) // Guardar ID del menú notificado
                        apply()
                    }
                    
                    android.util.Log.d("MenuPollingWorker", "✅ Notificación enviada exitosamente para el menú del día")
                } else {
                    // Actualizar información del menú aunque no se notifique
                    prefs.edit().apply {
                        putString(KEY_LAST_MENU_ID, menu.id)
                        putString(KEY_LAST_MENU_CREATED_AT, menu.created_at)
                        putString(KEY_LAST_MENU_UPDATED_AT, menu.updated_at)
                        putString(KEY_LAST_MENU_STATUS, menu.status)
                        apply()
                    }
                    android.util.Log.d("MenuPollingWorker", "Menú ya conocido o ya notificado hoy, no se envía notificación")
                }
            } else {
                android.util.Log.d("MenuPollingWorker", "No hay menú disponible para el día de hoy o es un borrador")
            }

            android.util.Log.d("MenuPollingWorker", "✅ Verificación de menú completada")
            
            // CRÍTICO: Re-programar el trabajo para crear un ciclo continuo
            // Esto nos permite tener intervalos más cortos que el mínimo de 15 minutos de PeriodicWorkRequest
            scheduleNextPoll(applicationContext)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("MenuPollingWorker", "❌ Error al verificar menú", e)
            
            // Aún así intentar re-programar, pero con un delay más largo en caso de error
            scheduleNextPoll(applicationContext, delayMinutes = 10)
            
            // En caso de error, retry con backoff
            Result.retry()
        }
    }
    
    /**
     * Programa el próximo trabajo de polling
     * @param context Contexto de la aplicación
     * @param delayMinutes Minutos de espera antes del próximo polling (default: 5 minutos)
     */
    private fun scheduleNextPoll(context: Context, delayMinutes: Long = 5) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val nextWork = OneTimeWorkRequestBuilder<MenuPollingWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("menu_polling")
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "menu_polling_work",
                androidx.work.ExistingWorkPolicy.REPLACE,
                nextWork
            )
            
            android.util.Log.d("MenuPollingWorker", "✅ Próximo polling programado en $delayMinutes minutos")
        } catch (e: Exception) {
            android.util.Log.e("MenuPollingWorker", "Error al programar próximo polling", e)
        }
    }
}
