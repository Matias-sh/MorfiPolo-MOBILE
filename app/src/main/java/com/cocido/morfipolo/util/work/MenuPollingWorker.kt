package com.cocido.morfipolo.util.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.util.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class MenuPollingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as MorfipoloApplication
            val menuRepository = app.menuRepository
            val notificationHelper = NotificationHelper(applicationContext)

            // Obtener menú del día actual
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val menu = menuRepository.getMenuByDate(today.time)

            // Si hay un menú disponible y fue cargado recientemente (en los últimos 5 minutos)
            // En producción, esto se verificaría comparando con la última vez que se notificó
            if (menu != null) {
                // Por simplicidad, notificamos si el menú existe
                // En producción, se debería verificar si es un menú nuevo
                notificationHelper.showMenuLoadedNotification(menu.descripcion)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}


