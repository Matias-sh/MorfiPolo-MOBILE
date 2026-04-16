package com.cocido.morfipolo.ui.menu.weekly

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.ui.theme.FoodTextError
import com.cocido.morfipolo.ui.theme.FoodTextPrimary
import com.cocido.morfipolo.ui.theme.FoodTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun WeeklyMenuRoute(
    onOpenDailyForDate: (String) -> Unit,
    onSessionExpired: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MorfipoloApplication
    val viewModel: WeeklyMenuViewModel = viewModel(
        factory = WeeklyMenuViewModelFactory(
            app.menuRepository,
            app.authManager,
            app.voteRepository,
            app.sessionManager
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionExpired by viewModel.sessionExpired.collectAsStateWithLifecycle()
    var localMessage by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadWeeklyMenus()
    }
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) onSessionExpired()
    }

    WeeklyMenuScreen(
        state = state,
        message = localMessage,
        onRetry = { viewModel.loadWeeklyMenus(forceReload = true) },
        onOpenDaily = onOpenDailyForDate,
        onDeleteVote = { voteId ->
            scope.launch {
                val result = app.voteRepository.deleteVote(voteId)
                if (result.isSuccess) {
                    viewModel.loadWeeklyMenus(forceReload = true)
                    localMessage = null
                } else {
                    localMessage = result.exceptionOrNull()?.message ?: "No se pudo eliminar el voto"
                }
            }
        },
        onSelectOption = { menuId, optionId ->
            scope.launch {
                val userId = app.sessionManager.getCurrentUserId()
                if (userId == null) {
                    localMessage = "No hay usuario logueado"
                    return@launch
                }
                val result = app.voteRepository.createVoteOrReplace(optionId, menuId, userId)
                if (result.isSuccess) {
                    viewModel.loadWeeklyMenus(forceReload = true)
                    localMessage = null
                } else {
                    localMessage = result.exceptionOrNull()?.message ?: "No se pudo registrar el voto"
                }
            }
        }
    )
}

@Composable
private fun WeeklyMenuScreen(
    state: WeeklyMenuUiState,
    message: String?,
    onRetry: () -> Unit,
    onOpenDaily: (String) -> Unit,
    onDeleteVote: (String) -> Unit,
    onSelectOption: (String, String) -> Unit
) {
    when (state) {
        is WeeklyMenuUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
        }
        is WeeklyMenuUiState.Error -> {
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
        is WeeklyMenuUiState.Success -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Vista semanal", color = FoodTextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("Menús de la semana", style = MaterialTheme.typography.displaySmall)
                    message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = FoodTextError)
                    }
                }
                items(state.menus) { item ->
                    val date = runCatching {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.menu.date)
                    }.getOrNull()
                    val label = date?.let {
                        SimpleDateFormat("EEEE d MMM", Locale("es", "AR")).format(it)
                    } ?: item.menu.date

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDaily(item.menu.date) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "AR")) else it.toString() })
                                Text(if (item.userVote != null) "Elegido" else "Sin voto")
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.userVote?.option?.name ?: item.menu.description,
                                color = FoodTextPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            if (item.userVote != null) {
                                Button(
                                    onClick = { onDeleteVote(item.userVote.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Quitar selección") }
                            } else {
                                val firstOption = item.menu.getOptionsOrEmpty().firstOrNull()
                                if (firstOption != null) {
                                    Button(
                                        onClick = { onSelectOption(item.menu.id, firstOption.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Elegir ${firstOption.name}") }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

