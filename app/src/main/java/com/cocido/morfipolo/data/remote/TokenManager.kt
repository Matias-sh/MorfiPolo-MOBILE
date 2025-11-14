package com.cocido.morfipolo.data.remote

import android.util.Log
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.remote.api.MorfiPoloApiService
import com.cocido.morfipolo.domain.model.LoginResponse
import com.cocido.morfipolo.domain.model.RefreshTokenRequest
import android.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class TokenManager(
    private val sessionManager: SessionManager,
    private val apiServiceProvider: (String, String) -> MorfiPoloApiService
) {
    private val mutex = Mutex()
    
    companion object {
        private const val TAG = "TokenManager"
        private const val REFRESH_THRESHOLD_MINUTES = 5 // Refrescar 5 minutos antes de expirar
    }
    
    /**
     * Obtiene el access token, refrescándolo si es necesario
     */
    suspend fun getValidAccessToken(): String? {
        mutex.withLock {
            val accessToken = sessionManager.getAccessToken()
            if (accessToken == null) {
                return null
            }
            
            if (shouldRefreshToken(accessToken)) {
                return refreshAccessToken()
            }
            
            return accessToken
        }
    }
    
    /**
     * Fuerza el refresh del access token (útil cuando se recibe un 401)
     */
    suspend fun forceRefreshAccessToken(): String? {
        mutex.withLock {
            return refreshAccessToken()
        }
    }
    
    /**
     * Verifica si el token debe ser refrescado
     */
    private fun shouldRefreshToken(token: String): Boolean {
        return try {
            val expirationTime = getTokenExpirationTime(token)
            if (expirationTime == null) return true
            
            val currentTime = System.currentTimeMillis() / 1000
            val threshold = REFRESH_THRESHOLD_MINUTES * 60 // 5 minutos en segundos
            
            (expirationTime - currentTime) < threshold
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando expiración del token", e)
            true
        }
    }
    
    /**
     * Obtiene el tiempo de expiración del token JWT
     */
    private fun getTokenExpirationTime(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val json = JSONObject(payload)
            json.optLong("exp", 0).takeIf { it > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "Error decodificando token", e)
            null
        }
    }
    
    /**
     * Refresca el access token usando el refresh token
     */
    private suspend fun refreshAccessToken(): String? {
        return try {
            val dni = sessionManager.getCurrentUserDni()
            val refreshToken = sessionManager.getRefreshToken()
            
            if (dni == null || refreshToken == null) {
                Log.w(TAG, "No hay DNI o refresh token disponible")
                return null
            }
            
            // Verificar si el refresh token está expirado antes de intentar usarlo
            if (isTokenExpired(refreshToken)) {
                Log.w(TAG, "Refresh token expirado, requiere login")
                return null
            }
            
            // Nota: Según la colección de Postman, el refresh-token requiere dni y password
            // Esto es inusual, pero seguimos el formato del API
            // Si el backend cambia esto, se debe actualizar
            val password = sessionManager.getCurrentUserPassword() // Necesitamos guardar password temporalmente
            if (password == null) {
                Log.w(TAG, "No hay password guardado para refresh token")
                return null
            }
            
            Log.d(TAG, "Intentando refrescar access token...")
            
            // Usar el servicio temporal para refresh (sin interceptor de auth)
            val refreshApiService = apiServiceProvider(dni, password)
            val request = RefreshTokenRequest(dni, password)
            val response = refreshApiService.refreshToken(request)
            
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    sessionManager.saveTokens(
                        loginResponse.accessToken,
                        loginResponse.refreshToken
                    )
                    Log.d(TAG, "Token refrescado exitosamente")
                    return loginResponse.accessToken
                } else {
                    Log.e(TAG, "Respuesta de refresh exitosa pero body es null")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Error refrescando token: ${response.code()} ${response.message()}, body: $errorBody")
                
                // Si el error es 401, el refresh token expiró
                if (response.code() == 401) {
                    Log.w(TAG, "Refresh token expirado o inválido (401)")
                    // No limpiar la sesión aquí, lo hará AuthManager si es necesario
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al refrescar token", e)
            null
        }
    }
    
    /**
     * Verifica si el token está expirado
     */
    fun isTokenExpired(token: String): Boolean {
        val expirationTime = getTokenExpirationTime(token) ?: return true
        val currentTime = System.currentTimeMillis() / 1000
        return currentTime >= expirationTime
    }
}

