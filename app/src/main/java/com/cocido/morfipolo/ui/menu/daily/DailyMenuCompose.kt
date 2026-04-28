package com.cocido.morfipolo.ui.menu.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.ui.components.MorfiButton
import com.cocido.morfipolo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
    Box(modifier = Modifier.fillMaxSize().background(MorfiBackground)) {
        when (state) {
            is DailyMenuUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MorfiOrange)
            }
            is DailyMenuUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, style = MorfiTypography.bodyLarge, color = MorfiRed)
                    Spacer(Modifier.height(16.dp))
                    MorfiButton(text = "Reintentar", onClick = onRetry)
                }
            }
            is DailyMenuUiState.Success -> {
                SuccessContent(state, onSelectOption, onDeleteVote)
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: DailyMenuUiState.Success,
    onSelectOption: (String) -> Unit,
    onDeleteVote: () -> Unit
) {
    val menu = state.menu
    val options = menu.getOptionsOrEmpty()
    val selectedOptionId = state.userVote?.option?.id
    var pendingSelection by remember(menu.id, selectedOptionId) { mutableStateOf(selectedOptionId) }
    LaunchedEffect(menu.id, selectedOptionId, options.size) {
        if (selectedOptionId == null && pendingSelection == null && options.size == 1) {
            pendingSelection = options.first().id
        }
    }

    val date = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menu.date)
    }.getOrNull() ?: Date()
    val arLocale = Locale.Builder().setLanguage("es").setRegion("AR").build()
    val dateText = SimpleDateFormat("'Hoy, 'd 'de' MMMM", arLocale).format(date)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            HeaderRow()
            Spacer(Modifier.height(16.dp))
            Text(
                "MENÚ DEL DÍA",
                style = MorfiTypography.labelMedium.copy(color = MorfiIndigo, letterSpacing = 1.sp)
            )
            Text(
                dateText,
                style = MorfiTypography.headlineLarge.copy(fontSize = 32.sp)
            )
            Spacer(Modifier.height(8.dp))
            
            VotingWindowCard(isOpen = state.isActuallyOpen)
            if (!state.infoMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.infoMessage,
                    style = MorfiTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = MorfiRed
                )
            }
        }

        if (selectedOptionId != null) {
            item { Spacer(Modifier.height(2.dp)) }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Opciones del Chef", style = MorfiTypography.titleLarge)
                Badge(
                    containerColor = MorfiGrayLight,
                    contentColor = MorfiGrayMedium
                ) {
                    Text(
                        "${options.size} DISPONIBLES",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MorfiTypography.labelSmall
                    )
                }
            }
        }

        items(options) { option ->
            val isSelected = pendingSelection == option.id
            val isActuallyVoted = selectedOptionId == option.id
            
            MenuOptionCardRedesign(
                optionName = option.name,
                optionDesc = menu.description.takeIf { it.isNotBlank() } ?: "Selección del día",
                isSelected = isSelected,
                isActuallyVoted = isActuallyVoted,
                onClick = { pendingSelection = option.id }
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { pendingSelection?.let(onSelectOption) },
                enabled = pendingSelection != null && pendingSelection != selectedOptionId && state.isActuallyOpen,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MorfiOrange,
                    disabledContainerColor = MorfiOrange.copy(alpha = 0.35f),
                    contentColor = MorfiWhite,
                    disabledContentColor = MorfiWhite.copy(alpha = 0.85f)
                )
            ) {
                Text(
                    when {
                        selectedOptionId != null && pendingSelection == selectedOptionId -> "Voto confirmado"
                        pendingSelection == null -> "Elegí una opción"
                        else -> "Confirmar voto"
                    },
                    style = MorfiTypography.titleMedium.copy(fontSize = 17.sp)
                )
            }

            if (selectedOptionId != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onDeleteVote,
                    enabled = state.isActuallyOpen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quitar selección",
                        style = MorfiTypography.labelLarge.copy(fontSize = 14.sp),
                        color = if (state.isActuallyOpen) MorfiGrayDark else MorfiGrayMedium
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun HeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("MorfiPolo", style = MorfiTypography.titleLarge.copy(color = MorfiGrayDark, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun VotingWindowCard(isOpen: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MorfiIndigo,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Ventana de votación", color = Color.White.copy(alpha = 0.7f), style = MorfiTypography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(painterResource(R.drawable.ic_clock), null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("08:00 — 11:00", color = Color.White, style = MorfiTypography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(100.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(if (isOpen) R.drawable.ic_check_circle else R.drawable.ic_clock),
                        contentDescription = null,
                        tint = MorfiOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isOpen) "ABIERTO — PODÉS VOTAR TU MENÚ" else "CERRADO — FUERA DE HORARIO",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MorfiTypography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun MenuOptionCardRedesign(
    optionName: String,
    optionDesc: String,
    isSelected: Boolean,
    isActuallyVoted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = MorfiWhite,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MorfiGreen.copy(alpha = 0.5f)) else null,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    optionName,
                    style = MorfiTypography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Text("SELECCIONADO", style = MorfiTypography.labelSmall.copy(color = MorfiGreen))
                }
            }
            Text(optionDesc, style = MorfiTypography.bodyMedium.copy(fontSize = 13.sp), modifier = Modifier.padding(top = 4.dp))
            if (isActuallyVoted) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_check_circle), null, tint = MorfiGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Voto actual", style = MorfiTypography.labelSmall.copy(color = MorfiGreen))
                }
            }
        }
    }
}
