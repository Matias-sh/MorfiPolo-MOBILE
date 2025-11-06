package com.cocido.morfipolo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
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
            val userId = userRepository.getCurrentUser()?.id ?: return@launch
            val result = userRepository.changePassword(userId, currentPassword, newPassword)
            result.getOrNull()?.let {
                _passwordChangeState.value = PasswordChangeState.Success
            } ?: run {
                val exception = result.exceptionOrNull()
                _passwordChangeState.value = PasswordChangeState.Error(
                    exception?.message ?: "Error al cambiar la contraseña"
                )
            }
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

