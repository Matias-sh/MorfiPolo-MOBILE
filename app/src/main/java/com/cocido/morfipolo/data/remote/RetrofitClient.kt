package com.cocido.morfipolo.data.remote

import com.cocido.morfipolo.data.local.preferences.SessionManager
import com.cocido.morfipolo.data.remote.api.MorfiPoloApiService
import com.cocido.morfipolo.data.remote.interceptor.AuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://secytdi.formosa.gob.ar/morfi-polo/api/"
    
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    // Configurar Moshi para que no falle con campos faltantes
    // KotlinJsonAdapterFactory ya maneja campos nullable automáticamente
    
    private fun createOkHttpClient(sessionManager: SessionManager, tokenManager: TokenManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // En release, ProGuard eliminará estos logs automáticamente
            // En producción, los logs están desactivados (Level.NONE)
            // ProGuard ya está configurado para eliminar logs de debug/info/verbose
            level = HttpLoggingInterceptor.Level.NONE
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    fun createApiService(sessionManager: SessionManager, tokenManager: TokenManager? = null): MorfiPoloApiService {
        val okHttpClient = if (tokenManager != null) {
            createOkHttpClient(sessionManager, tokenManager)
        } else {
            createOkHttpClientWithoutAuth()
        }
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        
        return retrofit.create(MorfiPoloApiService::class.java)
    }
    
    fun createApiServiceForRefresh(sessionManager: SessionManager): MorfiPoloApiService {
        return createApiService(sessionManager, null)
    }
    
    private fun createOkHttpClientWithoutAuth(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // En release, ProGuard eliminará estos logs automáticamente
            // En producción, los logs están desactivados (Level.NONE)
            // ProGuard ya está configurado para eliminar logs de debug/info/verbose
            level = HttpLoggingInterceptor.Level.NONE
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

