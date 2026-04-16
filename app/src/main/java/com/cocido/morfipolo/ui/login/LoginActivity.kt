package com.cocido.morfipolo.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.ui.main.MainActivity
import com.cocido.morfipolo.ui.theme.MorfiPoloTheme
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory((application as MorfipoloApplication).userRepository)
    }
    
    // Launcher para solicitar permiso de notificaciones
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("LoginActivity", "✅ Permiso de notificaciones concedido")
        } else {
            android.util.Log.w("LoginActivity", "⚠️ Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar status bar con iconos oscuros (compatible con Android 15+)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = true
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        requestNotificationPermissionIfNeeded()
        
        setContent {
            MorfiPoloTheme {
                LoginRoute(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        com.cocido.morfipolo.util.work.DailyReminderWorker.scheduleDailyReminder(this)
                        navigateToMain()
                    }
                )
            }
        }

        // Verificar y refrescar autenticación automáticamente
        lifecycleScope.launch {
            val app = application as MorfipoloApplication
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    // Usuario autenticado correctamente, ir a MainActivity
                    navigateToMain()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                    // Error temporal (servidor/red), pero la sesión sigue válida localmente
                    // Verificar si la sesión es válida localmente e ir a MainActivity
                    if (app.authManager.isSessionLocallyValid()) {
                        android.util.Log.d("LoginActivity", "Error temporal pero sesión válida localmente, continuando...")
                        navigateToMain()
                    } else {
                        // La sesión expiró localmente, mantener en login
                        android.util.Log.w("LoginActivity", "Error temporal y sesión expirada localmente")
                    }
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed,
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                    // Mantener en login, no hacer nada
                }
            }
        }
    }

    // Animaciones de entrada y manejo fino de errores ahora se resuelven dentro de la UI Compose.

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("LoginActivity", "✅ Permiso de notificaciones ya concedido")
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // El usuario denegó el permiso anteriormente, explicar por qué lo necesitamos
                    android.util.Log.d("LoginActivity", "Solicitando permiso de notificaciones (ya denegado antes)")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Primera vez que se solicita
                    android.util.Log.d("LoginActivity", "Solicitando permiso de notificaciones por primera vez")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

class LoginViewModelFactory(private val userRepository: com.cocido.morfipolo.data.repository.UserRepository) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}




