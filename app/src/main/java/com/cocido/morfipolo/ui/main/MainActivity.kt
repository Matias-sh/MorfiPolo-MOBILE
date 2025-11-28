package com.cocido.morfipolo.ui.main

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.setupWithNavController
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ActivityMainBinding
import com.cocido.morfipolo.ui.login.LoginActivity
import com.cocido.morfipolo.util.widget.MenuWidgetProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
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
        // Configurar status bar transparente para que se integre con el fondo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        requestNotificationPermissionIfNeeded()

        // Verificar y refrescar autenticación automáticamente
        // El SessionRefreshWorker debería mantener la sesión activa automáticamente
        lifecycleScope.launch {
            val app = application as MorfipoloApplication
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    // Usuario autenticado correctamente, continuar con la app
                    setupNavigation()
                    updateWidget()
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
        // Actualizar widget cuando la app vuelve al foreground
        updateWidget()
    }
    
    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(this, MenuWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                val updateIntent = Intent(this, MenuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            // Error silencioso - no crítico
        }
    }
    
    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        
        // Esperar a que el FragmentContainerView esté completamente inicializado
        binding.root.post {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
            val navController = navHostFragment?.navController ?: return@post

            // Configurar BottomNavigationView con NavController
            navView.setupWithNavController(navController)
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

