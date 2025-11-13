package com.cocido.morfipolo.util.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cocido.morfipolo.R
import com.cocido.morfipolo.ui.main.MainActivity
import com.cocido.morfipolo.util.widget.MenuWidgetProvider

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                // Habilitar sonido y vibración
                enableVibration(true)
                enableLights(true)
                // Usar sonido por defecto del sistema
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMenuLoadedNotification(menuDescription: String, optionsText: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (optionsText != null && optionsText.isNotEmpty()) {
            "${context.getString(R.string.menu_loaded_notification_text)}\n\n$menuDescription\n\nOpciones disponibles:\n$optionsText"
        } else {
            "${context.getString(R.string.menu_loaded_notification_text)}\n\n$menuDescription"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_menu) // Icono de notificación personalizado
            .setContentTitle(context.getString(R.string.menu_loaded_notification_title))
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración y luz por defecto
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Actualizar widget cuando se envía la notificación
        updateWidget(context)
    }

    /**
     * Actualiza el widget cuando se envía una notificación de nuevo menú
     */
    private fun updateWidget(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MenuWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                android.util.Log.d("NotificationHelper", "Actualizando ${appWidgetIds.size} widgets después de notificación")
                val updateIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error al actualizar widget", e)
        }
    }

    companion object {
        private const val CHANNEL_ID = "menu_updates_channel"
        private const val NOTIFICATION_ID = 1
    }
}




