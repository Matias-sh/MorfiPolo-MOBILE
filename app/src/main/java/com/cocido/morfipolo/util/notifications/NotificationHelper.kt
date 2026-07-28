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
import androidx.core.content.ContextCompat
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
            val accentColor = ContextCompat.getColor(context, R.color.comedor_brown_primary)
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                // Habilitar sonido y vibración estilo WhatsApp
                enableVibration(true)
                enableLights(true)
                lightColor = accentColor // Color de la app para la luz LED
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

        val baseText = context.getString(R.string.menu_loaded_notification_text)
        val contentText = if (optionsText != null && optionsText.isNotEmpty()) {
            "$baseText\n\n$menuDescription\n\nOpciones disponibles:\n$optionsText"
        } else {
            "$baseText\n\n$menuDescription"
        }

        val smallIcon = R.drawable.ic_notification_white
        val accentColor = ContextCompat.getColor(context, R.color.food_primary_orange)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(context.getString(R.string.menu_loaded_notification_title))
            .setContentText(baseText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
                    .setSummaryText("Comedor Polo")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración y luz
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Auto-ocultar al hacer click
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(System.currentTimeMillis()) // Timestamp actual
            .setShowWhen(true) // Mostrar timestamp
            .setOnlyAlertOnce(false) // Alertar cada vez (como WhatsApp)
            .setColor(accentColor)
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

    /**
     * Muestra una notificación de recordatorio diario a las 9am
     * para que el usuario se anote en la comida del día
     */
    fun showDailyReminderNotification(menuDescription: String? = null): Boolean {
        return try {
            // Verificar permisos de notificaciones en Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                
                if (!hasPermission) {
                    android.util.Log.e("NotificationHelper", "❌ No hay permiso de notificaciones")
                    return false
                }
                android.util.Log.d("NotificationHelper", "✅ Permiso de notificaciones verificado")
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                1, // ID diferente para el recordatorio
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Construir contenido del recordatorio con mejor formato
            val baseText = context.getString(R.string.daily_reminder_notification_text)
            val bigText = if (menuDescription != null && menuDescription.isNotEmpty()) {
                "$baseText\n\nOpciones disponibles:\n${formatOptionsForNotification(menuDescription)}"
            } else {
                baseText
            }

            // Usar icono blanco para notificaciones (requerido por Android)
            val smallIcon = R.drawable.ic_notification_white
            
            // Obtener color de acento de la app
            val accentColor = ContextCompat.getColor(context, R.color.comedor_brown_primary)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(context.getString(R.string.daily_reminder_notification_title))
                .setContentText(baseText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bigText)
                        .setSummaryText("Comedor Polo")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración y luz
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setOnlyAlertOnce(false)
                .setColor(accentColor) // Color de acento de la app
                .build()

            notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)
            
            android.util.Log.d("NotificationHelper", "📢✅ Recordatorio diario enviado exitosamente (ID: $REMINDER_NOTIFICATION_ID)")
            true // Notificación enviada exitosamente
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error al enviar notificación de recordatorio", e)
            false // Error al enviar notificación
        }
    }

    /**
     * Muestra una notificación de seguimiento a las 10am
     * para recordar al usuario que aún no votó
     */
    fun showFollowUpReminderNotification(menuDescription: String? = null): Boolean {
        return try {
            android.util.Log.d("NotificationHelper", "🔔 Intentando enviar notificación de seguimiento (10AM)...")
            
            // Verificar permisos de notificaciones en Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                
                if (!hasPermission) {
                    android.util.Log.e("NotificationHelper", "❌ No hay permiso de notificaciones (POST_NOTIFICATIONS)")
                    return false
                }
                android.util.Log.d("NotificationHelper", "✅ Permiso de notificaciones verificado")
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                2, // ID diferente para el recordatorio de seguimiento
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val title = "Todavía estás a tiempo de votar"
            val baseText = "Queda menos de una hora para anotarte en el menú de hoy."
            val bigText = if (menuDescription != null && menuDescription.isNotEmpty()) {
                "$baseText\n\nOpciones disponibles:\n${formatOptionsForNotification(menuDescription)}\n\nVotá antes de las 11:00."
            } else {
                "$baseText\n\nVotá antes de las 11:00."
            }

            // Usar icono blanco para notificaciones
            val smallIcon = R.drawable.ic_notification_white
            
            // Usar color naranja/rojo para urgencia
            val urgentColor = ContextCompat.getColor(context, R.color.comedor_accent_warm)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(baseText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bigText)
                        .setSummaryText("Comedor Polo")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setOnlyAlertOnce(false)
                .setColor(urgentColor)
                .build()

            notificationManager.notify(FOLLOWUP_NOTIFICATION_ID, notification)
            
            android.util.Log.d("NotificationHelper", "📢✅ Recordatorio de seguimiento (10AM) enviado exitosamente (ID: $FOLLOWUP_NOTIFICATION_ID)")
            android.util.Log.d("NotificationHelper", "   - Título: $title")
            android.util.Log.d("NotificationHelper", "   - Canal: $CHANNEL_ID")
            android.util.Log.d("NotificationHelper", "   - NotificationManager: ${notificationManager.javaClass.simpleName}")
            true // Notificación enviada exitosamente
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error al enviar notificación de seguimiento", e)
            android.util.Log.e("NotificationHelper", "   - Tipo de error: ${e.javaClass.simpleName}")
            android.util.Log.e("NotificationHelper", "   - Mensaje: ${e.message}")
            e.printStackTrace()
            false // Error al enviar notificación
        }
    }

    companion object {
        private const val CHANNEL_ID = "menu_updates_channel_v2"
        private const val NOTIFICATION_ID = 1
        private const val REMINDER_NOTIFICATION_ID = 2
        private const val FOLLOWUP_NOTIFICATION_ID = 3
    }

    private fun formatOptionsForNotification(raw: String): String {
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(4)
            .joinToString("\n")
    }
}




