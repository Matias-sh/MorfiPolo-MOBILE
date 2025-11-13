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
     * - Si el access token está expirado, intenta refrescarlo
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
        
        // Verificar si el access token está expirado
        if (tokenManager.isTokenExpired(accessToken)) {
            Log.d(TAG, "Access token expirado, intentando refrescar...")
            
            // Verificar si el refresh token también expiró
            if (tokenManager.isTokenExpired(refreshToken)) {
                Log.w(TAG, "Refresh token también expirado, requiere login")
                sessionManager.logout()
                return@withContext AuthResult.RefreshFailed
            }
            
            // Intentar refrescar el access token
            val newAccessToken = tokenManager.getValidAccessToken()
            if (newAccessToken == null) {
                Log.e(TAG, "No se pudo refrescar el access token")
                // Verificar si el refresh token sigue siendo válido
                if (tokenManager.isTokenExpired(refreshToken)) {
                    Log.w(TAG, "Refresh token expirado después de intentar refrescar, limpiando sesión")
                    sessionManager.logout()
                    return@withContext AuthResult.RefreshFailed
                }
                // Si el refresh token sigue siendo válido pero no se pudo refrescar,
                // puede ser un error temporal, pero por seguridad requerimos login
                sessionManager.logout()
                return@withContext AuthResult.RefreshFailed
            }
            
            Log.d(TAG, "Access token refrescado exitosamente")
            return@withContext AuthResult.Authenticated
        }
        
        // El access token es válido, pero verificar si está cerca de expirar y refrescarlo preventivamente
        val newAccessToken = tokenManager.getValidAccessToken()
        if (newAccessToken == null) {
            Log.w(TAG, "No se pudo obtener un token válido, limpiando sesión")
            sessionManager.logout()
            return@withContext AuthResult.RefreshFailed
        }
        
        Log.d(TAG, "Usuario autenticado correctamente")
        return@withContext AuthResult.Authenticated
    }
}



