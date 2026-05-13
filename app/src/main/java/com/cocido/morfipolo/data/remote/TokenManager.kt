package com.cocido.morfipolo.data.remote

import android.util.Log
import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.remote.api.MorfiPoloApiService
import com.cocido.morfipolo.domain.model.RefreshTokenRequest
import android.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class TokenManager(
    private val sessionManager: SessionManager,
    private val refreshApiServiceProvider: () -> MorfiPoloApiService
) {
    private val mutex = Mutex()
    
    companion object {
        private const val TAG = "TokenManager"
        private const val REFRESH_THRESHOLD_MINUTES = 5 // Refrescar 5 minutos antes de expirar
    }
    
    /**
     * Resultado del intento de obtener/refrescar un token
     */
    sealed class TokenResult {
        data class Success(val accessToken: String) : TokenResult()
        object TokenExpired : TokenResult() // Refresh token expiró (401 o expirado localmente)
        object ServerError : TokenResult() // Error del servidor (5xx) - no limpiar sesión
        object NetworkError : TokenResult() // Error de red - no limpiar sesión
        object NoCredentials : TokenResult() // No hay credenciales guardadas
    }
    
    /**
     * Obtiene el access token, refrescándolo si es necesario.
     * Retorna TokenResult para que el caller pueda distinguir el tipo de error.
     */
    suspend fun getValidAccessTokenResult(): TokenResult {
        mutex.withLock {
            val accessToken = sessionManager.getAccessToken()
            if (accessToken == null) {
                return TokenResult.NoCredentials
            }
            
            if (shouldRefreshToken(accessToken)) {
                return refreshAccessTokenResult()
            }
            
            return TokenResult.Success(accessToken)
        }
    }
    
    /**
     * Obtiene el access token, refrescándolo si es necesario.
     * Retorna null si no se puede obtener (mantener compatibilidad).
     */
    suspend fun getValidAccessToken(): String? {
        return when (val result = getValidAccessTokenResult()) {
            is TokenResult.Success -> result.accessToken
            else -> null
        }
    }
    
    /**
     * Fuerza el refresh del access token (útil cuando se recibe un 401)
     * Retorna TokenResult para distinguir el tipo de error.
     */
    suspend fun forceRefreshAccessTokenResult(): TokenResult {
        mutex.withLock {
            return refreshAccessTokenResult()
        }
    }
    
    /**
     * Fuerza el refresh del access token (útil cuando se recibe un 401)
     */
    suspend fun forceRefreshAccessToken(): String? {
        return when (val result = forceRefreshAccessTokenResult()) {
            is TokenResult.Success -> result.accessToken
            else -> null
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
     * Refresca el access token usando el refresh token.
     * Retorna TokenResult para distinguir el tipo de error.
     */
    private suspend fun refreshAccessTokenResult(): TokenResult {
        return try {
            val refreshToken = sessionManager.getRefreshToken()
            
            if (refreshToken == null) {
                Log.w(TAG, "No hay refresh token disponible")
                return TokenResult.NoCredentials
            }
            
            // Verificar si el refresh token está expirado antes de intentar usarlo
            if (isTokenExpired(refreshToken)) {
                Log.w(TAG, "Refresh token expirado localmente, requiere login")
                return TokenResult.TokenExpired
            }
            
            Log.d(TAG, "Intentando refrescar access token...")
            
            // Usar el servicio temporal para refresh (sin interceptor de auth)
            val refreshApiService = refreshApiServiceProvider()
            val request = RefreshTokenRequest(refreshToken)
            val response = refreshApiService.refreshToken(request)
            
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    sessionManager.saveTokens(
                        loginResponse.accessToken,
                        loginResponse.refreshToken
                    )
                    Log.d(TAG, "✅ Token refrescado exitosamente")
                    return TokenResult.Success(loginResponse.accessToken)
                } else {
                    Log.e(TAG, "Respuesta de refresh exitosa pero body es null")
                    return TokenResult.ServerError
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorCode = response.code()
                Log.e(TAG, "Error refrescando token: $errorCode ${response.message()}, body: $errorBody")
                
                return when {
                    // 401 = Refresh token expirado o inválido
                    errorCode == 401 -> {
                        Log.w(TAG, "Refresh token expirado o inválido (401)")
                        TokenResult.TokenExpired
                    }
                    // 5xx = Error del servidor, no es culpa del token
                    errorCode in 500..599 -> {
                        Log.w(TAG, "Error del servidor ($errorCode), el token puede seguir siendo válido")
                        TokenResult.ServerError
                    }
                    // 4xx (excepto 401) = Otro error del cliente
                    errorCode in 400..499 -> {
                        Log.w(TAG, "Error del cliente ($errorCode)")
                        TokenResult.TokenExpired
                    }
                    // Otros códigos
                    else -> {
                        Log.w(TAG, "Error desconocido ($errorCode)")
                        TokenResult.ServerError
                    }
                }
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Error de red: No se puede resolver el host", e)
            TokenResult.NetworkError
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Error de red: Timeout", e)
            TokenResult.NetworkError
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error de red/IO", e)
            TokenResult.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "Excepción inesperada al refrescar token", e)
            TokenResult.NetworkError
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
    
    /**
     * Verifica si el refresh token guardado está expirado localmente.
     * No hace llamadas de red.
     */
    fun isRefreshTokenExpiredLocally(): Boolean {
        val refreshToken = sessionManager.getRefreshToken() ?: return true
        return isTokenExpired(refreshToken)
    }
    
    /**
     * Limpia la sesión cuando el refresh token expiró
     */
    suspend fun clearSession() {
        sessionManager.logout()
    }
    
    /**
     * Extrae el userId del token JWT.
     * Intenta obtenerlo de los campos comunes: 'sub', 'id', 'userId', 'user_id'
     */
    fun getUserIdFromToken(token: String?): String? {
        if (token == null) return null
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val json = JSONObject(payload)
            
            // Intentar obtener userId de diferentes campos comunes en JWT
            json.optString("sub", null)?.takeIf { it.isNotEmpty() }
                ?: json.optString("id", null)?.takeIf { it.isNotEmpty() }
                ?: json.optString("userId", null)?.takeIf { it.isNotEmpty() }
                ?: json.optString("user_id", null)?.takeIf { it.isNotEmpty() }
                ?: json.optString("user", null)?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo userId del token", e)
            null
        }
    }
    
    /**
     * Obtiene el userId del token actual.
     * Primero intenta desde SessionManager, luego desde el token JWT como fallback.
     */
    fun getCurrentUserId(): String? {
        // Primero intentar desde SessionManager
        val savedUserId = sessionManager.getCurrentUserId()
        if (savedUserId != null) {
            return savedUserId
        }
        
        // Si no hay userId guardado, intentar extraerlo del token
        val accessToken = sessionManager.getAccessToken()
        return getUserIdFromToken(accessToken)
    }

    /**
     * Retorna el access token almacenado sin intentar refresh.
     * Útil como fallback en fallos temporales de red/servidor.
     */
    fun getStoredAccessToken(): String? {
        return sessionManager.getAccessToken()
    }
}
