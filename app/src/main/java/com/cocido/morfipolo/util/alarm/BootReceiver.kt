package com.cocido.morfipolo.util.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cocido.morfipolo.MorfipoloApplication

/**
 * BroadcastReceiver que escucha cuando el dispositivo se reinicia
 * y reprograma las notificaciones personalizadas del usuario.
 * 
 * Esto es necesario porque las alarmas programadas con AlarmManager
 * se pierden cuando el dispositivo se apaga.
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "📱 Dispositivo reiniciado, reprogramando notificaciones...")
                
                try {
                    val app = context.applicationContext as? MorfipoloApplication
                    if (app != null) {
                        // Reprogramar las notificaciones personalizadas del usuario
                        AlarmScheduler.scheduleCustomNotifications(context, app.notificationConfigRepository)
                        Log.d(TAG, "✅ Notificaciones reprogramadas después del reinicio")
                    } else {
                        Log.w(TAG, "⚠️ No se pudo obtener MorfipoloApplication")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al reprogramar notificaciones: ${e.message}", e)
                }
            }
            else -> {
                Log.w(TAG, "Acción desconocida: ${intent?.action}")
            }
        }
    }
}
