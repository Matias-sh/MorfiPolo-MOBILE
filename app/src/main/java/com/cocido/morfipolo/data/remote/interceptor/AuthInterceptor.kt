package com.cocido.morfipolo.data.remote.interceptor

import android.util.Log
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
        private const val MAX_RETRY_ATTEMPTS = 1 // Solo reintentar una vez después de 401
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // No agregar token a endpoints de autenticación
        val url = originalRequest.url.toString()
        if (url.contains("/auth/login") || url.contains("/auth/refresh-token")) {
            return chain.proceed(originalRequest)
        }
        
        // Obtener token válido (se refresca automáticamente si es necesario)
        val accessToken = runBlocking {
            tokenManager.getValidAccessToken()
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
        
        // Si recibimos 401, intentar refrescar token y reintentar SOLO UNA VEZ
        if (response.code == 401 && !originalRequest.url.toString().contains("/auth/")) {
            Log.w(TAG, "Recibido 401, intentando refrescar token...")
            
            // Forzar refresh del token (sin cerrar la respuesta aún)
            val newToken = runBlocking {
                tokenManager.forceRefreshAccessToken()
            }
            
            if (newToken != null && newToken != accessToken) {
                // Token fue refrescado exitosamente, cerrar respuesta anterior y reintentar
                response.close()
                
                Log.d(TAG, "Token refrescado exitosamente, reintentando request...")
                authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                
                response = chain.proceed(authenticatedRequest)
                
                // Si el reintento también falla con 401, significa que el refresh token expiró o hay otro problema
                if (response.code == 401) {
                    Log.e(TAG, "Reintento falló con 401 después de refresh. Refresh token probablemente expirado o error del servidor")
                } else {
                    Log.d(TAG, "Reintento exitoso con código ${response.code}")
                }
            } else {
                // No se pudo refrescar el token (refresh token expirado o error de conexión)
                // Devolver la respuesta 401 original sin cerrarla
                Log.e(TAG, "No se pudo refrescar el token después de 401. Refresh token expirado o error de conexión")
                // La respuesta 401 original se devuelve sin cerrar para que el error se propague correctamente
            }
        }
        
        return response
    }
}




