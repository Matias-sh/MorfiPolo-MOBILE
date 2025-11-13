package com.cocido.morfipolo.ui.main

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar y refrescar autenticación automáticamente
        // El SessionRefreshWorker debería mantener la sesión activa automáticamente
        lifecycleScope.launch {
            val app = application as MorfipoloApplication
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    // Usuario autenticado correctamente, continuar con la app
                    android.util.Log.d("MainActivity", "Sesión válida")
                    setupNavigation()
                    updateWidget()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed -> {
                    // Si el refresh falló, intentar continuar de todas formas
                    // El SessionRefreshWorker intentará refrescar en segundo plano
                    android.util.Log.w("MainActivity", "Refresh falló, pero continuando (el worker refrescará automáticamente)")
                    setupNavigation()
                    updateWidget()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                    // Solo redirigir a login si realmente no hay sesión guardada
                    android.util.Log.d("MainActivity", "No hay sesión guardada, redirigiendo a login")
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
                android.util.Log.d("MainActivity", "Actualizando ${appWidgetIds.size} widgets")
                val updateIntent = Intent(this, MenuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error al actualizar widget", e)
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

}

