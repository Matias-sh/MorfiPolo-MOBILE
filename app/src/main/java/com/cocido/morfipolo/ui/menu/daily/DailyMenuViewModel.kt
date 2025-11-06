package com.cocido.morfipolo.ui.menu.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.data.repository.UserRepository
import com.cocido.morfipolo.domain.model.Menu
import com.cocido.morfipolo.domain.model.MenuStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class DailyMenuViewModel(
    private val menuRepository: MenuRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyMenuUiState>(DailyMenuUiState.Loading)
    val uiState: StateFlow<DailyMenuUiState> = _uiState

    private var currentDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    init {
        loadMenuForDate(currentDate.time)
    }

    fun loadMenuForDate(date: Date) {
        currentDate.time = date
        currentDate.set(Calendar.HOUR_OF_DAY, 0)
        currentDate.set(Calendar.MINUTE, 0)
        currentDate.set(Calendar.SECOND, 0)
        currentDate.set(Calendar.MILLISECOND, 0)

        _uiState.value = DailyMenuUiState.Loading

        viewModelScope.launch {
            val userId = userRepository.getCurrentUser()?.id ?: return@launch
            val menu = menuRepository.getMenuByDate(currentDate.time)

            if (menu != null) {
                val hasSelected = menuRepository.hasUserSelectedMenu(userId, menu.id)
                val isWithinTime = isWithinSelectionTime(menu)
                _uiState.value = DailyMenuUiState.Success(
                    menu = menu,
                    hasSelected = hasSelected,
                    isWithinTime = isWithinTime
                )
            } else {
                _uiState.value = DailyMenuUiState.Error("No hay menú disponible para esta fecha")
            }
        }
    }

    fun selectMenu() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUser()?.id ?: return@launch
            val state = _uiState.value
            if (state is DailyMenuUiState.Success) {
            val result = menuRepository.selectMenu(userId, state.menu.id)
            result.getOrNull()?.let {
                loadMenuForDate(currentDate.time)
            } ?: run {
                val exception = result.exceptionOrNull()
                _uiState.value = DailyMenuUiState.Error(exception?.message ?: "Error al seleccionar")
            }
            }
        }
    }

    fun deselectMenu() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUser()?.id ?: return@launch
            val state = _uiState.value
            if (state is DailyMenuUiState.Success) {
            val result = menuRepository.deselectMenu(userId, state.menu.id)
            result.getOrNull()?.let {
                loadMenuForDate(currentDate.time)
            } ?: run {
                val exception = result.exceptionOrNull()
                _uiState.value = DailyMenuUiState.Error(exception?.message ?: "Error al deseleccionar")
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
        if (menu.estado != MenuStatus.ABIERTO) return false

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val startTime = menu.horarioInicio.split(":")
        val endTime = menu.horarioFin.split(":")

        val startHour = startTime[0].toInt()
        val startMin = startTime[1].toInt()
        val endHour = endTime[0].toInt()
        val endMin = endTime[1].toInt()

        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val startTimeInMinutes = startHour * 60 + startMin
        val endTimeInMinutes = endHour * 60 + endMin

        return currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
    }
}

sealed class DailyMenuUiState {
    object Loading : DailyMenuUiState()
    data class Success(
        val menu: Menu,
        val hasSelected: Boolean,
        val isWithinTime: Boolean
    ) : DailyMenuUiState()
    data class Error(val message: String) : DailyMenuUiState()
}

