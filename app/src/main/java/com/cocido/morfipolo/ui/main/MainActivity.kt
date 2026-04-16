package com.cocido.morfipolo.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.ui.login.LoginActivity
import com.cocido.morfipolo.ui.theme.MorfiPoloTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // Launcher para solicitar permiso de notificaciones
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Permiso de notificaciones concedido")
        } else {
            android.util.Log.w("MainActivity", "⚠️ Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar status bar transparente con iconos oscuros (compatible con Android 15+)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = true
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        requestNotificationPermissionIfNeeded()

        // Verificar y refrescar autenticación automáticamente
        // El SessionRefreshWorker debería mantener la sesión activa automáticamente
        lifecycleScope.launch {
            val app = application as MorfipoloApplication
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    setComposeContent()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.TemporaryError -> {
                    // Error temporal (servidor/red), pero la sesión puede seguir válida localmente
                    if (app.authManager.isSessionLocallyValid()) {
                        android.util.Log.w("MainActivity", "Error temporal pero sesión válida localmente, continuando...")
                        setComposeContent()
                    } else {
                        android.util.Log.w("MainActivity", "Error temporal y sesión expirada localmente, redirigiendo al login")
                        navigateToLogin()
                    }
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed -> {
                    // Si el refresh falló, la sesión expiró - redirigir al login
                    android.util.Log.w("MainActivity", "Sesión expirada (RefreshFailed), redirigiendo al login")
                    navigateToLogin()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                    // No hay sesión guardada, redirigir al login
                    android.util.Log.d("MainActivity", "No hay sesión guardada, redirigiendo al login")
                    navigateToLogin()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Emitir broadcast para que las pantallas sincronizadas refresquen sus datos
        notifyMenuUpdated()
    }
    
    /**
     * Notifica a los fragments que deben refrescar el menú/votos.
     * Soluciona el bug de sincronización web-app.
     */
    private fun notifyMenuUpdated() {
        try {
            val updateIntent = Intent("com.cocido.morfipolo.MENU_UPDATED").apply {
                setPackage(packageName)
            }
            sendBroadcast(updateIntent)
            android.util.Log.d("MainActivity", "📱 Broadcast MENU_UPDATED enviado para sincronizar votos")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error al enviar broadcast de actualización", e)
        }
    }
    
    private fun setComposeContent() {
        setContent {
            MorfiPoloTheme {
                MorfiPoloApp(
                    onNavigateToLogin = { navigateToLogin() }
                )
            }
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
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
                    android.util.Log.d("MainActivity", "✅ Permiso de notificaciones ya concedido")
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // El usuario denegó el permiso anteriormente, explicar por qué lo necesitamos
                    android.util.Log.d("MainActivity", "Solicitando permiso de notificaciones (ya denegado antes)")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Primera vez que se solicita
                    android.util.Log.d("MainActivity", "Solicitando permiso de notificaciones por primera vez")
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

}

