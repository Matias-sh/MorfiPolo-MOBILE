package com.cocido.morfipolo.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.domain.model.CustomNotification
import com.cocido.morfipolo.ui.components.MorfiButton
import com.cocido.morfipolo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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
        onDelete = { viewModel.deleteNotification(it) },
        onToggle = { notification, enabled -> viewModel.toggleNotification(notification, enabled) }
    )

    if (creating || editing != null) {
        NotificationEditDialog(
            current = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = {
                viewModel.saveNotification(it)
                creating = false
                editing = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (CustomNotification) -> Unit,
    onDelete: (CustomNotification) -> Unit,
    onToggle: (CustomNotification, Boolean) -> Unit
) {
    Scaffold(
        containerColor = MorfiBackground,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", style = MorfiTypography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = MorfiOrange, modifier = Modifier.size(24.dp))
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.size(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MorfiBackground)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tus Alertas",
                    style = MorfiTypography.headlineLarge.copy(fontSize = 32.sp)
                )
                Text(
                    text = "Programadas",
                    style = MorfiTypography.headlineLarge.copy(fontSize = 32.sp, color = MorfiOrange)
                )
                Text(
                    text = "Gestiona tus recordatorios de comidas y pedidos automáticos para mantener tu ritmo gourmet.",
                    style = MorfiTypography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(Modifier.height(32.dp))

                when (state) {
                    is NotificationSettingsUiState.Loading -> Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center), color = MorfiOrange) }
                    is NotificationSettingsUiState.Error -> Text(state.message, color = MorfiRed)
                    is NotificationSettingsUiState.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            items(state.notifications) { notification ->
                                NotificationCardRedesign(
                                    notification = notification,
                                    onToggle = { onToggle(notification, it) },
                                    onEdit = { onEdit(notification) },
                                    onDelete = { onDelete(notification) }
                                )
                            }
                            
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(32.dp),
                                    colors = CardDefaults.cardColors(containerColor = MorfiIndigo),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Sugerencias del Chef", style = MorfiTypography.titleMedium.copy(color = MorfiWhite))
                                            Text("Recibe alertas basadas en tus platos favoritos.", style = MorfiTypography.bodyMedium.copy(color = MorfiWhite.copy(alpha = 0.8f), fontSize = 12.sp))
                                        }
                                        Icon(painterResource(R.drawable.ic_restaurant), null, tint = MorfiWhite.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Large FAB matching redesign
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(64.dp),
                shape = CircleShape,
                containerColor = MorfiOrange,
                contentColor = MorfiWhite,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Text("+", style = MorfiTypography.displaySmall.copy(color = MorfiWhite))
            }
        }
    }
}

@Composable
private fun NotificationCardRedesign(
    notification: CustomNotification,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val title = when (notification.hour) {
        9 -> "Desayuno Saludable"
        13 -> "Almuerzo Ejecutivo"
        20 -> "Cena Gourmet"
        else -> "Recordatorio"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MorfiWhite,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.getFormattedTime(),
                    style = MorfiTypography.displayMedium.copy(fontSize = 32.sp)
                )
                Switch(
                    checked = notification.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MorfiWhite,
                        checkedTrackColor = MorfiOrange,
                        uncheckedThumbColor = MorfiWhite,
                        uncheckedTrackColor = MorfiGrayLight
                    )
                )
            }
            
            Text(
                text = notification.getFormattedDays().uppercase(),
                style = MorfiTypography.labelSmall.copy(letterSpacing = 1.sp, color = MorfiGrayMedium)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(title, style = MorfiTypography.bodyMedium.copy(color = MorfiIndigo, fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
                    Text("Editar", style = MorfiTypography.labelLarge, color = MorfiIndigo)
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                    Text("Eliminar", style = MorfiTypography.labelLarge, color = MorfiRed)
                }
            }
        }
    }
}

@Composable
fun NotificationEditDialog(
    current: CustomNotification?,
    onDismiss: () -> Unit,
    onSave: (CustomNotification) -> Unit
) {
    var hour by remember(current) { mutableStateOf((current?.hour ?: 8).toFloat()) }
    var minute by remember(current) { mutableStateOf((current?.minute ?: 30).toFloat()) }
    var selectedDays by remember(current) { mutableStateOf(current?.daysOfWeek ?: setOf(1, 2, 3, 4, 5)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MorfiWhite,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_arrow_back), null, tint = MorfiOrange) }
                    Text("Notificaciones", style = MorfiTypography.titleMedium)
                    Spacer(Modifier.size(40.dp))
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Editar Alerta",
                    style = MorfiTypography.headlineMedium.copy(fontSize = 30.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Personaliza el horario de tus notificaciones gourmet.",
                    style = MorfiTypography.bodyMedium,
                    color = MorfiGrayMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                
                Spacer(Modifier.height(32.dp))
                Text(
                    text = String.format("Hora: %02d:%02d", hour.toInt(), minute.toInt()),
                    style = MorfiTypography.displayMedium.copy(fontSize = 34.sp)
                )
                Spacer(Modifier.height(12.dp))
                Slider(value = hour, onValueChange = { hour = it }, valueRange = 0f..23f, steps = 22)
                Slider(value = minute, onValueChange = { minute = it }, valueRange = 0f..59f, steps = 58)

                Spacer(Modifier.height(20.dp))
                
                Text("REPETIR CADA SEMANA", style = MorfiTypography.labelSmall.copy(letterSpacing = 1.sp))
                
                Spacer(Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val days = listOf(
                        1 to "L",
                        2 to "M",
                        3 to "X",
                        4 to "J",
                        5 to "V",
                        6 to "S",
                        7 to "D"
                    )
                    days.forEach { (day, label) ->
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (isSelected) Color(0xFF8B4513) else MorfiGrayLight, CircleShape)
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MorfiTypography.labelSmall, color = if (isSelected) Color.White else MorfiGrayMedium)
                        }
                    }
                }
                
                Spacer(Modifier.height(40.dp))
                
                MorfiButton(
                    text = "Guardar Cambios",
                    onClick = {
                        onSave(
                            CustomNotification(
                                id = current?.id ?: CustomNotification.generateId(hour.toInt(), minute.toInt()),
                                hour = hour.toInt(),
                                minute = minute.toInt(),
                                isEnabled = current?.isEnabled ?: true,
                                daysOfWeek = if (selectedDays.isEmpty()) setOf(1, 2, 3, 4, 5) else selectedDays
                            )
                        )
                    }
                )
                
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Cancelar", style = MorfiTypography.bodyLarge.copy(color = MorfiGrayMedium))
                }
            }
        }
    }
}
