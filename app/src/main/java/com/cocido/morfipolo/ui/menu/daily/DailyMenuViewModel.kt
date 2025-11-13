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
                    val isWithinTime = isWithinSelectionTime(menu)
                    
                    _uiState.value = DailyMenuUiState.Success(
                        menu = menu,
                        userVote = userVote,
                        isWithinTime = isWithinTime
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
                        android.util.Log.e("DailyMenuViewModel", "Error al seleccionar opción", exception)
                        _uiState.value = DailyMenuUiState.Error(exception?.message ?: "Error al seleccionar opción")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DailyMenuViewModel", "Excepción al seleccionar opción", e)
                _uiState.value = DailyMenuUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun deleteVote() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state is DailyMenuUiState.Success) {
                val userVote = state.userVote
                if (userVote != null) {
                    val result = voteRepository.deleteVote(userVote.id)
                    result.getOrNull()?.let {
                        // Recargar menú
                        loadMenuForDate(currentDate.time)
                    } ?: run {
                        val exception = result.exceptionOrNull()
                        _uiState.value = DailyMenuUiState.Error(exception?.message ?: "Error al eliminar voto")
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

    private fun isWithinSelectionTime(menu: Menu): Boolean {
        if (menu.status != "open") return false

        try {
            // Parsear start_time y end_time (formato ISO 8601)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            val startTime = isoFormat.parse(menu.start_time)
            val endTime = isoFormat.parse(menu.end_time)
            val now = Date()

            if (startTime != null && endTime != null) {
                return now.after(startTime) && now.before(endTime)
            }
        } catch (e: Exception) {
            // Si falla el parsing, intentar formato simple
            try {
                val simpleFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val now = Calendar.getInstance()
                val currentHour = now.get(Calendar.HOUR_OF_DAY)
                val currentMinute = now.get(Calendar.MINUTE)

                // Extraer hora y minuto de start_time y end_time
                val startTimeParts = menu.start_time.split("T")[1].split(":")
                val endTimeParts = menu.end_time.split("T")[1].split(":")

                val startHour = startTimeParts[0].toInt()
                val startMin = startTimeParts[1].toInt()
                val endHour = endTimeParts[0].toInt()
                val endMin = endTimeParts[1].toInt()

                val currentTimeInMinutes = currentHour * 60 + currentMinute
                val startTimeInMinutes = startHour * 60 + startMin
                val endTimeInMinutes = endHour * 60 + endMin

                return currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
            } catch (e2: Exception) {
                return false
            }
        }

        return false
    }
}

sealed class DailyMenuUiState {
    object Loading : DailyMenuUiState()
    data class Success(
        val menu: Menu,
        val userVote: com.cocido.morfipolo.domain.model.Vote?,
        val isWithinTime: Boolean
    ) : DailyMenuUiState()
    data class Error(val message: String) : DailyMenuUiState()
}
