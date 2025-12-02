package com.cocido.morfipolo.util.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver que escucha cuando el dispositivo se reinicia
 * y reprograma la alarma de las 9am.
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
                Log.d(TAG, "📱 Dispositivo reiniciado, reprogramando alarma...")
                
                try {
                    // Reprogramar la alarma diaria
                    AlarmScheduler.scheduleDailyAlarm(context)
                    Log.d(TAG, "✅ Alarma reprogramada después del reinicio")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error al reprogramar alarma: ${e.message}", e)
                }
            }
            else -> {
                Log.w(TAG, "Acción desconocida: ${intent?.action}")
            }
        }
    }
}

