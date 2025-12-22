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
            loadMenuForDateInternal(date)
        }
    }
    
    /**
     * Carga interna del menú sin cambiar estado de loading.
     * Útil para recargar después de operaciones de voto.
     */
    private suspend fun loadMenuForDateInternal(date: Date) {
        try {
            val userId = userRepository.getCurrentUser()?.id
            if (userId == null) {
                android.util.Log.e("DailyMenuViewModel", "No hay usuario logueado")
                _uiState.value = DailyMenuUiState.Error("No hay usuario logueado")
                return
            }
            
            android.util.Log.d("DailyMenuViewModel", "Obteniendo menú para fecha: ${currentDate.time}")
            val menu = menuRepository.getMenuByDate(currentDate.time)

            if (menu != null) {
                android.util.Log.d("DailyMenuViewModel", "Menú encontrado: ${menu.id}, opciones: ${menu.getOptionsOrEmpty().size}")
                // Obtener voto del usuario para este menú
                // Si es el menú de hoy, buscar solo en 3 páginas (más rápido)
                // Si es un menú antiguo, buscar en todas las páginas necesarias para encontrarlo
                val isToday = isMenuToday(menu)
                val maxPages = if (isToday) 3 else null // null = buscar en todas las páginas
                val userVote = voteRepository.getUserVoteForMenu(menu.id, userId, maxPagesToSearch = maxPages)
                android.util.Log.d("DailyMenuViewModel", "Voto del usuario: ${if (userVote != null) "Sí (${userVote.id}, opción: ${userVote.option.id})" else "No"}")
                val isWithinTime = isWithinSelectionTime(menu)
                val isActuallyOpen = menu.status == "open" && isWithinTime && isToday
                
                // Determinar mensaje informativo si el menú está cerrado (solo si no tiene voto)
                val infoMessage = if (!isActuallyOpen && isToday && userVote == null) {
                    "El horario de selección ha finalizado. Solo puedes votar entre las 08:00 y las 11:00."
                } else {
                    null
                }
                
                _uiState.value = DailyMenuUiState.Success(
                    menu = menu,
                    userVote = userVote,
                    isWithinTime = isWithinTime && isToday,
                    isActuallyOpen = isActuallyOpen,
                    infoMessage = infoMessage
                )
            } else {
                android.util.Log.w("DailyMenuViewModel", "No se encontró menú para la fecha")
                _uiState.value = DailyMenuUiState.Error("No hay menú disponible para esta fecha")
            }
        } catch (e: SessionExpiredException) {
            android.util.Log.w("DailyMenuViewModel", "Sesión expirada al cargar menú")
            _sessionExpired.value = true
            _uiState.value = DailyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
        } catch (e: Exception) {
            android.util.Log.e("DailyMenuViewModel", "Error al cargar menú", e)
            _uiState.value = DailyMenuUiState.Error("No se pudo cargar el menú. Intenta de nuevo.")
        }
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
                                infoMessage = "El menú está cerrado. No puedes agregar votos fuera del horario de selección (08:00 - 11:00)."
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
                            val isToday = isMenuToday(currentState.menu)
                            val isWithinTime = isWithinSelectionTime(currentState.menu)
                            val isActuallyOpen = currentState.menu.status == "open" && isWithinTime && isToday
                            
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
                        
                        // Mostrar mensaje de error si es por horario
                        if (errorMessage?.contains("cerrado", ignoreCase = true) == true ||
                            errorMessage?.contains("horario", ignoreCase = true) == true ||
                            errorMessage?.contains("time", ignoreCase = true) == true ||
                            errorMessage?.contains("08:00", ignoreCase = true) == true) {
                            val currentState = _uiState.value
                            if (currentState is DailyMenuUiState.Success) {
                                _uiState.value = currentState.copy(infoMessage = errorMessage)
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
                loadMenuForDateInternal(currentDate.time)
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
                                infoMessage = "El menú está cerrado. No puedes quitar votos fuera del horario de selección (08:00 - 11:00)."
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
                                val isToday = isMenuToday(currentState.menu)
                                val isWithinTime = isWithinSelectionTime(currentState.menu)
                                val isActuallyOpen = currentState.menu.status == "open" && isWithinTime && isToday
                                
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
                            
                            // Mostrar mensaje de error si es por horario
                            if (errorMessage?.contains("cerrado", ignoreCase = true) == true ||
                                errorMessage?.contains("horario", ignoreCase = true) == true ||
                                errorMessage?.contains("time", ignoreCase = true) == true ||
                                errorMessage?.contains("08:00", ignoreCase = true) == true) {
                                val currentState = _uiState.value
                                if (currentState is DailyMenuUiState.Success) {
                                    _uiState.value = currentState.copy(infoMessage = errorMessage)
                                }
                            }
                        }
                    } else {
                        android.util.Log.w("DailyMenuViewModel", "⚠️ No hay voto para eliminar")
                        // Recargar de todos modos por si hay desincronización
                        loadMenuForDateInternal(currentDate.time)
                    }
                }
            } catch (e: SessionExpiredException) {
                android.util.Log.w("DailyMenuViewModel", "Sesión expirada al eliminar voto")
                _sessionExpired.value = true
                _uiState.value = DailyMenuUiState.Error(e.message ?: "Sesión expirada. Por favor, inicia sesión nuevamente.")
            } catch (e: Exception) {
                android.util.Log.e("DailyMenuViewModel", "Error al eliminar voto", e)
                // Aún así recargar para sincronizar estado
                loadMenuForDateInternal(currentDate.time)
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
                val isToday = isMenuToday(currentState.menu)
                val isWithinTime = isWithinSelectionTime(currentState.menu)
                val isActuallyOpen = currentState.menu.status == "open" && isWithinTime && isToday
                
                _uiState.value = currentState.copy(
                    userVote = userVote,
                    isActuallyOpen = isActuallyOpen
                )
                android.util.Log.d("DailyMenuViewModel", "✅ Voto actualizado sin recargar menú completo")
            }
        } catch (e: Exception) {
            android.util.Log.e("DailyMenuViewModel", "Error al actualizar voto", e)
            // Si falla, recargar todo como fallback
            loadMenuForDateInternal(currentDate.time)
        }
    }
    
    private fun isWithinSelectionTime(menu: Menu): Boolean {
        if (menu.status != "open") return false

        try {
            // Horario fijo: 08:00 - 11:00
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)

            val startHour = 8
            val startMin = 0
            val endHour = 11
            val endMin = 0

            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val startTimeInMinutes = startHour * 60 + startMin
            val endTimeInMinutes = endHour * 60 + endMin

            return currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun isMenuToday(menu: Menu): Boolean {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val menuDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date)
            
            menuDate?.let {
                val menuCalendar = Calendar.getInstance().apply {
                    time = it
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                menuCalendar.timeInMillis == today.timeInMillis
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
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
