package com.cocido.morfipolo.data.remote

import android.util.Log
import com.cocido.morfipolo.data.local.preferences.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Maneja la autenticación automática de la app:
 * - Verifica si el usuario está logueado
 * - Refresca el access token si está expirado
 * - Limpia la sesión y requiere login SOLO si el refresh token expiró (401 o expirado localmente)
 * - NO limpia la sesión en errores temporales (errores de servidor 5xx, errores de red)
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
        object TemporaryError : AuthResult() // Error temporal (servidor/red), la sesión sigue válida
    }
    
    /**
     * Verifica y actualiza la autenticación del usuario.
     * - Si no hay sesión, retorna NotLoggedIn
     * - Si el access token está expirado o cerca de expirar, intenta refrescarlo
     * - Si el refresh token expiró (401 o expirado localmente), limpia la sesión y retorna RefreshFailed
     * - Si hay un error temporal (servidor/red), retorna TemporaryError pero NO limpia la sesión
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
            Log.w(TAG, "Refresh token expirado localmente, requiere login")
            sessionManager.logout()
            return@withContext AuthResult.RefreshFailed
        }
        
        // Intentar obtener un token válido (se refrescará automáticamente si es necesario)
        val tokenResult = tokenManager.getValidAccessTokenResult()
        
        return@withContext when (tokenResult) {
            is TokenManager.TokenResult.Success -> {
                Log.d(TAG, "✅ Usuario autenticado correctamente (token válido)")
                AuthResult.Authenticated
            }
            
            is TokenManager.TokenResult.TokenExpired -> {
                // El refresh token expiró (401 del servidor o expirado localmente)
                Log.w(TAG, "❌ Refresh token expiró, limpiando sesión")
                sessionManager.logout()
                AuthResult.RefreshFailed
            }
            
            is TokenManager.TokenResult.ServerError -> {
                // Error del servidor (5xx) - NO limpiar la sesión
                // El refresh token sigue siendo válido, el problema es del servidor
                Log.w(TAG, "⚠️ Error del servidor, la sesión sigue válida")
                AuthResult.TemporaryError
            }
            
            is TokenManager.TokenResult.NetworkError -> {
                // Error de red - NO limpiar la sesión
                // El refresh token sigue siendo válido, el problema es de conectividad
                Log.w(TAG, "⚠️ Error de red, la sesión sigue válida")
                AuthResult.TemporaryError
            }
            
            is TokenManager.TokenResult.NoCredentials -> {
                // No hay credenciales guardadas
                Log.w(TAG, "No hay credenciales guardadas")
                sessionManager.logout()
                AuthResult.NotLoggedIn
            }
        }
    }
    
    /**
     * Verifica si la sesión parece válida sin hacer llamadas de red.
     * Útil para verificaciones rápidas sin depender del servidor.
     */
    fun isSessionLocallyValid(): Boolean {
        if (!sessionManager.isLoggedIn()) {
            return false
        }
        
        val refreshToken = sessionManager.getRefreshToken() ?: return false
        
        // Solo verificar si el refresh token expiró localmente
        // El access token puede estar expirado, se puede refrescar
        return !tokenManager.isTokenExpired(refreshToken)
    }
    
    /**
     * Limpia la sesión manualmente (por ejemplo, cuando el usuario hace logout)
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Usuario cerró sesión manualmente")
        sessionManager.logout()
    }
}
