package com.cocido.morfipolo.ui.menu.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.ui.theme.Emerald500
import com.cocido.morfipolo.ui.theme.FoodTextError
import com.cocido.morfipolo.ui.theme.FoodTextPrimary
import com.cocido.morfipolo.ui.theme.FoodTextSecondary
import com.cocido.morfipolo.ui.theme.Teal600
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyMenuRoute(
    onSessionExpired: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MorfipoloApplication
    val viewModel: DailyMenuViewModel = viewModel(
        factory = DailyMenuViewModelFactory(
            app.menuRepository,
            app.userRepository,
            app.voteRepository
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionExpired by viewModel.sessionExpired.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadMenuForDate(Date())
    }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) onSessionExpired()
    }

    DailyMenuScreen(
        state = state,
        onSelectOption = { optionId -> viewModel.selectOption(optionId) },
        onDeleteVote = { viewModel.deleteVote() },
        onRetry = { viewModel.loadMenuForDate(viewModel.getCurrentDate()) }
    )
}

@Composable
private fun DailyMenuScreen(
    state: DailyMenuUiState,
    onSelectOption: (String) -> Unit,
    onDeleteVote: () -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is DailyMenuUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
        }
        is DailyMenuUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message, color = FoodTextError)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Reintentar") }
            }
        }
        is DailyMenuUiState.Success -> {
            val menu = state.menu
            val selectedOptionId = state.userVote?.option?.id
            val date = runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date)
            }.getOrNull() ?: Date()
            val dateText = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "AR"))
                .format(date)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "AR")) else it.toString() }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Menú del día", color = FoodTextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(dateText, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("08:00 - 11:00", color = FoodTextSecondary)
                    state.infoMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = FoodTextError, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                items(menu.getOptionsOrEmpty()) { option ->
                    val isSelected = selectedOptionId == option.id
                    val cardColors = if (isSelected) {
                        CardDefaults.cardColors(containerColor = Color.Transparent)
                    } else {
                        CardDefaults.cardColors(containerColor = Color.White)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = cardColors,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(Emerald500, Teal600)) else Brush.verticalGradient(listOf(Color.White, Color.White))
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (isSelected) Color.White else FoodTextPrimary
                                )
                                if (isSelected) {
                                    Text("✓", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (isSelected) onDeleteVote() else onSelectOption(option.id)
                                },
                                enabled = state.isActuallyOpen || isSelected,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isSelected) "Quitar selección" else "Elegir este plato",
                                    color = if (isSelected) FoodTextError else Color.White
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

