package com.cocido.morfipolo

import android.app.Application
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cocido.morfipolo.data.local.database.AppDatabase
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.mock.MockBackendService
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.data.repository.UserRepository
import com.cocido.morfipolo.util.work.MenuPollingWorker
import java.util.concurrent.TimeUnit

class MorfipoloApplication : Application() {
    
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "morfipolo_database"
        ).build()
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(applicationContext)
    }

    val mockBackend: MockBackendService by lazy {
        MockBackendService()
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database, mockBackend, sessionManager)
    }

    val menuRepository: MenuRepository by lazy {
        MenuRepository(database, mockBackend)
    }

    override fun onCreate() {
        super.onCreate()
        setupWorkManager()
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<MenuPollingWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "menu_polling_work",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
}

