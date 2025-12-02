package com.cocido.morfipolo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authManager: com.cocido.morfipolo.data.remote.AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _passwordChangeState = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState

    fun loadUser() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user != null) {
                _uiState.value = ProfileUiState.Success(user)
            } else {
                _uiState.value = ProfileUiState.Error("No se pudo cargar el usuario")
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                // CRÍTICO: Refrescar sesión antes de cambiar contraseña para asegurar token válido
                android.util.Log.d("ProfileViewModel", "Refrescando sesión antes de cambiar contraseña...")
                val authResult = authManager.verifyAndRefreshAuth()
                
                when (authResult) {
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                        android.util.Log.d("ProfileViewModel", "Sesión válida, cambiando contraseña...")
                        performPasswordChange(currentPassword, newPassword)
                    }
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                        // Error temporal - intentar cambiar contraseña de todos modos
                        android.util.Log.w("ProfileViewModel", "Error temporal de autenticación, intentando cambiar contraseña...")
                        performPasswordChange(currentPassword, newPassword)
                    }
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed,
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                        android.util.Log.w("ProfileViewModel", "Sesión inválida, no se puede cambiar contraseña")
                        _passwordChangeState.value = PasswordChangeState.Error("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error inesperado al cambiar contraseña", e)
                _passwordChangeState.value = PasswordChangeState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    private suspend fun performPasswordChange(currentPassword: String, newPassword: String) {
        val userId = userRepository.getCurrentUser()?.id
        if (userId == null) {
            _passwordChangeState.value = PasswordChangeState.Error("No se pudo obtener el ID del usuario")
            return
        }
        
        val result = userRepository.changePassword(userId, currentPassword, newPassword)
        result.getOrNull()?.let {
            android.util.Log.d("ProfileViewModel", "✅ Contraseña cambiada exitosamente")
            _passwordChangeState.value = PasswordChangeState.Success
        } ?: run {
            val exception = result.exceptionOrNull()
            android.util.Log.e("ProfileViewModel", "Error al cambiar contraseña", exception)
            _passwordChangeState.value = PasswordChangeState.Error(
                exception?.message ?: "Error al cambiar la contraseña"
            )
        }
    }

    fun resetPasswordChangeState() {
        _passwordChangeState.value = PasswordChangeState.Idle
    }

    fun logout() {
        userRepository.logout()
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: com.cocido.morfipolo.domain.model.User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class PasswordChangeState {
    object Idle : PasswordChangeState()
    object Success : PasswordChangeState()
    data class Error(val message: String) : PasswordChangeState()
}

