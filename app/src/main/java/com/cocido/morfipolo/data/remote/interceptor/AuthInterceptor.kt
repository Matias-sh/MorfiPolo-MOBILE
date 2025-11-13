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
            Log.w(TAG, "No hay access token disponible")
            return chain.proceed(originalRequest)
        }
        
        // Agregar token al header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        val response = chain.proceed(authenticatedRequest)
        
        // Si recibimos 401, intentar refrescar token y reintentar
        if (response.code == 401) {
            response.close()
            
            val newToken = runBlocking {
                tokenManager.getValidAccessToken()
            }
            
            if (newToken != null && newToken != accessToken) {
                // Token fue refrescado, reintentar request
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(retryRequest)
            } else {
                // No se pudo refrescar, retornar error 401
                Log.e(TAG, "No se pudo refrescar el token después de 401")
            }
        }
        
        return response
    }
}



