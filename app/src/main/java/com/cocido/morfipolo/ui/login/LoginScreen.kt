package com.cocido.morfipolo.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cocido.morfipolo.R
import com.cocido.morfipolo.ui.components.MorfiButton
import com.cocido.morfipolo.ui.components.MorfiTextField
import com.cocido.morfipolo.ui.theme.*

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    LoginScreen(
        uiState = uiState,
        onLogin = { dni, password -> viewModel.login(dni, password) }
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLogin: (String, String) -> Unit
) {
    var dni by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val isLoading = uiState is LoginUiState.Loading
    val errorMessage = (uiState as? LoginUiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFDFDFD), Color(0xFFF5F5F5))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // New Logo (Fork/Knife icon)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MorfiWhite, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_restaurant),
                    contentDescription = null,
                    tint = Color(0xFF8B4513), // Brownish color from redesign
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MorfiPolo",
                style = MorfiTypography.headlineLarge.copy(fontSize = 32.sp),
                color = MorfiGrayDark
            )
            Text(
                text = "Gestión de Comedor Premium",
                style = MorfiTypography.bodyMedium,
                color = MorfiGrayMedium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Main Login Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MorfiWhite,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    Text(
                        text = "Bienvenido",
                        style = MorfiTypography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Ingresa tus credenciales para continuar",
                        style = MorfiTypography.bodyMedium,
                        color = MorfiGrayMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    MorfiTextField(
                        value = dni,
                        onValueChange = { dni = it },
                        label = "DNI",
                        placeholder = "Tu número de documento",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_profile),
                                contentDescription = null,
                                tint = MorfiGrayMedium.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        MorfiTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Contraseña",
                            placeholder = "••••••••",
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lock),
                                    contentDescription = null,
                                    tint = MorfiGrayMedium.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        painter = painterResource(if (showPassword) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                        contentDescription = null,
                                        tint = MorfiGrayMedium.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                        TextButton(
                            onClick = { /* Forgot password */ },
                            modifier = Modifier.align(Alignment.TopEnd).offset(y = (-4).dp)
                        ) {
                            Text(
                                text = "Olvidé mi contraseña",
                                style = MorfiTypography.labelMedium.copy(
                                    color = Color(0xFFA0522D), // Sienna color from design
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    MorfiButton(
                        text = if (isLoading) "Ingresando..." else "Ingresar",
                        onClick = { onLogin(dni, password) },
                        enabled = !isLoading && dni.isNotEmpty() && password.isNotEmpty()
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MorfiRed,
                    style = MorfiTypography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Redesign info box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF0F2FA)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF525CB3), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("i", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Para el primer ingreso: Tu contraseña temporal es tu número de DNI. Te pediremos cambiarla inmediatamente por seguridad.",
                        style = MorfiTypography.bodyMedium.copy(
                            color = Color(0xFF3F4680),
                            lineHeight = 20.sp,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "POWERED BY COMEDOR TECH ECOSYSTEM",
                style = MorfiTypography.labelMedium.copy(
                    letterSpacing = 1.sp,
                    color = MorfiGrayMedium.copy(alpha = 0.5f),
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MorfiPoloTheme {
        LoginScreen(uiState = LoginUiState.Idle, onLogin = { _, _ -> })
    }
}
