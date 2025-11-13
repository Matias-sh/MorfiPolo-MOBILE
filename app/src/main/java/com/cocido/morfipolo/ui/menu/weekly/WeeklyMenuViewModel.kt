package com.cocido.morfipolo.ui.menu.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun loadWeeklyMenus() {
        _uiState.value = WeeklyMenuUiState.Loading

        viewModelScope.launch {
            try {
                // CRÍTICO: Refrescar sesión antes de cargar menús
                android.util.Log.d("WeeklyMenuViewModel", "Refrescando sesión antes de cargar menús...")
                val authResult = authManager.verifyAndRefreshAuth()
                
                // Intentar cargar menús independientemente del resultado de autenticación
                // El refresh automático debería mantener la sesión activa
                android.util.Log.d("WeeklyMenuViewModel", "Cargando menús...")
                val menus = try {
                    menuRepository.getWeeklyMenus()
                } catch (e: Exception) {
                    android.util.Log.e("WeeklyMenuViewModel", "Error al cargar menús", e)
                    emptyList()
                }
                
                android.util.Log.d("WeeklyMenuViewModel", "Menús cargados: ${menus.size}")
                
                // Obtener votos del usuario para cada menú (solo si hay sesión)
                val userId = sessionManager.getCurrentUserId()
                val menusWithVotes = if (userId != null && authResult is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated) {
                    menus.map { menu ->
                        try {
                            val userVote = voteRepository.getUserVoteForMenu(menu.id, userId)
                            WeeklyMenuItem(menu, userVote)
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
            } catch (e: Exception) {
                android.util.Log.e("WeeklyMenuViewModel", "Error al cargar menús semanales", e)
                _uiState.value = WeeklyMenuUiState.Error(e.message ?: "Error al cargar menús")
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






