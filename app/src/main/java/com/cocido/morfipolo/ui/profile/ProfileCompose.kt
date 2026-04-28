package com.cocido.morfipolo.ui.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.ui.login.LoginActivity
import com.cocido.morfipolo.ui.theme.*

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

    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    ProfileScreen(
        state = state,
        passwordState = passwordState,
        onShowChangePassword = { showPasswordDialog = true },
        onLogoutRequest = {
            viewModel.logout()
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            onLogout()
        },
        onOpenNotifications = onOpenNotifications
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
}

@Composable
private fun ProfileScreen(
    state: ProfileUiState,
    passwordState: PasswordChangeState,
    onShowChangePassword: () -> Unit,
    onLogoutRequest: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MorfiBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            ProfileHeader(title = "Perfil")
            
            Spacer(Modifier.height(24.dp))

            when (state) {
                is ProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MorfiOrange)
                    }
                }
                is ProfileUiState.Error -> {
                    Text(state.message, color = MorfiRed, style = MorfiTypography.bodyLarge, modifier = Modifier.padding(top = 24.dp))
                }
                is ProfileUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(90.dp),
                            shape = CircleShape,
                            color = MorfiGrayLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_nav_profile),
                                    contentDescription = null,
                                    tint = MorfiGrayDark,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("${state.user.name} ${state.user.lastName}", style = MorfiTypography.headlineMedium, fontSize = 34.sp)
                        Text("DNI: ${state.user.dni}", style = MorfiTypography.bodyMedium, color = MorfiGrayMedium)
                    }

                    Spacer(Modifier.height(30.dp))
                    ProfileSection(title = "Cuenta", icon = R.drawable.ic_nav_profile, iconColor = MorfiIndigo) {
                        ProfileItem(label = "Cambiar contraseña", icon = R.drawable.ic_lock, onClick = onShowChangePassword)
                        HorizontalDivider(color = MorfiGrayLight, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileItem(label = "Configurar notificaciones", icon = R.drawable.ic_bell, onClick = onOpenNotifications)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onLogoutRequest,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MorfiWhite,
                            contentColor = MorfiRed
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_logout), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar sesión", style = MorfiTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            if (passwordState is PasswordChangeState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(passwordState.message, color = MorfiRed, style = MorfiTypography.bodyMedium)
            }
            if (passwordState is PasswordChangeState.Success) {
                Spacer(Modifier.height(8.dp))
                Text("Contraseña actualizada correctamente", color = MorfiGreen, style = MorfiTypography.bodyMedium)
            }
        }
    }
}

@Composable
fun ProfileHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MorfiTypography.headlineMedium.copy(fontSize = 28.sp))
    }
}

@Composable
fun ProfileSection(
    title: String,
    sublabel: String? = null,
    icon: Int,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MorfiTypography.titleMedium)
                if (sublabel != null) {
                    Text(sublabel, style = MorfiTypography.bodyMedium.copy(fontSize = 12.sp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MorfiWhite,
            shadowElevation = 2.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun ProfileItem(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    labelColor: Color = MorfiGrayDark
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, tint = labelColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, style = MorfiTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = labelColor))
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MorfiGrayMedium.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
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
        title = { Text("Cambiar contraseña", style = MorfiTypography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                if (!localError.isNullOrBlank()) {
                    Text(localError.orEmpty(), color = MorfiRed, style = MorfiTypography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    localError = when {
                        current.isBlank() || new.isBlank() || confirm.isBlank() -> "Completá todos los campos"
                        new != confirm -> "Las contraseñas no coinciden"
                        else -> null
                    }
                    if (localError == null) onSave(current, new)
                },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MorfiIndigo, contentColor = MorfiWhite)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
