package com.cocido.morfipolo.data.remote.interceptor

import android.util.Log
import com.cocido.morfipolo.data.remote.SessionExpiredException
import com.cocido.morfipolo.data.remote.TemporaryServerException
import com.cocido.morfipolo.data.remote.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // No agregar token a endpoints de autenticación
        val url = originalRequest.url.toString()
        if (url.contains("/auth/login") || url.contains("/auth/refresh-token")) {
            return chain.proceed(originalRequest)
        }
        
        // Obtener token válido usando el nuevo método que retorna TokenResult
        val tokenResult = runBlocking {
            tokenManager.getValidAccessTokenResult()
        }
        
        val accessToken = when (tokenResult) {
            is TokenManager.TokenResult.Success -> tokenResult.accessToken
            
            is TokenManager.TokenResult.TokenExpired -> {
                // Refresh token expiró, limpiar sesión y lanzar excepción
                Log.e(TAG, "Refresh token expiró, limpiando sesión")
                runBlocking { tokenManager.clearSession() }
                throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
            }
            
            is TokenManager.TokenResult.ServerError,
            is TokenManager.TokenResult.NetworkError -> {
                // Error temporal - intentar con el access token actual si existe
                Log.w(TAG, "Error temporal obteniendo token, intentando con token actual")
                runBlocking { 
                    // Usar token actual si existe, aunque esté cerca de expirar
                    val currentToken = tokenManager.getValidAccessToken()
                    if (currentToken == null) {
                        // Si no hay token actual, verificar si el refresh token sigue válido localmente
                        if (tokenManager.isRefreshTokenExpiredLocally()) {
                            tokenManager.clearSession()
                            throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                        }
                        // El refresh token sigue válido, dejar pasar sin token
                        // El servidor devolverá 401 y lo manejaremos abajo
                        null
                    } else {
                        currentToken
                    }
                }
            }
            
            is TokenManager.TokenResult.NoCredentials -> {
                Log.w(TAG, "No hay credenciales, enviando request sin token")
                null
            }
        }
        
        if (accessToken == null) {
            Log.w(TAG, "No hay access token disponible, enviando request sin token")
            return chain.proceed(originalRequest)
        }
        
        // Agregar token al header
        var authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        var response = chain.proceed(authenticatedRequest)
        
        // Si recibimos 401, intentar refrescar token y reintentar UNA VEZ
        if (response.code == 401 && !originalRequest.url.toString().contains("/auth/")) {
            Log.w(TAG, "Recibido 401, intentando refrescar token...")
            
            // Forzar refresh del token usando el nuevo método que retorna TokenResult
            val refreshResult = runBlocking {
                tokenManager.forceRefreshAccessTokenResult()
            }
            
            when (refreshResult) {
                is TokenManager.TokenResult.Success -> {
                    val newToken = refreshResult.accessToken
                    if (newToken != accessToken) {
                        // Token fue refrescado exitosamente, cerrar respuesta anterior y reintentar
                        response.close()
                        
                        Log.d(TAG, "✅ Token refrescado exitosamente, reintentando request...")
                        authenticatedRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        
                        response = chain.proceed(authenticatedRequest)
                        
                        // Si el reintento también falla con 401, el refresh token expiró
                        if (response.code == 401) {
                            Log.e(TAG, "Reintento falló con 401 después de refresh. Refresh token expirado")
                            runBlocking { tokenManager.clearSession() }
                            response.close()
                            throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                        } else {
                            Log.d(TAG, "✅ Reintento exitoso con código ${response.code}")
                        }
                    }
                }
                
                is TokenManager.TokenResult.TokenExpired -> {
                    // Refresh token expirado, limpiar sesión
                    Log.e(TAG, "❌ Refresh token expirado, limpiando sesión")
                    runBlocking { tokenManager.clearSession() }
                    response.close()
                    throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                }
                
                is TokenManager.TokenResult.ServerError,
                is TokenManager.TokenResult.NetworkError -> {
                    // Error temporal - NO limpiar la sesión
                    // El refresh token puede seguir siendo válido
                    Log.w(TAG, "⚠️ Error temporal al refrescar token después de 401")
                    
                    // Verificar si el refresh token sigue siendo válido localmente
                    if (tokenManager.isRefreshTokenExpiredLocally()) {
                        Log.e(TAG, "Refresh token expirado localmente, limpiando sesión")
                        runBlocking { tokenManager.clearSession() }
                        response.close()
                        throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    }
                    
                    // El refresh token sigue válido localmente, el problema es temporal del servidor
                    // Lanzar excepción de error temporal (NO sesión expirada)
                    Log.w(TAG, "Error temporal del servidor de refresh, lanzando TemporaryServerException")
                    response.close()
                    throw TemporaryServerException("El servidor no está disponible en este momento. Por favor, intenta de nuevo.")
                }
                
                is TokenManager.TokenResult.NoCredentials -> {
                    Log.e(TAG, "No hay credenciales para refrescar")
                    runBlocking { tokenManager.clearSession() }
                    response.close()
                    throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                }
            }
        }
        
        return response
    }
}
