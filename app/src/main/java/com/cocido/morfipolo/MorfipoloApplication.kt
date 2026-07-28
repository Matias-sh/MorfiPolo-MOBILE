package com.cocido.morfipolo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cocido.morfipolo.data.local.database.AppDatabase
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.remote.RetrofitClient
import com.cocido.morfipolo.data.remote.TokenManager
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.data.repository.NotificationConfigRepository
import com.cocido.morfipolo.data.repository.UserRepository
import com.cocido.morfipolo.data.repository.VoteRepository
import com.cocido.morfipolo.util.alarm.AlarmScheduler
import com.cocido.morfipolo.util.work.SessionRefreshWorker
import java.util.concurrent.TimeUnit

class MorfipoloApplication : Application() {
    
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "morfipolo_database"
        )
            .addMigrations(*AppDatabase.getMigrations())
            .build()
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(applicationContext)
    }
    
    val tokenManager: TokenManager by lazy {
        TokenManager(sessionManager) { dni, password ->
            // Crear un servicio temporal sin interceptor para refresh token
            RetrofitClient.createApiServiceForRefresh(sessionManager)
        }
    }
    
    val apiService: com.cocido.morfipolo.data.remote.api.MorfiPoloApiService by lazy {
        RetrofitClient.createApiService(sessionManager, tokenManager)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database, apiService, sessionManager, tokenManager)
    }

    val menuRepository: MenuRepository by lazy {
        MenuRepository(database, apiService)
    }
    
    val voteRepository: VoteRepository by lazy {
        VoteRepository(apiService)
    }
    
    val notificationConfigRepository: NotificationConfigRepository by lazy {
        NotificationConfigRepository(applicationContext)
    }
    
    val authManager: com.cocido.morfipolo.data.remote.AuthManager by lazy {
        com.cocido.morfipolo.data.remote.AuthManager(sessionManager, tokenManager)
    }

    override fun onCreate() {
        super.onCreate()
        // Forzar modo claro en toda la app, independientemente de la configuración del sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setupWorkManager()
    }

    private fun setupWorkManager() {
        val workManager = WorkManager.getInstance(this)
        
        // Configurar worker para refrescar sesión automáticamente
        // Se ejecuta cada hora para mantener la sesión activa
        val sessionRefreshConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sessionRefreshWork = PeriodicWorkRequestBuilder<SessionRefreshWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(sessionRefreshConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "session_refresh_work",
            ExistingPeriodicWorkPolicy.KEEP,
            sessionRefreshWork
        )

        // Cancelar flujo legacy basado en WorkManager (recordatorio fijo 9:00)
        // para evitar notificaciones duplicadas o fuera de la configuración del usuario.
        workManager.cancelUniqueWork("daily_reminder_work")

        // Mantener método legacy como no-op por compatibilidad.
        notificationConfigRepository.createDefaultNotificationsIfNeeded()
        notificationConfigRepository.clearLegacyDefaultNotificationsIfNeeded()
        
        // Programar las notificaciones personalizadas del usuario
        // Usa AlarmManager con setExactAndAllowWhileIdle() para garantizar ejecución
        // incluso cuando la app está completamente cerrada
        AlarmScheduler.scheduleCustomNotifications(this, notificationConfigRepository)
    }
}

