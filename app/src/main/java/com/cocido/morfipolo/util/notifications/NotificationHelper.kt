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
                // Habilitar sonido y vibración estilo WhatsApp
                enableVibration(true)
                enableLights(true)
                // Usar sonido por defecto del sistema (notification sound)
                setShowBadge(true)
                // Configurar sonido de notificación (usar el predeterminado del sistema)
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
                // Vibración personalizada: corta, pausa, corta (estilo WhatsApp)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
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

        // Construir contenido mejorado
        val contentText = if (optionsText != null && optionsText.isNotEmpty()) {
            "${context.getString(R.string.menu_loaded_notification_text)}\n\n📋 $menuDescription\n\n🍽️ Opciones disponibles:\n$optionsText"
        } else {
            "${context.getString(R.string.menu_loaded_notification_text)}\n\n📋 $menuDescription"
        }

        // Usar icono de notificación (si existe) o icono de launcher como fallback
        // Android requiere un icono blanco para notificaciones, usamos el launcher
        val smallIcon = R.mipmap.ic_launcher

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(context.getString(R.string.menu_loaded_notification_title))
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
                    .setSummaryText(context.getString(R.string.menu_loaded_notification_text))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración y luz
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Auto-ocultar al hacer click
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(System.currentTimeMillis()) // Timestamp actual
            .setShowWhen(true) // Mostrar timestamp
            .setOnlyAlertOnce(false) // Alertar cada vez (como WhatsApp)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Actualizar widget cuando se envía la notificación
        updateWidget(context)
    }

    /**
     * Actualiza el widget y la app cuando se envía una notificación de nuevo menú
     */
    private fun updateWidget(context: Context) {
        try {
            // Actualizar todos los widgets instalados
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MenuWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                android.util.Log.d("NotificationHelper", "🔄 Actualizando ${appWidgetIds.size} widget(s) después de notificación")
                val updateIntent = Intent(context, MenuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(updateIntent)
            }
            
            // CRÍTICO: También enviar broadcast para actualizar la app si está abierta
            // Esto permite que los fragments actualicen su contenido automáticamente
            val appUpdateIntent = Intent("com.cocido.morfipolo.MENU_UPDATED").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(appUpdateIntent)
            android.util.Log.d("NotificationHelper", "📱 Broadcast enviado para actualizar la app")
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error al actualizar widget/app", e)
        }
    }

    companion object {
        private const val CHANNEL_ID = "menu_updates_channel"
        private const val NOTIFICATION_ID = 1
    }
}




