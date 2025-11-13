package com.cocido.morfipolo.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "morfipolo_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_DNI = "user_dni"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PASSWORD = "user_password" // Temporal para refresh token
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveSession(userId: String, dni: String, name: String, password: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_DNI, dni)
            putString(KEY_USER_NAME, name)
            password?.let { putString(KEY_USER_PASSWORD, it) }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    fun getCurrentUserId(): String? {
        return if (isLoggedIn()) {
            prefs.getString(KEY_USER_ID, null)
        } else {
            null
        }
    }

    fun getCurrentUserDni(): String? {
        return prefs.getString(KEY_USER_DNI, null)
    }

    fun getCurrentUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }
    
    fun getCurrentUserPassword(): String? {
        return prefs.getString(KEY_USER_PASSWORD, null)
    }
    
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken() != null
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}




