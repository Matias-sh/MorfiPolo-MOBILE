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
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveSession(userId: Long, dni: String, name: String) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_DNI, dni)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getCurrentUserId(): Long? {
        return if (isLoggedIn()) {
            prefs.getLong(KEY_USER_ID, -1).takeIf { it != -1L }
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

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}


