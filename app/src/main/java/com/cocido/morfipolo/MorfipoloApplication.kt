package com.cocido.morfipolo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cocido.morfipolo.data.local.database.AppDatabase
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.remote.RetrofitClient
import com.cocido.morfipolo.data.remote.TokenManager
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.data.repository.UserRepository
import com.cocido.morfipolo.data.repository.VoteRepository
import com.cocido.morfipolo.util.work.MenuPollingWorker
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

        // Configurar worker para verificar nuevos menús y enviar notificaciones
        // IMPORTANTE: Usamos OneTimeWorkRequest con re-programación para poder tener intervalos más cortos
        // PeriodicWorkRequest tiene un mínimo de 15 minutos, pero OneTimeWorkRequest permite intervalos más cortos
        // Sin embargo, Android puede aplicar limitaciones según el estado de la batería y Doze mode
        val menuPollingConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Usar OneTimeWorkRequest con intervalo de 5 minutos (más frecuente que el mínimo de 15min de Periodic)
        scheduleMenuPollingWork(workManager, menuPollingConstraints)
    }

    /**
     * Programa el trabajo de polling de menú usando OneTimeWorkRequest
     * que se re-programa automáticamente para permitir intervalos más cortos
     * @param workManager Instancia de WorkManager
     * @param constraints Restricciones para el trabajo (red conectada)
     */
    private fun scheduleMenuPollingWork(workManager: WorkManager, constraints: Constraints) {
        // Crear un OneTimeWorkRequest que se ejecuta inmediatamente la primera vez
        // El worker se re-programa a sí mismo al finalizar para crear un ciclo continuo
        val menuPollingWork = OneTimeWorkRequestBuilder<MenuPollingWorker>()
            .setConstraints(constraints)
            .addTag("menu_polling") // Tag para poder cancelar si es necesario
            .build()

        workManager.enqueueUniqueWork(
            "menu_polling_work",
            ExistingWorkPolicy.REPLACE, // Reemplazar si ya existe
            menuPollingWork
        )
    }
}

