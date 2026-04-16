package com.cocido.morfipolo.ui.notifications

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.domain.model.CustomNotification
import com.cocido.morfipolo.ui.theme.FoodTextError

@Composable
fun NotificationSettingsRoute(
    onBack: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as MorfipoloApplication
    val viewModel: NotificationSettingsViewModel = viewModel(
        factory = NotificationSettingsViewModelFactory(
            app.notificationConfigRepository,
            app.applicationContext
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CustomNotification?>(null) }
    var creating by remember { mutableStateOf(false) }

    NotificationSettingsScreen(
        state = state,
        onBack = onBack,
        onCreate = { creating = true },
        onEdit = { editing = it },
        onToggle = { notification, enabled ->
            viewModel.toggleNotification(notification, enabled)
        }
    )

    if (creating || editing != null) {
        NotificationEditDialog(
            current = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { notification ->
                viewModel.saveNotification(notification)
                creating = false
                editing = null
            },
            onDelete = { notification ->
                viewModel.deleteNotification(notification)
                creating = false
                editing = null
            }
        )
    }
}

@Composable
private fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (CustomNotification) -> Unit,
    onToggle: (CustomNotification, Boolean) -> Unit
) {
    androidx.compose.material3.Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Volver") }
                Text("Notificaciones", style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.height(12.dp))
            when (state) {
                is NotificationSettingsUiState.Loading -> CircularProgressIndicator()
                is NotificationSettingsUiState.Error -> Text(state.message, color = FoodTextError)
                is NotificationSettingsUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        Text("No hay notificaciones configuradas")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.notifications) { notification ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    onClick = { onEdit(notification) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(notification.getFormattedTime24(), style = MaterialTheme.typography.headlineMedium)
                                            Text(notification.getFormattedDays(), style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Switch(
                                            checked = notification.isEnabled,
                                            onCheckedChange = { onToggle(notification, it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationEditDialog(
    current: CustomNotification?,
    onDismiss: () -> Unit,
    onSave: (CustomNotification) -> Unit,
    onDelete: (CustomNotification) -> Unit
) {
    var hour by remember { mutableStateOf((current?.hour ?: 9).toFloat()) }
    var minute by remember { mutableStateOf((current?.minute ?: 0).toFloat()) }
    var selectedDays by remember {
        mutableStateOf(current?.daysOfWeek ?: setOf(1, 2, 3, 4, 5))
    }
    val dayValues = listOf(1, 2, 3, 4, 5, 6, 7)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "Nueva notificación" else "Editar notificación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hora: ${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}")
                Text("Hora")
                Slider(
                    value = hour,
                    onValueChange = { hour = it },
                    valueRange = 8f..11f,
                    steps = 2
                )
                Text("Minutos")
                Slider(
                    value = minute,
                    onValueChange = { minute = it },
                    valueRange = 0f..59f,
                    steps = 58
                )
                Text("Días")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayValues.forEach { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(CustomNotification.getDayName(day)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selectedDays.isEmpty()) return@TextButton
                val notification = CustomNotification(
                    id = current?.id ?: CustomNotification.generateId(hour.toInt(), minute.toInt()),
                    hour = hour.toInt(),
                    minute = minute.toInt(),
                    isEnabled = current?.isEnabled ?: true,
                    daysOfWeek = selectedDays
                )
                onSave(notification)
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (current != null) {
                    TextButton(onClick = { onDelete(current) }) { Text("Eliminar") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

