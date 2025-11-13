package com.cocido.morfipolo.util.work

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.util.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MenuPollingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val PREFS_NAME = "menu_polling_prefs"
        private const val KEY_LAST_MENU_ID = "last_menu_id"
        private const val KEY_LAST_MENU_CREATED_AT = "last_menu_created_at"
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

            if (menu != null && menu.status == "open") {
                val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastMenuId = prefs.getString(KEY_LAST_MENU_ID, null)
                val lastMenuCreatedAt = prefs.getString(KEY_LAST_MENU_CREATED_AT, null)

                // Verificar si es un menú nuevo
                val isNewMenu = lastMenuId != menu.id || 
                    (lastMenuCreatedAt != null && menu.created_at > lastMenuCreatedAt) ||
                    lastMenuId == null

                if (isNewMenu) {
                    android.util.Log.d("MenuPollingWorker", "🆕 Nuevo menú detectado: ${menu.id}")
                    
                    // Notificar sobre el nuevo menú
                    val optionsText = if (menu.getOptionsOrEmpty().isNotEmpty()) {
                        menu.getOptionsOrEmpty().joinToString(", ") { it.name }
                    } else {
                        menu.description
                    }
                    
                    android.util.Log.d("MenuPollingWorker", "Enviando notificación: ${menu.description}")
                    notificationHelper.showMenuLoadedNotification(
                        menu.description,
                        optionsText
                    )

                    // Guardar información del menú notificado
                    prefs.edit().apply {
                        putString(KEY_LAST_MENU_ID, menu.id)
                        putString(KEY_LAST_MENU_CREATED_AT, menu.created_at)
                        apply()
                    }
                    
                    android.util.Log.d("MenuPollingWorker", "✅ Notificación enviada exitosamente")
                } else {
                    android.util.Log.d("MenuPollingWorker", "Menú ya conocido, no se envía notificación")
                }
            }

            android.util.Log.d("MenuPollingWorker", "✅ Verificación de menú completada")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("MenuPollingWorker", "❌ Error al verificar menú", e)
            // En caso de error, retry con backoff
            Result.retry()
        }
    }
}
