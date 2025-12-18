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
    
    private var cachedUserVotes: Map<String, com.cocido.morfipolo.domain.model.Vote> = emptyMap() // Cache de votos del usuario
    private val maxMenusToShow = 10 // Solo mostrar últimos 10 menús (últimas 2 semanas)

    fun loadWeeklyMenus(forceReload: Boolean = false) {
        // Si se fuerza la recarga, limpiar el cache
        if (forceReload) {
            cachedUserVotes = emptyMap()
        }
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
                
                // Cargar solo los últimos 10 menús (últimas 2 semanas)
                android.util.Log.d("WeeklyMenuViewModel", "Cargando últimos $maxMenusToShow menús...")
                val allMenus = try {
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
                
                // Tomar solo los últimos 10 menús (más recientes primero)
                val menus = allMenus.take(maxMenusToShow)
                
                android.util.Log.d("WeeklyMenuViewModel", "Menús totales disponibles: ${allMenus.size}, mostrando últimos: ${menus.size}")
                
                // Obtener votos del usuario para cada menú (solo si hay sesión válida)
                val userIdFromSession = sessionManager.getCurrentUserId()
                
                // CRÍTICO: El endpoint /votes parece devolver todos los votos de un menú, no del usuario
                // Intentar extraer userId del token JWT para verificar
                val accessToken = sessionManager.getAccessToken()
                val userIdFromToken = if (accessToken != null) {
                    try {
                        val parts = accessToken.split(".")
                        if (parts.size == 3) {
                            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                            val json = org.json.JSONObject(payload)
                            // Intentar diferentes campos comunes en JWT
                            json.optString("sub", null)?.takeIf { it.isNotEmpty() }
                                ?: json.optString("id", null)?.takeIf { it.isNotEmpty() }
                                ?: json.optString("userId", null)?.takeIf { it.isNotEmpty() }
                                ?: json.optString("user_id", null)?.takeIf { it.isNotEmpty() }
                                ?: json.optString("user", null)?.takeIf { it.isNotEmpty() }
                        } else null
                    } catch (e: Exception) {
                        android.util.Log.w("WeeklyMenuViewModel", "Error extrayendo userId del token: ${e.message}")
                        null
                    }
                } else null
                
                val userId = userIdFromToken ?: userIdFromSession
                
                android.util.Log.d("WeeklyMenuViewModel", "UserId desde SessionManager: $userIdFromSession")
                android.util.Log.d("WeeklyMenuViewModel", "UserId desde Token JWT: $userIdFromToken")
                android.util.Log.d("WeeklyMenuViewModel", "UserId final a usar: $userId")
                
                val hasValidSession = authResult is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated ||
                    authResult is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError
                val menusWithVotes = if (userId != null && hasValidSession) {
                    try {
                        // OPTIMIZACIÓN: Obtener votos solo para los menús que estamos mostrando (últimos 10)
                        android.util.Log.d("WeeklyMenuViewModel", "Obteniendo votos para ${menus.size} menús (últimos 10)...")
                        android.util.Log.d("WeeklyMenuViewModel", "UserId actual: $userId")
                        
                        // Obtener todos los votos del usuario (el método busca en múltiples páginas si es necesario)
                        val allVotesResult = voteRepository.getAllUserVotes(userId)
                        val allVotes = allVotesResult.getOrNull() ?: emptyList()
                        android.util.Log.d("WeeklyMenuViewModel", "Votos obtenidos del servidor: ${allVotes.size}")
                        
                        // Filtrar votos del usuario actual
                        val userIdNormalized = userId.trim().lowercase()
                        val userVotes = allVotes.filter { vote ->
                            vote.user.id.trim().lowercase() == userIdNormalized
                        }
                        android.util.Log.d("WeeklyMenuViewModel", "Votos del usuario actual: ${userVotes.size}")
                        
                        // Crear mapa de votos por menuId (solo para los menús que estamos mostrando)
                        val menuIds = menus.map { it.id }.toSet()
                        val votesByMenuId = userVotes
                            .filter { it.menu.id in menuIds }
                            .associateBy { it.menu.id }
                        
                        android.util.Log.d("WeeklyMenuViewModel", "Mapa de votos creado con ${votesByMenuId.size} entradas")
                        
                        // Asignar votos a cada menú
                        menus.map { menu ->
                            val userVote = votesByMenuId[menu.id]
                            if (userVote != null) {
                                android.util.Log.d("WeeklyMenuViewModel", "✅ Voto encontrado para menú ${menu.id} (${menu.date}): opción ${userVote.option.name}")
                            } else {
                                android.util.Log.d("WeeklyMenuViewModel", "❌ No se encontró voto para menú ${menu.id} (${menu.date})")
                            }
                            WeeklyMenuItem(menu, userVote)
                        }
                    } catch (e: SessionExpiredException) {
                        // Sesión expirada al obtener votos
                        android.util.Log.w("WeeklyMenuViewModel", "Sesión expirada al obtener votos")
                        _sessionExpired.value = true
                        menus.map { WeeklyMenuItem(it, null) }
                    } catch (e: Exception) {
                        android.util.Log.e("WeeklyMenuViewModel", "Error al obtener votos", e)
                        // En caso de error, mostrar menús sin votos
                        menus.map { WeeklyMenuItem(it, null) }
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






