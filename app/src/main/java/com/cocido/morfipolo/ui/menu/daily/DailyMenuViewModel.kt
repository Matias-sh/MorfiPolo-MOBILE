package com.cocido.morfipolo.ui.menu.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                val userId = userRepository.getCurrentUser()?.id
                if (userId == null) {
                    android.util.Log.e("DailyMenuViewModel", "No hay usuario logueado")
                    _uiState.value = DailyMenuUiState.Error("No hay usuario logueado")
                    return@launch
                }
                
                android.util.Log.d("DailyMenuViewModel", "Obteniendo menú para fecha: ${currentDate.time}")
                val menu = menuRepository.getMenuByDate(currentDate.time)

                if (menu != null) {
                       android.util.Log.d("DailyMenuViewModel", "Menú encontrado: ${menu.id}, opciones: ${menu.getOptionsOrEmpty().size}")
                    // Obtener voto del usuario para este menú
                    val userVote = voteRepository.getUserVoteForMenu(menu.id, userId)
                    android.util.Log.d("DailyMenuViewModel", "Voto del usuario: ${if (userVote != null) "Sí (${userVote.id}, opción: ${userVote.option.id})" else "No"}")
                    val isToday = isMenuToday(menu)
                    val isWithinTime = isWithinSelectionTime(menu)
                    val isActuallyOpen = menu.status == "open" && isWithinTime && isToday
                    
                    // Determinar mensaje informativo si el menú está cerrado
                    val infoMessage = if (!isActuallyOpen && isToday) {
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
            } catch (e: Exception) {
                android.util.Log.e("DailyMenuViewModel", "Error al cargar menú", e)
                _uiState.value = DailyMenuUiState.Error("Error al cargar el menú: ${e.message}")
            }
        }
    }

    fun selectOption(optionId: String) {
        viewModelScope.launch {
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
                        return@launch
                    }
                    
                    val menu = state.menu
                    val userId = userRepository.getCurrentUser()?.id
                    
                    if (userId == null) {
                        _uiState.value = DailyMenuUiState.Error("No hay usuario logueado")
                        return@launch
                    }
                    
                    // Usar createVoteOrReplace que elimina el voto existente si hay uno
                    val result = voteRepository.createVoteOrReplace(optionId, menu.id, userId)
                    result.getOrNull()?.let {
                        // Recargar menú para obtener el nuevo voto
                        loadMenuForDate(currentDate.time)
                    } ?: run {
                        val exception = result.exceptionOrNull()
                        val errorMessage = exception?.message ?: "Error al seleccionar opción"
                        // Si el error es por horario cerrado, mostrar como mensaje informativo
                        if (errorMessage.contains("cerrado", ignoreCase = true) ||
                            errorMessage.contains("horario", ignoreCase = true) ||
                            errorMessage.contains("time", ignoreCase = true)) {
                            val currentState = _uiState.value
                            if (currentState is DailyMenuUiState.Success) {
                                _uiState.value = currentState.copy(infoMessage = errorMessage)
                            }
                        } else {
                            _uiState.value = DailyMenuUiState.Error(errorMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DailyMenuUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun deleteVote() {
        viewModelScope.launch {
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
                    return@launch
                }
                
                val userVote = state.userVote
                if (userVote != null) {
                    val result = voteRepository.deleteVote(userVote.id)
                    result.getOrNull()?.let {
                        // Recargar menú
                        loadMenuForDate(currentDate.time)
                    } ?: run {
                        val exception = result.exceptionOrNull()
                        val errorMessage = exception?.message ?: "Error al eliminar voto"
                        // Si el error es por horario cerrado, mostrar como mensaje informativo
                        if (errorMessage.contains("cerrado", ignoreCase = true) ||
                            errorMessage.contains("horario", ignoreCase = true) ||
                            errorMessage.contains("time", ignoreCase = true)) {
                            val currentState = _uiState.value
                            if (currentState is DailyMenuUiState.Success) {
                                _uiState.value = currentState.copy(infoMessage = errorMessage)
                            }
                        } else {
                            _uiState.value = DailyMenuUiState.Error(errorMessage)
                        }
                    }
                }
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
