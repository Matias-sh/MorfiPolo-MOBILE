package com.cocido.morfipolo.ui.menu.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.remote.SessionExpiredException
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.domain.model.Menu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private var cacheTimestamp: Long = 0 // Timestamp del caché
    private val cacheValidityMs = 5 * 60 * 1000L // Cache válido por 5 minutos
    private val maxMenusToShow = 10 // Solo mostrar últimos 10 menús (últimas 2 semanas)
    private val maxPagesToSearch = 25 // Máximo de páginas a buscar (suficiente para encontrar todos los votos de los últimos 10 menús)

    fun loadWeeklyMenus(forceReload: Boolean = false) {
        // Si se fuerza la recarga, limpiar el cache
        if (forceReload) {
            cachedUserVotes = emptyMap()
            cacheTimestamp = 0
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
                
                // Cargar solo los últimos 10 menús (ya limitado en el repositorio)
                android.util.Log.d("WeeklyMenuViewModel", "Cargando últimos $maxMenusToShow menús...")
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
                        // OPTIMIZACIÓN: Usar caché si está disponible y es reciente
                        val now = System.currentTimeMillis()
                        val isCacheValid = cachedUserVotes.isNotEmpty() && 
                                         (now - cacheTimestamp) < cacheValidityMs
                        
                        val votesByMenuId = if (isCacheValid && !forceReload) {
                            android.util.Log.d("WeeklyMenuViewModel", "✅ Usando caché de votos (edad: ${(now - cacheTimestamp) / 1000}s)")
                            // Usar caché existente
                            val menuIds = menus.map { it.id }.toSet()
                            cachedUserVotes.filterKeys { it in menuIds }
                        } else {
                            // OPTIMIZACIÓN: Obtener votos del usuario con búsqueda paginada
                            // Buscar en suficientes páginas para encontrar todos los votos de los menús mostrados
                            android.util.Log.d("WeeklyMenuViewModel", "🔄 Obteniendo votos del usuario (máximo $maxPagesToSearch páginas) para ${menus.size} menús...")
                            android.util.Log.d("WeeklyMenuViewModel", "Menús a buscar: ${menus.map { "${it.id} (${it.date})" }.joinToString(", ")}")
                            android.util.Log.d("WeeklyMenuViewModel", "UserId actual: $userId")
                            
                            // Obtener votos del usuario limitando páginas
                            val allVotesResult = voteRepository.getAllUserVotes(userId, maxPagesToSearch = maxPagesToSearch)
                            val allVotes = allVotesResult.getOrNull() ?: emptyList()
                            android.util.Log.d("WeeklyMenuViewModel", "Votos obtenidos del servidor: ${allVotes.size}")
                            
                            // Log de los votos encontrados para debugging
                            allVotes.take(10).forEach { vote ->
                                android.util.Log.d("WeeklyMenuViewModel", "  Voto encontrado: menú ${vote.menu.id} (${vote.menu.date}), opción: ${vote.option.name}")
                            }
                            
                            // Crear mapa de votos por menuId y actualizar caché
                            val menuIds = menus.map { it.id }.toSet()
                            val votesMap = allVotes
                                .filter { it.menu.id in menuIds }
                                .associateBy { it.menu.id }
                            
                            // Verificar si encontramos todos los votos esperados
                            val menusWithoutVotes = menus.filter { it.id !in votesMap.keys }
                            if (menusWithoutVotes.isNotEmpty()) {
                                android.util.Log.w("WeeklyMenuViewModel", "⚠️ No se encontraron votos para ${menusWithoutVotes.size} menús: ${menusWithoutVotes.map { "${it.id} (${it.date})" }.joinToString(", ")}")
                                android.util.Log.w("WeeklyMenuViewModel", "⚠️ Votos totales obtenidos: ${allVotes.size}, buscado en $maxPagesToSearch páginas")
                            }
                            
                            // Actualizar caché
                            cachedUserVotes = allVotes.associateBy { it.menu.id }
                            cacheTimestamp = now
                            
                            android.util.Log.d("WeeklyMenuViewModel", "Mapa de votos creado con ${votesMap.size} entradas para ${menus.size} menús")
                            
                            // Log detallado de qué votos se encontraron
                            menus.forEach { menu ->
                                val vote = votesMap[menu.id]
                                if (vote != null) {
                                    android.util.Log.d("WeeklyMenuViewModel", "✅ Voto encontrado para menú ${menu.id} (${menu.date}): ${vote.option.name}")
                                } else {
                                    android.util.Log.d("WeeklyMenuViewModel", "❌ No se encontró voto para menú ${menu.id} (${menu.date})")
                                }
                            }
                            
                            votesMap
                        }
                        
                        // Asignar votos a cada menú
                        menus.map { menu ->
                            val userVote = votesByMenuId[menu.id]
                            if (userVote != null) {
                                android.util.Log.d(
                                    "WeeklyMenuViewModel",
                                    "✅ Voto encontrado para menú ${menu.id} (${menu.date}): opción ${userVote.option.name}"
                                )
                            } else {
                                android.util.Log.d(
                                    "WeeklyMenuViewModel",
                                    "❌ No se encontró voto para menú ${menu.id} (${menu.date})"
                                )
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
                        // En caso de error, intentar usar caché si está disponible
                        val menuIds = menus.map { it.id }.toSet()
                        val cachedVotes = cachedUserVotes.filterKeys { it in menuIds }
                        if (cachedVotes.isNotEmpty()) {
                            android.util.Log.d("WeeklyMenuViewModel", "⚠️ Usando caché como fallback debido a error")
                            menus.map { menu ->
                                WeeklyMenuItem(menu, cachedVotes[menu.id])
                            }
                        } else {
                            // Si no hay caché, mostrar menús sin votos
                            menus.map { WeeklyMenuItem(it, null) }
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






