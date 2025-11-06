package com.cocido.morfipolo.ui.menu.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocido.morfipolo.data.repository.MenuRepository
import com.cocido.morfipolo.domain.model.Menu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeeklyMenuViewModel(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeeklyMenuUiState>(WeeklyMenuUiState.Loading)
    val uiState: StateFlow<WeeklyMenuUiState> = _uiState

    fun loadWeeklyMenus() {
        _uiState.value = WeeklyMenuUiState.Loading

        viewModelScope.launch {
            try {
                val menus = menuRepository.getWeeklyMenus()
                _uiState.value = WeeklyMenuUiState.Success(menus)
            } catch (e: Exception) {
                _uiState.value = WeeklyMenuUiState.Error(e.message ?: "Error al cargar menús")
            }
        }
    }
}

sealed class WeeklyMenuUiState {
    object Loading : WeeklyMenuUiState()
    data class Success(val menus: List<Menu>) : WeeklyMenuUiState()
    data class Error(val message: String) : WeeklyMenuUiState()
}


