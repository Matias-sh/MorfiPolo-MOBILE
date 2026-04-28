package com.cocido.morfipolo.ui.menu.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.cocido.morfipolo.ui.menu.daily.HeaderRow
import com.cocido.morfipolo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeeklyRoute(
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

    LaunchedEffect(Unit) {
        viewModel.loadWeeklyMenus()
    }
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) onSessionExpired()
    }

    WeeklyMenuScreen(
        state = state,
        onRetry = { viewModel.loadWeeklyMenus(forceReload = true) },
        onOpenDaily = onOpenDailyForDate
    )
}

@Composable
private fun WeeklyMenuScreen(
    state: WeeklyMenuUiState,
    onRetry: () -> Unit,
    onOpenDaily: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MorfiBackground)) {
        when (state) {
            is WeeklyMenuUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MorfiOrange)
            }
            is WeeklyMenuUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, style = MorfiTypography.bodyLarge, color = MorfiRed)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
            }
            is WeeklyMenuUiState.Success -> {
                SuccessContent(state, onOpenDaily)
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: WeeklyMenuUiState.Success,
    onOpenDaily: (String) -> Unit
) {
    val arLocale = Locale.Builder().setLanguage("es").setRegion("AR").build()
    val menusSorted = state.menus.sortedByDescending { it.menu.date }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            HeaderRow()
            Spacer(Modifier.height(24.dp))
            Text(
                "CALENDARIO GOURMET",
                style = MorfiTypography.labelMedium.copy(color = Color(0xFFA0522D), letterSpacing = 1.sp)
            )
            Spacer(Modifier.height(8.dp))
        }

        items(menusSorted) { item ->
            val date = runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.menu.date)
            }.getOrNull()
            val dateLabel = date?.let {
                SimpleDateFormat("EEEE, d MMM", arLocale).format(it)
            } ?: item.menu.date
            val dateTitle = date?.let {
                SimpleDateFormat("EEEE", arLocale).format(it)
            } ?: item.menu.date
            val status = when {
                item.userVote != null -> "Ya votaste"
                item.menu.status == "open" -> "Abierto"
                else -> "Cerrado"
            }
            val buttonText = when {
                item.userVote != null -> "Ver detalles"
                item.menu.status == "open" -> "Votar menú"
                else -> "Ver menú"
            }

            WeeklyMenuCardRedesign(
                dateLabel = dateLabel.replaceFirstChar { it.uppercase() },
                title = dateTitle.replaceFirstChar { it.uppercase() },
                status = status,
                description = item.menu.description,
                userVoteOption = item.userVote?.option?.name,
                buttonText = buttonText,
                onOpenMenu = { onOpenDaily(item.menu.date) }
            )
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun WeeklyMenuCardRedesign(
    dateLabel: String,
    title: String,
    status: String,
    description: String,
    userVoteOption: String?,
    buttonText: String,
    onOpenMenu: () -> Unit
) {
    val isVoted = status == "Ya votaste"
    val isOpen = status == "Abierto"
    
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onOpenMenu() },
        shape = RoundedCornerShape(32.dp),
        color = MorfiWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateLabel,
                    style = MorfiTypography.bodyMedium.copy(color = MorfiIndigo, fontWeight = FontWeight.Medium)
                )
                Badge(
                    containerColor = when {
                        isOpen -> MorfiOrange
                        isVoted -> MorfiIndigoLight
                        status == "Cerrado" -> MorfiGrayLight
                        else -> MorfiIndigoLight
                    },
                    contentColor = when {
                        isOpen -> MorfiWhite
                        isVoted -> MorfiIndigo
                        status == "Cerrado" -> MorfiGrayMedium
                        else -> MorfiIndigo
                    }
                ) {
                    Text(
                        status, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MorfiTypography.labelSmall
                    )
                }
            }

            Text(
                text = title,
                style = MorfiTypography.headlineMedium.copy(fontSize = 28.sp),
                modifier = Modifier.padding(top = 4.dp)
            )

            if (isVoted) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(painterResource(R.drawable.ic_check_circle), null, tint = Color(0xFFA0522D), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ya votaste", style = MorfiTypography.labelMedium.copy(color = Color(0xFFA0522D)))
                }
            }

            Spacer(Modifier.height(20.dp))

            if (userVoteOption != null) {
                Text(userVoteOption, style = MorfiTypography.titleMedium.copy(fontSize = 14.sp))
            } else {
                Text(description, style = MorfiTypography.bodyMedium, color = MorfiGrayMedium)
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onOpenMenu,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOpen) MorfiIndigo else MorfiGrayLight,
                    contentColor = if (isOpen) MorfiWhite else MorfiGrayDark
                )
            ) {
                Text(buttonText, style = MorfiTypography.titleMedium)
            }
        }
    }
}
