package com.cocido.morfipolo.ui.menu.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.remote.SessionExpiredException
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.data.repository.UserRepository
import com.cocido.morfipolo.data.repository.VoteRepository
import com.cocido.morfipolo.domain.model.Menu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyMenuViewModel(
    private val menuRepository: MenuRepository,
    private val userRepository: UserRepository,
    private val voteRepository: VoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyMenuUiState>(DailyMenuUiState.Loading)
    val uiState: StateFlow<DailyMenuUiState> = _uiState
    
    // Estado para notificar cuando la sesión expira
    private val _sessionExpired = MutableStateFlow<Boolean>(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired
    
    // Flag para evitar operaciones duplicadas (doble-click)
    private var isOperationInProgress = false

    private var currentDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // No cargar en init, esperar a que el Fragment lo solicite explícitamente

    fun loadMenuForDate(date: Date) {
        currentDate.time = date
        currentDate.set(Calendar.HOUR_OF_DAY, 0)
        currentDate.set(Calendar.MINUTE, 0)
        currentDate.set(Calendar.SECOND, 0)
        currentDate.set(Calendar.MILLISECOND, 0)

        _uiState.value = DailyMenuUiState.Loading

        viewModelScope.launch {
            try {
                val menu = menuRepository.getMenuByDate(currentDate.time)
                loadMenuState(menu)
            } catch (e: SessionExpiredException) {
                android.util.Log.w("DailyMenuViewModel", "Sesión expirada al cargar menú")
                _sessionExpired.value = true
                _uiState.value = DailyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
            } catch (e: Exception) {
                android.util.Log.e("DailyMenuViewModel", "Error al cargar menú", e)
                _uiState.value = DailyMenuUiState.Error("No se pudo cargar el menú. Intenta de nuevo.")
            }
        }
    }

    /**
     * Carga el menú realmente vigente para votar ahora mismo, que desde que el
     * backend abre la votación ~21:00 del día anterior puede tener fecha de
     * mañana. Es lo que debe usar la pantalla "Hoy" cuando no vino navegada
     * con una fecha explícita (p.ej. desde Semanal).
     */
    fun loadActiveMenu(showLoading: Boolean = true) {
        if (showLoading) {
            _uiState.value = DailyMenuUiState.Loading
        }
        viewModelScope.launch {
            try {
                val menu = menuRepository.getActiveMenu()
                if (menu != null) {
                    val menuDate = try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date)
                    } catch (e: Exception) {
                        null
                    } ?: Date()
                    currentDate.time = menuDate
                    currentDate.set(Calendar.HOUR_OF_DAY, 0)
                    currentDate.set(Calendar.MINUTE, 0)
                    currentDate.set(Calendar.SECOND, 0)
                    currentDate.set(Calendar.MILLISECOND, 0)
                }
                loadMenuState(menu)
            } catch (e: SessionExpiredException) {
                android.util.Log.w("DailyMenuViewModel", "Sesión expirada al cargar menú")
                _sessionExpired.value = true
                _uiState.value = DailyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
            } catch (e: Exception) {
                android.util.Log.e("DailyMenuViewModel", "Error al cargar menú activo", e)
                if (showLoading) {
                    _uiState.value = DailyMenuUiState.Error("No se pudo cargar el menú. Intenta de nuevo.")
                }
                // Si era un refresco silencioso (onResume), preferimos dejar el estado
                // anterior en pantalla antes que reemplazarlo por un error de golpe.
            }
        }
    }

    /**
     * Recarga sin cambiar el estado a Loading primero. Útil para recargar
     * después de operaciones de voto o de un broadcast de actualización,
     * manteniendo la fecha ya resuelta en currentDate (que puede ser mañana).
     */
    private suspend fun loadMenuForDateInternal() {
        try {
            val menu = menuRepository.getMenuByDate(currentDate.time)
            loadMenuState(menu)
        } catch (e: SessionExpiredException) {
            android.util.Log.w("DailyMenuViewModel", "Sesión expirada al cargar menú")
            _sessionExpired.value = true
            _uiState.value = DailyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
        } catch (e: Exception) {
            android.util.Log.e("DailyMenuViewModel", "Error al cargar menú", e)
            _uiState.value = DailyMenuUiState.Error("No se pudo cargar el menú. Intenta de nuevo.")
        }
    }

    /**
     * Construye el UiState a partir de un menú ya resuelto (por fecha
     * explícita o por getActiveMenu()). La elegibilidad para votar depende
     * únicamente de la ventana real start_time/end_time del menú — ya no de
     * si su fecha coincide con "hoy", porque el menú vigente puede tener
     * fecha de mañana durante la ventana nocturna.
     */
    private suspend fun loadMenuState(menu: Menu?) {
        val userId = userRepository.getCurrentUser()?.id
        if (userId == null) {
            android.util.Log.e("DailyMenuViewModel", "No hay usuario logueado")
            _uiState.value = DailyMenuUiState.Error("No hay usuario logueado")
            return
        }

        if (menu == null) {
            android.util.Log.w("DailyMenuViewModel", "No se encontró menú para la fecha")
            _uiState.value = DailyMenuUiState.Error("No hay menú disponible para esta fecha")
            return
        }

        android.util.Log.d("DailyMenuViewModel", "Menú encontrado: ${menu.id}, opciones: ${menu.getOptionsOrEmpty().size}")
        // Si el menú es de hoy o mañana, buscar solo en 3 páginas (más rápido,
        // los votos recientes están al principio). Si es un menú antiguo
        // (navegado desde Semanal), buscar en todas las páginas necesarias.
        val isRecent = isMenuToday(menu) || isMenuTomorrow(menu)
        val maxPages = if (isRecent) 3 else null // null = buscar en todas las páginas
        val userVote = voteRepository.getUserVoteForMenu(menu.id, userId, maxPagesToSearch = maxPages)
        android.util.Log.d("DailyMenuViewModel", "Voto del usuario: ${if (userVote != null) "Sí (${userVote.id}, opción: ${userVote.option.id})" else "No"}")
        val isWithinTime = isWithinSelectionTime(menu)
        val isActuallyOpen = isWithinTime

        val infoMessage = if (!isActuallyOpen && userVote == null) {
            "El horario de selección para este menú ya finalizó."
        } else {
            null
        }

        _uiState.value = DailyMenuUiState.Success(
            menu = menu,
            userVote = userVote,
            isWithinTime = isWithinTime,
            isActuallyOpen = isActuallyOpen,
            infoMessage = infoMessage
        )
    }

    fun selectOption(optionId: String) {
        // Evitar operaciones duplicadas (doble-click)
        if (isOperationInProgress) {
            android.util.Log.d("DailyMenuViewModel", "⚠️ Operación en progreso, ignorando click")
            return
        }
        
        viewModelScope.launch {
            isOperationInProgress = true
            try {
                val state = _uiState.value
                if (state is DailyMenuUiState.Success) {
                    // Validar horario antes de intentar seleccionar
                    if (!state.isActuallyOpen) {
                        val currentState = _uiState.value
                        if (currentState is DailyMenuUiState.Success) {
                            _uiState.value = currentState.copy(
                                infoMessage = "El menú está cerrado. El horario de selección ya finalizó."
                            )
                        }
                        isOperationInProgress = false
                        return@launch
                    }
                    
                    val menu = state.menu
                    val userId = userRepository.getCurrentUser()?.id
                    
                    if (userId == null) {
                        _uiState.value = DailyMenuUiState.Error("No hay usuario logueado")
                        isOperationInProgress = false
                        return@launch
                    }
                    
                    android.util.Log.d("DailyMenuViewModel", "🗳️ Iniciando selección de opción: $optionId")
                    
                    // Usar createVoteOrReplace que elimina el voto existente si hay uno
                    val result = voteRepository.createVoteOrReplace(optionId, menu.id, userId)
                    
                    val exception = result.exceptionOrNull()
                    val errorMessage = exception?.message
                    
                    if (result.isSuccess) {
                        android.util.Log.d("DailyMenuViewModel", "✅ Voto registrado exitosamente")
                        
                        // OPTIMIZACIÓN: Actualizar estado localmente sin recargar todo el menú
                        // Solo recargar el voto del usuario (mucho más rápido)
                        val newVote = result.getOrNull()
                        val currentState = _uiState.value
                        if (currentState is DailyMenuUiState.Success && newVote != null) {
                            // Actualizar el estado con el nuevo voto sin recargar todo
                            val isActuallyOpen = isWithinSelectionTime(currentState.menu)

                            _uiState.value = currentState.copy(
                                userVote = newVote,
                                isActuallyOpen = isActuallyOpen,
                                infoMessage = null
                            )
                            android.util.Log.d("DailyMenuViewModel", "✅ Estado actualizado localmente con nuevo voto")
                        } else {
                            // Si no se pudo obtener el voto, recargar solo el voto (no todo el menú)
                            android.util.Log.d("DailyMenuViewModel", "🔄 Recargando solo el voto del usuario...")
                            refreshUserVoteOnly(menu.id, userId)
                        }
                    } else {
                        android.util.Log.w("DailyMenuViewModel", "⚠️ Error al votar: $errorMessage")
                        
                        // Manejar errores de sesión
                        if (exception is SessionExpiredException ||
                            errorMessage?.contains("sesión", ignoreCase = true) == true ||
                            errorMessage?.contains("session", ignoreCase = true) == true) {
                            _sessionExpired.value = true
                            _uiState.value = DailyMenuUiState.Error(errorMessage ?: "Sesión expirada")
                            isOperationInProgress = false
                            return@launch
                        }
                        
                        // Mostrar el error siempre (antes se perdía en silencio si el
                        // mensaje no calzaba con "cerrado/horario/time/08:00": el usuario
                        // tocaba una opción y no pasaba nada visible).
                        val currentState = _uiState.value
                        if (currentState is DailyMenuUiState.Success) {
                            _uiState.value = currentState.copy(infoMessage = errorMessage)
                        }

                        // createVoteOrReplace puede fallar DESPUÉS de haber borrado el voto
                        // viejo (p.ej. si el create de reintento falla). Resincronizar con el
                        // servidor para que la UI no siga mostrando la selección anterior
                        // como si siguiera vigente.
                        val isHorarioError = errorMessage?.contains("cerrado", ignoreCase = true) == true ||
                            errorMessage?.contains("horario", ignoreCase = true) == true ||
                            errorMessage?.contains("time", ignoreCase = true) == true
                        if (!isHorarioError) {
                            val menuId = (currentState as? DailyMenuUiState.Success)?.menu?.id
                            if (menuId != null) {
                                refreshUserVoteOnly(menuId, userId)
                            }
                        }
                    }
                }
            } catch (e: SessionExpiredException) {
                android.util.Log.w("DailyMenuViewModel", "Sesión expirada al seleccionar opción")
                _sessionExpired.value = true
                _uiState.value = DailyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
            } catch (e: Exception) {
                android.util.Log.e("DailyMenuViewModel", "Error al seleccionar opción", e)
                // Aún así recargar para sincronizar estado
                loadMenuForDateInternal()
            } finally {
                isOperationInProgress = false
            }
        }
    }

    fun deleteVote() {
        // Evitar operaciones duplicadas (doble-click)
        if (isOperationInProgress) {
            android.util.Log.d("DailyMenuViewModel", "⚠️ Operación en progreso, ignorando click")
            return
        }
        
        viewModelScope.launch {
            isOperationInProgress = true
            try {
                val state = _uiState.value
                if (state is DailyMenuUiState.Success) {
                    // Validar horario antes de intentar eliminar
                    if (!state.isActuallyOpen) {
                        val currentState = _uiState.value
                        if (currentState is DailyMenuUiState.Success) {
                            _uiState.value = currentState.copy(
                                infoMessage = "El menú está cerrado. El horario de selección ya finalizó."
                            )
                        }
                        isOperationInProgress = false
                        return@launch
                    }
                    
                    val userVote = state.userVote
                    if (userVote != null) {
                        android.util.Log.d("DailyMenuViewModel", "🗑️ Iniciando eliminación de voto: ${userVote.id}")
                        
                        val result = voteRepository.deleteVote(userVote.id)
                        
                        val exception = result.exceptionOrNull()
                        val errorMessage = exception?.message
                        
                        if (result.isSuccess) {
                            android.util.Log.d("DailyMenuViewModel", "✅ Voto eliminado exitosamente")
                            
                            // OPTIMIZACIÓN: Actualizar estado localmente sin recargar todo el menú
                            val currentState = _uiState.value
                            if (currentState is DailyMenuUiState.Success) {
                                val isActuallyOpen = isWithinSelectionTime(currentState.menu)

                                _uiState.value = currentState.copy(
                                    userVote = null,
                                    isActuallyOpen = isActuallyOpen,
                                    infoMessage = null
                                )
                                android.util.Log.d("DailyMenuViewModel", "✅ Estado actualizado localmente (voto eliminado)")
                            }
                        } else {
                            android.util.Log.w("DailyMenuViewModel", "⚠️ Error al eliminar voto: $errorMessage")
                            
                            // Manejar errores de sesión
                            if (exception is SessionExpiredException ||
                                errorMessage?.contains("sesión", ignoreCase = true) == true ||
                                errorMessage?.contains("session", ignoreCase = true) == true) {
                                _sessionExpired.value = true
                                _uiState.value = DailyMenuUiState.Error(errorMessage ?: "Sesión expirada")
                                isOperationInProgress = false
                                return@launch
                            }
                            
                            // Mostrar el error siempre, no solo cuando el texto matchea
                            // "cerrado/horario/time/08:00" (antes se perdía en silencio).
                            val currentState = _uiState.value
                            if (currentState is DailyMenuUiState.Success) {
                                _uiState.value = currentState.copy(infoMessage = errorMessage)
                            }
                        }
                    } else {
                        android.util.Log.w("DailyMenuViewModel", "⚠️ No hay voto para eliminar")
                        // Recargar de todos modos por si hay desincronización
                        loadMenuForDateInternal()
                    }
                }
            } catch (e: SessionExpiredException) {
                android.util.Log.w("DailyMenuViewModel", "Sesión expirada al eliminar voto")
                _sessionExpired.value = true
                _uiState.value = DailyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
            } catch (e: Exception) {
                android.util.Log.e("DailyMenuViewModel", "Error al eliminar voto", e)
                // Aún así recargar para sincronizar estado
                loadMenuForDateInternal()
            } finally {
                isOperationInProgress = false
            }
        }
    }

    fun navigateToPreviousDay() {
        val newDate = Calendar.getInstance().apply {
            time = currentDate.time
            add(Calendar.DAY_OF_MONTH, -1)
        }
        loadMenuForDate(newDate.time)
    }

    fun navigateToNextDay() {
        val newDate = Calendar.getInstance().apply {
            time = currentDate.time
            add(Calendar.DAY_OF_MONTH, 1)
        }
        loadMenuForDate(newDate.time)
    }
    
    fun getCurrentDate(): Date {
        return currentDate.time
    }

    /**
     * Actualiza solo el voto del usuario sin recargar todo el menú.
     * Esto es mucho más rápido que recargar todo.
     */
    private suspend fun refreshUserVoteOnly(menuId: String, userId: String) {
        try {
            val userVote = voteRepository.getUserVoteForMenu(menuId, userId, maxPagesToSearch = 3)
            val currentState = _uiState.value
            if (currentState is DailyMenuUiState.Success) {
                val isActuallyOpen = isWithinSelectionTime(currentState.menu)

                _uiState.value = currentState.copy(
                    userVote = userVote,
                    isActuallyOpen = isActuallyOpen
                )
                android.util.Log.d("DailyMenuViewModel", "✅ Voto actualizado sin recargar menú completo")
            }
        } catch (e: Exception) {
            android.util.Log.e("DailyMenuViewModel", "Error al actualizar voto", e)
            // Si falla, recargar todo como fallback
            loadMenuForDateInternal()
        }
    }
    
    private fun isWithinSelectionTime(menu: Menu) = com.cocido.morfipolo.util.MenuTimeUtils.isWithinSelectionTime(menu)

    private fun isMenuToday(menu: Menu) = com.cocido.morfipolo.util.MenuTimeUtils.isMenuToday(menu)

    private fun isMenuTomorrow(menu: Menu) = com.cocido.morfipolo.util.MenuTimeUtils.isMenuTomorrow(menu)
}

sealed class DailyMenuUiState {
    object Loading : DailyMenuUiState()
    data class Success(
        val menu: Menu,
        val userVote: com.cocido.morfipolo.domain.model.Vote?,
        val isWithinTime: Boolean,
        val isActuallyOpen: Boolean,
        val infoMessage: String? = null // Mensaje informativo para mostrar en banner
    ) : DailyMenuUiState()
    data class Error(val message: String) : DailyMenuUiState()
}
