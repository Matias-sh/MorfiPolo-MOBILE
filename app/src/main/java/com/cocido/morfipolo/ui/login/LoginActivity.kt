package com.cocido.morfipolo.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.appwidget.AppWidgetManager
import com.cocido.morfipolo.MorfipoloApplication
import com.cocido.morfipolo.R
import com.cocido.morfipolo.databinding.ActivityLoginBinding
import com.cocido.morfipolo.ui.main.MainActivity
import com.cocido.morfipolo.util.ValidationUtils
import com.cocido.morfipolo.util.widget.MenuWidgetProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
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
        // Configurar status bar transparente para que se integre con el fondo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        requestNotificationPermissionIfNeeded()

        setupObservers()
        setupListeners()

        // Verificar y refrescar autenticación automáticamente
        lifecycleScope.launch {
            val app = application as MorfipoloApplication
            val authResult = app.authManager.verifyAndRefreshAuth()
            
            when (authResult) {
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.Authenticated -> {
                    // Usuario autenticado correctamente, ir a MainActivity
                    navigateToMain()
                }
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.RefreshFailed,
                is com.cocido.morfipolo.data.remote.AuthManager.AuthResult.NotLoggedIn -> {
                    // Mantener en login, no hacer nada
                }
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.loginButton.isEnabled = true
                        binding.errorTextView.visibility = android.view.View.GONE
                    }
                    is LoginUiState.Loading -> {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        binding.loginButton.isEnabled = false
                    }
                    is LoginUiState.Success -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.loginButton.isEnabled = true
                        // Actualizar widget después del login exitoso
                        updateWidget()
                        // Programar recordatorio diario después del login
                        com.cocido.morfipolo.util.work.DailyReminderWorker.scheduleDailyReminder(this@LoginActivity)
                        navigateToMain()
                    }
                    is LoginUiState.Error -> {
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.loginButton.isEnabled = true
                        // Mostrar mensaje de error amigable sin información técnica
                        val friendlyMessage = when {
                            state.message.contains("conexión", ignoreCase = true) || 
                            state.message.contains("connection", ignoreCase = true) ||
                            state.message.contains("conectar", ignoreCase = true) -> {
                                getString(R.string.error_connection)
                            }
                            state.message.contains("servidor", ignoreCase = true) ||
                            state.message.contains("server", ignoreCase = true) -> {
                                getString(R.string.error_server)
                            }
                            state.message.contains("DNI") || state.message.contains("contraseña") -> {
                                state.message // Mantener mensajes de validación
                            }
                            else -> getString(R.string.error_login_failed)
                        }
                        binding.errorTextView.text = friendlyMessage
                        binding.errorTextView.visibility = android.view.View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val dni = binding.dniEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            
            // Validar formato de DNI
            if (!ValidationUtils.isValidDni(dni)) {
                binding.dniEditText.error = getString(R.string.dni_invalid_format)
                return@setOnClickListener
            }
            
            viewModel.login(dni, password)
        }
        
        // Limpiar error cuando el usuario empieza a escribir
        binding.dniEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.dniEditText.error = null
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(this, MenuWidgetProvider::class.java)
            )
            if (appWidgetIds.isNotEmpty()) {
                android.util.Log.d("LoginActivity", "Actualizando ${appWidgetIds.size} widgets después del login")
                val updateIntent = Intent(this, MenuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(updateIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Error al actualizar widget después del login", e)
        }
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




