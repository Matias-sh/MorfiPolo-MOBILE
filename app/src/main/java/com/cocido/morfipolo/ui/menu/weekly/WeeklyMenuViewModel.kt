package com.cocido.morfipolo.ui.menu.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.remote.SessionExpiredException
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.domain.model.Menu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeeklyMenuViewModel(
    private val menuRepository: MenuRepository,
    private val authManager: com.cocido.morfipolo.data.remote.AuthManager,
    private val voteRepository: com.cocido.morfipolo.data.repository.VoteRepository,
    private val sessionManager: com.cocido.morfipolo.data.local.preferences.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeeklyMenuUiState>(WeeklyMenuUiState.Loading)
    val uiState: StateFlow<WeeklyMenuUiState> = _uiState
    
    // Estado para notificar cuando la sesión expira
    private val _sessionExpired = MutableStateFlow<Boolean>(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired

    fun loadWeeklyMenus() {
        _uiState.value = WeeklyMenuUiState.Loading

        viewModelScope.launch {
            try {
                // CRÍTICO: Refrescar sesión antes de cargar menús
                android.util.Log.d("WeeklyMenuViewModel", "Refrescando sesión antes de cargar menús...")
                val authResult = authManager.verifyAndRefreshAuth()
                
                // Si no hay sesión válida, marcar como expirada
                when (authResult) {
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn,
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed -> {
                        android.util.Log.w("WeeklyMenuViewModel", "Sesión no válida, marcando como expirada")
                        _sessionExpired.value = true
                        _uiState.value = WeeklyMenuUiState.Error("Sesión expirada. Por favor, inicia sesión nuevamente.")
                        return@launch
                    }
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                        // Error temporal - continuar intentando cargar menús
                        android.util.Log.w("WeeklyMenuViewModel", "Error temporal de autenticación, intentando cargar menús...")
                    }
                    is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                        // Todo bien, continuar
                    }
                }
                
                // Intentar cargar menús
                android.util.Log.d("WeeklyMenuViewModel", "Cargando menús...")
                val menus = try {
                    menuRepository.getWeeklyMenus()
                } catch (e: SessionExpiredException) {
                    // Sesión expirada, marcar y propagar
                    android.util.Log.w("WeeklyMenuViewModel", "Sesión expirada al cargar menús")
                    _sessionExpired.value = true
                    _uiState.value = WeeklyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
                    return@launch
                } catch (e: Exception) {
                    android.util.Log.e("WeeklyMenuViewModel", "Error al cargar menús", e)
                    emptyList()
                }
                
                android.util.Log.d("WeeklyMenuViewModel", "Menús cargados: ${menus.size}")
                
                // Obtener votos del usuario para cada menú (solo si hay sesión válida)
                val userId = sessionManager.getCurrentUserId()
                val hasValidSession = authResult is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated ||
                    authResult is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError
                val menusWithVotes = if (userId != null && hasValidSession) {
                    menus.map { menu ->
                        try {
                            val userVote = voteRepository.getUserVoteForMenu(menu.id, userId)
                            WeeklyMenuItem(menu, userVote)
                        } catch (e: SessionExpiredException) {
                            // Sesión expirada al obtener votos
                            android.util.Log.w("WeeklyMenuViewModel", "Sesión expirada al obtener votos")
                            _sessionExpired.value = true
                            WeeklyMenuItem(menu, null)
                        } catch (e: Exception) {
                            android.util.Log.e("WeeklyMenuViewModel", "Error al obtener voto para menú ${menu.id}", e)
                            WeeklyMenuItem(menu, null)
                        }
                    }
                } else {
                    // Si no hay sesión válida, mostrar menús sin votos
                    android.util.Log.d("WeeklyMenuViewModel", "No hay sesión válida, mostrando menús sin votos")
                    menus.map { WeeklyMenuItem(it, null) }
                }
                
                android.util.Log.d("WeeklyMenuViewModel", "Menús con votos procesados: ${menusWithVotes.size}")
                _uiState.value = WeeklyMenuUiState.Success(menusWithVotes)
            } catch (e: SessionExpiredException) {
                // Sesión expirada
                android.util.Log.w("WeeklyMenuViewModel", "Sesión expirada")
                _sessionExpired.value = true
                _uiState.value = WeeklyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
            } catch (e: Exception) {
                android.util.Log.e("WeeklyMenuViewModel", "Error al cargar menús semanales", e)
                _uiState.value = WeeklyMenuUiState.Error("No se pudo cargar el menú. Intenta de nuevo.")
            }
        }
    }
}

data class WeeklyMenuItem(
    val menu: Menu,
    val userVote: com.cocido.morfipolo.domain.model.Vote?
)

sealed class WeeklyMenuUiState {
    object Loading : WeeklyMenuUiState()
    data class Success(val menus: List<WeeklyMenuItem>) : WeeklyMenuUiState()
    data class Error(val message: String) : WeeklyMenuUiState()
}






