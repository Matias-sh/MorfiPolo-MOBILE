package com.cocido.morfipolo.util.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cocido.morfipolo.MorfipoloApplication
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker que refresca automáticamente la sesión del usuario en segundo plano
 * para mantener la sesión activa sin necesidad de iniciar sesión constantemente.
 * 
 * Se ejecuta periódicamente para verificar y refrescar el access token
 * antes de que expire, usando el refresh token.
 */
class SessionRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SessionRefreshWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as? MorfipoloApplication
                ?: run {
                    Log.w(TAG, "No se pudo obtener MorfipoloApplication")
                    return@withContext Result.failure()
                }

            // Verificar si hay sesión activa
            if (!app.sessionManager.isLoggedIn()) {
                Log.d(TAG, "No hay sesión activa, no se requiere refresh")
                return@withContext Result.success()
            }

            Log.d(TAG, "Iniciando refresh automático de sesión...")
            
            // Verificar y refrescar autenticación
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    Log.d(TAG, "✅ Sesión refrescada exitosamente")
                    Result.success()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed -> {
                    Log.w(TAG, "⚠️ Refresh token expirado, el usuario debe iniciar sesión manualmente")
                    // El refresh token expiró, pero no es un error del worker
                    Result.success()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                    Log.d(TAG, "No hay sesión guardada, no se requiere refresh")
                    Result.success()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                    Log.w(TAG, "⚠️ Error temporal (servidor/red), reintentando más tarde...")
                    // Error temporal - la sesión sigue válida, pero hacer retry
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al refrescar sesión", e)
            // Retry con backoff exponencial en caso de error de red
            Result.retry()
        }
    }
}

