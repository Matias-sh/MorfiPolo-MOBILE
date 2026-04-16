package com.cocido.morfipolo.ui.profile

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.ui.login.LoginActivity
import com.cocido.morfipolo.ui.theme.FoodTextError
import com.cocido.morfipolo.ui.theme.FoodTextPrimary
import com.cocido.morfipolo.ui.theme.FoodTextSecondary

@Composable
fun ProfileRoute(
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MorfipoloApplication
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(app.userRepository, app.authManager)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val passwordState by viewModel.passwordChangeState.collectAsStateWithLifecycle()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    ProfileScreen(
        state = state,
        passwordState = passwordState,
        onShowChangePassword = { showPasswordDialog = true },
        onOpenNotifications = onOpenNotifications,
        onLogoutRequest = { showLogoutDialog = true }
    )

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { current, new ->
                viewModel.changePassword(current, new)
                showPasswordDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    onLogout()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ProfileScreen(
    state: ProfileUiState,
    passwordState: PasswordChangeState,
    onShowChangePassword: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogoutRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator()
            }
            is ProfileUiState.Error -> {
                Text(state.message, color = FoodTextError)
            }
            is ProfileUiState.Success -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "${state.user.name} ${state.user.lastName}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = FoodTextPrimary
                        )
                        Text("DNI: ${state.user.dni}", color = FoodTextSecondary)
                    }
                }
                Button(onClick = onShowChangePassword, modifier = Modifier.fillMaxWidth()) {
                    Text("Cambiar contraseña")
                }
                Button(onClick = onOpenNotifications, modifier = Modifier.fillMaxWidth()) {
                    Text("Notificaciones")
                }
                Button(onClick = onLogoutRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar sesión")
                }
            }
        }

        if (passwordState is PasswordChangeState.Error) {
            Text(passwordState.message, color = FoodTextError)
        }
        if (passwordState is PasswordChangeState.Success) {
            Text("Contraseña actualizada correctamente", color = FoodTextPrimary)
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Contraseña actual") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text("Nueva contraseña") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirmar contraseña") },
                    modifier = Modifier.fillMaxWidth()
                )
                localError?.let { Text(it, color = FoodTextError) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val validRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")
                localError = when {
                    current.isBlank() -> "La contraseña actual es obligatoria"
                    !validRegex.matches(new) -> "Debe incluir mayúscula, minúscula, número y mínimo 8 caracteres"
                    new != confirm -> "Las contraseñas no coinciden"
                    else -> null
                }
                if (localError == null) onSave(current, new)
            }) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

