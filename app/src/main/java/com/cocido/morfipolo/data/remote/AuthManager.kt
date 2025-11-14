package com.cocido.morfipolo.data.remote

import android.util.Log
import com.cocido.morfipolo.data.local.preferences.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Maneja la autenticación automática de la app:
 * - Verifica si el usuario está logueado
 * - Refresca el access token si está expirado
 * - Limpia la sesión y requiere login si el refresh token también expiró
 */
class AuthManager(
    private val sessionManager: SessionManager,
    private val tokenManager: TokenManager
) {
    
    companion object {
        private const val TAG = "AuthManager"
    }
    
    /**
     * Resultado de la verificación de autenticación
     */
    sealed class AuthResult {
        object Authenticated : AuthResult() // Usuario autenticado y token válido
        object RefreshFailed : AuthResult() // Refresh token expiró, requiere login
        object NotLoggedIn : AuthResult() // No hay sesión guardada
    }
    
    /**
     * Verifica y actualiza la autenticación del usuario.
     * - Si no hay sesión, retorna NotLoggedIn
     * - Si el access token está expirado o cerca de expirar, intenta refrescarlo
     * - Si el refresh token también expiró, limpia la sesión y retorna RefreshFailed
     */
    suspend fun verifyAndRefreshAuth(): AuthResult = withContext(Dispatchers.IO) {
        // Verificar si hay sesión guardada
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "No hay sesión guardada")
            return@withContext AuthResult.NotLoggedIn
        }
        
        val accessToken = sessionManager.getAccessToken()
        val refreshToken = sessionManager.getRefreshToken()
        
        if (accessToken == null || refreshToken == null) {
            Log.w(TAG, "Tokens no encontrados, limpiando sesión")
            sessionManager.logout()
            return@withContext AuthResult.NotLoggedIn
        }
        
        // Verificar si el refresh token está expirado ANTES de intentar refrescar
        if (tokenManager.isTokenExpired(refreshToken)) {
            Log.w(TAG, "Refresh token expirado, requiere login")
            sessionManager.logout()
            return@withContext AuthResult.RefreshFailed
        }
        
        // Intentar obtener un token válido (se refrescará automáticamente si es necesario)
        val newAccessToken = tokenManager.getValidAccessToken()
        if (newAccessToken == null) {
            Log.e(TAG, "No se pudo obtener un access token válido")
            
            // Verificar nuevamente si el refresh token sigue siendo válido
            val currentRefreshToken = sessionManager.getRefreshToken()
            if (currentRefreshToken == null || tokenManager.isTokenExpired(currentRefreshToken)) {
                Log.w(TAG, "Refresh token expirado o no disponible después de intentar refrescar, limpiando sesión")
                sessionManager.logout()
                return@withContext AuthResult.RefreshFailed
            }
            
            // Si el refresh token sigue siendo válido pero no se pudo refrescar,
            // puede ser un error temporal de red, pero requerimos login por seguridad
            Log.w(TAG, "Error al refrescar token (posible error de red), limpiando sesión")
            sessionManager.logout()
            return@withContext AuthResult.RefreshFailed
        }
        
        Log.d(TAG, "Usuario autenticado correctamente (token válido)")
        return@withContext AuthResult.Authenticated
    }
}




