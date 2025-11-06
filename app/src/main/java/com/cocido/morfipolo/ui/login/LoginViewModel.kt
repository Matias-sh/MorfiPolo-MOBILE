package com.cocido.morfipolo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(dni: String, password: String) {
        if (dni.isBlank()) {
            _uiState.value = LoginUiState.Error("El DNI no puede estar vacío")
            return
        }

        if (password.isBlank()) {
            _uiState.value = LoginUiState.Error("La contraseña no puede estar vacía")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            val result = userRepository.login(dni, password)
            result.getOrNull()?.let { user ->
                _uiState.value = LoginUiState.Success(user)
            } ?: run {
                val exception = result.exceptionOrNull()
                _uiState.value = LoginUiState.Error(exception?.message ?: "Error al iniciar sesión")
            }
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: com.cocido.morfipolo.domain.model.User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

